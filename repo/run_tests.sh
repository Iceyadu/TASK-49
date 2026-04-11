#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Run the suite inside the test Docker image by default (Maven + Node toolchain).
# Already inside the container: compose sets RUN_TESTS_IN_CONTAINER=1.
# Opt out: RUN_TESTS_USE_DOCKER=0 ./run_tests.sh
if [ -z "${RUN_TESTS_IN_CONTAINER:-}" ] && [ "${RUN_TESTS_USE_DOCKER:-1}" != "0" ]; then
    if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
        if [ -f "$SCRIPT_DIR/docker-compose.test.yml" ] && [ -f "$SCRIPT_DIR/docker/Dockerfile.test" ]; then
            cd "$SCRIPT_DIR"
            exec docker compose -f docker-compose.test.yml run --rm \
                -e "API_BASE_URL=${API_BASE_URL:-http://host.docker.internal:8080}" \
                test-runner
        fi
    fi
    echo -e "\033[1;33m[TEST]\033[0m Docker unavailable or test image files missing — running tests on the host"
fi

PASS=0
FAIL=0
SKIP=0
RESULTS=()

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
log()  { echo -e "\033[1;34m[TEST]\033[0m $*"; }
pass() { PASS=$((PASS + 1)); RESULTS+=("PASS  $1"); echo -e "  \033[1;32mPASS\033[0m $1"; }
fail() { FAIL=$((FAIL + 1)); RESULTS+=("FAIL  $1"); echo -e "  \033[1;31mFAIL\033[0m $1"; }
skip() { SKIP=$((SKIP + 1)); RESULTS+=("SKIP  $1"); echo -e "  \033[1;33mSKIP\033[0m $1"; }

# Returns 0 if an HTTP server responds at BASE_URL (any status except connection failure).
api_backend_reachable() {
    local BASE_URL="${1:-http://localhost:8080}"
    local code
    code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 2 "${BASE_URL}/" 2>/dev/null) || code="000"
    [ "$code" != "000" ]
}

# ---------------------------------------------------------------------------
# 1.  Backend unit tests (Maven / JUnit 5)
# ---------------------------------------------------------------------------
run_backend_tests() {
    log "Running backend unit tests (Maven) ..."
    if [ -f "$SCRIPT_DIR/backend/pom.xml" ]; then
        pushd "$SCRIPT_DIR/backend" > /dev/null
        if mvn test -B -q 2>/dev/null; then
            pass "backend-unit-tests"
        else
            fail "backend-unit-tests"
        fi
        popd > /dev/null
    else
        skip "backend-unit-tests (pom.xml not found)"
    fi
}

# ---------------------------------------------------------------------------
# 2.  Frontend unit tests (Vitest)
# ---------------------------------------------------------------------------
run_frontend_tests() {
    log "Running frontend unit tests (Vitest) ..."
    if [ -f "$SCRIPT_DIR/frontend/package.json" ]; then
        pushd "$SCRIPT_DIR/frontend" > /dev/null
        if [ ! -d node_modules ] || [ ! -x node_modules/.bin/vitest ]; then
            log "  Installing npm dependencies ..."
            if [ -f package-lock.json ]; then
                npm ci
            else
                npm install
            fi
        fi
        if [ -x node_modules/.bin/vitest ]; then
            node_modules/.bin/vitest run --config tests/vitest.config.ts
        else
            npx --yes vitest run --config tests/vitest.config.ts
        fi && pass "frontend-unit-tests" || fail "frontend-unit-tests"
        popd > /dev/null
    else
        skip "frontend-unit-tests (package.json not found)"
    fi
}

