#!/bin/sh
# verify-red-proof.sh -- mechanically re-execute a red->green proof (the test-first
# mandate): freeze-hash check, revert the src paths to the pre-fix baseline (tests stay),
# expect the pinning test to FAIL, restore, expect it to PASS. The single implementation
# the delegation gates and independent verifiers reference.
#
# Usage: verify-red-proof.sh --worktree PATH --test-cmd 'CMD' --src PATH [--src PATH ...]
#                            [--hash FILE=SHA ...] [--base-ref REF]
#   --test-cmd  run via `sh -c` from the worktree root; nonzero exit = red, zero = green.
#               Caller-supplied -- typically a Maven test invocation, e.g.
#               'mvn -B -q -pl <module> -Dtest=<Class>#<method> test'.
#   --src       production path(s) reverted to --base-ref for the red run (repo-relative)
#   --hash      committed reproduction-test freeze check: `git hash-object FILE` must
#               equal SHA (the handoff's red-time hash) -- an edited test proves nothing
#   --base-ref  the pre-fix baseline (default HEAD~1). Pass the true pre-fix commit
#               explicitly whenever the step landed more than one commit (a follow-up
#               doc reconciliation, a review fix) -- HEAD~1 then names the wrong commit.
#               Omitting it keeps today's behaviour byte-identical. Every --src must exist
#               at --base-ref; added-only paths fail with REVERT-FAIL.
#
# Prints FREEZE-OK / RED-OK / GREEN-OK step lines, then final `VERDICT: PASS`.
# Any step failing prints the failing step + `VERDICT: FAIL` and exits 1. Requires a
# clean tree (exit 2 otherwise); the src paths are restored on every exit path. An
# unresolvable --base-ref (not a single existing commit) also exits 2, tree untouched.

worktree='' test_cmd='' base_ref='HEAD~1'
srcs='' hashes=''

usage() {
	echo "usage: verify-red-proof.sh --worktree PATH --test-cmd 'CMD' --src PATH [--src ...] [--hash FILE=SHA ...] [--base-ref REF]" >&2
	exit 2
}

fail() {
	printf '%s\nVERDICT: FAIL\n' "$1"
	exit 1
}

restore_srcs() {
	restore_failed=0
	for src in $srcs; do
		path_failed=0
		if git -C "$worktree" cat-file -e "HEAD:$src" 2>/dev/null; then
			git -C "$worktree" checkout HEAD -- "$src" || path_failed=1
		else
			git -C "$worktree" rm -q -r -f --ignore-unmatch -- "$src" || path_failed=1
			git -C "$worktree" clean -q -d -f -f -x -- "$src" || path_failed=1
			if [ -e "$worktree/$src" ] || [ -L "$worktree/$src" ]; then
				path_failed=1
			fi
		fi
		if [ "$path_failed" -ne 0 ]; then
			echo "restore failed for --src path: $src" >&2
			restore_failed=1
		fi
	done
	return "$restore_failed"
}

main() {
	# shellcheck source=scripts/agent/agent_env.sh
	. "$(dirname "$0")/agent_env.sh"
	scrub_git_env "$0"
	while [ $# -gt 0 ]; do
		case "$1" in
			--worktree) worktree=$2; shift 2 ;;
			--test-cmd) test_cmd=$2; shift 2 ;;
			--src)
				# $srcs is later word-split into git pathspecs and an expanded trap --
				# reject anything outside the safe filename set at the door.
				case "$2" in *[!A-Za-z0-9._/-]*|'')
					echo "unsafe --src path: $2" >&2
					exit 2 ;;
				esac
				srcs="$srcs $2"; shift 2 ;;
			--hash) hashes="$hashes $2"; shift 2 ;;
			--base-ref) [ -n "$2" ] || usage; base_ref=$2; shift 2 ;;
			*) usage ;;
		esac
	done
	{ [ -n "$worktree" ] && [ -n "$test_cmd" ] && [ -n "$srcs" ]; } || usage
	require_tool git

	status_output=$(git -C "$worktree" status --porcelain) || {
		echo "DIRTY-TREE: cannot inspect the worktree; refusing to continue." >&2
		exit 2
	}
	if [ -n "$status_output" ]; then
		echo "DIRTY-TREE: the gate re-derives from committed state; commit or clean first." >&2
		exit 2
	fi

	for pair in $hashes; do
		file=${pair%%=*}
		want=${pair#*=}
		got=$(git -C "$worktree" hash-object "$file") || fail "FREEZE-FAIL: cannot hash $file"
		if [ "$got" != "$want" ]; then
			fail "FREEZE-FAIL: $file is $got, red-time hash was $want (test edited between red and green)"
		fi
		printf 'FREEZE-OK: %s\n' "$file"
	done

	# --end-of-options: a ref is data, never a git option, whatever it looks like.
	base_sha=$(git -C "$worktree" rev-parse --verify --quiet --end-of-options "${base_ref}^{commit}") || {
		echo "BAD-BASE-REF: '$base_ref' does not resolve to a single existing commit" >&2
		exit 2
	}
	for src in $srcs; do
		if ! git -C "$worktree" cat-file -e "HEAD:$src" 2>/dev/null; then
			ignored=$(git -C "$worktree" status --porcelain=v1 --ignored --untracked-files=all -- "$src") || {
				echo "DIRTY-TREE: the gate re-derives from committed state; commit or clean first." >&2
				exit 2
			}
			case "$ignored" in
				*'!! '* )
					echo "DIRTY-TREE: the gate re-derives from committed state; commit or clean first." >&2
					exit 2 ;;
			esac
		fi
	done
	# INT/TERM trapped explicitly: under dash (Linux /bin/sh) an EXIT trap does NOT run
	# on an untrapped signal, which would strand the src paths at the base ref.
	trap 'restore_srcs' EXIT
	trap 'restore_srcs; trap - EXIT; exit 130' INT TERM
	# shellcheck disable=SC2086 # srcs is a space-separated path list by construction
	git -C "$worktree" checkout "$base_sha" -- $srcs || fail "REVERT-FAIL: could not check out $base_ref src paths"

	if (cd "$worktree" && sh -c "$test_cmd" >/dev/null 2>&1); then
		fail "RED-FAIL: test passed against $base_ref src -- it does not pin the defect"
	fi
	printf 'RED-OK: test fails against %s src\n' "$base_ref"

	restore_srcs || fail "RESTORE-FAIL"
	trap - EXIT INT TERM

	if ! (cd "$worktree" && sh -c "$test_cmd" >/dev/null 2>&1); then
		fail "GREEN-FAIL: test fails against HEAD -- the fix does not satisfy its own pin"
	fi
	printf 'GREEN-OK: test passes against HEAD\nVERDICT: PASS\n'
}

case "${AGENT_SOURCE_ONLY:-0}" in
	1) ;;
	*) main "$@" ;;
esac
