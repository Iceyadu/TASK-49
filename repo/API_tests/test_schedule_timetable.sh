#!/usr/bin/env bash
# API Test: Schedule/timetable CRUD, locked periods, merge/split, undo/redo
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

echo "=== Schedule & Timetable Tests ==="

# Login as admin, create student if needed
ADMIN_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"Admin@12345678"}' \
    | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
[ -z "$ADMIN_TOKEN" ] && { echo "SKIP"; exit 0; }

# Ensure student2 exists
curl -s -o /dev/null -X POST "$BASE_URL/api/users" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
    -d '{"username":"student2","email":"stu2@scholarops.local","password":"Student@12345","fullName":"Schedule Student"}'
STU2_ID=$(curl -s "$BASE_URL/api/users?keyword=student2" \
    -H "Authorization: Bearer $ADMIN_TOKEN" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
[ -n "$STU2_ID" ] && curl -s -o /dev/null -X POST "$BASE_URL/api/users/$STU2_ID/roles" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"roleId":5}'

STU_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"student2","password":"Student@12345"}' \
    | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
[ -z "$STU_TOKEN" ] && { echo "SKIP: Cannot auth student2"; exit 0; }
AUTH="Authorization: Bearer $STU_TOKEN"

# 1. Create schedule
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/schedules" \
    -H "$AUTH" -H "Content-Type: application/json" \
    -d '{"title":"Study Session","startTime":"2026-04-15T09:00:00","endTime":"2026-04-15T10:00:00","dayOfWeek":3,"color":"#3182ce"}')
STATUS=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | head -n -1)
assert_status "create-schedule" "201" "$STATUS"
SCHED_ID=$(echo "$BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

# 2. List schedules
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/schedules" -H "$AUTH")
assert_status "list-schedules" "200" "$STATUS"

# 3. Move schedule (drag-and-drop)
if [ -n "$SCHED_ID" ]; then
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/schedules/$SCHED_ID/move" \
        -H "$AUTH" -H "Content-Type: application/json" \
        -d '{"newStartTime":"2026-04-15T14:00:00","newEndTime":"2026-04-15T15:00:00"}')
    assert_status "move-schedule" "200" "$STATUS"
fi

# 4. Create second schedule for merge
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/schedules" \
    -H "$AUTH" -H "Content-Type: application/json" \
    -d '{"title":"Lab Session","startTime":"2026-04-15T15:00:00","endTime":"2026-04-15T16:00:00","dayOfWeek":3,"color":"#38a169"}')
STATUS=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | head -n -1)
SCHED_ID2=$(echo "$BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
assert_status "create-schedule-for-merge" "201" "$STATUS"

# 5. Merge schedules
if [ -n "$SCHED_ID" ] && [ -n "$SCHED_ID2" ]; then
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/schedules/merge" \
        -H "$AUTH" -H "Content-Type: application/json" \
        -d "{\"scheduleIds\":[$SCHED_ID,$SCHED_ID2]}")
    assert_status "merge-schedules" "200" "$STATUS"
fi

# 6. Create locked period
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/locked-periods" \
    -H "$AUTH" -H "Content-Type: application/json" \
    -d '{"title":"Exam Block","startTime":"2026-04-15T10:00:00","endTime":"2026-04-15T12:00:00","reason":"Final exam"}')
assert_status "create-locked-period" "201" "$STATUS"

# 7. Creating schedule during locked period should fail (409)
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/schedules" \
    -H "$AUTH" -H "Content-Type: application/json" \
    -d '{"title":"Conflict Session","startTime":"2026-04-15T10:30:00","endTime":"2026-04-15T11:30:00","dayOfWeek":3}')
assert_status "locked-period-conflict-rejected" "409" "$STATUS"

# 8. Change journal
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/schedules/change-journal" -H "$AUTH")
assert_status "change-journal" "200" "$STATUS"

# 9. Undo
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/schedules/undo" -H "$AUTH")
assert_status "undo" "200" "$STATUS"

# 10. Redo
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/schedules/redo" -H "$AUTH")
assert_status "redo" "200" "$STATUS"

# 11. List locked periods
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/locked-periods" -H "$AUTH")
assert_status "list-locked-periods" "200" "$STATUS"

echo ""
echo "Schedule & timetable tests: Pass=$PASS Fail=$FAIL"
[ "$FAIL" -eq 0 ]
