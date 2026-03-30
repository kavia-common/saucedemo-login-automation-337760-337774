from __future__ import annotations

import json
import logging
import os
import threading
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List, Optional
from uuid import UUID, uuid4

logger = logging.getLogger(__name__)


def _utcnow() -> datetime:
    return datetime.now(timezone.utc)


def _dt_to_iso(dt: Optional[datetime]) -> Optional[str]:
    if dt is None:
        return None
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=timezone.utc)
    return dt.isoformat()


def _iso_to_dt(s: Optional[str]) -> Optional[datetime]:
    if not s:
        return None
    # datetime.fromisoformat supports offsets; we normalize to aware datetime.
    dt = datetime.fromisoformat(s)
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=timezone.utc)
    return dt


@dataclass(frozen=True)
class PersistenceStatus:
    """Status describing which persistence backend is active."""
    backend: str
    details: str


class Persistence:
    """Persistence adapter for runs and artifacts.

    This is the single canonical entrypoint for storing/querying runs and artifacts.

    Contract:
    - Inputs: typed parameters from orchestrator/flows.
    - Output: dict rows compatible with the existing REST API responses.
    - Errors: raises RuntimeError with context; does not swallow underlying exceptions.
    - Side effects:
      - DB backend: writes to Postgres.
      - File backend: writes to JSON file on local disk.
    """

    def status(self) -> PersistenceStatus:  # pragma: no cover - interface only
        raise NotImplementedError

    def create_run(
        self,
        *,
        trigger_source: str,
        scenario_tag: str,
        browser: str,
        metadata: Dict[str, Any],
    ) -> Dict[str, Any]:  # pragma: no cover - interface only
        raise NotImplementedError

    def mark_run_running(self, run_id: int) -> None:  # pragma: no cover - interface only
        raise NotImplementedError

    def mark_run_finished(
        self,
        *,
        run_id: int,
        status: str,
        finished_at: datetime,
        duration_ms: Optional[int],
        totals: Dict[str, int],
    ) -> None:  # pragma: no cover - interface only
        raise NotImplementedError

    def mark_run_cancelled(self, run_id: int) -> None:  # pragma: no cover - interface only
        raise NotImplementedError

    def get_run(self, run_id: int) -> Optional[Dict[str, Any]]:  # pragma: no cover - interface only
        raise NotImplementedError

    def list_runs(self, limit: int = 25) -> List[Dict[str, Any]]:  # pragma: no cover - interface only
        raise NotImplementedError

    def attach_run_log(
        self, *, run_id: int, name: str, content_text: str, content_type: str = "text/plain"
    ) -> None:  # pragma: no cover - interface only
        raise NotImplementedError

    def get_run_artifacts(self, run_id: int) -> List[Dict[str, Any]]:  # pragma: no cover - interface only
        raise NotImplementedError


class DbPersistence(Persistence):
    """Postgres-backed persistence."""

    def __init__(self) -> None:
        from backend_api.app.repositories import artifacts_repo, runs_repo

        self._runs_repo = runs_repo
        self._artifacts_repo = artifacts_repo

    def status(self) -> PersistenceStatus:
        return PersistenceStatus(backend="postgres", details="Using psycopg/Postgres repositories.")

    def create_run(
        self,
        *,
        trigger_source: str,
        scenario_tag: str,
        browser: str,
        metadata: Dict[str, Any],
    ) -> Dict[str, Any]:
        return self._runs_repo.create_run(
            trigger_source=trigger_source,
            scenario_tag=scenario_tag,
            browser=browser,
            metadata=metadata,
        )

    def mark_run_running(self, run_id: int) -> None:
        self._runs_repo.mark_run_running(run_id)

    def mark_run_finished(
        self,
        *,
        run_id: int,
        status: str,
        finished_at: datetime,
        duration_ms: Optional[int],
        totals: Dict[str, int],
    ) -> None:
        self._runs_repo.mark_run_finished(
            run_id=run_id,
            status=status,
            finished_at=finished_at,
            duration_ms=duration_ms,
            totals=totals,
        )

    def mark_run_cancelled(self, run_id: int) -> None:
        self._runs_repo.mark_run_cancelled(run_id)

    def get_run(self, run_id: int) -> Optional[Dict[str, Any]]:
        return self._runs_repo.get_run(run_id)

    def list_runs(self, limit: int = 25) -> List[Dict[str, Any]]:
        return self._runs_repo.list_runs(limit=limit)

    def attach_run_log(
        self, *, run_id: int, name: str, content_text: str, content_type: str = "text/plain"
    ) -> None:
        self._artifacts_repo.attach_run_log(
            run_id=run_id,
            name=name,
            content_text=content_text,
            content_type=content_type,
        )

    def get_run_artifacts(self, run_id: int) -> List[Dict[str, Any]]:
        return list(self._artifacts_repo.get_run_artifacts(run_id))


