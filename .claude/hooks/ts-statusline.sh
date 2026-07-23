#!/bin/sh
# ts-statusline.sh — statusline badge ` [TS <saved>↓]` (per-project token savings from
# token-savior's stats files, tokens = chars/4 per its dashboard) when the MCP server is
# running; silent otherwise; plain ` [TS]` without jq/data. The number is a rolling delta
# per statusline-rolling.sh (STATUSLINE_SAVINGS_WINDOW; off = lifetime).
# Env (optional): TOKEN_SAVIOR_STATS_DIR — stats location (default: ~/.local/share/token-savior).
set -eu

pgrep -f 'bin/token-savio[r]' >/dev/null 2>&1 || exit 0

root=$(cd "${CLAUDE_PROJECT_DIR:-.}" && pwd -P)
dir="${TOKEN_SAVIOR_STATS_DIR:-$HOME/.local/share/token-savior}"
tokens=$(jq -rs --arg p "$root" '
	[.[] | select(.project? == $p)]
	| (map(.total_naive_chars // 0) | add // 0) - (map(.total_chars_returned // 0) | add // 0)
	| . / 4 | floor' "$dir"/*.json 2>/dev/null) \
	|| tokens=''
saved=$(sh "$(dirname "$0")/statusline-rolling.sh" "ts-$(printf '%s' "$root" | cksum | awk '{print $1}')" "$tokens")
printf ' \033[38;5;39m[TS%s]\033[0m' "${saved:+ ${saved}↓}"
