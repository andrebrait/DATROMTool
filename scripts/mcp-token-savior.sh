#!/bin/sh
# mcp-token-savior.sh — shared Claude/Codex stdio launcher for the token-savior MCP
# server (wired via .mcp.json and .codex/config.toml).
# Installs TS_SOURCE into a per-user cached venv, then execs it. The default TS_SOURCE is the
# andrebrait/token-savior fork's `integration` branch pinned by commit — it carries fixes and
# language support not yet merged upstream (Mibayy/token-savior PRs #47 #53 #54 #57 #58 #59
# #60 #62 #64 #66 #68); repoint to PyPI's token-savior-recall[mcp,memory-vector] once those merge. A TS_SOURCE change (e.g. a
# new pinned commit) is detected via the .pfb-ts-source stamp and triggers a clean venv rebuild;
# a mkdir lock serializes concurrent sessions racing that rebuild (the venv is one shared
# per-user cache). Requires python3 >= 3.11 and git (pip installs from a git URL).
# Env (all optional):
#   WORKSPACE_ROOTS        comma-separated project roots (default: every usable worktree
#                          of the current Git repository, otherwise current directory)
#   CLAUDE_PROJECT_ROOT    active root hint understood by Token Savior (default: current
#                          Git worktree; applies to Claude and Codex clients)
#   TOKEN_SAVIOR_CLIENT    telemetry client label (default: claude-code; Codex config
#                          passes codex explicitly)
#   TOKEN_SAVIOR_PROFILE   server tool profile (default: optimized)
#   TS_VENV                venv location (default: ${XDG_CACHE_HOME:-$HOME/.cache}/token-savior/venv)
#   TS_SOURCE              pip requirement to install (default: the pinned fork commit)
#   TS_LOCK_WAIT           max seconds to wait on another session's rebuild (default 300)
#   INCLUDE_PATTERNS       colon-separated index globs; the default below REPLACES the
#                          server's built-in list with Java-oriented globs for this repo
#   TOKEN_SAVIOR_MAX_FILE_SIZE  index size cap in bytes (default here 2000000 — a
#                          generous cap for large generated/Java sources)
set -eu

venv="${TS_VENV:-${XDG_CACHE_HOME:-$HOME/.cache}/token-savior/venv}"
# Trim trailing slashes so the lock and dirname derivations below treat $venv
# as the final path component (a trailing slash once nested the lock inside
# the not-yet-created venv and broke every fresh install).
while :; do case "$venv" in */) venv="${venv%/}" ;; *) break ;; esac; done
# The rebuild path below rm -rf's $venv — string checks never resolve '..'
# (only the kernel does, at rm time), so refuse '..' segments and '//' along
# with anything non-absolute ('.', '', '/', relative paths).
case "$venv" in
	*//*|*/..|*/../*) venv_bad=1 ;;
	/?*) venv_bad=0 ;;
	*) venv_bad=1 ;;
esac
if [ "$venv_bad" = 1 ]; then
	echo "mcp-token-savior: refusing venv path '$venv' — TS_VENV must be an absolute path without '..' or '//' segments" >&2
	exit 1
fi
bin="$venv/bin/token-savior"
stamp="$venv/.pfb-ts-source"
TS_SOURCE="${TS_SOURCE:-token-savior-recall[mcp,memory-vector] @ git+https://github.com/andrebrait/token-savior@9110e4341951f13fb112fb5b6c54c6e6e5540b30}"

# stdout is the MCP stdio channel — install chatter must stay on stderr
if [ ! -x "$bin" ] || [ "$(cat "$stamp" 2>/dev/null || true)" != "$TS_SOURCE" ]; then
	lock="${venv}.rebuild.lock"
	mkdir -p "$(dirname "$venv")"
	if mkdir "$lock" 2>/dev/null; then
		# set -e: a failed install still fires the trap, so a crashed rebuild
		# never leaves the lock behind for the next session to time out on.
		trap 'rmdir "$lock" 2>/dev/null' EXIT
		# Re-check under the lock — a concurrent session may have finished the
		# same rebuild while this one waited on mkdir.
		if [ ! -x "$bin" ] || [ "$(cat "$stamp" 2>/dev/null || true)" != "$TS_SOURCE" ]; then
			rm -rf "$venv"
			python3 -m venv "$venv" 1>&2
			"$venv/bin/pip" install --quiet "$TS_SOURCE" 1>&2
			printf '%s\n' "$TS_SOURCE" > "$stamp"
		fi
		rmdir "$lock" 2>/dev/null
		trap - EXIT
	else
		waited=0
		max_wait="${TS_LOCK_WAIT:-300}"
		while [ -d "$lock" ] && [ "$waited" -lt "$max_wait" ]; do
			sleep 1
			waited=$((waited + 1))
		done
		if [ ! -x "$bin" ] || [ "$(cat "$stamp" 2>/dev/null || true)" != "$TS_SOURCE" ]; then
			echo "mcp-token-savior: concurrent rebuild did not complete within ${max_wait}s — if no other session is installing, remove '$lock' and retry" >&2
			exit 1
		fi
	fi
fi

workspace_root=$PWD
if command -v git >/dev/null 2>&1; then
	repo_root=$(git rev-parse --show-toplevel 2>/dev/null || true)
	[ -z "$repo_root" ] || workspace_root=$repo_root
fi
# '+x' (not ':-') so an explicitly empty override survives: WORKSPACE_ROOTS=''
# means "index nothing", a caller decision, not an unset variable.
if [ "${WORKSPACE_ROOTS+x}" != x ]; then
	WORKSPACE_ROOTS=$workspace_root
	if command -v git >/dev/null 2>&1 && [ -x "$venv/bin/python3" ]; then
		worktree_roots=$(git worktree list --porcelain -z 2>/dev/null | "$venv/bin/python3" -c '
import os
import sys

roots = []
for record in sys.stdin.buffer.read().split(b"\0\0"):
    fields = record.split(b"\0")
    if any(field.startswith(b"prunable") for field in fields):
        continue
    path = next((field[9:] for field in fields if field.startswith(b"worktree ")), None)
    if path is None:
        continue
    root = os.path.abspath(os.fsdecode(path))
    if os.path.isdir(root):
        roots.append(root)
sys.stdout.write(",".join(roots))
' || true)
		[ -z "$worktree_roots" ] || WORKSPACE_ROOTS=$worktree_roots
	fi
fi
if [ "${CLAUDE_PROJECT_ROOT+x}" != x ]; then
	CLAUDE_PROJECT_ROOT=$workspace_root
fi
TOKEN_SAVIOR_CLIENT="${TOKEN_SAVIOR_CLIENT:-claude-code}"
TOKEN_SAVIOR_PROFILE="${TOKEN_SAVIOR_PROFILE:-optimized}"
INCLUDE_PATTERNS="${INCLUDE_PATTERNS:-**/*.java:**/*.xml:**/*.yml:**/*.yaml:**/*.md:**/*.txt:**/*.json:**/*.jsonc:**/*.toml:**/*.properties:**/*.sh}"
TOKEN_SAVIOR_MAX_FILE_SIZE="${TOKEN_SAVIOR_MAX_FILE_SIZE:-2000000}"
export WORKSPACE_ROOTS CLAUDE_PROJECT_ROOT TOKEN_SAVIOR_CLIENT TOKEN_SAVIOR_PROFILE INCLUDE_PATTERNS TOKEN_SAVIOR_MAX_FILE_SIZE
exec "$bin"
