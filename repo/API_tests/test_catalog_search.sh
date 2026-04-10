#!/usr/bin/env bash
# API Test: Catalog search, filtering, sorting, and pagination
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

echo "=== Catalog Search Tests ==="

ADMIN_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"Admin@12345678"}' \
    | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
[ -z "$ADMIN_TOKEN" ] && { echo "SKIP"; exit 0; }
AUTH="Authorization: Bearer $ADMIN_TOKEN"

# 1. Search with keyword
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET \
    "$BASE_URL/api/catalog?keyword=test" -H "$AUTH")
assert_status "search-by-keyword" "200" "$STATUS"

# 2. Search with price range
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET \
    "$BASE_URL/api/catalog?minPrice=0&maxPrice=100" -H "$AUTH")
assert_status "search-by-price-range" "200" "$STATUS"

# 3. Sort by popularity
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET \
    "$BASE_URL/api/catalog?sortBy=popularity&sortDirection=desc" -H "$AUTH")
assert_status "sort-by-popularity" "200" "$STATUS"

# 4. Sort by newest
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET \
    "$BASE_URL/api/catalog?sortBy=newest&sortDirection=desc" -H "$AUTH")
assert_status "sort-by-newest" "200" "$STATUS"

# 5. Pagination
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET \
    "$BASE_URL/api/catalog?page=0&size=5" -H "$AUTH")
assert_status "pagination" "200" "$STATUS"

# 6. Content type filter
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET \
    "$BASE_URL/api/catalog?contentType=article" -H "$AUTH")
assert_status "filter-by-content-type" "200" "$STATUS"

# 7. Combined filters
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET \
    "$BASE_URL/api/catalog?keyword=learning&minPrice=0&maxPrice=50&sortBy=popularity&page=0&size=10" -H "$AUTH")
assert_status "combined-filters" "200" "$STATUS"

# 8. Non-existent catalog item
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/catalog/999999" -H "$AUTH")
assert_status "catalog-item-not-found" "404" "$STATUS"

# 9. Unauthenticated access denied
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/catalog")
assert_status "catalog-unauthenticated" "401" "$STATUS"

echo ""
echo "Catalog tests: Pass=$PASS Fail=$FAIL"
[ "$FAIL" -eq 0 ]
