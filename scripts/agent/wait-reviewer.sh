#!/bin/sh
# wait-reviewer.sh -- poll a PR until a bot/human reviewer engages or finishes.
# The single implementation of the reviewer-wait state machine the landing policy
# references (content-first FINISHED, CodeRabbit QUOTA with resume minutes, DECLINE/PAUSE,
# Snyk status handling, NOTPRESENT presence window).
#
# Usage: wait-reviewer.sh --repo OWNER/REPO --pr N --handle LOGIN [options]
#   --until ack|finished  ack = ANY message from the handle counts (default: finished)
#   --since ISO8601       only activity after this instant counts (default: the PR head
#                         commit time in finished mode; unset = all activity in ack mode)
#   --presence N          polls with zero engagement before NOTPRESENT (default 10; 0=off)
#   --interval SECONDS    poll interval (default 30)
#   --max-iter N          hard iteration cap (default: 20 ack / 60 finished)
#
# The LAST stdout line is the verdict; the handle's recent ISSUE comments (the notice
# bodies the verdicts parse) precede it as detail:
#   ACK | NOACK | FINISHED | QUOTA <mins> | DECLINE | PAUSE | NOTPRESENT | TIMEOUT
# Handle matching is case-insensitive and ANCHORED, so `--handle copilot` matches
# copilot-pull-request-reviewer[bot] and `coderabbitai` matches coderabbitai[bot], but a
# login that merely CONTAINS the handle does not count.
# `--handle snyk` reads the head-SHA commit status/check-runs instead of comments (Snyk
# posts no review comments); its `error` state reports QUOTA, never a clean pass.
# Login match is ANCHORED: == handle, == handle[bot], or startswith(handle-).
# A wall-clock deadline (max-iter x interval + 300 s slack; DRT_WAIT_DEADLINE overrides)
# bounds the wait even when individual gh calls stall.
# Exit codes: see agent_env.sh (0 verdict, 1 GH-ERROR after 3 failed polls, 2 usage,
# 3 gh unavailable -> MCP fallback, 4 TOOL-MISSING).
# Self-terminating by construction ("No orphaned waits"): iteration cap AND wall-clock
# deadline; run it in the background and read the verdict.

repo='' pr='' handle='' mode='finished' since='' presence=10 interval=30 max_iter=''
inline='' review='' issuec='' sinfo=''
inline_any='' review_any='' issuec_any=''

usage() {
	echo "usage: wait-reviewer.sh --repo O/R --pr N --handle LOGIN [--until ack|finished] [--since ISO] [--presence N] [--interval S] [--max-iter N]" >&2
	exit 2
}

# Decide the verdict from the currently fetched state; prints nothing = keep polling.
# Order is load-bearing: real review content is checked BEFORE any quota phrase, so a
# transient/stale rate-limit notice sitting beside actual comments never masks them.
classify() {
	if [ "$handle" = "snyk" ]; then
		if printf '%s' "$sinfo" | grep -Eqi 'limit reached|^(error|action_required|timed_out|cancelled|stale)'; then
			printf 'QUOTA 999'
			return 0
		fi
		if printf '%s' "$sinfo" | grep -Eqi '^(success|failure|neutral|completed)'; then
			printf 'FINISHED'
			return 0
		fi
		return 0
	fi
	if [ "$mode" = "ack" ]; then
		[ -n "${inline}${review}${issuec}" ] && printf 'ACK'
		return 0
	fi
	if [ -n "$inline" ] || [ -n "$review" ] || printf '%s' "$issuec" | grep -qiE 'actionable comments posted|no actionable comments'; then
		printf 'FINISHED'
		return 0
	fi
	if printf '%s' "$issuec" | grep -Eqi 'run out of usage credits|review limit reached|rate limited by coderabbit|reached your .*review (rate )?limit'; then
		mins=$(printf '%s' "$issuec" | grep -oEi 'available in:.{0,10}[0-9]+ *(minute|hour)' | grep -oE '[0-9]+' | head -1)
		if printf '%s' "$issuec" | grep -oEi 'available in:.{0,10}[0-9]+ *hour' | grep -q .; then
			mins=$(( ${mins:-1} * 60 ))
		fi
		printf 'QUOTA %s' "${mins:-999}"
		return 0
	fi
	if printf '%s' "$issuec" | grep -qi 'review skipped' &&
	   printf '%s' "$issuec" | grep -Eqi 'base branch|base branches|default branch'; then
		printf 'DECLINE'
		return 0
	fi
	if printf '%s' "$issuec" | grep -Eqi '^[[:space:]>#*-]*reviews? (are )?paused([[:space:].!]|$)|⏸'; then
		printf 'PAUSE'
		return 0
	fi
	return 0
}

