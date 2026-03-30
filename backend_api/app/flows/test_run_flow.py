from __future__ import annotations

import asyncio
import logging
import os
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any, Dict, Optional

from backend_api.app.adapters.subprocess_runner import run_command
from backend_api.app.core.settings import Settings
from backend_api.app.repositories.artifacts_repo import attach_run_log
from backend_api.app.repositories.runs_repo import (
    create_run,
    get_run,
    list_runs,
    mark_run_cancelled,
    mark_run_finished,
    mark_run_running,
)

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
    In production you'd externalize scheduling/locks and process state.
    """

    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self._semaphore = asyncio.Semaphore(settings.runner_max_concurrency)
        self._active: dict[int, asyncio.Event] = {}  # run_id -> cancel_event

    def _runner_env(self) -> Dict[str, str]:
        env = dict(os.environ)
        # If Sauce Labs credentials exist in the environment, pass them through.
        # (No additional transformation here; runner reads SAUCE_USERNAME/SAUCE_ACCESS_KEY.)
        return env

    async def _execute_run(self, run_id: int, cancel_event: asyncio.Event, req: StartRunRequest) -> None:
        start_ts = datetime.now(timezone.utc)
        mark_run_running(run_id)

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
        attach_run_log(run_id=run_id, name="maven-output.log", content_text=combined[:1_000_000])

        if cancel_event.is_set():
            mark_run_cancelled(run_id)
            return

        status = "passed" if rc == 0 else "failed"
        # We don't parse per-scenario counts in this minimal iteration; store totals=0.
        mark_run_finished(
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
          - DB errors bubble up.
        - Side effects:
          - Inserts/updates DB rows.
          - Spawns a subprocess running Maven.
          - Writes a log artifact row.
        """
        if self._semaphore.locked() and self._settings.runner_max_concurrency == 1:
            raise RunConflictError("A run is already in progress. Please wait.")

        await self._semaphore.acquire()
        try:
            run = create_run(
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

            # Fire-and-forget task; completion updates DB.
            async def _task() -> None:
                try:
                    await self._execute_run(run_id, cancel_event, req)
                except Exception as e:
                    logger.exception("testrun.error", extra={"run_id": run_id})
                    # Ensure status is error if unexpected exception occurs.
                    mark_run_finished(
                        run_id=run_id,
                        status="error",
                        finished_at=datetime.now(timezone.utc),
                        duration_ms=None,
                        totals={"total_scenarios": 0, "passed_scenarios": 0, "failed_scenarios": 0},
                    )
                    attach_run_log(run_id=run_id, name="orchestrator-error.log", content_text=str(e))
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
        """GetRunFlow: return run record."""
        return get_run(run_id)

    # PUBLIC_INTERFACE
    def list_runs(self, limit: int) -> list[Dict[str, Any]]:
        """ListRunsFlow: list recent runs."""
        return list_runs(limit=limit)
