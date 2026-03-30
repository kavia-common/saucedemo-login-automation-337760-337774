# backend_api

FastAPI service that orchestrates SauceDemo Login QA automation runs (Cucumber + Selenium), persists run metadata in Postgres (`test_metadata_db`), and exposes REST endpoints consumed by `qa_automation_frontend`.

## What this service provides

### REST API (consumed by frontend)

- `GET /health`
- `GET /runs?limit=25` — list recent runs
- `POST /runs` — start a run
- `GET /runs/{run_id}` — get run details
- `POST /runs/{run_id}/cancel` — best-effort cancel of a running run
- `GET /docs/websocket` — usage note (WebSocket is not implemented in this iteration; endpoint exists to satisfy documentation conventions)

### Execution model

- Each test run is executed as an OS subprocess: `mvn test` inside `backend_api/runner-java/`.
- Only one run is executed at a time by default (simple global concurrency guard) to keep the environment predictable.
- Cancellation is implemented by terminating the subprocess.

### Database

This backend writes to the schema created by `test_metadata_db/migrations/001_init.sql`:
- `test_runs` rows are created on run start and updated on completion/cancel.
- For this iteration, scenario/step tables are not populated (raw Maven output is stored as a `test_artifacts` row).

## Environment variables

This container must be configured via `.env` by the orchestrator (do not commit `.env`).

Required:
- `DATABASE_URL` — Postgres connection string, e.g. `postgresql://user:pass@host:5432/dbname`

Optional:
- `BACKEND_HOST` (default `0.0.0.0`)
- `BACKEND_PORT` (default `8000`)
- `BACKEND_LOG_LEVEL` (default `INFO`)
- `RUNNER_WORKDIR` (default `backend_api/runner-java`)
- `RUNNER_MVN_CMD` (default `mvn`)
- `RUNNER_MAX_CONCURRENCY` (default `1`)

If you plan to run Selenium against Sauce Labs, also provide (optional for local execution):
- `SAUCE_USERNAME`
- `SAUCE_ACCESS_KEY`

## Local dev (conceptual)

```bash
pip install -r backend_api/requirements.txt
uvicorn backend_api.main:app --host 0.0.0.0 --port 8000
```

Then browse:
- http://localhost:8000/docs
- http://localhost:8000/health

## Notes

- This service is designed as a reusable flow (see `backend_api/app/flows/test_run_flow.py`) rather than scattered endpoint logic.
- The runner project is included under `backend_api/runner-java/` for portability.
