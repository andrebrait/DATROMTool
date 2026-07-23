# RTK - Rust Token Killer

**Usage**: Token-optimized CLI proxy (60-90% savings on dev operations).

## Golden rule

**Always prefix shell commands with `rtk`.** If RTK has a dedicated filter it uses it; if
not, the command passes through unchanged — so `rtk` is always safe. This holds inside
command chains too:

```bash
# Correct
rtk git add . && rtk git commit -m "msg" && rtk git push
```

## Commands by workflow

### Build & test (Java / Maven)

```bash
rtk mvn verify          # JUnit failures surfaced, noise trimmed
rtk mvn package         # Package output compacted
rtk mvn -B clean compile test-compile
```

### Git (59-80% savings)

```bash
rtk git status          # Compact status
rtk git log             # Compact log (all flags)
rtk git diff            # Compact diff
rtk git show            # Compact show
rtk git add / commit / push / pull / branch / fetch / worktree
```

Passthrough works for ALL git subcommands, even those not listed.

### GitHub (26-87% savings)

```bash
rtk gh pr view <num>    # Compact PR view
rtk gh pr checks        # Compact PR checks
rtk gh run list         # Compact workflow runs
rtk gh issue list       # Compact issue list
rtk gh api              # Compact API responses
```

### Files & search (60-75% savings)

```bash
rtk ls <path>           # Tree format, compact
rtk read <file>         # Code reading with filtering
rtk grep <pattern>      # Search grouped by file (format flags -c/-l/-L/-o/-Z run raw)
rtk find <pattern>      # Find grouped by directory
```

### Meta commands (always use rtk directly)

```bash
rtk gain                # Token-savings analytics
rtk gain --history      # Command usage history with savings
rtk discover            # Analyze Claude Code history for missed opportunities
rtk proxy <cmd>         # Execute raw command without filtering (for debugging)
rtk --version           # Verify install (name collision: reachingforthejack/rtk lacks `gain`)
```

## Hook-based usage

Commands are auto-rewritten by the Claude/Codex PreToolUse hook
(`.claude/hooks/rtk-hook.sh`): `git status` → `rtk git status`, transparently, 0 tokens
overhead. The hook strips RTK's self-issued permission decisions so the permission system
still judges the rewritten command.
