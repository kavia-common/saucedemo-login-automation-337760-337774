#!/usr/bin/env bash
# start_db.sh — Reliable Postgres startup for PreviewManager
#
# Flow: StartPostgres → CreateRole → CreateDB → ApplyMigrations → GenerateConnectionFile → HealthLoop
#
# Contract:
#   - Inputs: None required (uses peer auth via sudo -u postgres)
#   - Outputs: db_connection.txt with psql connection string
#   - Side effects: Postgres cluster started, kavia role + test_metadata_db database created,
#                   migrations applied
#   - Errors: Exits non-zero on critical failures, logs context for debugging
#
# PreviewManager readiness:
#   The script prints "DB ready" on stdout when the database is fully initialised,
#   then enters an infinite health-check loop so the process stays alive (required
#   by PreviewManager strict policy to keep the container "running").
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
MIGRATIONS_DIR="${PROJECT_ROOT}/migrations"

DB_NAME="test_metadata_db"
DB_USER="kavia"
DB_PORT="5432"
DB_HOST="localhost"

log() {
  echo "[start_db] $(date '+%Y-%m-%d %H:%M:%S') $*"
}

# ---------- Step 1: Ensure Postgres cluster is running ----------
log "Ensuring PostgreSQL 16 cluster is started..."
sudo pg_ctlcluster 16 main start 2>/dev/null || true
sleep 1

# Wait for Postgres to accept connections (up to 15 seconds)
for i in $(seq 1 15); do
  if pg_isready -h "${DB_HOST}" -p "${DB_PORT}" -q 2>/dev/null; then
    log "PostgreSQL is accepting connections."
    break
  fi
  if [ "$i" -eq 15 ]; then
    log "ERROR: PostgreSQL did not become ready within 15 seconds."
    exit 1
  fi
  sleep 1
done

# ---------- Step 2: Create role if it doesn't exist ----------
log "Ensuring database role '${DB_USER}' exists..."
ROLE_EXISTS=$(sudo -u postgres psql -tAc "SELECT 1 FROM pg_roles WHERE rolname='${DB_USER}';" 2>/dev/null || true)
if [ "${ROLE_EXISTS}" != "1" ]; then
  sudo -u postgres psql -c "CREATE ROLE ${DB_USER} WITH LOGIN CREATEDB;" 2>&1
  log "Created role '${DB_USER}'."
else
  log "Role '${DB_USER}' already exists."
fi

# ---------- Step 3: Create database if it doesn't exist ----------
log "Ensuring database '${DB_NAME}' exists..."
DB_EXISTS=$(sudo -u postgres psql -tAc "SELECT 1 FROM pg_database WHERE datname='${DB_NAME}';" 2>/dev/null || true)
if [ "${DB_EXISTS}" != "1" ]; then
  sudo -u postgres psql -c "CREATE DATABASE ${DB_NAME} OWNER ${DB_USER};" 2>&1
  log "Created database '${DB_NAME}'."
else
  log "Database '${DB_NAME}' already exists."
fi

# Grant all privileges to be safe
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE ${DB_NAME} TO ${DB_USER};" 2>/dev/null || true

# ---------- Step 4: Apply migrations ----------
log "Applying migrations from ${MIGRATIONS_DIR}..."
if [ -d "${MIGRATIONS_DIR}" ]; then
  shopt -s nullglob
  MIGRATION_FILES=("${MIGRATIONS_DIR}"/*.sql)
  if [ ${#MIGRATION_FILES[@]} -eq 0 ]; then
    log "No .sql migration files found. Skipping."
  else
    for f in "${MIGRATION_FILES[@]}"; do
      log "Applying migration: $(basename "${f}")"
      sudo -u postgres psql -d "${DB_NAME}" -v ON_ERROR_STOP=1 -f "${f}" 2>&1 || {
        log "WARNING: Migration $(basename "${f}") had errors (may already be applied)."
      }
    done
    log "All migrations processed."

    # Grant privileges on all objects to the application role.
    # Migrations run as postgres (superuser), so the created tables/sequences
    # are owned by postgres. The application role needs explicit access.
    log "Granting privileges on schema objects to '${DB_USER}'..."
    sudo -u postgres psql -d "${DB_NAME}" -c "
      GRANT ALL ON SCHEMA public TO ${DB_USER};
      GRANT ALL ON ALL TABLES IN SCHEMA public TO ${DB_USER};
      GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO ${DB_USER};
      ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO ${DB_USER};
      ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO ${DB_USER};
    " 2>&1 || log "WARNING: Could not grant privileges."
  fi
else
  log "WARNING: Migrations directory not found at ${MIGRATIONS_DIR}"
fi

# ---------- Step 5: Generate db_connection.txt ----------
CONNECTION_STRING="postgresql://${DB_USER}@${DB_HOST}:${DB_PORT}/${DB_NAME}"
echo "psql ${CONNECTION_STRING}" > "${PROJECT_ROOT}/db_connection.txt"
log "Wrote db_connection.txt: psql ${CONNECTION_STRING}"

# ---------- Step 6: Signal readiness and keep alive ----------
log "Database initialisation complete."
echo "DB ready"

# Health-check loop keeps the process alive for PreviewManager.
# Checks Postgres connectivity every 10 seconds; exits if Postgres goes down
# so PreviewManager can detect the failure and restart.
while true; do
  if ! pg_isready -h "${DB_HOST}" -p "${DB_PORT}" -q 2>/dev/null; then
    log "ERROR: PostgreSQL health check failed. Exiting."
    exit 1
  fi
  sleep 10
done
