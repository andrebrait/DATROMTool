# Fresh-session ticket workflow — the contract

- **Scope:** vendor-neutral execution protocol from a work item to a merged PR.
- **Load-when:** coordinating, claiming, executing, reviewing, or continuing a GitHub ticket
  under the fresh-session workflow.
- **Owner:** repo owner.

## Principles

- GitHub Issues and pull requests are the durable execution state; session transcripts are
  disposable.
- One bounded ticket per fresh top-level session (research fan-out excepted).
- No worker inherits a parent transcript; workers receive a task packet.
- Explorer, implementer, and reviewer run in fresh bounded contexts; the reviewer is
  independent and read-only.
- Mechanical gates and CI are mandatory; extra verification is risk-triggered.
- Approaching compaction means checkpoint and terminate; correctness-critical work never
  continues through compaction.
- No committed plan or handoff ledger duplicates GitHub state.

## Artifacts and schemas

### Spec

A repo-committed document: feature/system specs in `docs/specs/<slug>.md`; agent-protocol
policy in `.agents/policy/`. Required sections: **Goal**, **Fixed constraints**,
**Decisions**, **Acceptance criteria**, **Out of scope**, **Open forks** (each an issue link,
or "none"). A spec with open forks is not implementable — the forks are tickets.

### Task packet

The issue body IS the packet: a fresh session must be able to execute from it plus its linked
references alone. Required fields:

- **Objective** — the one outcome.
- **Required reading** — `file:line` and doc pointers (bootstrap routing rows), never pasted
  bodies.
- **Constraints** — the do-not-touch list; a packet may never weaken a policy mandate.
- **Verification** — the canonical gates plus per-item acceptance checks, each "WHEN
  `<command/input>` THEN `<observable>`".
- **Escalation** — a falsified premise or a mechanism the packet never named means STOP:
  checkpoint and route `needs-info`/`ready-for-human`; never silently patch the plan.

Conditional fields, mandatory when applicable:

- **Coverage matrix** — sibling axes enumerated from source (grep/definition output, never
  memory) whenever the change touches anything with siblings.
- **Hostile-input rows** — for any new or changed parser, regex, or input guard.
- **Risk triggers** — when present, name the extra verification they require.

### Claim

The assignee on the issue, set **before any work**. Open + unassigned = unclaimed; the
frontier is open + unblocked + unclaimed. One claimed ticket maps to one live session. Release
a claim by posting a checkpoint and unassigning. Default staleness rule: a claim with no
pushed commit or comment for 24 h may be taken over after posting a takeover comment.

### Checkpoint

A structured comment posted whenever a session stops short of done (compaction approaching,
blocker, needs-info, session end). All fields mandatory:

- **State** — branch, pushed commit SHA(s), what is complete.
- **Verified** — executed commands + output tails backing every done claim.
- **Next** — the ordered remaining steps.
- **Open** — blockers, unanswered forks, ASSUMED facts.
- **Continue-with** — the exact reading a continuation session needs (this ticket, this
  checkpoint, listed refs — nothing else).

Push before checkpointing (unpushed work is lost work), update the state markers, unassign,
terminate.

### Evidence

A claim without a run artifact is ASSUMED. Every load-bearing claim in a ticket, checkpoint,
or PR carries its executed command + output tail. Red→green proofs are executed and pasted
per the repo test policy — never reasoned through.

### Review

Every code PR gets an independent adversarial review in a fresh read-only context using the
client's native reviewer surface, in addition to mechanical gates and CI. The reviewer never
edits; findings return as PR review comments. Landing mechanics — review sources, the reviewer
contract, CI waits, rebase-merge — are specified in [`landing.md`](landing.md).

### Continuation

A fresh session re-claims the ticket, reads the packet + the latest checkpoint + its
Continue-with refs — never a transcript — and resumes from **Next**.

## Dependency and sub-issue semantics

- Parent/child: native GitHub sub-issues (epic→tickets; oversized ticket→subtasks).
- Ordering: native blocked-by relations; a ticket is unblocked when every blocker is closed.
- Workers take frontier tickets only; re-read frontier state immediately before claiming (the
  assignee write is the atomic claim).

## Ticket states and transition ownership

| State | Marker | Set by |
| ----- | ------ | ------ |
| intake | `needs-triage` label | opener/automation |
| under-specified | `needs-info` label | triager or worker |
| ready | `ready-for-agent` / `ready-for-human` label | triager |
| claimed/active | assignee | worker |
| waiting on PR | open PR with a `Fixes #N` closing reference | worker |
| blocked | open blocked-by relation | whoever discovers the dependency |
| done | closed + resolution comment | worker after gates pass / merger |
| cancelled | closed + comment stating why and what is NOT done | human (or worker on explicit human instruction) |

Native GitHub signals (assignee, `Fixes #N`, dependencies) carry state — no WIP/Waiting-PR
labels (scheme in [`issues.md`](issues.md) "Issue state (lifecycle)"). A worker may move its
claimed ticket between agent states but never cancels without a human and never overrides a
human-set `needs-info`/`ready-for-human` routing.

## Model escalation and risk triggers

- Tiers come from [`../model-tiers.conf`](../model-tiers.conf) (top/mid/small). Default:
  top/mid plans, gates, and reviews; small implements bounded steps.
- Mid-ticket escalation to a higher tier requires documented evidence **in the ticket**: a
  failed executed attempt, a falsified packet premise, or cross-cutting design surfaced
  mid-step. "Feels hard" is not evidence.
- A separate verifier or reproducer session is risk-triggered, never default. Triggers:
  new/changed parser, guard, or security surface; a data-loss path (file reorganization,
  archive rewrite); a recurring reviewer-confirmed defect class. The packet names the trigger;
  absent one, gates + review suffice.

## Retry and fix-loop limits (defaults, amendable by pilot evidence)

- Implementer: at most 2 executed attempts per step; the second failure checkpoints and
  escalates, citing both runs.
- Review fix loop: continues only while the latest round has a blocking finding; an
  all-nitpick or clean round closes it; hard cap 3 fix rounds, then checkpoint +
  `ready-for-human`.
- CI: the same failure cause twice after a fix attempt = stop, checkpoint, route
  `ready-for-human` or a tracking issue — never open-ended CI round-trips.

## What lives where

| Home | Content |
| ---- | ------- |
| Issue body | the task packet / current truth — edited in place |
| Issue comments | append-only events: checkpoints, evidence, resolution, cancellation |
| Branch | code, named `issue/{NN}-{slug}` (`scripts/agent/work-branch.sh`) |
| Pull request | the change + evidence summary, linked to its ticket; review lives here |
| Repo documents | durable norms: specs, policy, context docs |
| Nowhere | committed plan/handoff ledgers; transcript dumps |

## Parallel work

- Across tickets: allowed — claim discipline, one worktree/branch per ticket, never touching
  another claim's branch or PR.
- Same ticket: never two sessions. Sub-agents inside the one session are fine: fresh bounded
  contexts with packet-scoped briefs.

## Vendor mapping

Behavioral equivalence, not surface parity: each client uses its native orchestration (Claude
workflows/subagents, Codex roles) provided packet, claim, checkpoint, evidence, review, and
continuation land exactly as specified here.
