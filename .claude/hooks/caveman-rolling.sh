#!/bin/sh
# caveman-rolling.sh — print the bare humanized caveman savings number (rolling delta per
# statusline-rolling.sh; lifetime when the window is off) from the per-session snapshots in
# .caveman-history.jsonl — the same aggregation caveman-stats.js uses. Only as fresh as the
# last caveman-stats run (auto: the caveman-stats-refresh.sh Stop hook); silent without
# jq/history/data. statusline.sh owns the badge formatting.
set -eu

h="${CLAUDE_CONFIG_DIR:-$HOME/.claude}/.caveman-history.jsonl"
{ [ -f "$h" ] && [ ! -L "$h" ]; } || exit 0
total=$(jq -rs '[group_by(.session_id)[] | max_by(.ts) | .est_saved_tokens // 0] | add // 0 | floor' "$h" 2>/dev/null) || exit 0
sh "$(dirname "$0")/statusline-rolling.sh" caveman "$total"
