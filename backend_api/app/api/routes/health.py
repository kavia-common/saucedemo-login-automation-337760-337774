"""Health check route.

Reports service health. The service is considered healthy as long as it
can respond — database availability is reported but is not a hard requirement
because the backend can operate with file/memory persistence fallback.
"""
from __future__ import annotations

import logging

from fastapi import APIRouter
from pydantic import BaseModel, Field

from backend_api.app.core.persistence import get_persistence

logger = logging.getLogger(__name__)

router = APIRouter(tags=["Health"])


class HealthResponse(BaseModel):
    """Health response payload."""

    ok: bool = Field(..., description="True if service is healthy (always true when reachable).")
    db_ok: bool = Field(..., description="True if DB connectivity works.")
    persistence_backend: str = Field(
        ..., description="Active persistence backend: postgres | file | memory."
    )
    message: str = Field(..., description="Human-friendly message.")


@router.get(
    "/health",
    summary="Health check",
    description="Returns service and database health status.",
    response_model=HealthResponse,
    operation_id="healthCheck",
)
def health() -> HealthResponse:
    """Return basic health check info.

    Contract:
    - Output: HealthResponse with ok=True when the service can respond.
    - db_ok reflects actual Postgres connectivity.
    - persistence_backend indicates which storage layer is active.
    - Errors: never raises; always returns a valid response.
    """
    persistence = get_persistence()
    status = persistence.status()

    db_ok = False
    if status.backend == "postgres":
        try:
            from backend_api.app.adapters.db import fetch_one

            row = fetch_one("SELECT 1 as ok")
            db_ok = bool(row and row.get("ok") == 1)
        except Exception:
            logger.warning("health.db_check_failed", exc_info=True)
            db_ok = False

    # Service is healthy as long as it's reachable — DB is optional.
    return HealthResponse(
        ok=True,
        db_ok=db_ok,
        persistence_backend=status.backend,
        message=f"ok (persistence={status.backend})",
    )
