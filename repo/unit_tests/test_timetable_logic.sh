#!/usr/bin/env bash
# Unit Test: Timetable service — move, merge, split, undo/redo, locked period enforcement
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/../backend"
PASS=0; FAIL=0

echo "=== Timetable Logic Unit Tests ==="

if [ ! -f "$BACKEND_DIR/pom.xml" ]; then
    echo "SKIP: pom.xml not found"; exit 0
fi

cd "$BACKEND_DIR"

if mvn test -pl . -Dtest="com.scholarops.service.TimetableServiceTest" -B -q 2>/dev/null; then
    echo "  PASS  TimetableServiceTest (move, merge, split, undo, redo, locked conflict)"
    PASS=$((PASS + 1))
else
    echo "  FAIL  TimetableServiceTest"
    FAIL=$((FAIL + 1))
fi

echo ""
echo "Timetable unit tests: Pass=$PASS Fail=$FAIL"
[ "$FAIL" -eq 0 ]
