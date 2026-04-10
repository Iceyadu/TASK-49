#!/usr/bin/env bash
# Unit Test: Per-source rate limiter (token bucket, default 30 req/min)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/../backend"
PASS=0; FAIL=0

echo "=== Rate Limiter Unit Tests ==="

if [ ! -f "$BACKEND_DIR/pom.xml" ]; then
    echo "SKIP: pom.xml not found"; exit 0
fi

cd "$BACKEND_DIR"

if mvn test -pl . -Dtest="com.scholarops.service.RateLimiterServiceTest" -B -q 2>/dev/null; then
    echo "  PASS  RateLimiterServiceTest (token bucket, 30/min default, per-source isolation)"
    PASS=$((PASS + 1))
else
    echo "  FAIL  RateLimiterServiceTest"; FAIL=$((FAIL + 1))
fi

echo ""
echo "Rate limiter unit tests: Pass=$PASS Fail=$FAIL"
[ "$FAIL" -eq 0 ]
