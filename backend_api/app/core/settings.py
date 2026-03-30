from __future__ import annotations

from functools import lru_cache
from typing import List

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

    database_url: str = Field(..., alias="DATABASE_URL", description="Postgres connection URL.")
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


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    """Cached settings retrieval."""
    return Settings()
