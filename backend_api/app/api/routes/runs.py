from __future__ import annotations

import logging
from typing import Any, Dict, Optional

from fastapi import APIRouter, HTTPException, Path, Query
from pydantic import BaseModel, Field

from backend_api.app.core.orchestrator import get_orchestrator
from backend_api.app.flows.test_run_flow import RunConflictError, StartRunRequest
from backend_api.app.models.runs import CancelRunResponse, RunCreateRequest, RunListResponse

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/runs", tags=["Runs"])


class RunDetailsResponse(BaseModel):
    """Run details response.

    Note: frontend renders raw payload; include a stable subset + metadata and artifacts when available.
    """

    run: Dict[str, Any] = Field(..., description="Run record as stored in DB.")
    artifacts: list[Dict[str, Any]] = Field(default_factory=list, description="Artifacts attached to the run.")


@router.get(
    "",
    summary="List recent runs",
    description="Returns recent test runs, newest first.",
    response_model=RunListResponse,
    operation_id="listRuns",
)
def list_runs_endpoint(
    limit: int = Query(25, ge=1, le=200, description="Maximum number of runs to return."),
) -> RunListResponse:
    """List runs endpoint."""
    orch = get_orchestrator()
    items = orch.list_runs(limit=limit)

    # Normalize for UI convenience: add `scenarios_total` for table display.
    for r in items:
        r["scenarios_total"] = r.get("total_scenarios", 0)
        meta = r.get("metadata") or {}
        r["browser"] = meta.get("browser")
        r["scenario_tag"] = meta.get("scenario_tag")
        r["scenarioTag"] = meta.get("scenario_tag")
    return RunListResponse(items=items)


@router.post(
    "",
    summary="Start a test run",
    description="Creates a run record and starts the Maven Cucumber+Selenium runner as a subprocess.",
    operation_id="createRun",
)
async def create_run_endpoint(body: RunCreateRequest) -> Dict[str, Any]:
    """Create run endpoint."""
    orch = get_orchestrator()
    try:
        result = await orch.start_run(
            StartRunRequest(scenario_tag=body.scenario_tag, browser=body.browser, trigger_source=body.trigger_source)
        )
        run = result.run
        run["scenarios_total"] = run.get("total_scenarios", 0)
        return run
    except RunConflictError as e:
        raise HTTPException(status_code=409, detail=str(e)) from e


@router.get(
    "/{run_id}",
    summary="Get run details",
    description="Returns a single run record plus its artifacts (logs, etc.).",
    operation_id="getRun",
)
def get_run_endpoint(
    run_id: int = Path(..., ge=1, description="Run numeric id."),
) -> Dict[str, Any]:
    """Get run endpoint."""
    orch = get_orchestrator()
    run = orch.get_run(run_id)
    if not run:
        raise HTTPException(status_code=404, detail="Run not found")

    meta = run.get("metadata") or {}
    run["browser"] = meta.get("browser")
    run["scenario_tag"] = meta.get("scenario_tag")
    run["scenarioTag"] = meta.get("scenario_tag")

    # Include artifacts inline since UI renders raw payload.
    from backend_api.app.repositories.artifacts_repo import get_run_artifacts

    artifacts = get_run_artifacts(run_id)
    run["artifacts"] = artifacts
    return run


@router.post(
    "/{run_id}/cancel",
    summary="Cancel a running run",
    description="Best-effort cancellation. If the run is running, it will be terminated and marked cancelled.",
    response_model=CancelRunResponse,
    operation_id="cancelRun",
)
def cancel_run_endpoint(
    run_id: int = Path(..., ge=1, description="Run numeric id."),
) -> CancelRunResponse:
    """Cancel run endpoint."""
    orch = get_orchestrator()
    ok = orch.cancel_run(run_id)
    if not ok:
        return CancelRunResponse(ok=False, message="No active running process found for this run_id.")
    return CancelRunResponse(ok=True, message="Cancellation requested.")
