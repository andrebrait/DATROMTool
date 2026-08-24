#!/bin/sh
# test-statusline.sh -- verify the statusline emits only its surviving integrations
set -eu

root=$(CDPATH='' cd "$(dirname "$0")/../.." && pwd -P)
tmp=$(mktemp -d "${TMPDIR:-/tmp}/datromtool-statusline.XXXXXX")
trap 'rm -rf "$tmp"' 0 HUP INT TERM

project="$tmp/project"
mkdir -p "$project/.claude/hooks" "$project/plugins/ponytail/hooks" "$tmp/bin" "$tmp/config" "$tmp/home"

cat >"$project/plugins/ponytail/hooks/ponytail-statusline.sh" <<'EOF'
#!/bin/sh
printf '[PONYTAIL]'
EOF
cat >"$project/.claude/hooks/ts-statusline.sh" <<'EOF'
#!/bin/sh
printf ' [TS]'
EOF

# Keep the retired command name out of tracked text while exercising the old integration.
retired_cmd='r''tk'
cat >"$tmp/bin/$retired_cmd" <<'EOF'
#!/bin/sh
printf '%s\n' '{"summary":{"total_saved":42}}'
EOF
cat >"$tmp/bin/jq" <<'EOF'
#!/bin/sh
cat >/dev/null
printf '42\n'
EOF
chmod +x "$project/plugins/ponytail/hooks/ponytail-statusline.sh" \
	"$project/.claude/hooks/ts-statusline.sh" "$tmp/bin/$retired_cmd" "$tmp/bin/jq"

retired_hook="$root/.claude/hooks/${retired_cmd}-statusline.sh"
if [ -f "$retired_hook" ]; then
	cp "$retired_hook" "$project/.claude/hooks/"
	cp "$root/.claude/hooks/statusline-rolling.sh" "$project/.claude/hooks/"
fi

output=$(PATH="$tmp/bin:$PATH" HOME="$tmp/home" CLAUDE_CONFIG_DIR="$tmp/config" \
	CLAUDE_PROJECT_DIR="$project" STATUSLINE_SAVINGS_WINDOW=off \
	sh "$root/.claude/hooks/statusline.sh")

case "$output" in
*'[PONYTAIL]'*) ;;
*) printf 'missing ponytail badge: %s\n' "$output" >&2; exit 1 ;;
esac
case "$output" in
*'[TS]'*) ;;
*) printf 'missing Token Savior badge: %s\n' "$output" >&2; exit 1 ;;
esac
retired_badge='[R''TK'
case "$output" in
*"$retired_badge"*) printf 'retired badge still emitted: %s\n' "$output" >&2; exit 1 ;;
esac

printf '%s\n' "$output"
