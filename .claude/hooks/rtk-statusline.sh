#!/bin/sh
# rtk-statusline.sh — statusline badge ` [RTK <saved>↓]` (per-project token savings via
# `rtk gain -p`) when the rtk binary is available; silent otherwise; plain ` [RTK]` without
# jq/data. The number is a rolling delta per statusline-rolling.sh
# (STATUSLINE_SAVINGS_WINDOW; off = lifetime).
# Same no-PATH-prepend rule as rtk-hook.sh: the badge means "rewrite active this session".
set -eu

command -v rtk >/dev/null 2>&1 || exit 0
root=$(cd "${CLAUDE_PROJECT_DIR:-.}" && pwd -P)
# rtk gain -p scopes by cwd PREFIX over each command's recorded project_path — run it from
# the project root, or a session whose cwd wandered into a worktree shrinks the badge's scope.
tokens=$(cd "$root" && rtk gain -p -f json 2>/dev/null | jq -r '.summary.total_saved // 0' 2>/dev/null) || tokens=''
saved=$(sh "$(dirname "$0")/statusline-rolling.sh" "rtk-$(printf '%s' "$root" | cksum | awk '{print $1}')" "$tokens")
printf ' \033[38;5;208m[RTK%s]\033[0m' "${saved:+ ${saved}↓}"
