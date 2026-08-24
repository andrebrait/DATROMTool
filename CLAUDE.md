# Claude Code adapter — DATROMTool

[`AGENTS.md`](AGENTS.md) is the canonical, vendor-neutral agent policy bootstrap; this file
is only the Claude Code adapter. The import below inlines it — if it did not expand, read
`AGENTS.md` now and follow it, including its routing table into `.agents/policy/` and
`.agents/context/`.

@AGENTS.md

## Claude-only surfaces

- Hooks live in `.claude/settings.json` (SessionStart / UserPromptSubmit / SubagentStart
  capsules, token-savior capture, statusline); skills at `.claude/skills/` are symlinks onto
  the canonical `.agents/skills/`.
- The shared git hooks recognise Claude via `CLAUDECODE=1`; Claude's verified coauthor
  identity is `Claude <noreply@anthropic.com>`.
- Claude sessions may start inside a harness session worktree (`.claude/worktrees/…`) — see
  `.agents/policy/sessions.md`.
- Code lookup: prefer the `mcp__token-savior-recall__` MCP tools (`search_codebase`,
  `find_symbol`, `get_function_source`, `get_call_chain`; load via ToolSearch) over raw
  Grep/Read whole-file dumps when locating a symbol or reading a single function/class
  body; fall back to Grep/Read only for files the index does not cover.
