#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PASS=0
FAIL=0
RESULTS=()

log()  { echo -e "\033[1;34m[TEST]\033[0m $*"; }
pass() { PASS=$((PASS + 1)); RESULTS+=("PASS  $1"); echo -e "  \033[1;32mPASS\033[0m $1"; }
fail() { FAIL=$((FAIL + 1)); RESULTS+=("FAIL  $1"); echo -e "  \033[1;31mFAIL\033[0m $1"; }

run_backend_tests() {
  log "Running backend tests (Maven) ..."
  if [ -f "$ROOT_DIR/backend/pom.xml" ]; then
    pushd "$ROOT_DIR/backend" >/dev/null
    if mvn -B -q test; then
      pass "backend-maven-tests"
    else
      fail "backend-maven-tests"
    fi
    popd >/dev/null
  else
    fail "backend-maven-tests (pom.xml missing)"
  fi
}

run_frontend_tests() {
  log "Running frontend tests (Vitest) ..."
  if [ -f "$ROOT_DIR/frontend/package.json" ]; then
    pushd "$ROOT_DIR/frontend" >/dev/null
    if [ -f package-lock.json ]; then
      npm ci
    else
      npm install
    fi
    if npm run test -- --config tests/vitest.config.ts; then
      pass "frontend-vitest"
    else
      fail "frontend-vitest"
    fi
    popd >/dev/null
  else
    fail "frontend-vitest (package.json missing)"
  fi
}

echo ""
echo "============================================================"
echo "  ScholarOps — Container Test Runner"
echo "============================================================"
echo ""

run_backend_tests
echo ""
run_frontend_tests
echo ""

echo "============================================================"
echo "  Summary"
echo "============================================================"
for r in "${RESULTS[@]}"; do echo "  $r"; done
echo ""
echo "  Total: $((PASS + FAIL))  |  Pass: $PASS  |  Fail: $FAIL"
echo "============================================================"

[ "$FAIL" -eq 0 ]
