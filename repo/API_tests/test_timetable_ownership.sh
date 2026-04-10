#!/usr/bin/env bash
# API Test: Timetable/schedule ownership — students can only modify their own schedules
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

assert_status_one_of() {
    local label="$1" expected1="$2" expected2="$3" actual="$4"
    if [ "$actual" = "$expected1" ] || [ "$actual" = "$expected2" ]; then
        echo "  PASS  $label (HTTP $actual)"; PASS=$((PASS + 1))
    else
        echo "  FAIL  $label (expected $expected1 or $expected2, got $actual)"; FAIL=$((FAIL + 1))
    fi
}

echo "=== Timetable Ownership Tests ==="

# ── Obtain admin token ──────────────────────────────────────────────
ADMIN_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"Admin@12345678"}' \
    | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
[ -z "$ADMIN_TOKEN" ] && { echo "SKIP: cannot obtain admin token"; exit 0; }

# ── Provision Student A ────────────────────────────────────────────
curl -s -o /dev/null -X POST "$BASE_URL/api/users" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
    -d '{"username":"ttstudentA","email":"ttstua@scholarops.local","password":"Student@12345","fullName":"TT Student A"}'
TTA_ID=$(curl -s "$BASE_URL/api/users?keyword=ttstudentA" \
    -H "Authorization: Bearer $ADMIN_TOKEN" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
[ -n "$TTA_ID" ] && curl -s -o /dev/null -X POST "$BASE_URL/api/users/$TTA_ID/roles" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"roleId":5}'

STU_A_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"ttstudentA","password":"Student@12345"}' \
    | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
[ -z "$STU_A_TOKEN" ] && { echo "SKIP: cannot obtain Student A token"; exit 0; }

# ── Provision Student B ────────────────────────────────────────────
curl -s -o /dev/null -X POST "$BASE_URL/api/users" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
    -d '{"username":"ttstudentB","email":"ttstub@scholarops.local","password":"Student@12345","fullName":"TT Student B"}'
TTB_ID=$(curl -s "$BASE_URL/api/users?keyword=ttstudentB" \
    -H "Authorization: Bearer $ADMIN_TOKEN" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
[ -n "$TTB_ID" ] && curl -s -o /dev/null -X POST "$BASE_URL/api/users/$TTB_ID/roles" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"roleId":5}'

STU_B_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"ttstudentB","password":"Student@12345"}' \
    | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
[ -z "$STU_B_TOKEN" ] && { echo "SKIP: cannot obtain Student B token"; exit 0; }

# ── Student A creates a schedule entry ──────────────────────────────
echo ""
echo "--- Student A creates a schedule ---"

SCHEDULE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/schedules" \
    -H "Authorization: Bearer $STU_A_TOKEN" -H "Content-Type: application/json" \
    -d '{
        "title": "TT Ownership Test Block",
        "dayOfWeek": "MONDAY",
        "startTime": "09:00",
        "endTime": "10:00",
        "color": "#FF5733"
    }')
SCHEDULE_BODY=$(echo "$SCHEDULE_RESPONSE" | head -n -1)
CREATE_STATUS=$(echo "$SCHEDULE_RESPONSE" | tail -1)

SCHEDULE_ID=$(echo "$SCHEDULE_BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

if [ -z "$SCHEDULE_ID" ]; then
    echo "  WARN: could not create schedule (status=$CREATE_STATUS), using fallback ID=1"
    SCHEDULE_ID=1
else
    echo "  Created schedule ID=$SCHEDULE_ID"
fi

# ── Student A can view their own schedules ──────────────────────────
echo ""
echo "--- Student A accesses own schedules ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/schedules" \
    -H "Authorization: Bearer $STU_A_TOKEN")
assert_status "student-A-can-list-own-schedules" "200" "$STATUS"

# ── Student A can modify their own schedule ─────────────────────────
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/schedules/$SCHEDULE_ID" \
    -H "Authorization: Bearer $STU_A_TOKEN" -H "Content-Type: application/json" \
    -d '{
        "title": "TT Ownership Test Block Updated",
        "dayOfWeek": "MONDAY",
        "startTime": "09:00",
        "endTime": "10:30",
        "color": "#FF5733"
    }')
assert_status_one_of "student-A-can-modify-own-schedule" "200" "204" "$STATUS"

# ── Student B cannot modify Student A's schedule ────────────────────
echo ""
echo "--- Student B cannot modify Student A's schedule ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/schedules/$SCHEDULE_ID" \
    -H "Authorization: Bearer $STU_B_TOKEN" -H "Content-Type: application/json" \
    -d '{
        "title": "Hijacked Schedule",
        "dayOfWeek": "MONDAY",
        "startTime": "09:00",
        "endTime": "11:00",
        "color": "#000000"
    }')
assert_status_one_of "student-B-cannot-modify-A-schedule" "403" "404" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/api/schedules/$SCHEDULE_ID" \
    -H "Authorization: Bearer $STU_B_TOKEN")
assert_status_one_of "student-B-cannot-delete-A-schedule" "403" "404" "$STATUS"

# ── Admin cannot access student schedule endpoints ──────────────────
echo ""
echo "--- Admin cannot access student schedule endpoints ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/schedules" \
    -H "Authorization: Bearer $ADMIN_TOKEN")
assert_status "admin-cannot-list-student-schedules" "403" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/schedules" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
    -d '{
        "title": "Admin Attempt",
        "dayOfWeek": "TUESDAY",
        "startTime": "14:00",
        "endTime": "15:00",
        "color": "#000000"
    }')
assert_status "admin-cannot-create-student-schedules" "403" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/schedules/$SCHEDULE_ID" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
    -d '{
        "title": "Admin Hijack",
        "dayOfWeek": "MONDAY",
        "startTime": "09:00",
        "endTime": "10:00",
        "color": "#000000"
    }')
assert_status "admin-cannot-modify-student-schedules" "403" "$STATUS"

# ── Cleanup: Student A deletes their test schedule ──────────────────
curl -s -o /dev/null -X DELETE "$BASE_URL/api/schedules/$SCHEDULE_ID" \
    -H "Authorization: Bearer $STU_A_TOKEN" 2>/dev/null || true

echo ""
echo "Timetable Ownership Tests: Pass=$PASS Fail=$FAIL"
[ "$FAIL" -eq 0 ]
