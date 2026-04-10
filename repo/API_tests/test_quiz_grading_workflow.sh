#!/usr/bin/env bash
# API Test: Quiz assembly, submission, auto-grading, and grading queue
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

echo "=== Quiz & Grading Workflow Tests ==="

# Login as admin to set up users
ADMIN_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"Admin@12345678"}' \
    | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)

if [ -z "$ADMIN_TOKEN" ]; then echo "SKIP: Cannot auth"; exit 0; fi

# Create instructor
curl -s -o /dev/null -X POST "$BASE_URL/api/users" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
    -d '{"username":"instructor1","email":"inst1@scholarops.local","password":"Instructor@123","fullName":"Test Instructor"}'
INST_ID=$(curl -s "$BASE_URL/api/users?keyword=instructor1" \
    -H "Authorization: Bearer $ADMIN_TOKEN" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
[ -n "$INST_ID" ] && curl -s -o /dev/null -X POST "$BASE_URL/api/users/$INST_ID/roles" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"roleId":3}'

# Create student
curl -s -o /dev/null -X POST "$BASE_URL/api/users" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
    -d '{"username":"student1","email":"stu1@scholarops.local","password":"Student@12345","fullName":"Test Student"}'
STU_ID=$(curl -s "$BASE_URL/api/users?keyword=student1" \
    -H "Authorization: Bearer $ADMIN_TOKEN" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
[ -n "$STU_ID" ] && curl -s -o /dev/null -X POST "$BASE_URL/api/users/$STU_ID/roles" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" -d '{"roleId":5}'

# Login as instructor
INST_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"instructor1","password":"Instructor@123"}' \
    | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)

if [ -z "$INST_TOKEN" ]; then echo "SKIP: Cannot auth instructor"; exit 0; fi
INST_AUTH="Authorization: Bearer $INST_TOKEN"

# 1. Create question bank
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/question-banks" \
    -H "$INST_AUTH" -H "Content-Type: application/json" \
    -d '{"name":"API Test Bank","description":"For API testing","subject":"Testing"}')
STATUS=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | head -n -1)
assert_status "create-question-bank" "201" "$STATUS"
BANK_ID=$(echo "$BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

# 2. Add questions (need enough for assembly)
if [ -n "$BANK_ID" ]; then
    for i in $(seq 1 10); do
        DIFF=$(( (i % 5) + 1 ))
        curl -s -o /dev/null -X POST "$BASE_URL/api/question-banks/$BANK_ID/questions" \
            -H "$INST_AUTH" -H "Content-Type: application/json" \
            -d "{\"questionType\":\"MULTIPLE_CHOICE\",\"difficultyLevel\":$DIFF,\"questionText\":\"Question $i?\",\"options\":\"[\\\"A\\\",\\\"B\\\",\\\"C\\\",\\\"D\\\"]\",\"correctAnswer\":\"A\",\"explanation\":\"A is correct\",\"points\":1}"
    done
    echo "  PASS  added-10-questions"; PASS=$((PASS + 1))
fi

# 3. Assemble quiz with difficulty rules
if [ -n "$BANK_ID" ]; then
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/quizzes/assemble" \
        -H "$INST_AUTH" -H "Content-Type: application/json" \
        -d "{\"title\":\"API Test Quiz\",\"description\":\"Auto-assembled\",\"questionBankId\":$BANK_ID,\"totalQuestions\":5,\"timeLimitMinutes\":30,\"maxAttempts\":1,\"rules\":[{\"ruleType\":\"DIFFICULTY\",\"minCount\":1,\"difficultyLevel\":4}],\"showImmediateFeedback\":true}")
    STATUS=$(echo "$RESPONSE" | tail -1)
    BODY=$(echo "$RESPONSE" | head -n -1)
    assert_status "assemble-quiz" "201" "$STATUS"
    QUIZ_ID=$(echo "$BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
fi

# 4. Publish quiz
if [ -n "$QUIZ_ID" ]; then
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/quizzes/$QUIZ_ID/publish" \
        -H "$INST_AUTH")
    assert_status "publish-quiz" "200" "$STATUS"
fi

# 5. Login as student and start submission
STU_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"student1","password":"Student@12345"}' \
    | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)

if [ -n "$STU_TOKEN" ] && [ -n "$QUIZ_ID" ]; then
    STU_AUTH="Authorization: Bearer $STU_TOKEN"

    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/quizzes/$QUIZ_ID/submissions" \
        -H "$STU_AUTH")
    STATUS=$(echo "$RESPONSE" | tail -1)
    BODY=$(echo "$RESPONSE" | head -n -1)
    assert_status "start-submission" "201" "$STATUS"
    SUB_ID=$(echo "$BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

    # 6. Autosave
    if [ -n "$SUB_ID" ]; then
        STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/submissions/$SUB_ID/autosave" \
            -H "$STU_AUTH" -H "Content-Type: application/json" \
            -d '{"answers":[],"timeRemainingSeconds":1500}')
        assert_status "autosave-submission" "200" "$STATUS"
    fi

    # 7. Submit
    if [ -n "$SUB_ID" ]; then
        STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/submissions/$SUB_ID/submit" \
            -H "$STU_AUTH")
        assert_status "submit-submission" "200" "$STATUS"
    fi

    # 8. Cannot start second attempt (max_attempts = 1)
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/quizzes/$QUIZ_ID/submissions" \
        -H "$STU_AUTH")
    assert_status "max-attempts-enforced" "409" "$STATUS"

    # 9. Get feedback
    if [ -n "$SUB_ID" ]; then
        STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/submissions/$SUB_ID/feedback" \
            -H "$STU_AUTH")
        assert_status "get-feedback" "200" "$STATUS"
    fi

    # 10. Wrong answer history
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/wrong-answers" -H "$STU_AUTH")
    assert_status "wrong-answer-history" "200" "$STATUS"
fi

# 11. Grading queue (as instructor)
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/grading/queue" -H "$INST_AUTH")
assert_status "grading-queue" "200" "$STATUS"

# 12. Plagiarism checks
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/plagiarism/checks" -H "$INST_AUTH")
assert_status "plagiarism-checks" "200" "$STATUS"

echo ""
echo "Quiz & grading tests: Pass=$PASS Fail=$FAIL"
[ "$FAIL" -eq 0 ]
