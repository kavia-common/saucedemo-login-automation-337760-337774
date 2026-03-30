-- SauceDemo QA Automation - test metadata database
-- Migration: 001_init
-- Minimal schema for tracking test runs, scenarios, steps, and artifacts/logs.

BEGIN;

-- --- Runs (one orchestration/execution) ---
CREATE TABLE IF NOT EXISTS test_runs (
  id            BIGSERIAL PRIMARY KEY,
  run_uuid      UUID NOT NULL UNIQUE,
  trigger_source TEXT NOT NULL DEFAULT 'manual',  -- manual|ci|api
  status        TEXT NOT NULL DEFAULT 'queued',   -- queued|running|passed|failed|error|cancelled
  started_at    TIMESTAMPTZ,
  finished_at   TIMESTAMPTZ,
  duration_ms   INTEGER,
  total_scenarios INTEGER NOT NULL DEFAULT 0,
  passed_scenarios INTEGER NOT NULL DEFAULT 0,
  failed_scenarios INTEGER NOT NULL DEFAULT 0,
  metadata      JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_test_runs_status_created_at
  ON test_runs(status, created_at DESC);

-- --- Scenarios (BDD scenarios within a run) ---
CREATE TABLE IF NOT EXISTS test_scenarios (
  id            BIGSERIAL PRIMARY KEY,
  run_id        BIGINT NOT NULL REFERENCES test_runs(id) ON DELETE CASCADE,
  scenario_name TEXT NOT NULL,
  feature_name  TEXT,
  tags          TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
  status        TEXT NOT NULL DEFAULT 'queued',  -- queued|running|passed|failed|skipped|error
  started_at    TIMESTAMPTZ,
  finished_at   TIMESTAMPTZ,
  duration_ms   INTEGER,
  error_message TEXT,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_test_scenarios_run_id
  ON test_scenarios(run_id);

CREATE INDEX IF NOT EXISTS idx_test_scenarios_status
  ON test_scenarios(status);

-- --- Steps (individual Gherkin steps executed) ---
CREATE TABLE IF NOT EXISTS test_steps (
  id            BIGSERIAL PRIMARY KEY,
  scenario_id   BIGINT NOT NULL REFERENCES test_scenarios(id) ON DELETE CASCADE,
  step_order    INTEGER NOT NULL DEFAULT 0,
  keyword       TEXT,        -- Given/When/Then/And/But
  step_text     TEXT NOT NULL,
  status        TEXT NOT NULL DEFAULT 'queued',  -- queued|running|passed|failed|skipped|error
  started_at    TIMESTAMPTZ,
  finished_at   TIMESTAMPTZ,
  duration_ms   INTEGER,
  error_message TEXT,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (scenario_id, step_order)
);

CREATE INDEX IF NOT EXISTS idx_test_steps_scenario_id
  ON test_steps(scenario_id);

CREATE INDEX IF NOT EXISTS idx_test_steps_status
  ON test_steps(status);

-- --- Artifacts/Logs (attachments produced by runs/scenarios/steps) ---
CREATE TABLE IF NOT EXISTS test_artifacts (
  id            BIGSERIAL PRIMARY KEY,
  run_id        BIGINT REFERENCES test_runs(id) ON DELETE CASCADE,
  scenario_id   BIGINT REFERENCES test_scenarios(id) ON DELETE CASCADE,
  step_id       BIGINT REFERENCES test_steps(id) ON DELETE CASCADE,
  artifact_type TEXT NOT NULL,  -- log|screenshot|html|video|trace|json|other
  name          TEXT,
  content_type  TEXT,
  -- Prefer external storage/path in real deployments; for local/CI small payloads can be stored inline.
  storage_path  TEXT,
  content_text  TEXT,
  content_bytes BYTEA,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT chk_artifact_parent
    CHECK (run_id IS NOT NULL OR scenario_id IS NOT NULL OR step_id IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS idx_test_artifacts_run_id
  ON test_artifacts(run_id);

CREATE INDEX IF NOT EXISTS idx_test_artifacts_scenario_id
  ON test_artifacts(scenario_id);

CREATE INDEX IF NOT EXISTS idx_test_artifacts_step_id
  ON test_artifacts(step_id);

-- --- updated_at trigger helper ---
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_test_runs_set_updated_at ON test_runs;
CREATE TRIGGER trg_test_runs_set_updated_at
BEFORE UPDATE ON test_runs
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_test_scenarios_set_updated_at ON test_scenarios;
CREATE TRIGGER trg_test_scenarios_set_updated_at
BEFORE UPDATE ON test_scenarios
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_test_steps_set_updated_at ON test_steps;
CREATE TRIGGER trg_test_steps_set_updated_at
BEFORE UPDATE ON test_steps
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMIT;
