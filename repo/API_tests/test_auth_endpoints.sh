#!/usr/bin/env bash
# API Test: Authentication endpoints
set -euo pipefail

BASE_URL="${API_BASE_URL:-http://localhost:8080}"
PASS=0; FAIL=0

assert_status() {
    local label="$1" expected="$2" actual="$3"
    if [ "$actual" = "$expected" ]; then
        echo "  PASS  $label (HTTP $actual)"
        PASS=$((PASS + 1))
    else
        echo "  FAIL  $label (expected $expected, got $actual)"
        FAIL=$((FAIL + 1))
    fi
}

echo "=== Auth Endpoint Tests ==="

# 1. Login with empty body returns 400
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" -d '{}')
assert_status "login-empty-body" "400" "$STATUS"

# 2. Login with invalid credentials returns 401
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" -d '{"username":"invalid","password":"Wrong@Pass1234"}')
assert_status "login-invalid-creds" "401" "$STATUS"

# 3. Login with valid admin credentials returns 200
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" -d '{"username":"admin","password":"Admin@12345678"}')
STATUS=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | head -n -1)
assert_status "login-valid-admin" "200" "$STATUS"

# 4. Response contains accessToken
if echo "$BODY" | grep -q "accessToken"; then
    echo "  PASS  login-response-has-token"
    PASS=$((PASS + 1))
else
    echo "  FAIL  login-response-has-token"
    FAIL=$((FAIL + 1))
fi

# 5. Response contains roles
if echo "$BODY" | grep -q "ADMINISTRATOR"; then
    echo "  PASS  login-response-has-admin-role"
    PASS=$((PASS + 1))
else
    echo "  FAIL  login-response-has-admin-role"
    FAIL=$((FAIL + 1))
fi

# 6. Token refresh with invalid token returns 401
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/auth/refresh" \
    -H "Content-Type: application/json" -d '{"refreshToken":"invalid-token"}')
assert_status "refresh-invalid-token" "401" "$STATUS"

# 7. Logout returns 200 with valid token
TOKEN=$(echo "$BODY" | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
if [ -n "$TOKEN" ]; then
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/auth/logout" \
        -H "Authorization: Bearer $TOKEN")
    assert_status "logout-with-token" "200" "$STATUS"
fi

echo ""
echo "Auth tests: Pass=$PASS Fail=$FAIL"
[ "$FAIL" -eq 0 ]