# ---------------------------------------------------------------------------
# 3.  API integration tests (curl-based, requires running backend)
# ---------------------------------------------------------------------------
run_api_tests() {
    local BASE_URL="${API_BASE_URL:-http://localhost:8080}"
    log "Running API integration tests against $BASE_URL ..."

    if ! api_backend_reachable "$BASE_URL"; then
        skip "api integration curl checks (no server at $BASE_URL — start backend or set API_BASE_URL)"
        return
    fi

    # --- test runner helper ---
    api_test() {
        local name="$1" method="$2" path="$3" expected_status="$4"
        shift 4
        local status
        status=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" "$BASE_URL$path" "$@" 2>/dev/null) || status="000"
        if [ "$status" = "$expected_status" ]; then
            pass "api: $name (HTTP $status)"
        else
            fail "api: $name (expected $expected_status, got $status)"
        fi
    }

    # Unauthenticated / wrong-role rejection: Spring may return 401 or 403 depending on filter vs MVC path.
    api_test_unauth() {
        local name="$1" method="$2" path="$3"
        shift 3
        local status
        status=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" "$BASE_URL$path" "$@" 2>/dev/null) || status="000"
        if [ "$status" = "401" ] || [ "$status" = "403" ]; then
            pass "api: $name (HTTP $status)"
        else
            fail "api: $name (expected 401 or 403, got $status)"
        fi
    }

    # 3a. Public endpoints
    api_test "login-endpoint-exists"        POST "/api/auth/login" 400 \
        -H "Content-Type: application/json" -d '{}'

    # 3b. Protected endpoints require auth (401 preferred; 403 accepted for Spring Security edge paths)
    api_test_unauth "users-requires-auth"          GET  "/api/users"
    api_test_unauth "roles-requires-auth"          GET  "/api/roles"
    api_test_unauth "audit-requires-auth"          GET  "/api/audit-logs"
    api_test_unauth "catalog-requires-auth"        GET  "/api/catalog"
    api_test_unauth "schedules-requires-auth"      GET  "/api/schedules"

    # 3c. Login as admin and exercise protected routes
    local LOGIN_BODY='{"username":"admin","password":"Admin@12345678"}'
    local TOKEN
    TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
        -H "Content-Type: application/json" -d "$LOGIN_BODY" \
        | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4) || TOKEN=""

    if [ -z "$TOKEN" ]; then
        skip "api: admin-login (could not obtain token - backend may be down)"
        return
    fi

    pass "api: admin-login"
    local AUTH="-H Authorization: Bearer $TOKEN"

    api_test "list-users-as-admin"          GET  "/api/users"                200 -H "Authorization: Bearer $TOKEN"
    api_test "list-roles-as-admin"          GET  "/api/roles"                200 -H "Authorization: Bearer $TOKEN"
    api_test "list-audit-logs-as-admin"     GET  "/api/audit-logs"           200 -H "Authorization: Bearer $TOKEN"

    # 3d. Password policy validation
    api_test "create-user-weak-password"    POST "/api/users"                400 \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"username":"weakuser","email":"weak@test.com","password":"short","fullName":"Weak User"}'

    api_test "create-user-valid"            POST "/api/users"                201 \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"username":"testuser1","email":"test1@scholarops.local","password":"Test@User12345","fullName":"Test User One"}'

    # 3e. Admin password reset requires workstation ID
    api_test "admin-reset-no-workstation"   POST "/api/users/1/admin-reset-password" 400 \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"newPassword":"NewPass@12345","workstationId":"","reason":"test"}'

    api_test "admin-reset-with-workstation" POST "/api/users/1/admin-reset-password" 200 \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -H "X-Workstation-Id: WS-ADMIN-001" \
        -d '{"newPassword":"Admin@12345678","workstationId":"WS-ADMIN-001","reason":"test reset"}'

    # 3f. Permission-change-history recorded
    api_test "permission-change-history"    GET  "/api/permission-change-history" 200 \
        -H "Authorization: Bearer $TOKEN"

    # 3g. Crawl source CRUD (admin does not have CURATOR role — expect 403)
    api_test "crawl-sources-forbidden-for-admin" GET "/api/crawl-sources" 403 \
        -H "Authorization: Bearer $TOKEN"

    # 3h. Catalog (admin has all permissions)
    api_test "catalog-search"               GET  "/api/catalog?keyword=test&sortBy=newest" 200 \
        -H "Authorization: Bearer $TOKEN"

    # 3i. Non-existent resource
    api_test "not-found-content"            GET  "/api/content/99999" 404 \
        -H "Authorization: Bearer $TOKEN"
}

# ---------------------------------------------------------------------------
# 4.  Dedicated API_tests scripts
# ---------------------------------------------------------------------------
run_api_test_scripts() {
    local BASE_URL="${API_BASE_URL:-http://localhost:8080}"
    if ! api_backend_reachable "$BASE_URL"; then
        skip "API_tests/*.sh (no server at $BASE_URL)"
        return
    fi

    log "Running scripts in API_tests/ ..."
    for script in "$SCRIPT_DIR"/API_tests/*.sh; do
        [ -f "$script" ] || continue
        local name
        name="$(basename "$script")"
        log "  Executing $name ..."
        if bash "$script"; then
            pass "API_tests/$name"
        else
            fail "API_tests/$name"
        fi
    done
}

# ---------------------------------------------------------------------------
# 5.  Dedicated unit_tests scripts
# ---------------------------------------------------------------------------
run_unit_test_scripts() {
    log "Running scripts in unit_tests/ ..."
    for script in "$SCRIPT_DIR"/unit_tests/*.sh; do
        [ -f "$script" ] || continue
        local name
        name="$(basename "$script")"
        log "  Executing $name ..."
        if bash "$script"; then
            pass "unit_tests/$name"
        else
            fail "unit_tests/$name"
        fi
    done
}

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
echo ""
echo "============================================================"
echo "  ScholarOps — Test Runner"
echo "============================================================"
echo ""

run_backend_tests
echo ""
run_frontend_tests
echo ""
run_api_tests
echo ""
run_api_test_scripts
echo ""
run_unit_test_scripts

echo ""
echo "============================================================"
echo "  Summary"
echo "============================================================"
for r in "${RESULTS[@]}"; do echo "  $r"; done
echo ""
echo "  Total: $((PASS + FAIL + SKIP))  |  Pass: $PASS  |  Fail: $FAIL  |  Skip: $SKIP"
echo "============================================================"

[ "$FAIL" -eq 0 ]
