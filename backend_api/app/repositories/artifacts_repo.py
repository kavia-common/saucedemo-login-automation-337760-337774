from __future__ import annotations

from backend_api.app.core.persistence import get_persistence


# PUBLIC_INTERFACE
def attach_run_log(*, run_id: int, name: str, content_text: str, content_type: str = "text/plain") -> None:
    """Attach a text artifact (typically Maven console output) to a run.

    Contract:
    - Inputs: run_id, name, content_text.
    - Output: None.
    - Side effects: stores an artifact associated with run_id.
    """
    store = get_persistence()
    store.attach_run_log(run_id=run_id, name=name, content_text=content_text, content_type=content_type)


# PUBLIC_INTERFACE
def get_run_artifacts(run_id: int) -> list[dict]:
    """Return artifacts for a run."""
    store = get_persistence()
    return list(store.get_run_artifacts(run_id))
