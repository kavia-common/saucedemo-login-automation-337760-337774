from __future__ import annotations

from datetime import datetime, timezone
from typing import Any, Dict, List, Optional
from uuid import UUID, uuid4

from backend_api.app.adapters.db import execute, execute_returning_one, fetch_all, fetch_one


def _utcnow() -> datetime:
    return datetime.now(timezone.utc)


# PUBLIC_INTERFACE
def create_run(
    *,
    trigger_source: str,
    scenario_tag: str,
    browser: str,
    metadata: Dict[str, Any],
) -> Dict[str, Any]:
    """Create a new run row with status=queued.

    Contract:
    - Inputs: trigger_source/scenario_tag/browser plus metadata dict.
    - Output: inserted run record as a dict with DB columns.
    - Errors: raises on DB errors.
    - Side effects: inserts into test_runs.
    """
    run_uuid = uuid4()
    row = execute_returning_one(
        """
        INSERT INTO test_runs (run_uuid, trigger_source, status, started_at, metadata)
        VALUES (%s, %s, 'queued', NULL, %s::jsonb)
        RETURNING *
        """,
        (str(run_uuid), trigger_source, metadata),
    )
    # Store "requested parameters" in metadata for easy UI inspection.
    # Note: schema has no explicit columns for browser/tag; we keep it in metadata.
    meta = dict(row.get("metadata") or {})
    meta.update({"scenario_tag": scenario_tag, "browser": browser})
    execute("UPDATE test_runs SET metadata=%s::jsonb WHERE id=%s", (meta, row["id"]))
    row["metadata"] = meta
    return row


# PUBLIC_INTERFACE
def mark_run_running(run_id: int) -> None:
    """Transition run to running and set started_at."""
    execute(
        "UPDATE test_runs SET status='running', started_at=%s WHERE id=%s",
        (_utcnow(), run_id),
    )


# PUBLIC_INTERFACE
def mark_run_finished(
    *,
    run_id: int,
    status: str,
    finished_at: datetime,
    duration_ms: Optional[int],
    totals: Dict[str, int],
) -> None:
    """Mark run finished with status and computed totals."""
    execute(
        """
        UPDATE test_runs
        SET status=%s,
            finished_at=%s,
            duration_ms=%s,
            total_scenarios=%s,
            passed_scenarios=%s,
            failed_scenarios=%s
        WHERE id=%s
        """,
        (
            status,
            finished_at,
            duration_ms,
            totals.get("total_scenarios", 0),
            totals.get("passed_scenarios", 0),
            totals.get("failed_scenarios", 0),
            run_id,
        ),
    )


# PUBLIC_INTERFACE
def mark_run_cancelled(run_id: int) -> None:
    """Mark run cancelled (best-effort)."""
    execute("UPDATE test_runs SET status='cancelled', finished_at=%s WHERE id=%s", (_utcnow(), run_id))


# PUBLIC_INTERFACE
def get_run(run_id: int) -> Optional[Dict[str, Any]]:
    """Get a single run by id."""
    return fetch_one("SELECT * FROM test_runs WHERE id=%s", (run_id,))


# PUBLIC_INTERFACE
def list_runs(limit: int = 25) -> List[Dict[str, Any]]:
    """List recent runs."""
    return fetch_all("SELECT * FROM test_runs ORDER BY created_at DESC LIMIT %s", (limit,))
