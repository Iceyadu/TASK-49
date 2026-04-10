#!/usr/bin/env bash
# API Test: Sensitive data leakage — verify no internal details leak in responses
set -euo pipefail

BASE_URL="${API_BASE_URL:-http://localhost:8080}"
PASS=0; FAIL=0

assert_status() {
    local label="$1" expected="$2" actual="$3"
    if [ "$actual" = "$expected" ]; then
        echo "  PASS  $label (HTTP $actual)"; PASS=$((PASS + 1))
    else
        echo "  FAIL  $label (expected $expected, got $actual)"; FAIL=$((FAIL + 1))
    fi
}

assert_no_leak() {
    local label="$1" body="$2" pattern="$3"
    if echo "$body" | grep -qiE "$pattern"; then
        echo "  FAIL  $label (matched pattern: $pattern)"; FAIL=$((FAIL + 1))
    else
        echo "  PASS  $label"; PASS=$((PASS + 1))
    fi
}

assert_has() {
    local label="$1" body="$2" pattern="$3"
    if echo "$body" | grep -q "$pattern"; then
        echo "  PASS  $label"; PASS=$((PASS + 1))
    else
        echo "  FAIL  $label (missing: $pattern)"; FAIL=$((FAIL + 1))
    fi
}

echo "=== Sensitive Data Leakage Tests ==="

# ── Obtain admin token ──────────────────────────────────────────────
ADMIN_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"Admin@12345678"}' \
    | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
[ -z "$ADMIN_TOKEN" ] && { echo "SKIP: cannot obtain admin token"; exit 0; }

# ── Provision student for later tests ───────────────────────────────
curl -s -o /dev/null -X POST "$BASE_URL/api/users" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
    -d '{"username":"secstudent","email":"secstu@scholarops.local","password":"Student@12345","fullName":"Sec Student"}'
SSID=$(curl -s "$BASE_URL/api/users?keyword=secstudent" \
    -H "Authorization: Bearer $ADMIN_TOKEN" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
[ -n "$SSID" ] && curl -s -o /dev/null -X POST "$BASE_URL/api/users/$SSID/roles" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"roleId":5}'

STU_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"secstudent","password":"Student@12345"}' \
    | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
[ -z "$STU_TOKEN" ] && { echo "SKIP: cannot obtain student token"; exit 0; }

# ══════════════════════════════════════════════════════════════════════
# 1. Login failure does not expose sensitive internals
# ══════════════════════════════════════════════════════════════════════
echo ""
echo "--- Login failure response ---"

RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"wrongpassword1"}')

assert_no_leak "login-fail-no-password-hash" "$RESPONSE" "password_hash|passwordHash|\\\$2[aby]\\\$"
assert_no_leak "login-fail-no-bcrypt" "$RESPONSE" "bcrypt|\\$2a\\$|\\$2b\\$|\\$2y\\$"
assert_no_leak "login-fail-no-stack-trace" "$RESPONSE" "stackTrace|stack_trace|\.java:[0-9]+|at [a-z]+\.[a-z]+\."
assert_no_leak "login-fail-no-sql" "$RESPONSE" "SELECT |INSERT |UPDATE |DELETE |FROM |WHERE |jdbc:"
assert_no_leak "login-fail-no-internal-path" "$RESPONSE" "/home/|/opt/|/var/|/usr/|C:\\\\|src/main"

# ══════════════════════════════════════════════════════════════════════
# 2. 401 response does not expose internal details
# ══════════════════════════════════════════════════════════════════════
echo ""
echo "--- 401 response content ---"

RESPONSE=$(curl -s -X GET "$BASE_URL/api/users")

assert_no_leak "401-no-stack-trace" "$RESPONSE" "stackTrace|stack_trace|\.java:[0-9]+|at [a-z]+\.[a-z]+\."
assert_no_leak "401-no-internal-path" "$RESPONSE" "/home/|/opt/|/var/|/usr/|C:\\\\|src/main"
assert_no_leak "401-no-server-details" "$RESPONSE" "Spring Boot|Tomcat|Jetty|Netty|X-Powered-By"

# ══════════════════════════════════════════════════════════════════════
# 3. 403 response does not expose internal details
# ══════════════════════════════════════════════════════════════════════
echo ""
echo "--- 403 response content ---"

RESPONSE=$(curl -s -X GET "$BASE_URL/api/users" \
    -H "Authorization: Bearer $STU_TOKEN")

