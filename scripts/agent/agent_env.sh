#!/bin/sh
# agent_env.sh -- shared environment probe for the agent-ops scripts in this directory.
#
# AGENT-MAINTAINED: these scripts encode environment mechanics that drift (managed cloud
# sessions, proxies, tool availability). When an environment change breaks one, the agent
# fixes the script in the same session and lands it via the normal flow -- never works
# around it silently in a transcript.
#
# Portability contract:
#   - All GitHub/network access goes through the `gh` and `git` CLIs, never raw endpoints:
#     managed environments route git/ssh/https through a localhost proxy that only those
#     CLIs inherit.
#   - `gh` absent (some managed environments): exit 3 with a GH-UNAVAILABLE message; the
#     caller falls back to the session's mcp__github__* tools with wakeup-paced checks
#     ("No orphaned waits"). MCP tools are harness tools, unreachable from inside a shell,
#     so the fallback CANNOT live in these scripts.
#   - Any other required tool missing: exit 4 with TOOL-MISSING.
#
# Exit codes reserved across scripts/agent/: 0 verdict/success, 1 check failed,
# 2 usage/precondition, 3 gh unavailable (use MCP fallback), 4 tool missing.

require_gh() {
	if ! command -v gh >/dev/null 2>&1; then
		echo "GH-UNAVAILABLE: gh CLI not found (managed environment?)." >&2
		echo "Fall back to mcp__github__* tools with wakeup-paced checks ('No orphaned waits')." >&2
		exit 3
	fi
}

require_tool() {
	if ! command -v "$1" >/dev/null 2>&1; then
		echo "TOOL-MISSING: $1" >&2
		exit 4
	fi
}

# Run one gh request within both its per-call cap and the wait's remaining deadline.
gh_bounded() {
	gh_now=$(date +%s)
	gh_remaining=$((deadline - gh_now))
	[ "$gh_remaining" -gt 0 ] || return 124
	[ "$gh_remaining" -le 30 ] || gh_remaining=30
	gh_stderr=$(mktemp "${TMPDIR:-/tmp}/drt-gh-stderr.XXXXXX") || return 1
	if [ "$gh_remaining" -gt 1 ]; then
		gh_soft=$((gh_remaining - 1))
		{ timeout -k 1 "$gh_soft" gh "$@"; } 2>"$gh_stderr"
		gh_status=$?
	else
		{ timeout -s KILL "$gh_remaining" gh "$@"; } 2>"$gh_stderr"
		gh_status=$?
	fi
	if [ "$gh_status" -ne 0 ]; then
		cat "$gh_stderr"
	fi
	rm -f "$gh_stderr"
	return "$gh_status"
}

# Pause no longer than the wait's remaining wall-clock budget.
sleep_bounded() {
	sleep_now=$(date +%s)
	sleep_remaining=$((deadline - sleep_now))
	[ "$sleep_remaining" -gt 0 ] || return 1
	sleep_delay=$1
	[ "$sleep_delay" -le "$sleep_remaining" ] || sleep_delay=$sleep_remaining
	[ "$sleep_delay" -gt 0 ] || return 0
	sleep "$sleep_delay"
}

# Under a git hook, exported GIT_DIR/GIT_INDEX_FILE would aim every git op at the hook's
# repo instead of the --worktree the caller named. Git-touching scripts call this first.
# $1 = the calling script's $0 (to locate the shared lib). Sourcing the shared lib
# (re)defines scrub_git_env with the actual unset, so the final call below resolves to
# that library definition -- not this wrapper (no recursion).
scrub_git_env() {
	# shellcheck source=scripts/lib/git-env-scrub.sh
	. "$(dirname "$1")/../lib/git-env-scrub.sh"
	scrub_git_env
}
