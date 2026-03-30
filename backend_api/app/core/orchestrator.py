from __future__ import annotations

from functools import lru_cache

from backend_api.app.core.settings import get_settings
from backend_api.app.flows.test_run_flow import TestRunOrchestrator


@lru_cache(maxsize=1)
def get_orchestrator() -> TestRunOrchestrator:
    """Get a singleton orchestrator instance for the process."""
    return TestRunOrchestrator(get_settings())
