#!/usr/bin/env bash
# Unit Test: Plagiarism fingerprinting and similarity detection
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/../backend"
PASS=0; FAIL=0

echo "=== Plagiarism Detection Unit Tests ==="

if [ ! -f "$BACKEND_DIR/pom.xml" ]; then
    echo "SKIP: pom.xml not found"; exit 0
fi

cd "$BACKEND_DIR"

if mvn test -pl . -Dtest="com.scholarops.util.FingerprintUtilTest" -B -q 2>/dev/null; then
    echo "  PASS  FingerprintUtilTest (threshold 0.85, winnowing algorithm)"
    PASS=$((PASS + 1))
else
    echo "  FAIL  FingerprintUtilTest"
    FAIL=$((FAIL + 1))
fi

if mvn test -pl . -Dtest="com.scholarops.service.PlagiarismServiceTest" -B -q 2>/dev/null; then
    echo "  PASS  PlagiarismServiceTest"
    PASS=$((PASS + 1))
else
    echo "  FAIL  PlagiarismServiceTest"
    FAIL=$((FAIL + 1))
fi

echo ""
echo "Plagiarism unit tests: Pass=$PASS Fail=$FAIL"
[ "$FAIL" -eq 0 ]