# jq filter for one comment source: ANCHORED login match + optional time floor.
# Anchored (== handle, == handle[bot], startswith(handle-)) rather than free substring:
# a public account whose login merely CONTAINS the handle must not satisfy the wait,
# and a verbatim bracketed login must match itself (no regex-metachar surface).
jq_filter() {
	# $1 = timestamp field name, $2 = "notime" to skip the --since floor.
	# Presence must survive a rebase moving $since past a real prior review.
	# shellcheck disable=SC2016 # $l is jq syntax, not a shell expansion
	m=$(printf '((.user.login|ascii_downcase) as $l | ($l == "%s") or ($l == "%s[bot]") or ($l | startswith("%s-")))' "$handle" "$handle" "$handle")
	if [ -n "$since" ] && [ "$2" != "notime" ]; then
		printf '.[] | select(%s) | select(.%s > "%s")' "$m" "$1" "$since"
	else
		printf '.[] | select(%s)' "$m"
	fi
}

fetch_state() {
	fetch_error=''
	if [ "$default_since" -eq 1 ]; then
		if ! since=$(gh_bounded pr view "$pr" --repo "$repo" --json commits -q '.commits[-1].committedDate'); then
			fetch_error=$since
			since=''
			return 1
		fi
		default_since=0
	fi
	if ! fetch_inline=$(gh_bounded api "repos/$repo/pulls/$pr/comments" --paginate -q "$(jq_filter created_at) | .id"); then
		fetch_error=$fetch_inline
		return 1
	fi
	if ! fetch_review=$(gh_bounded api "repos/$repo/pulls/$pr/reviews" --paginate -q "$(jq_filter submitted_at) | (.body // \"x\")"); then
		fetch_error=$fetch_review
		return 1
	fi
	if ! fetch_issuec=$(gh_bounded api "repos/$repo/issues/$pr/comments" --paginate -q "$(jq_filter updated_at) | (.body // \"\")"); then
		fetch_error=$fetch_issuec
		return 1
	fi
	# Presence (any commit) vs content (since $since) split: only fetched when $since
	# narrows the content query, else presence == content already.
	if [ -n "$since" ]; then
		if ! fetch_inline_any=$(gh_bounded api "repos/$repo/pulls/$pr/comments" --paginate -q "$(jq_filter created_at notime) | .id"); then
			fetch_error=$fetch_inline_any
			return 1
		fi
		if ! fetch_review_any=$(gh_bounded api "repos/$repo/pulls/$pr/reviews" --paginate -q "$(jq_filter submitted_at notime) | .id"); then
			fetch_error=$fetch_review_any
			return 1
		fi
		if ! fetch_issuec_any=$(gh_bounded api "repos/$repo/issues/$pr/comments" --paginate -q "$(jq_filter updated_at notime) | .id"); then
			fetch_error=$fetch_issuec_any
			return 1
		fi
	else
		fetch_inline_any=$fetch_inline
		fetch_review_any=$fetch_review
		fetch_issuec_any=$fetch_issuec
	fi
	fetch_sinfo=''
	if [ "$handle" = "snyk" ]; then
		if ! fetch_sha=$(gh_bounded pr view "$pr" --repo "$repo" --json headRefOid -q .headRefOid); then
			fetch_error=$fetch_sha
			return 1
		fi
		if ! fetch_status=$(gh_bounded api "repos/$repo/commits/$fetch_sha/status" \
			-q '.statuses[] | select((.context|ascii_downcase)|test("snyk")) | "\(.state) \(.description)"'); then
			fetch_error=$fetch_status
			return 1
		fi
		if ! fetch_checks=$(gh_bounded api "repos/$repo/commits/$fetch_sha/check-runs" --paginate \
			-q '.check_runs[] | select((.name|ascii_downcase)|test("snyk")) | "\(.conclusion // .status) \(.output.title // "")"'); then
			fetch_error=$fetch_checks
			return 1
		fi
		fetch_sinfo="$fetch_status
$fetch_checks"
	fi
	inline=$fetch_inline
	review=$fetch_review
	issuec=$fetch_issuec
	inline_any=$fetch_inline_any
	review_any=$fetch_review_any
	issuec_any=$fetch_issuec_any
	sinfo=$fetch_sinfo
}

