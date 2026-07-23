# Claude Code adapter — DATROMTool

[`AGENTS.md`](AGENTS.md) is the canonical, vendor-neutral agent policy bootstrap; this file
is only the Claude Code adapter. The import below inlines it — if it did not expand, read
`AGENTS.md` now and follow it, including its routing table into `.agents/policy/` and
`.agents/context/`.

@AGENTS.md

## Claude-only surfaces

- Hooks live in `.claude/settings.json` (SessionStart / UserPromptSubmit / SubagentStart
  capsules, RTK rewrite, token-savior capture, statusline); skills at `.claude/skills/` are
  symlinks onto the canonical `.agents/skills/`.
- The shared git hooks recognise Claude via `CLAUDECODE=1`; Claude's verified coauthor
  identity is `Claude <noreply@anthropic.com>`.
- Claude sessions may start inside a harness session worktree (`.claude/worktrees/…`) — see
  `.agents/policy/sessions.md`.
- Code lookup: prefer the `mcp__token-savior-recall__` MCP tools (`search_codebase`,
  `find_symbol`, `get_function_source`, `get_call_chain`; load via ToolSearch) over raw
  Grep/Read whole-file dumps when locating a symbol or reading a single function/class
  body; fall back to Grep/Read only for files the index does not cover.

## RTK (Rust Token Killer) — token-optimized commands

**Always prefix shell commands with `rtk`** (even inside `&&` chains). If RTK has a dedicated
filter it uses it; otherwise the command passes through unchanged, so `rtk` is always safe.
Full command reference in [`RTK.md`](RTK.md). Common ones here:

```bash
rtk mvn verify          # Maven test output, failures surfaced
rtk mvn package         # Build output compacted
rtk git status          # Compact status (also log/diff/show/add/commit/push)
rtk gh pr view <num>    # Compact PR view (also pr checks, run list, issue list)
rtk grep <pattern>      # Search grouped by file
rtk gain                # Token-savings analytics
```
