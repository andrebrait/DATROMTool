#!/bin/sh
# caveman-stats-refresh.sh — Stop hook: refresh caveman's savings snapshot (history +
# statusline suffix) by running the vendored caveman-stats.js on this session's transcript,
# throttled to one run per 5 min — keeps the rolling ⛏ badge fresh without a per-turn
# session-log parse and without manual /caveman-stats. Always exits 0: stats never block.
set -eu

stamp="${XDG_CACHE_HOME:-$HOME/.cache}/statusline-rolling/.caveman-stats-last"
now=$(date +%s)
last=$(cat "$stamp" 2>/dev/null) || last=0
case $last in '' | *[!0-9]*) last=0 ;; esac
[ $((now - last)) -ge 300 ] || exit 0
command -v node >/dev/null 2>&1 || exit 0

tp=$(jq -r '.transcript_path // empty' 2>/dev/null) || tp=''
mkdir -p "${stamp%/*}"
printf '%s\n' "$now" >"$stamp"
js="${CLAUDE_PROJECT_DIR:-.}/.claude/skills/caveman/src/hooks/caveman-stats.js"
if [ -n "$tp" ]; then
	node "$js" --session-file "$tp" >/dev/null 2>&1 || :
else
	node "$js" >/dev/null 2>&1 || :
fi
