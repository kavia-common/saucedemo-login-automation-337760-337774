from __future__ import annotations

from functools import lru_cache
from typing import List, Optional

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Application settings loaded from environment variables.

    Contract:
    - Inputs: environment variables.
    - Output: typed settings object used throughout the app.
    - Errors: raises validation errors if required settings are missing.
    """

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    # Database configuration
    #
    # We support both:
    # - DATABASE_URL (canonical)
    # - DB_* parts (provided by some database containers)
    #
    # If DATABASE_URL is not provided, it is composed from DB_* parts.
    database_url: Optional[str] = Field(
        None, alias="DATABASE_URL", description="Postgres connection URL (canonical)."
    )
    db_host: Optional[str] = Field(None, alias="DB_HOST", description="DB host (used if DATABASE_URL missing).")
    db_port: int = Field(5432, alias="DB_PORT", description="DB port (used if DATABASE_URL missing).")
    db_name: Optional[str] = Field(None, alias="DB_NAME", description="DB name (used if DATABASE_URL missing).")
    db_user: Optional[str] = Field(None, alias="DB_USER", description="DB user (used if DATABASE_URL missing).")
    db_password: Optional[str] = Field(
        None, alias="DB_PASSWORD", description="DB password (used if DATABASE_URL missing)."
    )

    # Persistence configuration (optional DB)
    persistence_mode: str = Field(
        "auto",
        alias="BACKEND_PERSISTENCE",
        description="Persistence backend selection: auto|postgres|file|memory. 'auto' uses postgres if configured else file.",
    )
    persistence_path: str = Field(
        "./data/persistence.json",
        alias="BACKEND_PERSISTENCE_PATH",
        description="Path to JSON persistence file when using file persistence.",
    )

    log_level: str = Field("INFO", alias="BACKEND_LOG_LEVEL", description="Logging level.")
    host: str = Field("0.0.0.0", alias="BACKEND_HOST", description="Bind host.")
    port: int = Field(8000, alias="BACKEND_PORT", description="Bind port.")

    runner_workdir: str = Field(
        "backend_api/runner-java",
        alias="RUNNER_WORKDIR",
        description="Working directory where the Maven runner project lives.",
    )
    runner_mvn_cmd: str = Field(
        "mvn",
        alias="RUNNER_MVN_CMD",
        description="Maven command (e.g. mvn or ./mvnw).",
    )
    runner_max_concurrency: int = Field(
        1,
        alias="RUNNER_MAX_CONCURRENCY",
        description="Maximum number of concurrent runs (global).",
        ge=1,
        le=8,
    )

    # Allow local dev + configurable overrides.
    cors_allow_origins_raw: str = Field(
        "*",
        alias="CORS_ALLOW_ORIGINS",
        description="Comma-separated list of allowed CORS origins or '*'",
    )

    @property
    def cors_allow_origins(self) -> List[str]:
        """Returns normalized CORS allow origins list."""
        raw = (self.cors_allow_origins_raw or "*").strip()
        if raw == "*":
            return ["*"]
        return [o.strip() for o in raw.split(",") if o.strip()]

    # PUBLIC_INTERFACE
    def get_database_url(self) -> str:
        """Return a usable Postgres connection URL.

        Contract:
        - Inputs:
          - DATABASE_URL OR (DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD)
        - Output: SQLAlchemy/psycopg compatible URL string.
        - Errors: ValueError if insufficient configuration is present.
        """
        if self.database_url and str(self.database_url).strip():
            return str(self.database_url).strip()

        missing = [k for k, v in {
            "DB_HOST": self.db_host,
            "DB_NAME": self.db_name,
            "DB_USER": self.db_user,
            "DB_PASSWORD": self.db_password,
        }.items() if not v]
        if missing:
            raise ValueError(
                "Database not configured. Set DATABASE_URL or provide: "
                + ", ".join(missing)
            )

        # Basic URL construction; values are assumed safe for URL usage in this template context.
        return f"postgresql://{self.db_user}:{self.db_password}@{self.db_host}:{self.db_port}/{self.db_name}"

    # PUBLIC_INTERFACE
    def is_database_configured(self) -> bool:
        """Return True if enough environment variables exist to attempt a DB connection.

        Contract:
        - Inputs: DATABASE_URL or DB_* parts.
        - Output: bool.
        - Errors: never raises; only indicates presence of config, not connectivity.
        """
        if self.database_url and str(self.database_url).strip():
            return True
        parts_present = all([self.db_host, self.db_name, self.db_user, self.db_password])
        return bool(parts_present)


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    """Cached settings retrieval."""
    return Settings()