assert_no_leak "403-no-stack-trace" "$RESPONSE" "stackTrace|stack_trace|\.java:[0-9]+|at [a-z]+\.[a-z]+\."
assert_no_leak "403-no-internal-path" "$RESPONSE" "/home/|/opt/|/var/|/usr/|C:\\\\|src/main"
assert_no_leak "403-no-sql" "$RESPONSE" "SELECT |INSERT |UPDATE |DELETE |FROM |WHERE |jdbc:"
assert_no_leak "403-no-class-names" "$RESPONSE" "\.controller\.|\.service\.|\.repository\.|\.config\."

# ══════════════════════════════════════════════════════════════════════
# 4. Error responses follow consistent envelope format
# ══════════════════════════════════════════════════════════════════════
echo ""
echo "--- Error envelope consistency ---"

# 401 envelope
RESPONSE_401=$(curl -s -X GET "$BASE_URL/api/users")
assert_has "401-envelope-has-success-false" "$RESPONSE_401" '"success":false'
assert_has "401-envelope-has-error-field" "$RESPONSE_401" '"error"'

# 403 envelope
RESPONSE_403=$(curl -s -X GET "$BASE_URL/api/users" \
    -H "Authorization: Bearer $STU_TOKEN")
assert_has "403-envelope-has-success-false" "$RESPONSE_403" '"success":false'
assert_has "403-envelope-has-error-field" "$RESPONSE_403" '"error"'

# Login failure envelope
RESPONSE_LOGIN_FAIL=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"wrongpassword1"}')
assert_has "login-fail-envelope-has-success-false" "$RESPONSE_LOGIN_FAIL" '"success":false'
assert_has "login-fail-envelope-has-error-field" "$RESPONSE_LOGIN_FAIL" '"error"'

# ══════════════════════════════════════════════════════════════════════
# 5. User list responses do not include passwordHash field
# ══════════════════════════════════════════════════════════════════════
echo ""
echo "--- User list does not leak password hashes ---"

USER_LIST=$(curl -s -X GET "$BASE_URL/api/users" \
    -H "Authorization: Bearer $ADMIN_TOKEN")

assert_no_leak "user-list-no-passwordHash" "$USER_LIST" "passwordHash|password_hash"
assert_no_leak "user-list-no-bcrypt-value" "$USER_LIST" "\\\$2[aby]\\\$[0-9]+\\\$"
assert_no_leak "user-list-no-password-field" "$USER_LIST" '"password":'

# ── Check individual user response too ──────────────────────────────
if [ -n "$SSID" ]; then
    USER_DETAIL=$(curl -s -X GET "$BASE_URL/api/users/$SSID" \
        -H "Authorization: Bearer $ADMIN_TOKEN")
    assert_no_leak "user-detail-no-passwordHash" "$USER_DETAIL" "passwordHash|password_hash"
    assert_no_leak "user-detail-no-bcrypt-value" "$USER_DETAIL" "\\\$2[aby]\\\$[0-9]+\\\$"
    assert_no_leak "user-detail-no-password-field" "$USER_DETAIL" '"password":'
fi

# ══════════════════════════════════════════════════════════════════════
# 6. Invalid token response does not leak internals
# ══════════════════════════════════════════════════════════════════════
echo ""
echo "--- Invalid token response ---"

RESPONSE=$(curl -s -X GET "$BASE_URL/api/users" \
    -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.fake.payload")

assert_no_leak "invalid-token-no-stack-trace" "$RESPONSE" "stackTrace|stack_trace|\.java:[0-9]+"
assert_no_leak "invalid-token-no-secret-key" "$RESPONSE" "secret|signing.key|jwt.secret"
assert_no_leak "invalid-token-no-internal-path" "$RESPONSE" "/home/|/opt/|/var/|/usr/|src/main"

# ══════════════════════════════════════════════════════════════════════
# 7. Server headers do not leak technology stack
# ══════════════════════════════════════════════════════════════════════
echo ""
echo "--- Response headers ---"

HEADERS=$(curl -s -I -X GET "$BASE_URL/api/users")

if echo "$HEADERS" | grep -qi "X-Powered-By"; then
    echo "  FAIL  headers-no-x-powered-by (header present)"; FAIL=$((FAIL + 1))
else
    echo "  PASS  headers-no-x-powered-by"; PASS=$((PASS + 1))
fi

if echo "$HEADERS" | grep -qi "Server:.*Tomcat\|Server:.*Jetty\|Server:.*Spring"; then
    echo "  FAIL  headers-no-server-details (server name leaked)"; FAIL=$((FAIL + 1))
else
    echo "  PASS  headers-no-server-details"; PASS=$((PASS + 1))
fi

echo ""
echo "Sensitive Data Leakage Tests: Pass=$PASS Fail=$FAIL"
[ "$FAIL" -eq 0 ]
