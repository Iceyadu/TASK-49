#!/usr/bin/env bash
# Thin wrapper: ./run_tests.sh already runs inside Docker by default.
set -euo pipefail
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"
exec ./run_tests.sh
