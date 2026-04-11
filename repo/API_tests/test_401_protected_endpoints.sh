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

# Reject unauthenticated access: 401 is ideal; 403 is acceptable (filter vs @PreAuthorize paths).
assert_unauth() {
    local label="$1" actual="$2"
    if [ "$actual" = "401" ] || [ "$actual" = "403" ]; then
        echo "  PASS  $label (HTTP $actual)"; PASS=$((PASS + 1))
    else
        echo "  FAIL  $label (expected 401 or 403, got $actual)"; FAIL=$((FAIL + 1))
    fi
}

echo "=== 401 Protected Endpoints Tests ==="
echo "Verifying unauthenticated requests are rejected (HTTP 401 preferred; 403 also accepted)"

# ── User & Role Management ─────────────────────────────────────────
echo ""
echo "--- User & Role Management ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/users")
assert_unauth "GET /api/users (no token)" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/roles")
assert_unauth "GET /api/roles (no token)" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/audit-logs")
assert_unauth "GET /api/audit-logs (no token)" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/permission-change-history")
assert_unauth "GET /api/permission-change-history (no token)" "$STATUS"

# ── Quiz Endpoints ──────────────────────────────────────────────────
echo ""
echo "--- Quiz Endpoints ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/quizzes")
assert_unauth "GET /api/quizzes (no token)" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/quizzes/assemble" \
    -H "Content-Type: application/json" -d '{}')
assert_unauth "POST /api/quizzes/assemble (no token)" "$STATUS"

# ── Question Bank Endpoints ─────────────────────────────────────────
echo ""
echo "--- Question Bank Endpoints ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/question-banks")
assert_unauth "GET /api/question-banks (no token)" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/question-banks" \
    -H "Content-Type: application/json" -d '{}')
assert_unauth "POST /api/question-banks (no token)" "$STATUS"

# ── Knowledge Tags ──────────────────────────────────────────────────
echo ""
echo "--- Knowledge Tags ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/knowledge-tags")
assert_unauth "GET /api/knowledge-tags (no token)" "$STATUS"

# ── Submission Endpoints ────────────────────────────────────────────
echo ""
echo "--- Submission Endpoints ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/quizzes/1/submissions" \
    -H "Content-Type: application/json" -d '{}')
assert_unauth "POST /api/quizzes/1/submissions (no token)" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/submissions/1")
assert_unauth "GET /api/submissions/1 (no token)" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/submissions/1/autosave" \
    -H "Content-Type: application/json" -d '{}')
assert_unauth "PUT /api/submissions/1/autosave (no token)" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/submissions/1/submit" \
    -H "Content-Type: application/json" -d '{}')
assert_unauth "PUT /api/submissions/1/submit (no token)" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/submissions/1/feedback")
assert_unauth "GET /api/submissions/1/feedback (no token)" "$STATUS"

# ── Grading Endpoints ──────────────────────────────────────────────
echo ""
echo "--- Grading Endpoints ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/grading/queue")
assert_unauth "GET /api/grading/queue (no token)" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/grading/submissions/1")
assert_unauth "GET /api/grading/submissions/1 (no token)" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/grading/submissions/1/grade" \
    -H "Content-Type: application/json" -d '{}')
assert_unauth "POST /api/grading/submissions/1/grade (no token)" "$STATUS"

# ── Schedule / Timetable Endpoints ──────────────────────────────────
echo ""
echo "--- Schedule / Timetable Endpoints ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/schedules")
assert_unauth "GET /api/schedules (no token)" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/schedules" \
    -H "Content-Type: application/json" -d '{}')
assert_unauth "POST /api/schedules (no token)" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/schedules/change-journal")
assert_unauth "GET /api/schedules/change-journal (no token)" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/locked-periods")
assert_unauth "GET /api/locked-periods (no token)" "$STATUS"

# ── Catalog Endpoints ──────────────────────────────────────────────
echo ""
echo "--- Catalog Endpoints ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/catalog")
assert_unauth "GET /api/catalog (no token)" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/catalog/1")
assert_unauth "GET /api/catalog/1 (no token)" "$STATUS"

# ── Content & Crawl Endpoints ──────────────────────────────────────
echo ""
echo "--- Content & Crawl Endpoints ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/content")
assert_unauth "GET /api/content (no token)" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/crawl-sources")
assert_unauth "GET /api/crawl-sources (no token)" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/crawl-runs")
assert_unauth "GET /api/crawl-runs (no token)" "$STATUS"

# ── Plagiarism Endpoints ───────────────────────────────────────────
echo ""
echo "--- Plagiarism Endpoints ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/plagiarism/checks")
assert_unauth "GET /api/plagiarism/checks (no token)" "$STATUS"

# ── Wrong Answers Endpoint ─────────────────────────────────────────
echo ""
echo "--- Wrong Answers Endpoint ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/wrong-answers")
assert_unauth "GET /api/wrong-answers (no token)" "$STATUS"

# ── Bonus: Invalid token must also be rejected ─────────────────────
echo ""
echo "--- Invalid Token Rejection ---"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/users" \
    -H "Authorization: Bearer invalid.token.value")
assert_unauth "GET /api/users (invalid token)" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/grading/queue" \
    -H "Authorization: Bearer invalid.token.value")
assert_unauth "GET /api/grading/queue (invalid token)" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/schedules" \
    -H "Authorization: Bearer invalid.token.value")
assert_unauth "GET /api/schedules (invalid token)" "$STATUS"

echo ""
echo "401 Protected Endpoints: Pass=$PASS Fail=$FAIL"
[ "$FAIL" -eq 0 ]
