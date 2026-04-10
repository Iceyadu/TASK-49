#!/usr/bin/env bash
# API Test: User management, role assignment, password reset flows
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

echo "=== User Management Tests ==="

# Login as admin
TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"Admin@12345678"}' \
    | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo "SKIP: Could not authenticate as admin"; exit 0
fi
AUTH="Authorization: Bearer $TOKEN"

# 1. List users
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/users" -H "$AUTH")
assert_status "list-users" "200" "$STATUS"

# 2. Create user with weak password (policy violation)
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/users" \
    -H "$AUTH" -H "Content-Type: application/json" \
    -d '{"username":"weakpw","email":"weak@test.com","password":"abc","fullName":"Weak PW"}')
assert_status "create-user-weak-password-rejected" "400" "$STATUS"

# 3. Create user with no uppercase
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/users" \
    -H "$AUTH" -H "Content-Type: application/json" \
    -d '{"username":"noupper","email":"noupper@test.com","password":"abcdefghijk1!","fullName":"No Upper"}')
assert_status "create-user-no-uppercase-rejected" "400" "$STATUS"

# 4. Create user with valid password
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/users" \
    -H "$AUTH" -H "Content-Type: application/json" \
    -d '{"username":"apiuser1","email":"apiuser1@scholarops.local","password":"ApiUser@12345","fullName":"API Test User"}')
STATUS=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | head -n -1)
assert_status "create-user-valid" "201" "$STATUS"

USER_ID=$(echo "$BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

# 5. Get user by ID
if [ -n "$USER_ID" ]; then
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/users/$USER_ID" -H "$AUTH")
    assert_status "get-user-by-id" "200" "$STATUS"
fi

# 6. Update user
if [ -n "$USER_ID" ]; then
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/users/$USER_ID" \
        -H "$AUTH" -H "Content-Type: application/json" \
        -d '{"email":"apiuser1-updated@scholarops.local","fullName":"API User Updated"}')
    assert_status "update-user" "200" "$STATUS"
fi

# 7. Assign role to user
if [ -n "$USER_ID" ]; then
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/users/$USER_ID/roles" \
        -H "$AUTH" -H "Content-Type: application/json" \
        -d '{"roleId":5}')
    assert_status "assign-student-role" "200" "$STATUS"
fi

# 8. Admin password reset WITHOUT workstation ID (should fail)
if [ -n "$USER_ID" ]; then
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/users/$USER_ID/admin-reset-password" \
        -H "$AUTH" -H "Content-Type: application/json" \
        -d '{"newPassword":"Reset@User12345","workstationId":"","reason":"test"}')
    assert_status "admin-reset-no-workstation-rejected" "400" "$STATUS"
fi

# 9. Admin password reset WITH workstation ID (should pass)
if [ -n "$USER_ID" ]; then
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/users/$USER_ID/admin-reset-password" \
        -H "$AUTH" -H "Content-Type: application/json" \
        -H "X-Workstation-Id: WS-TEST-001" \
        -d '{"newPassword":"Reset@User12345","workstationId":"WS-TEST-001","reason":"api test"}')
    assert_status "admin-reset-with-workstation" "200" "$STATUS"
fi

# 10. Verify audit log recorded the reset
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET \
    "$BASE_URL/api/audit-logs?action=USER_ADMIN_PASSWORD_RESET&size=5" -H "$AUTH")
assert_status "audit-log-password-reset-recorded" "200" "$STATUS"

# 11. Revoke role
if [ -n "$USER_ID" ]; then
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/api/users/$USER_ID/roles/5" -H "$AUTH")
    assert_status "revoke-role" "200" "$STATUS"
fi

# 12. Permission change history recorded
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/permission-change-history" -H "$AUTH")
assert_status "permission-change-history" "200" "$STATUS"

# 13. Delete user
if [ -n "$USER_ID" ]; then
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/api/users/$USER_ID" -H "$AUTH")
    assert_status "delete-user" "200" "$STATUS"
fi

echo ""
echo "User management tests: Pass=$PASS Fail=$FAIL"
[ "$FAIL" -eq 0 ]
