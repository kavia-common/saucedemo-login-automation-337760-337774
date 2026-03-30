"""
backend_api entrypoint.

Exposes REST endpoints to:
- start/list/get/cancel test runs
- persist and query run metadata (Postgres when available, file/memory fallback)
- orchestrate execution of the Java/Maven Cucumber+Selenium runner

Run (conceptual):
    uvicorn backend_api.main:app --host 0.0.0.0 --port 8000
"""

from __future__ import annotations

import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from backend_api.app.api.routes.health import router as health_router
from backend_api.app.api.routes.runs import router as runs_router
from backend_api.app.api.routes.websocket_docs import router as websocket_docs_router
from backend_api.app.core.logging import configure_logging
from backend_api.app.core.persistence import get_persistence
from backend_api.app.core.settings import get_settings

logger = logging.getLogger(__name__)

openapi_tags = [
    {"name": "Health", "description": "Service health and diagnostics."},
    {"name": "Runs", "description": "Start/list/get/cancel test runs."},
    {"name": "Docs", "description": "Additional documentation endpoints."},
]


def create_app() -> FastAPI:
    """Creates the FastAPI application with configured routes and middleware.

    Flow:
    1. Load settings from environment.
    2. Configure logging.
    3. Initialize persistence (auto-selects postgres/file/memory).
    4. Register CORS middleware and route handlers.
    """
    settings = get_settings()
    configure_logging(settings.log_level)

    app = FastAPI(
        title="SauceDemo Login QA Automation - backend_api",
        description=(
            "Orchestrates Java/Maven Cucumber+Selenium test runs for SauceDemo login scenarios, "
            "and persists run metadata (Postgres when available, file/memory fallback)."
        ),
        version="1.0.0",
        openapi_tags=openapi_tags,
    )

    # CORS: allow frontend dev origins.
    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.cors_allow_origins,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    app.include_router(health_router)
    app.include_router(runs_router)
    app.include_router(websocket_docs_router)

    @app.on_event("startup")
    async def _on_startup() -> None:
        """Log persistence backend selection on startup for observability."""
        persistence = get_persistence()
        status = persistence.status()
        logger.info(
            "backend_api.startup",
            extra={"persistence_backend": status.backend, "persistence_details": status.details},
        )

    # Add /healthz alias to match the configured HEALTHCHECK_PATH.
    @app.get(
        "/healthz",
        summary="Health check (alias)",
        description="Alias for /health, used by container health checks.",
        tags=["Health"],
        operation_id="healthCheckAlias",
    )
    def healthz():
        """Alias for /health endpoint, returns same health status."""
        from backend_api.app.api.routes.health import health
        return health()

    return app


app = create_app()
