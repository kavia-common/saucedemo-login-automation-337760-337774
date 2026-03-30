from __future__ import annotations

from fastapi import APIRouter
from pydantic import BaseModel, Field

router = APIRouter(tags=["Docs"])


class WebSocketDocsResponse(BaseModel):
    """Documentation payload for WebSocket usage."""

    implemented: bool = Field(..., description="Whether a websocket endpoint is implemented.")
    message: str = Field(..., description="Usage notes.")


@router.get(
    "/docs/websocket",
    summary="WebSocket usage notes",
    description="This template endpoint documents real-time capabilities. WebSockets are not implemented in this iteration.",
    response_model=WebSocketDocsResponse,
    operation_id="websocketDocs",
)
def websocket_docs() -> WebSocketDocsResponse:
    """Return WebSocket usage help."""
    return WebSocketDocsResponse(
        implemented=False,
        message="WebSocket streaming is not implemented. The UI polls HTTP endpoints for updates.",
    )
