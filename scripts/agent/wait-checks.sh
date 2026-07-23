#!/bin/sh
# wait-checks.sh -- poll a PR's checks until every non-excluded check completes.
# The single implementation of the CI wait.
#
# Usage: wait-checks.sh --repo OWNER/REPO --pr N [options]
#   --exclude REGEX    case-insensitive check-name exclusion (default 'coderabbit|snyk' --
#                      both are advisory and must never gate a merge)
#   --interval SECONDS poll interval (default 30)
#   --max-iter N       hard iteration cap (default 80, ~40 min at 30s)
#
# The LAST stdout line is the verdict: PASS | FAIL | TIMEOUT. On PASS/FAIL the relevant
# checks JSON precedes it as detail. `bucket` semantics: skipping counts as done-not-
# failed; PASS requires at least one relevant check registered (never green-by-absence).
# Exit codes: see agent_env.sh. Self-terminating: iteration cap AND wall-clock deadline
# (max-iter x interval + 300 s slack; DRT_WAIT_DEADLINE overrides) per "No orphaned waits".

repo='' pr='' exclude='coderabbit|snyk' interval=30 max_iter=80

usage() {
	echo "usage: wait-checks.sh --repo O/R --pr N [--exclude REGEX] [--interval S] [--max-iter N]" >&2
	exit 2
}

# Reduce one checks-JSON snapshot to PASS / FAIL / PENDING / EMPTY (prints verdict).
evaluate_checks() {
	# $1 = checks JSON array of {name, bucket}
	rel=$(printf '%s' "$1" | jq -c "[.[] | select((.name|ascii_downcase|test(\"$exclude\"))|not)]")
	total=$(printf '%s' "$rel" | jq 'length')
	fail=$(printf '%s' "$rel" | jq '[.[] | select(.bucket=="fail" or .bucket=="cancel")] | length')
	pend=$(printf '%s' "$rel" | jq '[.[] | select(.bucket=="pending")] | length')
	if [ "$fail" -gt 0 ]; then
		printf 'FAIL'
	elif [ "$total" -eq 0 ]; then
		printf 'EMPTY'
	elif [ "$pend" -eq 0 ]; then
		printf 'PASS'
	else
		printf 'PENDING'
	fi
}

main() {
	# shellcheck source=scripts/agent/agent_env.sh
	. "$(dirname "$0")/agent_env.sh"
	while [ $# -gt 0 ]; do
		case "$1" in
			--repo) repo=$2; shift 2 ;;
			--pr) pr=$2; shift 2 ;;
			--exclude) exclude=$2; shift 2 ;;
			--interval) interval=$2; shift 2 ;;
			--max-iter) max_iter=$2; shift 2 ;;
			*) usage ;;
		esac
	done
	{ [ -n "$repo" ] && [ -n "$pr" ]; } || usage
	require_gh
	require_tool timeout
	require_tool jq

	# Wall-clock deadline alongside the cap ("No orphaned waits"); DRT_WAIT_DEADLINE
	# (epoch seconds) overrides for tests/ops.
	deadline=${DRT_WAIT_DEADLINE:-$(( $(date +%s) + max_iter * interval + 300 ))}
	i=0
	ghfail=0
	while [ "$i" -lt "$max_iter" ]; do
		if [ "$(date +%s)" -ge "$deadline" ]; then
			break
		fi
		if ! json=$(gh_bounded pr checks "$pr" --repo "$repo" --json name,bucket); then
			# 3 consecutive gh failures = a real problem (bad repo/pr, auth, outage),
			# not "no checks yet" -- fail loudly instead of polling to a blind TIMEOUT.
			ghfail=$((ghfail + 1))
			if [ "$ghfail" -ge 3 ]; then
				printf 'GH-ERROR\n%s\n' "$json"
				exit 1
			fi
			i=$((i + 1))
			sleep_bounded "$interval" || break
			continue
		fi
		ghfail=0
		[ -n "$json" ] || json='[]'
		v=$(evaluate_checks "$json")
		case "$v" in
			PASS|FAIL)
				printf '%s' "$json" | jq -c "[.[] | select((.name|ascii_downcase|test(\"$exclude\"))|not)]"
				printf '%s\n' "$v"
				exit 0
				;;
		esac
		i=$((i + 1))
		sleep_bounded "$interval" || break
	done
	printf 'TIMEOUT\n'
}

case "${AGENT_SOURCE_ONLY:-0}" in
	1) ;;
	*) main "$@" ;;
esac
