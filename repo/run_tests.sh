#!/bin/bash
# ScholarOps - Docker-based Test Runner
# Usage: ./run_tests.sh [backend|frontend|api|e2e|all]
# Runs backend + frontend + API + browser E2E tests from one repository tree.
#
# Test layout: unit_tests/ (Vitest specs), e2e/ (Playwright), api_tests/ (marker;
# API/integration suites run via Maven in backend/ — see run_api_tests).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$SCRIPT_DIR"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"
MYSQL_STARTED_BY_SCRIPT=0
OVERALL_FAIL=0
E2E_BACKEND_CONTAINER="scholarops-e2e-backend"
E2E_FRONTEND_CONTAINER="scholarops-e2e-frontend"
E2E_NETWORK=""

require_docker() {
    if ! command -v docker >/dev/null 2>&1; then
        echo "docker not found; install Docker Desktop / Engine first"
        exit 127
    fi
    if ! docker compose version >/dev/null 2>&1; then
        echo "docker compose not available; install Docker Compose v2 plugin"
        exit 127
    fi
}

ensure_mysql() {
    if docker compose -f "$ROOT_DIR/docker-compose.yml" ps --status running --services | rg -q "^mysql$"; then
        return
    fi

    echo "=== Starting MySQL test dependency (docker compose up -d mysql) ==="
    docker compose -f "$ROOT_DIR/docker-compose.yml" up -d mysql
    MYSQL_STARTED_BY_SCRIPT=1

    local max_attempts=60
    local attempt=1
    while [ "$attempt" -le "$max_attempts" ]; do
        if docker compose -f "$ROOT_DIR/docker-compose.yml" exec -T mysql \
            mysqladmin ping -h localhost -u root -p"${MYSQL_ROOT_PASSWORD:-root_secret}" --silent >/dev/null 2>&1; then
            echo "MySQL is ready."
            return
        fi
        sleep 2
        attempt=$((attempt + 1))
    done

    echo "MySQL did not become ready in time." >&2
    docker compose -f "$ROOT_DIR/docker-compose.yml" logs mysql --tail 80 >&2 || true
    exit 1
}

cleanup_mysql() {
    if [ "$MYSQL_STARTED_BY_SCRIPT" -eq 1 ]; then
        docker compose -f "$ROOT_DIR/docker-compose.yml" stop mysql >/dev/null 2>&1 || true
    fi
}

cleanup_e2e_containers() {
    docker rm -f "$E2E_FRONTEND_CONTAINER" >/dev/null 2>&1 || true
    docker rm -f "$E2E_BACKEND_CONTAINER" >/dev/null 2>&1 || true
}

cleanup_all() {
    cleanup_e2e_containers
    cleanup_mysql
}

wait_for_http() {
    local url="$1"
    local expected_regex="$2"
    local max_attempts="${3:-90}"
    local label="$4"

    local attempt=1
    while [ "$attempt" -le "$max_attempts" ]; do
        local code
        code="$(curl -s -o /dev/null -w "%{http_code}" "$url" || echo "000")"
        if [[ "$code" =~ $expected_regex ]]; then
            echo "$label is ready (HTTP $code)."
            return
        fi
        sleep 2
        attempt=$((attempt + 1))
    done

    echo "$label did not become ready in time ($url)." >&2
    exit 1
}

resolve_mysql_network() {
    E2E_NETWORK="$(docker inspect scholarops-mysql --format '{{range $k, $_ := .NetworkSettings.Networks}}{{println $k}}{{end}}' | tr -d '\r' | head -n 1)"
    if [ -z "$E2E_NETWORK" ]; then
        echo "Unable to resolve MySQL Docker network." >&2
        exit 1
    fi
}

wait_for_http_in_network() {
    local url="$1"
    local expected_regex="$2"
    local max_attempts="${3:-90}"
    local label="$4"

    local attempt=1
    while [ "$attempt" -le "$max_attempts" ]; do
        local code
        code="$(docker run --rm --network "$E2E_NETWORK" curlimages/curl:8.7.1 -s -o /dev/null -w "%{http_code}" "$url" || echo "000")"
        if [[ "$code" =~ $expected_regex ]]; then
            echo "$label is ready (HTTP $code)."
            return
        fi
        sleep 2
        attempt=$((attempt + 1))
    done

    echo "$label did not become ready in time ($url)." >&2
    exit 1
}

run_backend_tests() {
    echo "=== Running Backend Unit Tests (Docker, backend/src/test) ==="
    ensure_mysql
    if ! docker run --rm \
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
        mvn test --no-transfer-progress 2>&1; then
        echo "Backend unit tests completed with failures"
        OVERALL_FAIL=1
    fi
    echo ""
}

