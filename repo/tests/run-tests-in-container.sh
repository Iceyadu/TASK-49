#!/usr/bin/env bash
# ScholarOps - Docker-based test runner for backend + frontend.
# Requires Docker and a running MySQL instance (start with: docker compose up -d mysql).
# Usage: ./tests/run-tests-in-container.sh [backend|frontend|all]
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PASS=0
FAIL=0
RESULTS=()

log()  { echo -e "\033[1;34m[TEST]\033[0m $*"; }
pass() { PASS=$((PASS + 1)); RESULTS+=("PASS  $1"); echo -e "  \033[1;32mPASS\033[0m $1"; }
fail() { FAIL=$((FAIL + 1)); RESULTS+=("FAIL  $1"); echo -e "  \033[1;31mFAIL\033[0m $1"; }

require_docker() {
    if ! command -v docker >/dev/null 2>&1; then
        echo "docker not found; install Docker Desktop / Engine first" >&2
        exit 127
    fi
}

run_backend_tests() {
    log "Running backend unit + integration tests (Docker / Maven) ..."
    if docker run --rm \
        -v "$ROOT_DIR:/repo" \
        -w /repo/backend \
        -e DB_HOST=host.docker.internal \
        -e DB_PORT=3306 \
        -e DB_NAME=scholarops \
        -e DB_USERNAME=scholarops \
        -e DB_PASSWORD="${DB_PASSWORD:-scholarops_secret}" \
        -e JWT_SECRET="${JWT_SECRET:-c2Nob2xhcm9wcy1vZmZsaW5lLWxlYXJuaW5nLXN5c3RlbS1zZWNyZXQta2V5LW11c3QtYmUtYXQtbGVhc3QtMjU2LWJpdHM=}" \
        -e SCHOLAROPS_AES_KEY="${AES_KEY:-0123456789abcdef0123456789abcdef}" \
        --add-host host.docker.internal:host-gateway \
        maven:3.9.6-eclipse-temurin-17 \
        mvn test --no-transfer-progress; then
        pass "backend-docker-tests"
    else
        fail "backend-docker-tests"
    fi
}

run_frontend_tests() {
    log "Running frontend unit tests (Docker / Vitest) ..."
    if docker run --rm \
        -v "$ROOT_DIR:/repo" \
        -w /repo/frontend \
        node:20-alpine \
        sh -lc "npm ci && npx vitest run --reporter=verbose"; then
        pass "frontend-docker-tests"
    else
        fail "frontend-docker-tests"
    fi
}

require_docker

echo ""
echo "============================================================"
echo "  ScholarOps — Docker-based Test Runner (backend + frontend)"
echo "============================================================"
echo ""

case "${1:-all}" in
    backend)
        run_backend_tests
        ;;
    frontend)
        run_frontend_tests
        ;;
    all)
        run_backend_tests
        echo ""
        run_frontend_tests
        echo ""
        ;;
    *)
        echo "Usage: $0 [backend|frontend|all]"
        exit 1
        ;;
esac

echo "============================================================"
echo "  Summary"
echo "============================================================"
for r in "${RESULTS[@]}"; do echo "  $r"; done
echo ""
echo "  Total: $((PASS + FAIL))  |  Pass: $PASS  |  Fail: $FAIL"
echo "============================================================"

[ "$FAIL" -eq 0 ]
