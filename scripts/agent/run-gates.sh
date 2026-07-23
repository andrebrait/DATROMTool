#!/bin/sh
# run-gates.sh -- run the canonical gates for whatever a diff touches.
# Mechanical runner for the gate table below (keep the table and the documented policy
# in sync -- change them together).
#
# Usage: run-gates.sh [--worktree PATH] [--diff BASE] [--plan] [--allow-missing]
#   --worktree       repo checkout to run in (default: cwd's repo root)
#   --diff BASE      compute touched files: BASE...HEAD merge-base diff UNIONED with
#                     any uncommitted (staged+unstaged+untracked, .gitignore-filtered)
#                     changes vs HEAD (default origin/master)
#   --plan           print the gate commands that WOULD run, one per line, and exit
#   --allow-missing  a missing tool reports SKIP without failing the run (default: fails;
#                     the markdownlint docs gate is always optional regardless)
#
# Gate table (touched = union of committed-diff + uncommitted file types):
#   *.java OR pom.xml -> mvn -B -q verify   (whole reactor)
#   *.sh             -> sh -n <file> per file + shellcheck <file> (scripts/, .claude/hooks/,
#                        .githooks/ only)
#   *.md             -> npx markdownlint-cli2  (optional: SKIP if the tool is absent)
#
# Per-gate lines `GATE PASS|FAIL|SKIP: <cmd>`; final line `GATES: PASS|FAIL`. Exit 0 only
# when nothing failed (and nothing skipped without --allow-missing, except the optional
# docs gate).

worktree='' base='origin/master' plan=0 allow_missing=0
overall=0

usage() {
	echo "usage: run-gates.sh [--worktree PATH] [--diff BASE] [--plan] [--allow-missing]" >&2
	exit 2
}

# Map a touched-file list (stdin, one path per line) to gate commands (stdout, one per
# line). Per-file gates (sh -n, shellcheck) emit one command per touched file.
gates_for() {
	files=$(cat)
	out=''
	nl='
'
	# Per-file commands (sh -n / shellcheck) are re-parsed by run_gate's `sh -c`; a diff
	# filename carrying shell metacharacters would inject there. The guard applies ONLY to
	# those buckets -- aggregate gates (mvn) and gate-less file types never embed the
	# filename, so an unusual name there must neither fail the run nor drop the gates.
	unsafe=$(printf '%s\n' "$files" | grep -E '\.sh$' | grep '[^A-Za-z0-9._/-]' || true)
	if [ -n "$unsafe" ]; then
		files=$(printf '%s\n' "$files" | grep -v '[^A-Za-z0-9._/-]' || true)
		out="${out}printf 'unsafe filename in diff\\n' >&2; false${nl}"
	fi
	# Java sources or the build config -> one whole-reactor verify.
	# Future optimization: scope to touched modules with `mvn -B -q -pl <module> -am verify`.
	if printf '%s\n' "$files" | grep -Eq '(\.java$|(^|/)pom\.xml$)'; then
		out="${out}mvn -B -q verify${nl}"
	fi
	if printf '%s\n' "$files" | grep -q '\.sh$'; then
		for f in $(printf '%s\n' "$files" | grep '\.sh$'); do
			out="${out}sh -n $f${nl}"
			# shellcheck scope: the shell source of truth (scripts, .claude/hooks, .githooks).
			case "$f" in
			scripts/* | .claude/hooks/* | .githooks/*) out="${out}shellcheck $f${nl}" ;;
			esac
		done
	fi
	if printf '%s\n' "$files" | grep -q '\.md$'; then
		out="${out}npx markdownlint-cli2${nl}"
	fi
	# GUI module not present yet; add a ui-test gate row when it lands.
	printf '%s' "$out"
}

run_gate() {
	# $1 = command line
	tool=${1%% *}
	if ! (cd "$worktree" && { command -v "$tool" >/dev/null 2>&1 || [ -x "$tool" ]; }); then
		printf 'GATE SKIP: %s (TOOL-MISSING: %s)\n' "$1" "$tool"
		# The markdownlint docs gate is optional: a missing linter is never a failure.
		case "$1" in
		'npx markdownlint-cli2') return 0 ;;
		esac
		[ "$allow_missing" -eq 1 ] || overall=1
		return 0
	fi
	# </dev/null -- a stdin-reading gate otherwise eats the command loop's remaining gate
	# lines, silently skipping those gates.
	if (cd "$worktree" && sh -c "$1" </dev/null >/dev/null 2>&1); then
		printf 'GATE PASS: %s\n' "$1"
	else
		printf 'GATE FAIL: %s\n' "$1"
		overall=1
	fi
	return 0
}

main() {
	# shellcheck source=scripts/agent/agent_env.sh
	. "$(dirname "$0")/agent_env.sh"
	scrub_git_env "$0"
	while [ $# -gt 0 ]; do
		case "$1" in
			--worktree) worktree=$2; shift 2 ;;
			--diff) base=$2; shift 2 ;;
			--plan) plan=1; shift ;;
			--allow-missing) allow_missing=1; shift ;;
			*) usage ;;
		esac
	done
	require_tool git
	[ -n "$worktree" ] || worktree=$(git rev-parse --show-toplevel) || exit 2

	# --diff-filter=ACMR: a pure deletion stages nothing to lint (same rule as the
	# pre-commit hook) -- per-file gates against ghost paths would always fail.
	committed=$(git -C "$worktree" diff --name-only --diff-filter=ACMR "$base...HEAD") || exit 2
	# Union with uncommitted changes, else gates see nothing pre-commit. staged/unstaged
	# unioned SEPARATELY, not via one `diff HEAD` -- `diff <commit>` reads the WORKING TREE,
	# so a staged-then-worktree-reverted file is invisible.
	staged=$(git -C "$worktree" diff --name-only --diff-filter=ACMR --cached) || exit 2
	unstaged=$(git -C "$worktree" diff --name-only --diff-filter=ACMR) || exit 2
	# neither diff form surfaces a brand-new file never `git add`ed.
	untracked=$(git -C "$worktree" ls-files --others --exclude-standard) || exit 2
	files=$(printf '%s\n%s\n%s\n%s\n' "$committed" "$staged" "$unstaged" "$untracked" | LC_ALL=C sort -u | grep -v '^$')
	cmds=$(printf '%s\n' "$files" | gates_for)

	if [ "$plan" -eq 1 ]; then
		printf '%s' "$cmds"
		exit 0
	fi

	# Pipelines run in subshells under POSIX sh, so `overall` cannot propagate out of a
	# `| while` loop -- run the loop in one subshell and carry the flag in its output.
	report=$(printf '%s\n' "$cmds" | {
		overall=0
		while IFS= read -r c; do
			[ -n "$c" ] || continue
			run_gate "$c"
		done
		echo "OVERALL=$overall"
	})
	overall_status=${report##*OVERALL=}
	printf '%s\n' "$report" | grep -v '^OVERALL='
	if [ "$overall_status" = 0 ]; then
		printf 'GATES: PASS\n'
		exit 0
	fi
	printf 'GATES: FAIL\n'
	exit 1
}

case "${AGENT_SOURCE_ONLY:-0}" in
	1) ;;
	*) main "$@" ;;
esac
