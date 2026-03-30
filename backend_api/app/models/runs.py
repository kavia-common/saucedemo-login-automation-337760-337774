from __future__ import annotations

from datetime import datetime
from typing import Any, Dict, List, Literal, Optional
from uuid import UUID

from pydantic import BaseModel, Field


RunStatus = Literal["queued", "running", "passed", "failed", "error", "cancelled"]


class RunCreateRequest(BaseModel):
    """Request body for creating a test run."""

    scenario_tag: str = Field(
        "all",
        description="Scenario tag to filter which scenarios to run. 'all' runs everything.",
        examples=["all", "@valid_login", "@invalid_login"],
    )
    browser: str = Field(
        "chrome",
        description="Browser name; passed to runner (best-effort).",
        examples=["chrome", "firefox"],
    )
    trigger_source: str = Field(
        "api",
        description="Who triggered the run (manual|ci|api).",
        examples=["api", "manual"],
    )


class RunListResponse(BaseModel):
    """Response payload for listing runs."""

    items: List[Dict[str, Any]] = Field(..., description="List of run records.")


class RunRecord(BaseModel):
    """Run record returned by the API."""

    id: int = Field(..., description="Database run id.")
    run_uuid: UUID = Field(..., description="Stable run UUID.")
    status: RunStatus = Field(..., description="Current run status.")
    started_at: Optional[datetime] = Field(None, description="Run start time.")
    finished_at: Optional[datetime] = Field(None, description="Run finish time.")
    duration_ms: Optional[int] = Field(None, description="Duration in ms.")

    total_scenarios: int = Field(0, description="Total scenarios.")
    passed_scenarios: int = Field(0, description="Passed scenarios.")
    failed_scenarios: int = Field(0, description="Failed scenarios.")

    metadata: Dict[str, Any] = Field(default_factory=dict, description="Free-form metadata.")


class CancelRunResponse(BaseModel):
    """Response payload for cancelling a run."""

    ok: bool = Field(..., description="True if cancellation was requested.")
    message: str = Field(..., description="Human-friendly result message.")
