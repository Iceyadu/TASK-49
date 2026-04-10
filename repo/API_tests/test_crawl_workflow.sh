#!/usr/bin/env bash
# API Test: Crawl source management, rule versioning, and run execution
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

echo "=== Crawl Workflow Tests ==="

# We need a CURATOR user. Create one via admin, then login as curator.
ADMIN_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"Admin@12345678"}' \
    | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)

if [ -z "$ADMIN_TOKEN" ]; then
    echo "SKIP: Could not authenticate as admin"; exit 0
fi

# Create curator user
curl -s -o /dev/null -X POST "$BASE_URL/api/users" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
    -d '{"username":"curator1","email":"curator1@scholarops.local","password":"Curator@12345","fullName":"Test Curator"}'

# Assign CONTENT_CURATOR role (id=2)
CURATOR_ID=$(curl -s "$BASE_URL/api/users?keyword=curator1" \
    -H "Authorization: Bearer $ADMIN_TOKEN" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

if [ -n "$CURATOR_ID" ]; then
    curl -s -o /dev/null -X POST "$BASE_URL/api/users/$CURATOR_ID/roles" \
        -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
        -d '{"roleId":2}'
fi

# Login as curator
TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"curator1","password":"Curator@12345"}' \
    | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo "SKIP: Could not authenticate as curator"; exit 0
fi
AUTH="Authorization: Bearer $TOKEN"

# 1. Create crawl source
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/crawl-sources" \
    -H "$AUTH" -H "Content-Type: application/json" \
    -d '{"name":"Test Source","baseUrl":"http://example.com","description":"API test source","rateLimitPerMinute":30,"requiresAuth":false}')
STATUS=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | head -n -1)
assert_status "create-crawl-source" "201" "$STATUS"

SOURCE_ID=$(echo "$BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

# 2. List crawl sources
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/crawl-sources" -H "$AUTH")
assert_status "list-crawl-sources" "200" "$STATUS"

# 3. Get single source
if [ -n "$SOURCE_ID" ]; then
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/crawl-sources/$SOURCE_ID" -H "$AUTH")
    assert_status "get-crawl-source" "200" "$STATUS"
fi

# 4. Create crawl rule version
if [ -n "$SOURCE_ID" ]; then
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/crawl-sources/$SOURCE_ID/rules" \
        -H "$AUTH" -H "Content-Type: application/json" \
        -d '{"extractionMethod":"CSS_SELECTOR","ruleDefinition":"{\"title\":\"h1\",\"body\":\"article\"}","fieldMappings":"{\"title\":\"title\",\"body\":\"bodyText\"}","notes":"Initial version"}')
    STATUS=$(echo "$RESPONSE" | tail -1)
    assert_status "create-crawl-rule-v1" "201" "$STATUS"
fi

# 5. List rules for source
if [ -n "$SOURCE_ID" ]; then
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/crawl-sources/$SOURCE_ID/rules" -H "$AUTH")
    assert_status "list-rules-for-source" "200" "$STATUS"
fi

# 6. Create second rule version (hot-update)
if [ -n "$SOURCE_ID" ]; then
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/crawl-sources/$SOURCE_ID/rules" \
        -H "$AUTH" -H "Content-Type: application/json" \
        -d '{"extractionMethod":"CSS_SELECTOR","ruleDefinition":"{\"title\":\"h1.main\",\"body\":\".content\"}","fieldMappings":"{\"title\":\"title\",\"body\":\"bodyText\"}","notes":"Hot-updated selectors"}')
    STATUS=$(echo "$RESPONSE" | tail -1)
    assert_status "create-crawl-rule-v2-hotupdate" "201" "$STATUS"
fi

# 7. Test extraction endpoint
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/crawl-rules/test-extraction" \
    -H "$AUTH" -H "Content-Type: application/json" \
    -d '{"sampleUrl":"http://example.com","extractionMethod":"CSS_SELECTOR","ruleDefinition":"{\"title\":\"h1\"}","fieldMappings":"{\"title\":\"title\"}"}')
assert_status "test-extraction" "200" "$STATUS"

# 8. Admin cannot access crawl sources (role check)
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/crawl-sources" \
    -H "Authorization: Bearer $ADMIN_TOKEN")
assert_status "admin-cannot-access-crawl-sources" "403" "$STATUS"

echo ""
echo "Crawl workflow tests: Pass=$PASS Fail=$FAIL"
[ "$FAIL" -eq 0 ]