class FilePersistence(Persistence):
    """File-backed persistence adapter.

    Data model:
    - JSON file stores a dict with keys: {"runs": [...], "artifacts": [...], "next_run_id": int, "next_artifact_id": int}
    - Each run/artifact is stored as a JSON-friendly dict.
    """

    def __init__(self, *, path: str) -> None:
        self._path = Path(path)
        self._lock = threading.RLock()
        self._state = self._load_or_init()

    def status(self) -> PersistenceStatus:
        return PersistenceStatus(backend="file", details=f"Using JSON store at {self._path}")

    def _load_or_init(self) -> dict:
        self._path.parent.mkdir(parents=True, exist_ok=True)
        if self._path.exists():
            try:
                data = json.loads(self._path.read_text(encoding="utf-8"))
                if isinstance(data, dict) and "runs" in data and "artifacts" in data:
                    return data
            except Exception:
                logger.exception("Failed to load persistence file; re-initializing: %s", self._path)

        data = {"runs": [], "artifacts": [], "next_run_id": 1, "next_artifact_id": 1}
        self._atomic_write(data)
        return data

    def _atomic_write(self, data: dict) -> None:
        tmp = self._path.with_suffix(self._path.suffix + ".tmp")
        tmp.write_text(json.dumps(data, indent=2, sort_keys=True), encoding="utf-8")
        os.replace(tmp, self._path)

    def _save(self) -> None:
        self._atomic_write(self._state)

    def _row_now_defaults(self) -> dict:
        now = _utcnow()
        return {
            "created_at": _dt_to_iso(now),
            "updated_at": _dt_to_iso(now),
        }

    def create_run(
        self,
        *,
        trigger_source: str,
        scenario_tag: str,
        browser: str,
        metadata: Dict[str, Any],
    ) -> Dict[str, Any]:
        with self._lock:
            run_id = int(self._state["next_run_id"])
            self._state["next_run_id"] = run_id + 1

            run_uuid: UUID = uuid4()
            meta = dict(metadata or {})
            meta.update({"scenario_tag": scenario_tag, "browser": browser})

            row = {
                "id": run_id,
                "run_uuid": str(run_uuid),
                "trigger_source": trigger_source,
                "status": "queued",
                "started_at": None,
                "finished_at": None,
                "duration_ms": None,
                "total_scenarios": 0,
                "passed_scenarios": 0,
                "failed_scenarios": 0,
                "metadata": meta,
                **self._row_now_defaults(),
            }
            self._state["runs"].append(row)
            self._save()
            return dict(row)

    def _find_run_idx(self, run_id: int) -> Optional[int]:
        for i, r in enumerate(self._state["runs"]):
            if int(r.get("id")) == int(run_id):
                return i
        return None

    def mark_run_running(self, run_id: int) -> None:
        with self._lock:
            idx = self._find_run_idx(run_id)
            if idx is None:
                raise RuntimeError(f"Run not found: {run_id}")
            r = self._state["runs"][idx]
            r["status"] = "running"
            r["started_at"] = _dt_to_iso(_utcnow())
            r["updated_at"] = _dt_to_iso(_utcnow())
            self._save()

    def mark_run_finished(
        self,
        *,
        run_id: int,
        status: str,
        finished_at: datetime,
        duration_ms: Optional[int],
        totals: Dict[str, int],
    ) -> None:
        with self._lock:
            idx = self._find_run_idx(run_id)
            if idx is None:
                raise RuntimeError(f"Run not found: {run_id}")
            r = self._state["runs"][idx]
            r["status"] = status
            r["finished_at"] = _dt_to_iso(finished_at)
            r["duration_ms"] = duration_ms
            r["total_scenarios"] = int(totals.get("total_scenarios", 0))
            r["passed_scenarios"] = int(totals.get("passed_scenarios", 0))
            r["failed_scenarios"] = int(totals.get("failed_scenarios", 0))
            r["updated_at"] = _dt_to_iso(_utcnow())
            self._save()

    def mark_run_cancelled(self, run_id: int) -> None:
        with self._lock:
            idx = self._find_run_idx(run_id)
            if idx is None:
                # Best-effort; same semantics as DB approach.
                return
            r = self._state["runs"][idx]
            r["status"] = "cancelled"
            r["finished_at"] = _dt_to_iso(_utcnow())
            r["updated_at"] = _dt_to_iso(_utcnow())
            self._save()

    def get_run(self, run_id: int) -> Optional[Dict[str, Any]]:
        with self._lock:
            idx = self._find_run_idx(run_id)
            if idx is None:
                return None
            return dict(self._state["runs"][idx])

    def list_runs(self, limit: int = 25) -> List[Dict[str, Any]]:
        with self._lock:
            # created_at is ISO; lexical order works for UTC isoformat; use reverse append order as primary.
            runs = list(self._state["runs"])
            runs.sort(key=lambda r: r.get("created_at") or "", reverse=True)
            return [dict(r) for r in runs[:limit]]

    def attach_run_log(
        self, *, run_id: int, name: str, content_text: str, content_type: str = "text/plain"
    ) -> None:
        with self._lock:
            # Validate run exists to match DB semantics (foreign key would fail).
            if self._find_run_idx(run_id) is None:
                raise RuntimeError(f"Run not found for artifact attachment: {run_id}")

            artifact_id = int(self._state["next_artifact_id"])
            self._state["next_artifact_id"] = artifact_id + 1
            row = {
                "id": artifact_id,
                "run_id": int(run_id),
                "artifact_type": "log",
                "name": name,
                "content_type": content_type,
                "content_text": content_text,
                **self._row_now_defaults(),
            }
            self._state["artifacts"].append(row)
            self._save()

    def get_run_artifacts(self, run_id: int) -> List[Dict[str, Any]]:
        with self._lock:
            items = [a for a in self._state["artifacts"] if int(a.get("run_id")) == int(run_id)]
            items.sort(key=lambda a: a.get("created_at") or "")
            return [dict(a) for a in items]


