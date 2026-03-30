#!/usr/bin/env bash
set -euo pipefail

# Apply SQL migrations for the test_metadata_db Postgres schema.
#
# Order is lexical by filename. This script is CI-friendly and non-interactive.
#
# Connection resolution:
# 1) If ../db_connection.txt exists and includes a "psql postgresql://..." connection string,
#    we use that (Kavia database containers typically provide it).
# 2) Else, use $DATABASE_URL (must be a postgres connection string)
#
# Usage:
#   ./scripts/apply_migrations.sh
#
# Optional env:
#   MIGRATIONS_DIR - override default migrations directory

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
MIGRATIONS_DIR="${MIGRATIONS_DIR:-${PROJECT_ROOT}/migrations}"

if [[ ! -d "${MIGRATIONS_DIR}" ]]; then
  echo "Migrations directory not found: ${MIGRATIONS_DIR}" >&2
  exit 1
fi

PSQL_CMD=""

# Prefer db_connection.txt if present (common in Kavia DB container environments)
if [[ -f "${PROJECT_ROOT}/db_connection.txt" ]]; then
  # Extract the first occurrence of a psql command from the file.
  # Expected format: psql postgresql://user:pass@host:port/dbname
  PSQL_CMD="$(grep -Eo '^psql[[:space:]]+postgresql://[^[:space:]]+' "${PROJECT_ROOT}/db_connection.txt" | head -n 1 || true)"
fi

# Fall back to DATABASE_URL if db_connection.txt is absent or doesn't match expected format
if [[ -z "${PSQL_CMD}" ]]; then
  if [[ -z "${DATABASE_URL:-}" ]]; then
    echo "No database connection found." >&2
    echo "Provide ${PROJECT_ROOT}/db_connection.txt containing 'psql postgresql://...'" >&2
    echo "OR set DATABASE_URL to a postgres connection string." >&2
    exit 1
  fi
  PSQL_CMD="psql ${DATABASE_URL}"
fi

echo "Using migrations dir: ${MIGRATIONS_DIR}"
echo "Using connection command: ${PSQL_CMD}"

# Apply migrations in lexical order to keep it simple and deterministic.
shopt -s nullglob
mapfile -t MIGRATIONS < <(find "${MIGRATIONS_DIR}" -maxdepth 1 -type f -name '*.sql' -print0 | sort -z | xargs -0 -n1 echo)

if [[ "${#MIGRATIONS[@]}" -eq 0 ]]; then
  echo "No .sql migrations found in ${MIGRATIONS_DIR}. Nothing to do."
  exit 0
fi

for f in "${MIGRATIONS[@]}"; do
  echo "Applying migration: $(basename "${f}")"
  # -v ON_ERROR_STOP=1 ensures failures stop the script.
  ${PSQL_CMD} -v ON_ERROR_STOP=1 -f "${f}"
done

echo "All migrations applied successfully."
