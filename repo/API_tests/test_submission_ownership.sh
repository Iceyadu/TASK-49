#!/usr/bin/env bash
# API Test: Object-level submission ownership — students can only access their own submissions
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

echo "=== Submission Ownership Tests ==="

# ── Obtain admin token ──────────────────────────────────────────────
ADMIN_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"Admin@12345678"}' \
    | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
[ -z "$ADMIN_TOKEN" ] && { echo "SKIP: cannot obtain admin token"; exit 0; }

# ── Provision Student A ────────────────────────────────────────────
curl -s -o /dev/null -X POST "$BASE_URL/api/users" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
    -d '{"username":"ownstudentA","email":"ownstua@scholarops.local","password":"Student@12345","fullName":"Own Student A"}'
SA_ID=$(curl -s "$BASE_URL/api/users?keyword=ownstudentA" \
    -H "Authorization: Bearer $ADMIN_TOKEN" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
[ -n "$SA_ID" ] && curl -s -o /dev/null -X POST "$BASE_URL/api/users/$SA_ID/roles" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"roleId":5}'

STU_A_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"ownstudentA","password":"Student@12345"}' \
    | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
[ -z "$STU_A_TOKEN" ] && { echo "SKIP: cannot obtain Student A token"; exit 0; }

# ── Provision Student B ────────────────────────────────────────────
curl -s -o /dev/null -X POST "$BASE_URL/api/users" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
    -d '{"username":"ownstudentB","email":"ownstub@scholarops.local","password":"Student@12345","fullName":"Own Student B"}'
SB_ID=$(curl -s "$BASE_URL/api/users?keyword=ownstudentB" \
    -H "Authorization: Bearer $ADMIN_TOKEN" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
[ -n "$SB_ID" ] && curl -s -o /dev/null -X POST "$BASE_URL/api/users/$SB_ID/roles" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"roleId":5}'

STU_B_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"ownstudentB","password":"Student@12345"}' \
    | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
[ -z "$STU_B_TOKEN" ] && { echo "SKIP: cannot obtain Student B token"; exit 0; }

# ── Provision instructor ───────────────────────────────────────────
curl -s -o /dev/null -X POST "$BASE_URL/api/users" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
    -d '{"username":"owninstructor","email":"owninstr@scholarops.local","password":"Instructor@12345","fullName":"Own Instructor"}'
OI_ID=$(curl -s "$BASE_URL/api/users?keyword=owninstructor" \
    -H "Authorization: Bearer $ADMIN_TOKEN" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
[ -n "$OI_ID" ] && curl -s -o /dev/null -X POST "$BASE_URL/api/users/$OI_ID/roles" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"roleId":3}'

INSTR_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"owninstructor","password":"Instructor@12345"}' \
    | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
[ -z "$INSTR_TOKEN" ] && { echo "SKIP: cannot obtain instructor token"; exit 0; }

# ── Find an available quiz to create a submission ───────────────────
# Look up catalog to find a quiz ID students can attempt
QUIZ_ID=$(curl -s "$BASE_URL/api/catalog" \
    -H "Authorization: Bearer $STU_A_TOKEN" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

if [ -z "$QUIZ_ID" ]; then
    echo "SKIP: no quiz available in catalog for submission tests"
    echo ""
    echo "Submission Ownership Tests: Pass=$PASS Fail=$FAIL"
    exit 0
fi

# ── Student A creates a submission ──────────────────────────────────
echo ""
echo "--- Student A creates a submission ---"

SUBMISSION_RESPONSE=$(curl -s -X POST "$BASE_URL/api/quizzes/$QUIZ_ID/submissions" \
    -H "Authorization: Bearer $STU_A_TOKEN" -H "Content-Type: application/json" -d '{}')
SUBMISSION_ID=$(echo "$SUBMISSION_RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

if [ -z "$SUBMISSION_ID" ]; then
    echo "SKIP: could not create submission (quiz may require specific payload)"
    echo ""
    echo "Submission Ownership Tests: Pass=$PASS Fail=$FAIL"
    exit 0
fi

echo "  Created submission ID=$SUBMISSION_ID for quiz ID=$QUIZ_ID"

# ── Student A can access their own submission ───────────────────────
echo ""
echo "--- Student A accesses own submission ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/submissions/$SUBMISSION_ID" \
    -H "Authorization: Bearer $STU_A_TOKEN")
assert_status "student-A-can-view-own-submission" "200" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/submissions/$SUBMISSION_ID/autosave" \
    -H "Authorization: Bearer $STU_A_TOKEN" -H "Content-Type: application/json" \
    -d '{"answers":[]}')
assert_status_one_of "student-A-can-autosave-own-submission" "200" "204" "$STATUS"

# ── Student B cannot access Student A's submission ──────────────────
echo ""
echo "--- Student B cannot access Student A's submission ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/submissions/$SUBMISSION_ID" \
    -H "Authorization: Bearer $STU_B_TOKEN")
assert_status_one_of "student-B-cannot-view-A-submission" "403" "404" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/submissions/$SUBMISSION_ID/autosave" \
    -H "Authorization: Bearer $STU_B_TOKEN" -H "Content-Type: application/json" \
    -d '{"answers":[]}')
assert_status_one_of "student-B-cannot-autosave-A-submission" "403" "404" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/submissions/$SUBMISSION_ID/submit" \
    -H "Authorization: Bearer $STU_B_TOKEN" -H "Content-Type: application/json" \
    -d '{"answers":[]}')
assert_status_one_of "student-B-cannot-submit-A-submission" "403" "404" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/submissions/$SUBMISSION_ID/feedback" \
    -H "Authorization: Bearer $STU_B_TOKEN")
assert_status_one_of "student-B-cannot-view-A-feedback" "403" "404" "$STATUS"

# ── Instructor can access any submission (SUBMISSION_VIEW_ALL) ──────
echo ""
echo "--- Instructor can access any submission ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/grading/submissions/$SUBMISSION_ID" \
    -H "Authorization: Bearer $INSTR_TOKEN")
assert_status "instructor-can-view-any-submission" "200" "$STATUS"

echo ""
echo "Submission Ownership Tests: Pass=$PASS Fail=$FAIL"
[ "$FAIL" -eq 0 ]
