---
name: new-terminal
description: Launch a new, detached interactive Claude Code instance that inherits THIS session's configuration directory (CLAUDE_CONFIG_DIR), starts in the same working directory, and — when the current session has remote control enabled — enables /remote-control on the new instance too. Use when the user says "new terminal", "spawn another claude", "launch a second instance", or invokes /new-terminal.
---

Launch a second, fully detached interactive Claude Code process that mirrors this
session's environment. Config-dir capture is the agnostic replacement for any
account-selecting shell alias (e.g. `alias claude2="CLAUDE_CONFIG_DIR=$HOME/.claude2 claude"`):
passing the current `CLAUDE_CONFIG_DIR` through reproduces the alias's effect without
knowing the alias exists.

## Steps

1. **Capture this session's environment** (all read from the Bash tool's own env —
   never hardcode paths or aliases):
   - `CFG="$CLAUDE_CONFIG_DIR"` — may be empty (default config); pass it through only
     when set.
   - `RC="$CLAUDE_CODE_BRIDGE_SESSION_ID"` — non-empty means this session has an
     active claude.ai bridge (remote control). Heuristic: it is the best
     machine-readable signal available; if it proves wrong someday, fix the signal
     here rather than prompting the user.
   - `DIR="$(pwd)"` — the new instance starts here.
   - `command -v claude` must resolve; if not, stop and report (do not guess an
     install path).

2. **Launch detached under a pseudo-tty.** Claude Code is a TUI: a plain
   `nohup claude &` dies without a tty, so wrap it in `script` (pty allocator), then
   `nohup … & disown` so it survives this session. Log goes to the session
   scratchpad. macOS `script` syntax (`script -q <file> <cmd> [args…]`); on Linux use
   `script -q -c "<cmd>" <file>` instead:

   ```sh
   LOG=<scratchpad>/new_terminal_$(date +%s).out
   PROMPT=""
   [ -n "$CLAUDE_CODE_BRIDGE_SESSION_ID" ] && PROMPT="/remote-control"
   cd "$DIR" && nohup script -q "$LOG" \
       env ${CLAUDE_CONFIG_DIR:+CLAUDE_CONFIG_DIR="$CLAUDE_CONFIG_DIR"} \
       claude ${PROMPT:+"$PROMPT"} >/dev/null 2>&1 & disown
   echo "pid=$!"
   ```

   Notes:
   - The sandbox blocks `nohup`/`disown` detachment — run this one command with
     `dangerouslyDisableSandbox: true`.
   - `${PROMPT:+"$PROMPT"}` passes `/remote-control` as the initial prompt only when
     the current session has the bridge active (step 1); otherwise the new instance
     starts idle.

3. **Verify it lives.** `sleep 8`, then `ps -p <pid>` (the `script` wrapper) must
   still be running. If remote control was requested, poll the log (strip ANSI:
   `perl -pe 's/\e\[[0-9;?]*[A-Za-z]//g'`) until it shows `/remote-control is active`
   and extract the `https://claude.ai/code/session_…` URL (give it ~30 s; report the
   log path if it never appears instead of looping forever).

4. **Report**: the wrapper PID, the working directory, the config dir passed through,
   whether `/remote-control` was sent, the claude.ai URL (when applicable), and the
   log path.
