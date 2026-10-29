#!/usr/bin/env bash
# Start the API (H2, seeded) and the UI. Ctrl-C stops both.
set -euo pipefail
root="$(cd "$(dirname "$0")" && pwd)"

if [[ ! -d "$root/frontend/node_modules" ]]; then
  (cd "$root/frontend" && npm install)
fi

cleanup() {
  if [[ -n "${api_pid:-}" ]]; then
    kill "$api_pid" 2>/dev/null || true
    wait "$api_pid" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

(cd "$root/backend" && mvn -B spring-boot:run) &
api_pid=$!

(cd "$root/frontend" && npm run dev)
