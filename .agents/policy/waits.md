# No orphaned waits — every trigger dies with its task

Scope: any background wait, poll, cron check-in, wakeup, or subscription. Load when:
waiting on anything external (CI, a reviewer bot, a remote queue) or arming any timer.

**First — is there anything to wait on at all?** Harness-tracked work re-invokes you when it
finishes: a `Workflow`, an `Agent`, a Bash call with `run_in_background: true`. For those,
**arm nothing** — no sleep, no poll, no `ScheduleWakeup`; end the turn and answer the
completion notification. A wait is for state the harness cannot see (a reviewer bot, a CI
run, a remote queue). Polling your own tracked work is pure waste, and each expiring timer
re-invokes you into thinking the wait is still unresolved — that loop, not the first sleep,
is the real defect.

Background waits have been found running **20+ hours** after their task ended; there is no
platform-level timeout on polls/cron/`ScheduleWakeup`/subscriptions, so the guarantees are
ours. Four, ALL mandatory:

1. **Self-terminating by construction.** Every background wait carries a hard iteration cap
   AND a wall-clock deadline *inside the loop itself* (`scripts/agent/wait-*.sh` are the
   exemplars — and the standard transport when `gh` exists) so it dies on its own even if orphaned. A wait without a cap is a defect —
   never launch one. Event waits also follow the heartbeat ladder (10, 10/10/15/15/30/30 min,
   ≈2 h total, then give up + report the wait abandoned; never re-arm past it).
2. **Cancel-on-resolution sweep.** The instant a work item reaches a terminal state by ANY
   path — success, failure, give-up, or a user-driven check that supersedes the wait — sweep
   every trigger tied to it, by class: background polls → `TaskStop`; cron check-ins →
   `CronDelete`; PR/event subscriptions → unsubscribe. `ScheduleWakeup` **cannot be
   cancelled**, so: **never one long wakeup at a speculative future time** — arm the
   SHORTEST sensible rung, do a minimal state check on firing, and re-arm the next rung
   only if still unresolved (the ladder). Every wakeup prompt uses the self-invalidating
   template: `CHECK <concrete state/command>; IF RESOLVED: no-op, do NOT re-arm; ELSE <next action> + re-arm <n> min`. Wakeups are a *fallback* to harness completion
   notifications, never the primary wake. The wait-spawning skills carry this sweep as an
   explicit terminal step; it is not optional and not from memory.
3. **Pickup hygiene.** When starting or finishing any work item, run `TaskList` once and
   stop every stale wait you own from earlier items. If the task moved on, its future
   triggers are dead — good or bad outcome alike.
4. **Portability — no `gh`, no bash polls.** Background bash loops presume the local
   toolbox (`gh`); managed environments may lack it, and MCP tools are harness tools —
   unreachable from inside a shell loop. Detect once at task start (`command -v gh` +
   `gh auth status`); when absent, do GitHub reads/writes via the `mcp__github__*`
   equivalents and run every wait as **wakeup-paced checks**: one minimal MCP state check
   now → still unresolved → `ScheduleWakeup` the next ladder rung (self-invalidating
   template) → repeat. Same rungs, same 2 h cap, same sweep — only the transport changes.

## The full ladder

Two independent guards, **both required** — but only once §0 says a wait is warranted at all:

### 0 — First: is the awaited thing harness-tracked? Then do not wait on it

`Workflow`, `Agent`, and Bash with `run_in_background: true` are **tracked**: their
completion re-invokes you. Arm **nothing** for them — no background `sleep`, no poll, no
`ScheduleWakeup`. Launch, end the turn, answer the notification.

Only **untracked** state gets a wait, and it is always a wait on something the harness has
no visibility into:

| Awaited thing | Tracked? | Correct move |
| ------------- | -------- | ------------ |
| A `Workflow` you launched | yes | end the turn; the completion notification wakes you |
| An `Agent` / subagent you spawned | yes | same |
| `wait-checks.sh` / `wait-reviewer.sh` run with `run_in_background: true` | yes | same — the script self-exits and notifies; do not also poll it |
| CodeRabbit posting a review; a CI run; a remote queue | **no** | a bounded wait: the script above, or the ladder in §1 |

The ladder's self-invalidating discipline applies to **every** timer you arm, not just
`ScheduleWakeup`: on firing, CHECK the concrete state first; if resolved, no-op and do NOT
re-arm. A chain of re-armed sleeps with no resolution check is an unbounded ladder wearing a
cap — the cap is per-rung, and the loop never ends.

### 1 — Never trust the event trigger alone: arm a self-check heartbeat ladder

A trigger can be mis-wired (wrong PR/run id, a webhook that never arrives) — then the
event-driven wake never fires. So **always** also arm a *self*-check-in, independent of the
event:

