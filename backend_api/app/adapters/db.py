from __future__ import annotations

import contextlib
from dataclasses import dataclass
from typing import Any, Dict, Iterator, List, Optional, Sequence, Tuple

import psycopg
from psycopg.rows import dict_row

from backend_api.app.core.settings import get_settings


@dataclass(frozen=True)
class DbConfig:
    """Database configuration object."""
    url: str


def _get_db_config() -> DbConfig:
    settings = get_settings()
    return DbConfig(url=settings.database_url)


@contextlib.contextmanager
def get_conn() -> Iterator[psycopg.Connection]:
    """Get a Postgres connection context manager."""
    cfg = _get_db_config()
    with psycopg.connect(cfg.url, row_factory=dict_row) as conn:
        yield conn


def fetch_all(sql: str, params: Optional[Sequence[Any]] = None) -> List[Dict[str, Any]]:
    """Fetch all rows as dictionaries."""
    with get_conn() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, params or ())
            return list(cur.fetchall())


def fetch_one(sql: str, params: Optional[Sequence[Any]] = None) -> Optional[Dict[str, Any]]:
    """Fetch a single row as dictionary or None."""
    with get_conn() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, params or ())
            return cur.fetchone()


def execute(sql: str, params: Optional[Sequence[Any]] = None) -> None:
    """Execute a statement."""
    with get_conn() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, params or ())
        conn.commit()


def execute_returning_one(sql: str, params: Optional[Sequence[Any]] = None) -> Dict[str, Any]:
    """Execute statement and return one row (RETURNING ...)."""
    with get_conn() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, params or ())
            row = cur.fetchone()
        conn.commit()
        if row is None:
            raise RuntimeError("Expected a row to be returned but got none.")
        return row


def execute_many(sql: str, params_seq: Sequence[Sequence[Any]]) -> None:
    """Execute statement with many parameter sets."""
    with get_conn() as conn:
        with conn.cursor() as cur:
            cur.executemany(sql, params_seq)
        conn.commit()
