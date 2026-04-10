#!/usr/bin/env bash
# Unit Test: AES-256 encryption utilities
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/../backend"
PASS=0; FAIL=0

echo "=== Encryption Unit Tests ==="

if [ ! -f "$BACKEND_DIR/pom.xml" ]; then
    echo "SKIP: pom.xml not found"; exit 0
fi

cd "$BACKEND_DIR"

if mvn test -pl . -Dtest="com.scholarops.util.AesEncryptionUtilTest" -B -q 2>/dev/null; then
    echo "  PASS  AesEncryptionUtilTest"
    PASS=$((PASS + 1))
else
    echo "  FAIL  AesEncryptionUtilTest"
    FAIL=$((FAIL + 1))
fi

if mvn test -pl . -Dtest="com.scholarops.service.EncryptionServiceTest" -B -q 2>/dev/null; then
    echo "  PASS  EncryptionServiceTest"
    PASS=$((PASS + 1))
else
    echo "  FAIL  EncryptionServiceTest"
    FAIL=$((FAIL + 1))
fi

echo ""
echo "Encryption unit tests: Pass=$PASS Fail=$FAIL"
[ "$FAIL" -eq 0 ]
