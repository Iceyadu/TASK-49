#!/usr/bin/env bash
# Unit Test: Password policy validation (runs backend tests for PasswordPolicyValidator)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/../backend"
PASS=0; FAIL=0

echo "=== Password Policy Unit Tests ==="

if [ ! -f "$BACKEND_DIR/pom.xml" ]; then
    echo "SKIP: pom.xml not found"; exit 0
fi

cd "$BACKEND_DIR"

if mvn test -pl . -Dtest="com.scholarops.security.PasswordPolicyValidatorTest" -B -q 2>/dev/null; then
    echo "  PASS  PasswordPolicyValidatorTest (all assertions passed)"
    PASS=$((PASS + 1))
else
    echo "  FAIL  PasswordPolicyValidatorTest"
    FAIL=$((FAIL + 1))
fi

echo ""
echo "Password policy unit tests: Pass=$PASS Fail=$FAIL"
[ "$FAIL" -eq 0 ]
