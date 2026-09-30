#!/usr/bin/env bash
# Start Postgres (Docker), Spring Boot backend, and React frontend together.
# Usage (from repo root):  ./scripts/dev.sh
# Stop:                    Ctrl+C

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

BACKEND_PID=""
FRONTEND_PID=""

log() { printf '\n[%s] %s\n' "$(date '+%H:%M:%S')" "$*"; }

die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

need() {
  command -v "$1" >/dev/null 2>&1 || die "Required command not found: $1"
}

cleanup() {
  log "Shutting down..."
  if [[ -n "${FRONTEND_PID}" ]] && kill -0 "${FRONTEND_PID}" 2>/dev/null; then
    kill "${FRONTEND_PID}" 2>/dev/null || true
  fi
  if [[ -n "${BACKEND_PID}" ]] && kill -0 "${BACKEND_PID}" 2>/dev/null; then
    kill "${BACKEND_PID}" 2>/dev/null || true
  fi
  wait 2>/dev/null || true
  log "Stopped. (Postgres container left running — use: docker compose down)"
}

trap cleanup EXIT INT TERM

need docker
need mvn
need npm
need java

if [[ ! -f .env.properties ]]; then
  [[ -f .env.example ]] || die "Missing .env.properties and .env.example"
  cp .env.example .env.properties
  log "Created .env.properties from .env.example — edit secrets before production use"
fi

# Compose interpolates ${db_*} from `.env` (not from env_file). Keep them in sync.
ln -sfn .env.properties .env

# Load DB credentials for health checks (Spring still reads .env.properties itself).
# shellcheck disable=SC1091
set -a
# shellcheck source=/dev/null
source <(grep -E '^[a-zA-Z_][a-zA-Z0-9_]*=' .env.properties | sed 's/\r$//')
set +a
DB_USER="${db_username:-postgres}"

COMPOSE=(docker compose --env-file .env.properties)

log "Starting Postgres (docker compose)..."
if "${COMPOSE[@]}" ps --status running --services 2>/dev/null | grep -qx db; then
  log "Postgres already running"
else
  "${COMPOSE[@]}" up -d db
fi

log "Waiting for Postgres on localhost:5433..."
ready=0
for _ in $(seq 1 60); do
  if "${COMPOSE[@]}" exec -T db pg_isready -U "$DB_USER" >/dev/null 2>&1; then
    ready=1
    break
  fi
  sleep 1
done
[[ "$ready" -eq 1 ]] || die "Postgres did not become ready on :5433"

if [[ ! -d frontend/node_modules ]]; then
  log "Installing frontend dependencies (npm install)..."
  (cd frontend && npm install)
fi

log "Starting backend (http://localhost:8080)..."
(
  cd "$ROOT"
  SPRING_DEVTOOLS_RESTART_ENABLED=false mvn -f backend/pom.xml spring-boot:run -DskipTests
) &
BACKEND_PID=$!

log "Starting frontend (http://localhost:3000)..."
(
  cd "$ROOT/frontend"
  BROWSER="${BROWSER:-none}" npm start
) &
FRONTEND_PID=$!

log "Backend PID=${BACKEND_PID}  Frontend PID=${FRONTEND_PID}"
log "Open http://localhost:3000  |  API http://localhost:8080/api/v1"
log "Press Ctrl+C to stop backend and frontend"

wait
