#!/usr/bin/env bash
# Unit Test: Content standardization — timestamp normalization, address normalization, language detection
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/../backend"
PASS=0; FAIL=0

echo "=== Content Standardization Unit Tests ==="

if [ ! -f "$BACKEND_DIR/pom.xml" ]; then
    echo "SKIP: pom.xml not found"; exit 0
fi

cd "$BACKEND_DIR"

if mvn test -pl . -Dtest="com.scholarops.util.TimestampNormalizerTest" -B -q 2>/dev/null; then
    echo "  PASS  TimestampNormalizerTest"; PASS=$((PASS + 1))
else
    echo "  FAIL  TimestampNormalizerTest"; FAIL=$((FAIL + 1))
fi

if mvn test -pl . -Dtest="com.scholarops.util.AddressNormalizerTest" -B -q 2>/dev/null; then
    echo "  PASS  AddressNormalizerTest"; PASS=$((PASS + 1))
else
    echo "  FAIL  AddressNormalizerTest"; FAIL=$((FAIL + 1))
fi

if mvn test -pl . -Dtest="com.scholarops.util.LanguageDetectorTest" -B -q 2>/dev/null; then
    echo "  PASS  LanguageDetectorTest"; PASS=$((PASS + 1))
else
    echo "  FAIL  LanguageDetectorTest"; FAIL=$((FAIL + 1))
fi

if mvn test -pl . -Dtest="com.scholarops.service.ContentStandardizationServiceTest" -B -q 2>/dev/null; then
    echo "  PASS  ContentStandardizationServiceTest"; PASS=$((PASS + 1))
else
    echo "  FAIL  ContentStandardizationServiceTest"; FAIL=$((FAIL + 1))
fi

echo ""
echo "Content standardization unit tests: Pass=$PASS Fail=$FAIL"
[ "$FAIL" -eq 0 ]