- **First self-check: 10 minutes** after arming the wait — wake and **check the real state
  yourself** (poll the PR / CI run / job directly via its CLI or API).
- **If still unresolved, re-arm on the ladder: 10, 10, 15, 15, 30, 30 minutes** — six further
  self-checks. Total budget ≈ **120 min (2 h)** across the seven checks.
- **After the final 30-minute rung with the awaited thing still not done → give up and die:**
  `unsubscribe` / `CronDelete` the check-in (and any subscription), then report that the wait
  was **abandoned because the event never fired** and that the trigger may have been
  mis-configured. **Never re-arm past the ladder.**
- **Any check where the awaited thing HAS happened ends the ladder early.** Genuine in-flight
  progress (CI still legitimately running) does not reset the ladder; the 2 h cap is hard —
  extending it is the user's call, never a silent re-arm.
- **Cancel on resolution — leave no orphaned trigger.** The instant the task reaches a
  terminal state by any path — a self-check or the event finds it done (good or bad), the
  give-up rung is hit, or the user interrupts to ask you to check — cancel **every**
  still-pending trigger tied to it (`CronDelete`, drop the `ScheduleWakeup`, `unsubscribe`).
  A user-driven check supersedes the scheduled ones. If the task moved on, its future
  triggers are dead.

### 2 — Event-deadline on the happy path

When waiting on a normal event (CI green, a PR merge, a queued job), the event-driven wait
still carries its own **explicit deadline** — never an open-ended re-arm. Default cap: the
same 2 h / seven-check budget unless the user sets a longer one.

### 3 — The cancel-on-resolution sweep, per trigger class

Background waits have been found alive **20+ hours** after their task ended. The sweep runs
the moment the awaited item reaches ANY terminal state, and again at work-item pickup/finish
(`TaskList` once; stop stale waits you own):

| Trigger class | Kill mechanism | Notes |
| ------------- | -------------- | ----- |
| Background Bash poll (`run_in_background`) | `TaskStop <task-id>` | Also self-terminating by construction: a hard iteration cap + wall-clock deadline INSIDE the loop. A poll without both is a defect — never launch it. |
| Cron check-in | `CronDelete` | The heartbeat ladder's rungs are crons — delete every remaining rung on resolution, not just the next. |
| PR/event subscription | unsubscribe | A user-driven check supersedes the subscription — kill it then and there. |
| `ScheduleWakeup` | **none — cannot be cancelled** | Fires regardless. Therefore: (a) FALLBACK only — harness completion notifications are the primary wake; (b) **short rung + minimal check + re-arm**, never one long wakeup at a speculative future time: arm the shortest sensible delay, and on firing do a minimal state check and re-arm the next ladder rung only if still unresolved (pick the rung by how fast the watched state actually changes; slow externals take 10 min+ rungs); (c) fixed self-invalidating prompt template: `CHECK <concrete state/command>; IF RESOLVED: no-op, do NOT re-arm; ELSE <next action> + re-arm <n> min` — a stale firing then costs one cheap turn. |

The sweep is an explicit terminal step of every wait-spawning flow (the review/CI waits in
[`landing.md`](landing.md)) — mechanical, not remembered.
It cannot be a workflow: `TaskStop`/`CronDelete` are orchestrator tools, invisible to
workflow agents.

### 4 — Managed environments (no `gh`): wakeup-paced MCP checks

Background bash polls presume `gh`; managed (web/app) environments may not ship it, and MCP
tools cannot be called from inside a shell loop. The portable wait:

1. **Detect once** at Step 0/1 of the skill: `command -v gh && gh auth status`. Present →
   the bash-poll snippets as written. Absent → the adaptation below; never mix per-call.
2. **Reads/writes** go through the session's GitHub MCP server (`mcp__github__*` — discover
   the exact tools via ToolSearch; names vary by server). Same data, same verdict logic.
3. **Waits become wakeup-paced checks:** do one minimal MCP state check NOW; if unresolved,
   `ScheduleWakeup` the next ladder rung with the self-invalidating template, and on firing
   check again + re-arm. The rung ladder IS the poll cadence — same escalation, same 2 h
   hard cap, same give-up-and-report rung. The sweep simplifies: there is no bash task to
   `TaskStop`; the wakeups self-invalidate by template.
4. **Workflows and scripts are unaffected:** the named workflows use only `git` + local
   commands (both present in managed environments), and workflow agents reach MCP tools via
   ToolSearch when they genuinely need GitHub state. Caveat: interactively-authenticated MCP
   servers may be absent in headless/cron runs — a skill that finds NEITHER `gh` nor a
   GitHub MCP server stops and reports rather than improvising.
