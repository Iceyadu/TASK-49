#!/usr/bin/env bash
# Unit Test: Quiz assembly with difficulty constraint rules
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/../backend"
PASS=0; FAIL=0

echo "=== Quiz Assembly Unit Tests ==="

if [ ! -f "$BACKEND_DIR/pom.xml" ]; then
    echo "SKIP: pom.xml not found"; exit 0
fi

cd "$BACKEND_DIR"

if mvn test -pl . -Dtest="com.scholarops.service.QuizAssemblyServiceTest" -B -q 2>/dev/null; then
    echo "  PASS  QuizAssemblyServiceTest (rule-based selection, difficulty constraints, fail-fast)"
    PASS=$((PASS + 1))
else
    echo "  FAIL  QuizAssemblyServiceTest"
    FAIL=$((FAIL + 1))
fi

echo ""
echo "Quiz assembly unit tests: Pass=$PASS Fail=$FAIL"
[ "$FAIL" -eq 0 ]
