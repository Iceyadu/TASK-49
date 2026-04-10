#!/usr/bin/env bash
# API Test: Security boundaries — role/permission enforcement and error shapes
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

echo "=== Security & Authorization Tests ==="

ADMIN_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"Admin@12345678"}' \
    | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
[ -z "$ADMIN_TOKEN" ] && { echo "SKIP"; exit 0; }

# Ensure student exists
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
[ -z "$STU_TOKEN" ] && { echo "SKIP: no student token"; exit 0; }

# 1. No token at all
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/users")
assert_status "no-token-401" "401" "$STATUS"

# 2. Invalid token
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/users" \
    -H "Authorization: Bearer invalid.token.here")
assert_status "invalid-token-401" "401" "$STATUS"

# 3. Student cannot access admin endpoints
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/users" \
    -H "Authorization: Bearer $STU_TOKEN")
assert_status "student-cannot-list-users" "403" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/roles" \
    -H "Authorization: Bearer $STU_TOKEN")
assert_status "student-cannot-list-roles" "403" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/audit-logs" \
    -H "Authorization: Bearer $STU_TOKEN")
assert_status "student-cannot-view-audit" "403" "$STATUS"

# 4. Student cannot access curator endpoints
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/crawl-sources" \
    -H "Authorization: Bearer $STU_TOKEN")
assert_status "student-cannot-access-crawl" "403" "$STATUS"

# 5. Student cannot access instructor endpoints
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/question-banks" \
    -H "Authorization: Bearer $STU_TOKEN")
assert_status "student-cannot-access-question-banks" "403" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/quizzes" \
    -H "Authorization: Bearer $STU_TOKEN")
assert_status "student-cannot-list-quizzes" "403" "$STATUS"

# 6. Admin cannot access student schedule endpoints
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/schedules" \
    -H "Authorization: Bearer $ADMIN_TOKEN")
assert_status "admin-cannot-access-student-schedules" "403" "$STATUS"

# 7. Error response has consistent envelope
RESPONSE=$(curl -s -X GET "$BASE_URL/api/users" -H "Authorization: Bearer $STU_TOKEN")
if echo "$RESPONSE" | grep -q '"success":false'; then
    echo "  PASS  error-envelope-has-success-false"; PASS=$((PASS + 1))
else
    echo "  FAIL  error-envelope-has-success-false"; FAIL=$((FAIL + 1))
fi

if echo "$RESPONSE" | grep -q '"error"'; then
    echo "  PASS  error-envelope-has-error-field"; PASS=$((PASS + 1))
else
    echo "  FAIL  error-envelope-has-error-field"; FAIL=$((FAIL + 1))
fi

# 8. Sensitive data not in error responses
RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"wrongpassword1"}')
if echo "$RESPONSE" | grep -qi "password_hash\|bcrypt\|stacktrace"; then
    echo "  FAIL  sensitive-data-leaked-in-error"; FAIL=$((FAIL + 1))
else
    echo "  PASS  no-sensitive-data-in-error"; PASS=$((PASS + 1))
fi

echo ""
echo "Security tests: Pass=$PASS Fail=$FAIL"
[ "$FAIL" -eq 0 ]
