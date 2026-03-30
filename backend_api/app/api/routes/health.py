from __future__ import annotations

from fastapi import APIRouter
from pydantic import BaseModel, Field

from backend_api.app.adapters.db import fetch_one

router = APIRouter(tags=["Health"])


class HealthResponse(BaseModel):
    """Health response payload."""

    ok: bool = Field(..., description="True if service is healthy.")
    db_ok: bool = Field(..., description="True if DB connectivity works.")
    message: str = Field(..., description="Human-friendly message.")


@router.get(
    "/health",
    summary="Health check",
    description="Returns service and database health status.",
    response_model=HealthResponse,
    operation_id="healthCheck",
)
def health() -> HealthResponse:
    """Return basic health check info."""
    try:
        row = fetch_one("SELECT 1 as ok")
        db_ok = bool(row and row.get("ok") == 1)
    except Exception:
        db_ok = False

    ok = db_ok
    return HealthResponse(ok=ok, db_ok=db_ok, message="ok" if ok else "db unavailable")
