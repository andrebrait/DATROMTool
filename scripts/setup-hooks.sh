#!/bin/sh
# One-time developer setup: point git at the repo's tracked hooks in .githooks.
#
# Run once after cloning (idempotent — safe to re-run):
#   sh scripts/setup-hooks.sh
#
# git cannot auto-apply a committed core.hooksPath (by design — cloning a repo
# must not silently install executable hooks), so this explicit opt-in is the
# closest to automatic. After running it, .githooks/prepare-commit-msg and
# .githooks/pre-push are active in this clone.

set -eu

root=$(git rev-parse --show-toplevel)

if [ "$(git -C "$root" config core.hooksPath 2>/dev/null || true)" = ".githooks" ]; then
	printf 'core.hooksPath already set to .githooks — nothing to do.\n'
else
	git -C "$root" config core.hooksPath .githooks
	printf 'core.hooksPath set to: %s\n' "$(git -C "$root" config core.hooksPath)"
fi

printf 'Active hooks:\n'
for hook in "$root"/.githooks/*; do
	[ -f "$hook" ] && printf '  %s\n' "$(basename "$hook")"
done
