#!/usr/bin/env bash
# Unit Test: Auto-grading, subjective routing, rubric scoring, wrong answer history
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/../backend"
PASS=0; FAIL=0

echo "=== Grading Workflow Unit Tests ==="

if [ ! -f "$BACKEND_DIR/pom.xml" ]; then
    echo "SKIP: pom.xml not found"; exit 0
fi

cd "$BACKEND_DIR"

if mvn test -pl . -Dtest="com.scholarops.service.AutoGradingServiceTest" -B -q 2>/dev/null; then
    echo "  PASS  AutoGradingServiceTest (objective grading, wrong answer history, subjective routing)"
    PASS=$((PASS + 1))
else
    echo "  FAIL  AutoGradingServiceTest"; FAIL=$((FAIL + 1))
fi

if mvn test -pl . -Dtest="com.scholarops.service.GradingWorkflowServiceTest" -B -q 2>/dev/null; then
    echo "  PASS  GradingWorkflowServiceTest (queue, grading, mandatory rubric scoring)"
    PASS=$((PASS + 1))
else
    echo "  FAIL  GradingWorkflowServiceTest"; FAIL=$((FAIL + 1))
fi

echo ""
echo "Grading workflow unit tests: Pass=$PASS Fail=$FAIL"
[ "$FAIL" -eq 0 ]
