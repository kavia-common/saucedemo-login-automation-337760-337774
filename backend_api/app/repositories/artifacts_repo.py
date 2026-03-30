from __future__ import annotations

from typing import Optional

from backend_api.app.adapters.db import execute


# PUBLIC_INTERFACE
def attach_run_log(*, run_id: int, name: str, content_text: str, content_type: str = "text/plain") -> None:
    """Attach a text artifact (typically Maven console output) to a run.

    Contract:
    - Inputs: run_id, name, content_text.
    - Output: None.
    - Side effects: INSERT into test_artifacts.
    """
    execute(
        """
        INSERT INTO test_artifacts (run_id, artifact_type, name, content_type, content_text)
        VALUES (%s, 'log', %s, %s, %s)
        """,
        (run_id, name, content_type, content_text),
    )


# PUBLIC_INTERFACE
def get_run_artifacts(run_id: int) -> list[dict]:
    """Return artifacts for a run."""
    from backend_api.app.adapters.db import fetch_all

    return fetch_all("SELECT * FROM test_artifacts WHERE run_id=%s ORDER BY created_at ASC", (run_id,))
