# Session layouts and managed-remote sessions

Scope: where a session runs, where work-item worktrees go, and cross-session resume.
Load when: starting work in an unfamiliar environment or resuming another session's item.

## Session layouts (three environments, one rule)

A session may start in the primary
checkout (CLI, one per terminal) or inside a **harness-made session worktree** — rc-mode cuts
`<primary>/.claude/worktrees/bridge-<session-id>` (branch `worktree-bridge-*`, locked);
managed environments cut one worktree per session named after the first-prompt issue. Detect
mechanically, never from memory: `git rev-parse --git-dir --git-common-dir` differing ⇒ you
are in a linked worktree. A session worktree is the harness's **orchestration home, not the
work-item worktree**: cut the per-item worktree from wherever you sit
(`scripts/agent/work-branch.sh --worktree` anchors it at the primary root — never derive
placement from `--show-toplevel`, which names the session tree and nests worktrees inside a
harness-lifecycle tree). Sole exception: the environment hard-pins pushes to the session
branch — then that branch replaces the convention per the managed-remote policy below.

## Managed-remote sessions: branch policy + cross-session resume

A managed remote (web/app) session mints its own branch — `claude/<slug>-<rand>`. Where you
may push is set by the environment's branch-push policy (configured in the environment/
trigger, not this repo — see <https://code.claude.com/docs/en/claude-code-on-the-web>). Two
configurations, in order of preference:

**Session-worktree environments (probe before assuming).** Some environments also cut **one
linked worktree per session** and start the session inside it: rc-mode (mobile Devices view)
at `<primary>/.claude/worktrees/bridge-<session-id>` on branch `worktree-bridge-<session-id>`
(pushes NOT pinned — skills mint canonical branches as usual), managed environments one
worktree per session named after the first-prompt issue. The session worktree is the
orchestration home, never the work-item worktree (see "Session layouts" above). In an
unfamiliar environment, probe at task start instead of assuming:
`git rev-parse --git-dir --git-common-dir` (differing ⇒ linked worktree) and the push policy;
an unverified "pushes are pinned here" is ASSUMED and must be probed before it shapes branch
choice.

**Preferred — allow pushing to the canonical work-item branch.** Configure the push policy to
permit the repo's own `adr/{NN}-{slug}` and `issue/{NN}-{slug}` branches (keep `master`
PR-only for code). Then a managed-remote session works the item on its canonical branch
exactly as locally, and resume is native — recompute the slug
(`scripts/agent/work-branch.sh`) and find the existing branch with no archaeology. The hard
"never push anywhere but the session branch" pin is the wrong default for this repo — always
carve out this exception.

**One branch per work item.** Whichever model is in force, a branch belongs to the single
issue/ADR it was opened for. Asked to handle a different item than the branch was minted for
⇒ cut a new branch named for the new item off the latest `origin/master` and push there. Only
when the environment hard-pins pushes to that one stale branch is reuse acceptable — and then
flag the name/item mismatch to the user before proceeding.

**Fallback — push hard-pinned to the minted `claude/*` branch.** The pinned branch replaces
the convention for the session. Each session gets a fresh branch, so work spans sessions only
if a resuming session finds the prior one:

- **Record the override loudly + machine-readably** in the first handoff: the prose override
  plus a greppable sentinel `ADR-RESUME: branch=<actual-branch> next-phase=<N>` (or
  `ISSUE-RESUME:` for an issue).
- **Before starting an ADR/issue fresh, DISCOVER prior work:** `git fetch origin`; scan remote
  branches for that item's committed handoffs (`RESULTS/{NN}_*`) and the `*-RESUME:` sentinel;
  select the candidate with the highest contiguous completed phase.
- **Resume by fast-forward onto your own branch** (push is pinned): replay/cherry-pick the
  discovered commits onto the current session branch (shared base `master` ⇒ clean linear
  replay), continue the remaining phases, push to *your* branch, carry the sentinel forward
  with an updated `next-phase`.
- **Auto-resume WITHOUT asking iff unambiguous:** exactly one viable candidate, a valid
  sentinel, no sign of a concurrent live session. `AskUserQuestion` only on genuine ambiguity.