class InMemoryPersistence(FilePersistence):
    """Pure in-memory persistence (no disk writes).

    This uses the same logic as FilePersistence but disables disk writes,
    so behavior is identical for callers.
    """

    def __init__(self) -> None:
        # Path is unused but required by base init; we bypass by setting our own state.
        self._path = Path("/dev/null")
        self._lock = threading.RLock()
        self._state = {"runs": [], "artifacts": [], "next_run_id": 1, "next_artifact_id": 1}

    def status(self) -> PersistenceStatus:
        return PersistenceStatus(backend="memory", details="Using in-memory store (non-persistent).")

    def _save(self) -> None:
        # Intentionally no-op.
        return

    def _atomic_write(self, data: dict) -> None:
        # Intentionally no-op.
        return


# PUBLIC_INTERFACE
def get_persistence() -> Persistence:
    """Return the active persistence backend.

    Selection flow:
    - If BACKEND_PERSISTENCE=file: always file persistence.
    - If BACKEND_PERSISTENCE=memory: always in-memory.
    - Else (auto):
      - If DB is configured: use Postgres (DbPersistence)
      - Otherwise: use FilePersistence with BACKEND_PERSISTENCE_PATH (default: ./data/persistence.json)

    Contract:
    - Inputs: env vars read via Settings.
    - Output: a Persistence implementation.
    - Errors: never fails solely due to missing DB config; DB backend may still fail at runtime if DB is unreachable.
    """
    from backend_api.app.core.settings import get_settings

    settings = get_settings()
    mode = (getattr(settings, "persistence_mode", None) or "auto").strip().lower()
    path = (getattr(settings, "persistence_path", None) or "./data/persistence.json").strip()

    if mode == "memory":
        logger.info("Persistence backend selected: memory")
        return InMemoryPersistence()
    if mode == "file":
        logger.info("Persistence backend selected: file path=%s", path)
        return FilePersistence(path=path)

    # auto selection:
    try:
        # Use the same configuration parser as DB adapter. If it errors due to missing env,
        # we treat that as "DB not configured" and fall back.
        from backend_api.app.adapters.db import is_db_configured

        if is_db_configured():
            logger.info("Persistence backend selected: postgres")
            return DbPersistence()
    except Exception:
        logger.exception("DB configuration check failed; falling back to file persistence.")

    logger.info("Persistence backend selected: file (auto fallback) path=%s", path)
    return FilePersistence(path=path)
