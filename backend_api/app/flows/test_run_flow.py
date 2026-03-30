"""TestRunFlow — orchestrates the lifecycle of a single test run.

Flow name: TestRunFlow
Single entrypoint: TestRunOrchestrator (methods: start_run, cancel_run, get_run, list_runs)
Callers: runs route handler (backend_api.app.api.routes.runs)

Contract:
- Inputs: StartRunRequest (scenario_tag, browser, trigger_source)
- Output: StartRunResult / run dicts / bool
- Errors: RunConflictError when concurrency limit hit; RuntimeError for persistence failures.
- Side effects: creates/updates persistence records; spawns Maven subprocess.

Observability:
- Logs start/end of each run, cancellation, and errors with run_id context.
- Stores Maven output as an artifact attached to the run.

Failure modes:
1. Persistence layer unreachable → RuntimeError bubbles to route handler → 500.
2. Maven process fails → run marked as "failed" with log artifact.
3. Cancellation requested → subprocess terminated, run marked "cancelled".
"""
from __future__ import annotations

import asyncio
import logging
import os
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional

from backend_api.app.adapters.subprocess_runner import run_command
from backend_api.app.core.persistence import Persistence, get_persistence
from backend_api.app.core.settings import Settings

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class StartRunRequest:
    """Request object for starting a run (flow boundary input)."""

    scenario_tag: str
    browser: str
    trigger_source: str = "api"


@dataclass(frozen=True)
class StartRunResult:
    """Result object for starting a run (flow boundary output)."""

    run: Dict[str, Any]


class RunConflictError(RuntimeError):
    """Raised when max concurrency is exceeded."""


class TestRunOrchestrator:
    """Coordinates run lifecycle and cancellation.

    Design note:
    This orchestrator is in-memory (single-process) and suitable for the template deployment model.
    All persistence calls go through the Persistence abstraction which automatically
    falls back to file/memory when Postgres is unavailable.
    """

    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self._semaphore = asyncio.Semaphore(settings.runner_max_concurrency)
        self._active: dict[int, asyncio.Event] = {}  # run_id -> cancel_event

    def _get_persistence(self) -> Persistence:
        """Return the active persistence backend (cached at module level)."""
        return get_persistence()

    def _runner_env(self) -> Dict[str, str]:
        env = dict(os.environ)
        # If Sauce Labs credentials exist in the environment, pass them through.
        # (No additional transformation here; runner reads SAUCE_USERNAME/SAUCE_ACCESS_KEY.)
        return env

    async def _execute_run(self, run_id: int, cancel_event: asyncio.Event, req: StartRunRequest) -> None:
        """Execute the Maven runner subprocess and update persistence on completion."""
        store = self._get_persistence()
        start_ts = datetime.now(timezone.utc)
        store.mark_run_running(run_id)

        mvn = self._settings.runner_mvn_cmd
        cwd = self._settings.runner_workdir

        # Tag filtering is implemented via cucumber.filter.tags. When scenario_tag == "all",
        # run everything.
        cucumber_tags = "" if req.scenario_tag in ("", "all") else req.scenario_tag

        cmd = [
            mvn,
            "-q",
            "test",
            f"-Dcucumber.filter.tags={cucumber_tags}",
            f"-Dqa.browser={req.browser}",
        ]

        rc, stdout, stderr = await run_command(cmd=cmd, cwd=cwd, env=self._runner_env(), cancel_event=cancel_event)
        end_ts = datetime.now(timezone.utc)
        duration_ms = int((end_ts - start_ts).total_seconds() * 1000)

        combined = (stdout or "") + ("\n\n--- STDERR ---\n" + stderr if stderr else "")
        store.attach_run_log(run_id=run_id, name="maven-output.log", content_text=combined[:1_000_000])

        if cancel_event.is_set():
            store.mark_run_cancelled(run_id)
            return

        status = "passed" if rc == 0 else "failed"
        # We don't parse per-scenario counts in this minimal iteration; store totals=0.
        store.mark_run_finished(
            run_id=run_id,
            status=status,
            finished_at=end_ts,
            duration_ms=duration_ms,
            totals={"total_scenarios": 0, "passed_scenarios": 0, "failed_scenarios": 0},
        )

    # PUBLIC_INTERFACE
    async def start_run(self, req: StartRunRequest) -> StartRunResult:
        """StartRunFlow: create a run record, then execute Maven runner asynchronously.

        Contract:
        - Inputs: StartRunRequest (scenario_tag, browser, trigger_source).
        - Output: StartRunResult containing the created run record.
        - Errors:
          - RunConflictError when max concurrency reached.
          - Persistence errors bubble up.
        - Side effects:
          - Inserts/updates persistence records.
          - Spawns a subprocess running Maven.
          - Writes a log artifact row.
        """
        if self._semaphore.locked() and self._settings.runner_max_concurrency == 1:
            raise RunConflictError("A run is already in progress. Please wait.")

        store = self._get_persistence()

        await self._semaphore.acquire()
        try:
            run = store.create_run(
                trigger_source=req.trigger_source,
                scenario_tag=req.scenario_tag,
                browser=req.browser,
                metadata={"requested_at": datetime.now(timezone.utc).isoformat()},
            )
            run_id = int(run["id"])
            cancel_event = asyncio.Event()
            self._active[run_id] = cancel_event

            logger.info(
                "testrun.start",
                extra={"run_id": run_id, "scenario_tag": req.scenario_tag, "browser": req.browser},
            )

            # Fire-and-forget task; completion updates persistence.
            async def _task() -> None:
                try:
                    await self._execute_run(run_id, cancel_event, req)
                except Exception:
                    logger.exception("testrun.error", extra={"run_id": run_id})
                    # Ensure status is error if unexpected exception occurs.
                    try:
                        err_store = self._get_persistence()
                        err_store.mark_run_finished(
                            run_id=run_id,
                            status="error",
                            finished_at=datetime.now(timezone.utc),
                            duration_ms=None,
                            totals={"total_scenarios": 0, "passed_scenarios": 0, "failed_scenarios": 0},
                        )
                    except Exception:
                        logger.exception("testrun.error_marking_failed", extra={"run_id": run_id})
                finally:
                    self._active.pop(run_id, None)
                    self._semaphore.release()
                    logger.info("testrun.end", extra={"run_id": run_id})

            asyncio.create_task(_task())

            return StartRunResult(run=run)
        except Exception:
            # If we failed before starting the background task, release semaphore.
            self._semaphore.release()
            raise

    # PUBLIC_INTERFACE
    def cancel_run(self, run_id: int) -> bool:
        """CancelRunFlow: request cancellation for a running run.

        Contract:
        - Inputs: run_id (int)
        - Output: bool indicating whether a running process was found and cancellation was signaled.
        - Side effects: sets cancellation event; subprocess adapter terminates process.
        """
        cancel_event = self._active.get(run_id)
        if cancel_event is None:
            return False
        cancel_event.set()
        return True

    # PUBLIC_INTERFACE
    def get_run(self, run_id: int) -> Optional[Dict[str, Any]]:
        """GetRunFlow: return run record from persistence."""
        store = self._get_persistence()
        return store.get_run(run_id)

    # PUBLIC_INTERFACE
    def list_runs(self, limit: int) -> List[Dict[str, Any]]:
        """ListRunsFlow: list recent runs from persistence."""
        store = self._get_persistence()
        return store.list_runs(limit=limit)
