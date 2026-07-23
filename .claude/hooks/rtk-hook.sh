#!/bin/sh
# rtk-hook.sh — PreToolUse Bash hook: rtk's auto-rewrite (e.g. `git status` -> `rtk git status`)
# with the permissionDecision fields STRIPPED — `rtk hook claude` self-allows mutating commands
# (git push/commit, gh pr merge), which would bypass the permission prompts and allow/deny lists.
# Only the updatedInput rewrite survives; the permission system judges the rewritten command.
# Pass-through no-op when rtk or jq is absent (e.g. a cloud session without the binary).
# No PATH prepend here: the rewrite must activate only when the Bash tool shell itself
# resolves `rtk`, or every rewritten command dies with "rtk: command not found".
set -eu

if command -v rtk >/dev/null 2>&1 && command -v jq >/dev/null 2>&1; then
	rtk hook claude | jq -c 'del(.hookSpecificOutput.permissionDecision, .hookSpecificOutput.permissionDecisionReason)'
else
	cat >/dev/null
	printf '{"continue": true}\n'
fi
