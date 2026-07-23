#!/bin/sh
# statusline.sh — the repo statusline chain: ponytail + caveman mode badges (vendored
# plugin scripts), then the caveman/TS/RTK savings badges. STATUSLINE_SAVINGS_WINDOW
# (default 24h; off|0|lifetime = lifetime totals) makes every savings number a rolling
# delta via statusline-rolling.sh. CAVEMAN_STATUSLINE_INLINE (default 1) folds caveman's
# number into the badge — `[CAVEMAN 252k↓]`; 0 keeps the plugin-style ` ⛏ 252k` suffix
# (and, with the window off, the plugin's own untouched lifetime suffix).
set -eu

d="${CLAUDE_PROJECT_DIR:-.}/.claude"
# Badge scripts: ponytail's lives in-repo (plugins/); caveman's only in the client's
# plugin cache — resolve the newest cached version, skip the badge when absent.
caveman_dir=$(ls -td "${CLAUDE_CONFIG_DIR:-$HOME/.claude}"/plugins/cache/caveman/caveman/*/ 2>/dev/null | head -1)
caveman_sl="${caveman_dir}src/hooks/caveman-statusline.sh"
bash "${CLAUDE_PROJECT_DIR:-.}/plugins/ponytail/hooks/ponytail-statusline.sh"
printf ' '

case ${STATUSLINE_SAVINGS_WINDOW:-24h} in
	off | 0 | lifetime) lifetime=1 ;;
	*) lifetime=0 ;;
esac
inline="${CAVEMAN_STATUSLINE_INLINE:-1}"

if [ -z "$caveman_dir" ] || [ ! -f "$caveman_sl" ]; then
	: # no cached caveman plugin — skip its badge
elif [ "$lifetime" = 1 ] && [ "$inline" = 0 ]; then
	bash "$caveman_sl"
else
	badge=$(CAVEMAN_STATUSLINE_SAVINGS=0 bash "$caveman_sl")
	saved=$(sh "$d/hooks/caveman-rolling.sh")
	if [ -n "$saved" ] && [ "$inline" != 0 ]; then
		printf '%s' "$badge" | sed "s/]/ ${saved}↓]/"
	else
		printf '%s' "$badge"
		if [ -n "$saved" ]; then
			printf ' \033[38;5;172m⛏ %s\033[0m' "$saved"
		fi
	fi
fi

sh "$d/hooks/ts-statusline.sh"
sh "$d/hooks/rtk-statusline.sh"
