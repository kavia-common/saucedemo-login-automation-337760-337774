from __future__ import annotations

import asyncio
import logging
import os
from dataclasses import dataclass
from typing import Dict, Optional

logger = logging.getLogger(__name__)


@dataclass
class ProcessHandle:
    """Handle for a running subprocess."""

    popen: asyncio.subprocess.Process
    stdout: str = ""
    stderr: str = ""


async def _read_stream(stream: Optional[asyncio.StreamReader]) -> str:
    if stream is None:
        return ""
    data = await stream.read()
    try:
        return data.decode("utf-8", errors="replace")
    except Exception:
        return repr(data)


# PUBLIC_INTERFACE
async def run_command(
    *,
    cmd: list[str],
    cwd: str,
    env: Dict[str, str],
    cancel_event: asyncio.Event,
    timeout_s: Optional[int] = None,
) -> tuple[int, str, str]:
    """Run a command as a subprocess with best-effort cancellation.

    Contract:
    - Inputs: cmd, cwd, env, cancel_event, optional timeout.
    - Output: (returncode, stdout, stderr)
    - Errors: raises on spawn issues; timeout returns process termination and non-zero code.
    - Side effects: executes a subprocess.
    """
    logger.info("subprocess.start", extra={"cmd": cmd, "cwd": cwd})

    proc = await asyncio.create_subprocess_exec(
        *cmd,
        cwd=cwd,
        env=env,
        stdout=asyncio.subprocess.PIPE,
        stderr=asyncio.subprocess.PIPE,
    )

    async def _wait() -> int:
        return await proc.wait()

    wait_task = asyncio.create_task(_wait())
    cancel_task = asyncio.create_task(cancel_event.wait())

    try:
        done, _pending = await asyncio.wait(
            {wait_task, cancel_task},
            timeout=timeout_s,
            return_when=asyncio.FIRST_COMPLETED,
        )

        if cancel_task in done and cancel_event.is_set():
            logger.warning("subprocess.cancel_requested", extra={"cmd": cmd})
            with contextlib.suppress(ProcessLookupError):
                proc.terminate()
            await asyncio.sleep(2)
            if proc.returncode is None:
                with contextlib.suppress(ProcessLookupError):
                    proc.kill()
            await proc.wait()

        elif wait_task not in done:
            logger.error("subprocess.timeout", extra={"cmd": cmd, "timeout_s": timeout_s})
            with contextlib.suppress(ProcessLookupError):
                proc.terminate()
            await asyncio.sleep(2)
            if proc.returncode is None:
                with contextlib.suppress(ProcessLookupError):
                    proc.kill()
            await proc.wait()

        stdout, stderr = await asyncio.gather(_read_stream(proc.stdout), _read_stream(proc.stderr))
        rc = proc.returncode if proc.returncode is not None else 1
        logger.info("subprocess.end", extra={"cmd": cmd, "returncode": rc})
        return rc, stdout, stderr
    finally:
        wait_task.cancel()
        cancel_task.cancel()


import contextlib  # placed at end to keep the top section compact
