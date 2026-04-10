#!/usr/bin/env bash
# API Test: All protected endpoints return 401 when no authentication token is provided
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

echo "=== 401 Protected Endpoints Tests ==="
echo "Verifying all protected endpoints reject unauthenticated requests"

# ── User & Role Management ─────────────────────────────────────────
echo ""
echo "--- User & Role Management ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/users")
assert_status "GET /api/users (no token)" "401" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/roles")
assert_status "GET /api/roles (no token)" "401" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/audit-logs")
assert_status "GET /api/audit-logs (no token)" "401" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/permission-change-history")
assert_status "GET /api/permission-change-history (no token)" "401" "$STATUS"

# ── Quiz Endpoints ──────────────────────────────────────────────────
echo ""
echo "--- Quiz Endpoints ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/quizzes")
assert_status "GET /api/quizzes (no token)" "401" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/quizzes/assemble" \
    -H "Content-Type: application/json" -d '{}')
assert_status "POST /api/quizzes/assemble (no token)" "401" "$STATUS"

# ── Question Bank Endpoints ─────────────────────────────────────────
echo ""
echo "--- Question Bank Endpoints ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/question-banks")
assert_status "GET /api/question-banks (no token)" "401" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/question-banks" \
    -H "Content-Type: application/json" -d '{}')
assert_status "POST /api/question-banks (no token)" "401" "$STATUS"

# ── Knowledge Tags ──────────────────────────────────────────────────
echo ""
echo "--- Knowledge Tags ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/knowledge-tags")
assert_status "GET /api/knowledge-tags (no token)" "401" "$STATUS"

# ── Submission Endpoints ────────────────────────────────────────────
echo ""
echo "--- Submission Endpoints ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/quizzes/1/submissions" \
    -H "Content-Type: application/json" -d '{}')
assert_status "POST /api/quizzes/1/submissions (no token)" "401" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/submissions/1")
assert_status "GET /api/submissions/1 (no token)" "401" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/submissions/1/autosave" \
    -H "Content-Type: application/json" -d '{}')
assert_status "PUT /api/submissions/1/autosave (no token)" "401" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/submissions/1/submit" \
    -H "Content-Type: application/json" -d '{}')
assert_status "PUT /api/submissions/1/submit (no token)" "401" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/submissions/1/feedback")
assert_status "GET /api/submissions/1/feedback (no token)" "401" "$STATUS"

# ── Grading Endpoints ──────────────────────────────────────────────
echo ""
echo "--- Grading Endpoints ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/grading/queue")
assert_status "GET /api/grading/queue (no token)" "401" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/grading/submissions/1")
assert_status "GET /api/grading/submissions/1 (no token)" "401" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/grading/submissions/1/grade" \
    -H "Content-Type: application/json" -d '{}')
assert_status "POST /api/grading/submissions/1/grade (no token)" "401" "$STATUS"

# ── Schedule / Timetable Endpoints ──────────────────────────────────
echo ""
echo "--- Schedule / Timetable Endpoints ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/schedules")
assert_status "GET /api/schedules (no token)" "401" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/schedules" \
    -H "Content-Type: application/json" -d '{}')
assert_status "POST /api/schedules (no token)" "401" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/schedules/change-journal")
assert_status "GET /api/schedules/change-journal (no token)" "401" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/locked-periods")
assert_status "GET /api/locked-periods (no token)" "401" "$STATUS"

# ── Catalog Endpoints ──────────────────────────────────────────────
echo ""
echo "--- Catalog Endpoints ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/catalog")
assert_status "GET /api/catalog (no token)" "401" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/catalog/1")
assert_status "GET /api/catalog/1 (no token)" "401" "$STATUS"

# ── Content & Crawl Endpoints ──────────────────────────────────────
echo ""
echo "--- Content & Crawl Endpoints ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/content")
assert_status "GET /api/content (no token)" "401" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/crawl-sources")
assert_status "GET /api/crawl-sources (no token)" "401" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/crawl-runs")
assert_status "GET /api/crawl-runs (no token)" "401" "$STATUS"

# ── Plagiarism Endpoints ───────────────────────────────────────────
echo ""
echo "--- Plagiarism Endpoints ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/plagiarism/checks")
assert_status "GET /api/plagiarism/checks (no token)" "401" "$STATUS"

# ── Wrong Answers Endpoint ─────────────────────────────────────────
echo ""
echo "--- Wrong Answers Endpoint ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/wrong-answers")
assert_status "GET /api/wrong-answers (no token)" "401" "$STATUS"

# ── Bonus: Invalid token must also be rejected ─────────────────────
echo ""
echo "--- Invalid Token Rejection ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/users" \
    -H "Authorization: Bearer invalid.token.value")
assert_status "GET /api/users (invalid token)" "401" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/grading/queue" \
    -H "Authorization: Bearer invalid.token.value")
assert_status "GET /api/grading/queue (invalid token)" "401" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/schedules" \
    -H "Authorization: Bearer invalid.token.value")
assert_status "GET /api/schedules (invalid token)" "401" "$STATUS"

echo ""
echo "401 Protected Endpoints: Pass=$PASS Fail=$FAIL"
[ "$FAIL" -eq 0 ]
