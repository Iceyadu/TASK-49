#!/usr/bin/env bash
# Unit Test: Frontend composables — autosave, countdown, undo/redo, permissions
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
FRONTEND_DIR="$SCRIPT_DIR/../frontend"
PASS=0; FAIL=0

echo "=== Frontend Composable Unit Tests ==="

if [ ! -f "$FRONTEND_DIR/package.json" ]; then
    echo "SKIP: package.json not found"; exit 0
fi

cd "$FRONTEND_DIR"

if [ ! -d node_modules ]; then
    echo "  Installing dependencies ..."
    npm install --silent 2>/dev/null || true
fi

if npx vitest run --config tests/vitest.config.ts 2>/dev/null; then
    echo "  PASS  Frontend unit tests (auth store, composables, validators, formatters)"
    PASS=$((PASS + 1))
else
    echo "  FAIL  Frontend unit tests"
    FAIL=$((FAIL + 1))
fi

echo ""
echo "Frontend composable tests: Pass=$PASS Fail=$FAIL"
[ "$FAIL" -eq 0 ]
