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

if [ ! -d node_modules ] || [ ! -x node_modules/.bin/vitest ]; then
    echo "  Installing npm dependencies ..."
    if [ -f package-lock.json ]; then
        npm ci
    else
        npm install
    fi
fi

run_vitest() {
    if [ -x node_modules/.bin/vitest ]; then
        node_modules/.bin/vitest run --config tests/vitest.config.ts "$@"
    else
        npx --yes vitest run --config tests/vitest.config.ts "$@"
    fi
}

if run_vitest; then
    echo "  PASS  Frontend unit tests (auth store, composables, validators, formatters)"
    PASS=$((PASS + 1))
else
    echo "  FAIL  Frontend unit tests (see Vitest output above)"
    FAIL=$((FAIL + 1))
fi

echo ""
echo "Frontend composable tests: Pass=$PASS Fail=$FAIL"
[ "$FAIL" -eq 0 ]