main() {
	# shellcheck source=scripts/agent/agent_env.sh
	. "$(dirname "$0")/agent_env.sh"
	while [ $# -gt 0 ]; do
		case "$1" in
			--repo) repo=$2; shift 2 ;;
			--pr) pr=$2; shift 2 ;;
			--handle) handle=$(printf '%s' "$2" | tr '[:upper:]' '[:lower:]'); shift 2 ;;
			--until) mode=$2; shift 2 ;;
			--since) since=$2; shift 2 ;;
			--presence) presence=$2; shift 2 ;;
			--interval) interval=$2; shift 2 ;;
			--max-iter) max_iter=$2; shift 2 ;;
			*) usage ;;
		esac
	done
	{ [ -n "$repo" ] && [ -n "$pr" ] && [ -n "$handle" ]; } || usage
	case "$mode" in ack|finished) ;; *) usage ;; esac
	require_gh
	require_tool timeout

	if [ -z "$max_iter" ]; then
		if [ "$mode" = "ack" ]; then max_iter=20; else max_iter=60; fi
	fi
	default_since=0
	if [ -z "$since" ] && [ "$mode" = "finished" ] && [ "$handle" != "snyk" ]; then
		default_since=1
	fi

	# Wall-clock deadline alongside the iteration cap ("No orphaned waits"): stalled gh
	# calls must not stretch the wait past its budget. DRT_WAIT_DEADLINE (epoch seconds)
	# overrides for tests/ops.
	deadline=${DRT_WAIT_DEADLINE:-$(( $(date +%s) + max_iter * interval + 300 ))}
	i=0
	seen=0
	ghfail=0
	while [ "$i" -lt "$max_iter" ]; do
		if [ "$(date +%s)" -ge "$deadline" ]; then
			break
		fi
		if ! fetch_state; then
			ghfail=$((ghfail + 1))
			if [ "$ghfail" -ge 3 ]; then
				printf 'GH-ERROR\n%s\n' "$fetch_error"
				exit 1
			fi
			i=$((i + 1))
			sleep_bounded "$interval" || break
			continue
		fi
		ghfail=0
		[ -n "${inline_any}${review_any}${issuec_any}$(printf '%s' "$sinfo" | tr -dc '[:lower:]')" ] && seen=1
		v=$(classify)
		if [ -n "$v" ]; then
			printf '%s\n' "$issuec" | head -c 3000
			printf '\n%s\n' "$v"
			exit 0
		fi
		if [ "$seen" -eq 0 ] && [ "$presence" -gt 0 ] && [ "$i" -ge "$presence" ]; then
			printf 'NOTPRESENT\n'
			exit 0
		fi
		i=$((i + 1))
		sleep_bounded "$interval" || break
	done
	if [ "$mode" = "ack" ]; then printf 'NOACK\n'; else printf 'TIMEOUT\n'; fi
}

case "${AGENT_SOURCE_ONLY:-0}" in
	1) ;;
	*) main "$@" ;;
esac