run_frontend_tests() {
    echo "=== Running Frontend Unit Tests (Docker, unit_tests/) ==="
    if ! docker run --rm \
        -v "$ROOT_DIR:/repo" \
        -w /repo/frontend \
        node:20-alpine \
        sh -lc "npm ci && npx vitest run --reporter=verbose" 2>&1; then
        echo "Frontend tests completed with failures"
        OVERALL_FAIL=1
    fi
    echo ""
}

run_api_tests() {
    echo "=== Running API + Controller Tests (Docker, boundary-only suites) ==="
    ensure_mysql
    if ! docker run --rm \
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
        mvn test -Pall-backend-tests \
        -Dtest='com.scholarops.controller.**,com.scholarops.integration.AuthIntegrationTest,com.scholarops.integration.RealApiBoundaryIntegrationTest,com.scholarops.integration.SubmissionApiIntegrationTest,com.scholarops.integration.TimetableApiIntegrationTest,com.scholarops.integration.AdminWorkstationIntegrationTest,com.scholarops.integration.CrawlWorkflowIntegrationTest' \
        --no-transfer-progress 2>&1; then
        echo "API/controller tests completed with failures"
        OVERALL_FAIL=1
    fi
    echo ""
}

run_e2e_tests() {
    echo "=== Running Browser E2E Tests (Playwright, real frontend + backend) ==="
    ensure_mysql
    cleanup_e2e_containers
    resolve_mysql_network

    docker run -d \
        --name "$E2E_BACKEND_CONTAINER" \
        --network "$E2E_NETWORK" \
        -v "$ROOT_DIR:/repo" \
        -w /repo/backend \
        -e DB_HOST=mysql \
        -e DB_PORT=3306 \
        -e DB_NAME=scholarops \
        -e DB_USERNAME=scholarops \
        -e DB_PASSWORD="${DB_PASSWORD:-scholarops_secret}" \
        -e JWT_SECRET="${JWT_SECRET:-c2Nob2xhcm9wcy1vZmZsaW5lLWxlYXJuaW5nLXN5c3RlbS1zZWNyZXQta2V5LW11c3QtYmUtYXQtbGVhc3QtMjU2LWJpdHM=}" \
        -e SCHOLAROPS_AES_KEY="${AES_KEY:-0123456789abcdef0123456789abcdef}" \
        maven:3.9.6-eclipse-temurin-17 \
        sh -lc "mvn -q spring-boot:run"

    wait_for_http_in_network "http://$E2E_BACKEND_CONTAINER:8080/api/auth/login" "^(400|401|403|405)$" 120 "Backend"

    docker run -d \
        --name "$E2E_FRONTEND_CONTAINER" \
        -p 5173:5173 \
        --network "$E2E_NETWORK" \
        -v "$ROOT_DIR:/repo" \
        -w /repo/frontend \
        -e VITE_API_BASE_URL=http://$E2E_BACKEND_CONTAINER:8080 \
        node:20-alpine \
        sh -lc "npm ci && npm run dev -- --host 0.0.0.0 --port 5173"

    wait_for_http_in_network "http://$E2E_FRONTEND_CONTAINER:5173/login" "^(200|304)$" 120 "Frontend"

    if ! docker run --rm \
        --network "$E2E_NETWORK" \
        -v "$ROOT_DIR:/repo" \
        -w /repo/frontend \
        -e E2E_BASE_URL=http://$E2E_FRONTEND_CONTAINER:5173 \
        -e E2E_API_BASE=http://$E2E_BACKEND_CONTAINER:8080 \
        mcr.microsoft.com/playwright:v1.52.0-jammy \
        sh -lc "npx -y playwright@1.52.0 test --config ../e2e/playwright.config.ts --reporter=line"; then
        echo "Browser E2E tests completed with failures"
        OVERALL_FAIL=1
    fi

    echo ""
}

require_docker
trap cleanup_all EXIT

case "${1:-all}" in
    backend)
        run_backend_tests
        ;;
    frontend)
        run_frontend_tests
        ;;
    api)
        run_api_tests
        ;;
    e2e)
        run_e2e_tests
        ;;
    all)
        run_backend_tests
        run_frontend_tests
        run_api_tests
        run_e2e_tests
        echo "=== All Tests Complete ==="
        ;;
    *)
        echo "Usage: $0 [backend|frontend|api|e2e|all]"
        exit 1
        ;;
esac

if [ "$OVERALL_FAIL" -ne 0 ]; then
    exit 1
fi
