#!/bin/sh
# statusline-rolling.sh <key> <cumulative-value> — print a humanized rolling delta of a
# monotonically growing counter over STATUSLINE_SAVINGS_WINDOW (Nh/Nd/Ns; default 24h;
# off|0|lifetime = print the humanized value as-is). Samples live in
# ${XDG_CACHE_HOME:-~/.cache}/statusline-rolling/<key>, throttled to one per 30s; a counter
# regression (stats reset) drops the stale samples so the window restarts cleanly.
# Prints nothing for a zero/empty result — callers render their no-data badge shape.
set -eu

key=$1
val=$2
case $val in '' | *[!0-9]*) exit 0 ;; esac

humanize() {
	if [ "$1" -ge 1000000 ]; then
		awk -v d="$1" 'BEGIN{printf "%.1fM", d / 1000000}'
	elif [ "$1" -ge 10000 ]; then
		printf '%sk' $(($1 / 1000))
	elif [ "$1" -gt 0 ]; then
		printf '%s' "$1"
	fi
}

win="${STATUSLINE_SAVINGS_WINDOW:-24h}"
case $win in
	off | 0 | lifetime)
		humanize "$val"
		exit 0
		;;
	*h) secs=$((${win%h} * 3600)) ;;
	*d) secs=$((${win%d} * 86400)) ;;
	*s) secs=${win%s} ;;
	*) secs=86400 ;;
esac

dir="${XDG_CACHE_HOME:-$HOME/.cache}/statusline-rolling"
mkdir -p "$dir"
f="$dir/$key"
now=$(date +%s)

# Keep in-window, non-regressed samples; append the current one unless a fresh sample exists.
awk -v now="$now" -v cutoff="$((now - secs))" -v val="$val" '
	NF == 2 && $1 >= cutoff && $2 <= val { print; last = $1 }
	END { if (now - last >= 30 || !last) print now, val }
' "$f" 2>/dev/null >"$f.tmp" || printf '%s %s\n' "$now" "$val" >"$f.tmp"
mv "$f.tmp" "$f"

baseline=$(awk 'NR == 1 {print $2}' "$f")
humanize $((val - ${baseline:-val}))
