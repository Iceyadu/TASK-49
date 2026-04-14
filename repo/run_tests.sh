#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

if ! command -v docker >/dev/null 2>&1; then
  echo "[ERROR] docker is required to run tests." >&2
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "[ERROR] docker daemon is not running or not accessible." >&2
  exit 1
fi

if [ ! -f "$SCRIPT_DIR/docker-compose.yml" ] || [ ! -f "$SCRIPT_DIR/docker/Dockerfile.test" ]; then
  echo "[ERROR] docker test runner files are missing (docker-compose.yml / docker/Dockerfile.test)." >&2
  exit 1
fi

cd "$SCRIPT_DIR"
exec docker compose -f docker-compose.yml run --rm \
  -e "API_BASE_URL=${API_BASE_URL:-http://host.docker.internal:8080}" \
  test-runner
