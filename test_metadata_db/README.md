# test_metadata_db (Postgres)

Minimal Postgres schema for storing QA automation execution metadata:

- `test_runs`: one orchestration/execution (manual, CI, API-triggered)
- `test_scenarios`: scenarios executed within a run (feature/scenario, tags, status, timings)
- `test_steps`: step-level execution detail (keyword, text, status, timings)
- `test_artifacts`: logs/screenshots/etc linked to run/scenario/step

## Migrations

Migrations live in:

- `test_metadata_db/migrations/`

Current migrations:

- `001_init.sql` — creates tables, indexes, and `updated_at` triggers

## Applying migrations

This repository intentionally keeps migrations as plain SQL so they can be applied by any runner in CI.

Example (conceptual):

```bash
psql "$DATABASE_URL" -f test_metadata_db/migrations/001_init.sql
```

In a Kavia environment, database containers typically provide a `db_connection.txt` with a `psql postgresql://...` connection string. Use that connection with `psql -f ...` to apply the migration.

### Helper script (local dev/CI)

You can apply all migrations in lexical order using:

```bash
./test_metadata_db/scripts/apply_migrations.sh
```

Connection resolution:

1. Uses `test_metadata_db/db_connection.txt` if present (expects a `psql postgresql://...` line)
2. Otherwise uses `DATABASE_URL`
