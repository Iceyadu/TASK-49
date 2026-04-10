#!/usr/bin/env bash
# API Test: Cross-role access control — users cannot reach endpoints reserved for other roles
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

echo "=== 403 Cross-Role Access Control Tests ==="

# ── Obtain admin token ──────────────────────────────────────────────
ADMIN_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"Admin@12345678"}' \
    | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
[ -z "$ADMIN_TOKEN" ] && { echo "SKIP: cannot obtain admin token"; exit 0; }

# ── Provision student user ──────────────────────────────────────────
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

# ── Provision instructor user ───────────────────────────────────────
curl -s -o /dev/null -X POST "$BASE_URL/api/users" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
    -d '{"username":"secinstructor","email":"secinstr@scholarops.local","password":"Instructor@12345","fullName":"Sec Instructor"}'
INSTR_ID=$(curl -s "$BASE_URL/api/users?keyword=secinstructor" \
    -H "Authorization: Bearer $ADMIN_TOKEN" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
[ -n "$INSTR_ID" ] && curl -s -o /dev/null -X POST "$BASE_URL/api/users/$INSTR_ID/roles" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"roleId":3}'

INSTR_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"secinstructor","password":"Instructor@12345"}' \
    | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
[ -z "$INSTR_TOKEN" ] && { echo "SKIP: cannot obtain instructor token"; exit 0; }

# ── Provision curator user ──────────────────────────────────────────
curl -s -o /dev/null -X POST "$BASE_URL/api/users" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
    -d '{"username":"seccurator","email":"seccur@scholarops.local","password":"Curator@12345","fullName":"Sec Curator"}'
CUR_ID=$(curl -s "$BASE_URL/api/users?keyword=seccurator" \
    -H "Authorization: Bearer $ADMIN_TOKEN" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
[ -n "$CUR_ID" ] && curl -s -o /dev/null -X POST "$BASE_URL/api/users/$CUR_ID/roles" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"roleId":4}'

CUR_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"seccurator","password":"Curator@12345"}' \
    | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
[ -z "$CUR_TOKEN" ] && { echo "SKIP: cannot obtain curator token"; exit 0; }

# ══════════════════════════════════════════════════════════════════════
# STUDENT RESTRICTIONS
# ══════════════════════════════════════════════════════════════════════
echo ""
echo "--- Student cannot access admin/instructor/curator endpoints ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/users" \
    -H "Authorization: Bearer $STU_TOKEN")
assert_status "student-cannot-list-users" "403" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/roles" \
    -H "Authorization: Bearer $STU_TOKEN")
assert_status "student-cannot-list-roles" "403" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/audit-logs" \
    -H "Authorization: Bearer $STU_TOKEN")
assert_status "student-cannot-view-audit-logs" "403" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/crawl-sources" \
    -H "Authorization: Bearer $STU_TOKEN")
assert_status "student-cannot-manage-crawl-sources" "403" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/quizzes" \
    -H "Authorization: Bearer $STU_TOKEN")
assert_status "student-cannot-list-quizzes-instructor" "403" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/question-banks" \
    -H "Authorization: Bearer $STU_TOKEN")
assert_status "student-cannot-list-question-banks" "403" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/grading/queue" \
    -H "Authorization: Bearer $STU_TOKEN")
assert_status "student-cannot-access-grading-queue" "403" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/content" \
    -H "Authorization: Bearer $STU_TOKEN")
assert_status "student-cannot-manage-content" "403" "$STATUS"

# ══════════════════════════════════════════════════════════════════════
# ADMIN RESTRICTIONS
# ══════════════════════════════════════════════════════════════════════
echo ""
echo "--- Admin cannot access student-only endpoints ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/schedules" \
    -H "Authorization: Bearer $ADMIN_TOKEN")
assert_status "admin-cannot-access-student-schedules" "403" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/quizzes/1/submissions" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{}')
assert_status "admin-cannot-start-submissions" "403" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/wrong-answers" \
    -H "Authorization: Bearer $ADMIN_TOKEN")
assert_status "admin-cannot-view-wrong-answers" "403" "$STATUS"

# ══════════════════════════════════════════════════════════════════════
# INSTRUCTOR RESTRICTIONS
# ══════════════════════════════════════════════════════════════════════
echo ""
echo "--- Instructor cannot access admin/curator/student endpoints ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/users" \
    -H "Authorization: Bearer $INSTR_TOKEN")
assert_status "instructor-cannot-manage-users" "403" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/crawl-sources" \
    -H "Authorization: Bearer $INSTR_TOKEN")
assert_status "instructor-cannot-manage-crawl-sources" "403" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/schedules" \
    -H "Authorization: Bearer $INSTR_TOKEN")
assert_status "instructor-cannot-access-student-schedules" "403" "$STATUS"

# ══════════════════════════════════════════════════════════════════════
# CURATOR RESTRICTIONS
# ══════════════════════════════════════════════════════════════════════
echo ""
echo "--- Curator cannot access admin/instructor/student endpoints ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/users" \
    -H "Authorization: Bearer $CUR_TOKEN")
assert_status "curator-cannot-manage-users" "403" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/quizzes/assemble" \
    -H "Authorization: Bearer $CUR_TOKEN" -H "Content-Type: application/json" -d '{}')
assert_status "curator-cannot-create-quizzes" "403" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/quizzes/1/submissions" \
    -H "Authorization: Bearer $CUR_TOKEN" -H "Content-Type: application/json" -d '{}')
assert_status "curator-cannot-start-submissions" "403" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/schedules" \
    -H "Authorization: Bearer $CUR_TOKEN")
assert_status "curator-cannot-access-schedules" "403" "$STATUS"

echo ""
echo "403 Cross-Role Tests: Pass=$PASS Fail=$FAIL"
[ "$FAIL" -eq 0 ]
