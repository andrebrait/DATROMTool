#!/bin/sh
# rtk-install.sh — SessionStart hook: make RTK available, then trust the checked-in filters
# from the current worktree. When absent, verify the immutable upstream installer pinned below,
# use it to install the pinned release to ~/.local/bin, best-effort symlink it into
# /usr/local/bin, and update shell profiles for the next shell. Never fails session start.
set -u

hook_dir=$(CDPATH='' cd "$(dirname "$0")" && pwd -L) || exit 0
# shellcheck source=../../scripts/lib/git-env-scrub.sh
. "$hook_dir/../../scripts/lib/git-env-scrub.sh"
scrub_git_env

rtk_bin=$(command -v rtk 2>/dev/null) || rtk_bin=''
if [ -z "$rtk_bin" ] && [ -x "$HOME/.local/bin/rtk" ]; then
	rtk_bin="$HOME/.local/bin/rtk"
fi

if [ -z "$rtk_bin" ]; then
	rtk_version='v0.43.0'
	rtk_installer_commit='5a7880d404db8364d602f2ecdc41dd790f64013f'
	rtk_installer_sha256='d6eb73a772903e13ff34ee1be8a8b24e896ba9a978f20d2279a08b4083ea6f77'
	rtk_installer=$(mktemp "${TMPDIR:-/tmp}/rtk-install.XXXXXX") || exit 0
	trap 'rm -f "$rtk_installer"' 0
	trap 'exit 0' HUP INT TERM
	curl -fsSL "https://raw.githubusercontent.com/rtk-ai/rtk/${rtk_installer_commit}/install.sh" \
		-o "$rtk_installer" >/dev/null 2>&1 || exit 0
	if command -v sha256sum >/dev/null 2>&1; then
		rtk_installer_actual=$(sha256sum "$rtk_installer" 2>/dev/null) || exit 0
	elif command -v shasum >/dev/null 2>&1; then
		rtk_installer_actual=$(shasum -a 256 "$rtk_installer" 2>/dev/null) || exit 0
	else
		exit 0
	fi
	rtk_installer_actual=${rtk_installer_actual%% *}
	[ "$rtk_installer_actual" = "$rtk_installer_sha256" ] || exit 0
	{ RTK_VERSION="$rtk_version" RTK_SKIP_CHECKSUM=0 sh "$rtk_installer"; } >/dev/null 2>&1 || exit 0

	[ -x "$HOME/.local/bin/rtk" ] || exit 0
	rtk_bin="$HOME/.local/bin/rtk"
	if [ -w /usr/local/bin ] && [ ! -e /usr/local/bin/rtk ]; then
		ln -s "$rtk_bin" /usr/local/bin/rtk 2>/dev/null
	fi
	for f in "$HOME/.profile" "$HOME/.bashrc"; do
		# shellcheck disable=SC2016 # literal $HOME/$PATH wanted: the profile line must expand at ITS read time
		grep -qs '\.local/bin' "$f" || printf 'export PATH="$HOME/.local/bin:$PATH"\n' >> "$f"
	done
fi

project_root=${CLAUDE_PROJECT_DIR:-}
if [ -z "$project_root" ]; then
	git_cdup=$(git rev-parse --show-cdup 2>/dev/null) || exit 0
	project_root=$(CDPATH='' cd "${git_cdup:-.}" && pwd -L) || exit 0
fi
filter_rel='.rtk/filters.toml'
filter_dir="$project_root/.rtk"
filter_path="$filter_dir/filters.toml"
[ -n "$project_root" ] && [ -d "$filter_dir" ] && [ ! -L "$filter_dir" ] || exit 0
[ -f "$filter_path" ] && [ ! -L "$filter_path" ] || exit 0
filter_head=$(git -C "$project_root" rev-parse "HEAD:$filter_rel" 2>/dev/null) || exit 0
filter_stage=$(git -C "$project_root" ls-files --stage -- "$filter_rel" 2>/dev/null) || exit 0
filter_expected=$(printf '100644 %s 0\t%s' "$filter_head" "$filter_rel")
[ "$filter_stage" = "$filter_expected" ] || exit 0
filter_work=$(git -C "$project_root" hash-object --no-filters -- "$filter_rel" 2>/dev/null) || exit 0
[ "$filter_work" = "$filter_head" ] || exit 0
(CDPATH='' cd "$project_root" && "$rtk_bin" trust >/dev/null 2>&1) || exit 0
exit 0
