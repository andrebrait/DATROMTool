#!/bin/sh
# ts-hook.sh <module> — run a token-savior Claude/Codex hook
# (token_savior.hooks.<module>, e.g. bash_rewriter_hook / tool_capture_hook) from the cached venv that
# scripts/mcp-token-savior.sh installs. Pass-through no-op when the venv is absent
# (first session, before the MCP launcher has installed it).
# Env (optional): TS_VENV — venv location (default: ${XDG_CACHE_HOME:-$HOME/.cache}/token-savior/venv).
# Only tool_capture_hook is wired in .claude/settings.json and .codex/hooks.json — the
# bash rewriter/compactors stay unwired (Bash-output compaction is rtk's job now).
set -eu

py="${TS_VENV:-${XDG_CACHE_HOME:-$HOME/.cache}/token-savior/venv}/bin/python3"

if [ ! -x "$py" ]; then
	cat >/dev/null
	printf '{"continue": true}\n'
	exit 0
fi
exec "$py" -m "token_savior.hooks.$1"
