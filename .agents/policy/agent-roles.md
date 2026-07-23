# Agent role families — shared contract

- **Scope:** vendor-neutral agent role families for Claude and Codex: what each role is
  for, what it may read and mutate, what it must return, and which model tier serves it
  (companion to the fresh-session ticket workflow in [`workflow.md`](workflow.md)).
- **Load-when:** defining or routing an agent role; changing `.codex/agents/`,
  `.agents/model-tiers.conf`, or this registry.
- **Owner:** repo owner.

## Goal

One semantic contract per role family, mapped explicitly onto each vendor's native
definitions, so both clients stay behaviorally aligned without identical files, models,
tools, or wording. Context-window pollution is the enemy: a role loads its purpose-built
context slice (its contract section plus the packet's Required reading), never the whole
policy corpus.

## Fixed constraints

- Tier vocabulary is **top / mid / small** from
  [`.agents/model-tiers.conf`](../model-tiers.conf). The contract names tiers, never
  vendor model ids; a tier selects the model, and procedures set effort independently.
- Expensive tiers require evidence (see Tier economics).
- The delegation contract (`.agents/policy/delegation.md`: brief → handoff → gate) and the task-packet /
  checkpoint schemas ([`workflow.md`](workflow.md)) bind every role unchanged; a role
  contract may narrow them, never weaken them.
- `scripts/check_agent_roles.py` validates the registry below against both vendors'
  definitions if and only if either side changes (pre-commit + CI). It checks semantic
  fields — tier vocabulary, mutation boundaries, binding targets, model-tier pins —
  never textual identity, so vendor-native wording stays free.

## Tier economics (both vendors)

- **small** is the default executor tier: implementation, verification, review, triage,
  publishing, and coordination all start small.
- **top** requires a named routing trigger: planning/gating substantial work; whole-PR
  review of a large/complex PR (>300 lines, >6 files, or core DAT/ROM parsing,
  matching, or 1g1r selection behaviour); verdict-quality triage of a complex issue.
  Mid-task escalation additionally requires documented evidence **in the ticket** — a
  failed executed attempt, a falsified packet premise, or cross-cutting design surfaced
  mid-step; "feels hard" is not evidence ([`workflow.md`](workflow.md) "Model escalation
  and risk triggers").
- **mid** is a fallback tier only: it substitutes when top is unavailable (planning), or
  runs as the second pass of the documented dual-review fallback — never a sole
  reviewer, never a default route.

## Role registry (machine-readable)

`scripts/check_agent_roles.py` parses this table. Column vocabularies: **Tiers** —
`top`/`mid`/`small`, primary (default) tier first, `+`-separated; **Mutation** —
`read-only`/`workspace-write`; **Independent** — `yes`/`no`; **bindings** —
comma-separated `kind:name` (Claude kinds: `skill` = `.agents/skills/<name>/`,
`policy` = `.agents/policy/<name>`, `session` = the top-level session itself or a
fresh native sub-agent it spawns with the role's contract; Codex kinds: `agent` =
`.codex/agents/<name>.toml`, plus `skill`/`policy`/`session` as for Claude).

<!-- role-registry:begin -->

| Role | Tiers | Mutation | Independent | Claude bindings | Codex bindings |
| ---- | ----- | -------- | ----------- | --------------- | -------------- |
| explorer | small+top | read-only | no | session | agent:analyst, agent:analyst-top |
| planner | top+mid | read-only | no | session | agent:planner |
| implementer | small | workspace-write | no | session | agent:implementer |
| verifier | small | read-only | yes | session | agent:adversarial-reviewer |
| reviewer | small+top+mid | read-only | yes | session | agent:adversarial-reviewer, agent:adversarial-reviewer-top, agent:adversarial-reviewer-mid |
| publisher | small | workspace-write | no | policy:landing.md | policy:landing.md |
| coordinator | small | workspace-write | no | policy:workflow.md | policy:workflow.md |

<!-- role-registry:end -->

## Role contracts

### explorer

- **Purpose & routing:** read-only investigation with cited evidence — locate code,
  gather facts, triage an issue, run an ADR investigation fan-out. Route here when the
  outcome is a report, never an edit.
- **Inputs & task packet:** a scoped question or issue, a worktree path, and the output
  schema; Required reading as `file:line`/doc pointers, never pasted bodies.
- **Outputs & evidence:** the requested schema, with every load-bearing fact tagged
  verified (command + output) or ASSUMED; never a different planning artifact.
- **Permissions & mutation:** read-only. May run read-only commands (grep, `git log`,
  `gh` reads); never edits, commits, pushes, or changes labels.
- **Context & skills:** the packet plus its named refs; code-search tooling. Not the full
  policy corpus. Floor: `issues.md` when triaging; the run/integration context for a live
  repro; the routing row of the suspect subsystem.
- **Stop & escalation:** a packet premise contradicted by source ⇒ STOP and return a
  structured blocker; an under-specified scope ⇒ route `needs-info`.
- **Independence:** not required — it serves its caller.
- **Tier intent:** small by default; top for verdict-quality triage of a complex issue
  or an evidence-heavy cross-cutting investigation. Never mid.

### planner

- **Purpose & routing:** decompose substantial work into bounded steps, author the
  brief/task packet (coverage matrix and hostile-input rows enumerated from source),
  and gate every delegated step mechanically. Route: substantial multi-step
  `src/`/`tests/`/CI work, ADR design, ambiguity forks.
- **Inputs & task packet:** the work item (issue, ADR, map ticket), the live tree, and
  the policy annexes the item touches.
- **Outputs & evidence:** the brief (mandatory sections per the delegation contract),
  the per-step gate record, and HALT/continue decisions — each check an executed
  command with pasted output.
- **Permissions & mutation:** read-only as the role: briefs and gates, not edits. The
  session hosting it may switch roles in place — implementer for a small direct fix or
  docs/config/skills work (CLAUDE.md carve-out), publisher/coordinator for landing and
  bookkeeping — but the planner never grades its own implementation work.
- **Context & skills:** the bootstrap (AGENTS.md) and its routed annexes, prior handoffs;
  the fresh-session workflow ([`workflow.md`](workflow.md)). Floor:
  [`delegation.md`](delegation.md) always; `issues.md` on issue work, `landing.md` when
  landing, `waits.md` when a wait is armed.
- **Stop & escalation:** a genuine user fork ⇒ ask the user; a falsified premise ⇒ stop
  and re-plan, loudly. Never silently patch the plan.
- **Independence:** not independent of the work item, but producer≠gater: the per-step
  verifier and PR reviewer are always different agents.
- **Tier intent:** top — every downstream artifact leans on the brief, and brief bugs
  demonstrably ship defects; mid only as the documented fallback when top is
  unavailable.

### implementer

- **Purpose & routing:** execute exactly one approved brief/packet in the assigned
  worktree. Two weights, one contract: **full** (default) and **light** — a
  behaviour-preserving mechanical step pinned by an earlier gate-passed oracle, run
  without a planning/reconcile wrapper (the "quick implementer"; same permissions and
  evidence schema, smaller scope).
- **Inputs & task packet:** THE BRIEF (mandatory sections) plus the prior step's
  handoff. Trust the brief — no re-investigating its evidence.
- **Outputs & evidence:** THE HANDOFF, fixed fields: verdict, what changed, gate
  commands + output tails, red→green proof (executed, test-first, frozen), coverage
  matrix ticks, deviations, carry-forward.
- **Permissions & mutation:** workspace-write inside its worktree; commits as directed.
  Never pushes protected branches, never merges, never edits the brief or policy.
- **Context & skills:** the brief, its named refs, the code it edits, and the language
  annex for the touched file types — nothing broader. Floor: `coding.md`, `testing.md`, the
  `lang-*.md` per touched file type; domain rows per the routing table.
- **Stop & escalation:** the ESCALATE contract — a contradicted premise or a mechanism
  the brief never named ⇒ BLOCKED (or DONE-WITH-DEVIATION), never plain DONE; at most
  2 executed attempts per step, then checkpoint and escalate citing both runs.
- **Independence:** none; accountability stays with the spawner. May re-delegate a
  genuine split, never the whole brief.
- **Tier intent:** small, always. A higher tier mid-step requires documented evidence
  in the ticket.

### verifier

- **Purpose & routing:** independently re-derive one completed step: re-run the gates,
  re-execute the red→green proof, read the full diff against every plan item and
  coverage row, audit test honesty and conventions. Route: after every delegated step,
  before the next starts.
- **Inputs & task packet:** the brief, the handoff, the diff, and the canonical gate
  commands.
- **Outputs & evidence:** the gate-record fields — commands + results, red/green
  evidence, per-item diff verdicts, matrix confirmation, and an explicit SKIPPED list.
- **Permissions & mutation:** read-only on sources; may execute gates and tests
  ephemerally. Never patches a finding — defects route back to the planner.
- **Context & skills:** the brief + handoff + diff and the canonical gate table;
  deliberately not the implementer's transcript. Floor: `testing.md`, `landing.md`; the
  touched `lang-*.md` and the domain rows of the diff.
- **Stop & escalation:** any defect or unnamed mechanism in the diff ⇒ reject the step;
  a check it cannot run is recorded SKIPPED with the reason, never silently dropped.
- **Independence:** required — never the agent (or model) that authored the brief or
  the diff.
- **Tier intent:** small, always (owner directive): a different model reads with
  different blind spots, and the step gate does not need the top tier.

### reviewer

- **Purpose & routing:** adversarial, execution-grounded review of a complete PR (or a
  delta-scoped feedback fix). Route: every code PR; delta re-reviews after fix rounds.
- **Inputs & task packet:** the PR number, base (a pre-fix SHA for delta re-reviews),
  a worktree, and the intent/acceptance spec.
- **Outputs & evidence:** schema-forced findings — severity, location, evidence,
  reproduction — returned as review output; never an edited tree.
- **Permissions & mutation:** read-only; may run discriminating probes and hostile
  inputs. Never edits, commits, or downgrades a real pre-existing defect — those route
  to a tracked follow-up.
- **Context & skills:** the full diff plus surrounding code; the policy annexes the diff
  touches. Floor: `testing.md`, `landing.md`; the touched `lang-*.md` and the domain rows
  of the diff.
- **Stop & escalation:** the fix→re-review loop converges — it continues only while the
  latest round has a blocking finding; hard cap 3 rounds, then a human decides.
- **Independence:** required — a fresh context, never the author of the change.
- **Tier intent:** small by default; top for a large/complex PR (whole-PR
  cross-referencing is the point); mid only as the second pass of the documented
  top-unavailable dual fallback, never a sole reviewer.

### publisher

- **Purpose & routing:** the commit-and-publish operator — mechanical landing:
  rebase onto the live base, clean the diff, push, open the PR, keep labels in sync,
  run bounded CI/review waits, merge only on instruction. Route: after gates and
  review, when the remaining work is procedure, not judgment.
- **Inputs & task packet:** the branch, the work item, and the landing instruction
  (which flow, which labels, merge or stop-before-merge).
- **Outputs & evidence:** the PR URL, label transitions, and merge/CI state — each
  claim with its executed command + output tail.
- **Permissions & mutation:** git/gh writes only — branch pushes, PR metadata, labels.
  No new source changes beyond rebase conflict resolution; never force-push over
  another session's PR; every wait is bounded and swept.
- **Context & skills:** [`landing.md`](landing.md) and the branch/release policy — not the
  implementation history. Floor: [`landing.md`](landing.md); the release context for a
  release, `git.md` for tag/push mechanics.
- **Stop & escalation:** the same CI failure cause twice after a fix attempt ⇒ stop and
  checkpoint; a blocking review finding routes back to the planner, never a silent
  self-fix.
- **Independence:** not required.
- **Tier intent:** small — procedure execution; never burn the top tier on waits.

### coordinator

- **Purpose & routing:** the low-cost ticket coordinator — shepherd the ticket
  lifecycle: pick frontier tickets, claim before work, keep state labels honest, post
  checkpoints, route `needs-info`/`ready-for-human`, dispatch workers with task
  packets. Route: fresh-session ticket workflow sessions and label hygiene.
- **Inputs & task packet:** the map/ticket state on GitHub — the durable execution
  state; never a parent transcript.
- **Outputs & evidence:** claims, structured checkpoints (all fields mandatory), label
  transitions, and dispatched packets.
- **Permissions & mutation:** GitHub metadata writes (labels, assignees, comments,
  sub-issue/blocked-by relations). No source edits; never cancels a ticket without a
  human; never overrides a human-set routing.
- **Context & skills:** [`workflow.md`](workflow.md) plus the bootstrap routing rows —
  deliberately minimal. Floor: [`workflow.md`](workflow.md); `issues.md`.
- **Stop & escalation:** approaching compaction ⇒ checkpoint, unassign, terminate;
  correctness-critical work never continues through compaction.
- **Independence:** not required.
- **Tier intent:** small — routing and bookkeeping. Escalation happens by dispatching a
  planner, not by upgrading the coordinator.

## Vendor mappings

Behavioral equivalence, not surface parity: each client keeps its native orchestration
as long as the role's semantic fields land as specified. Tier→model resolution always
goes through [`model-tiers.conf`](../model-tiers.conf).

### Claude

| Role | Native definition |
| ---- | ----------------- |
| explorer | fresh read-only sub-agents with packet-scoped briefs (small default; top allowed for verdict quality); the harness `Explore` agent type for ad-hoc read-only fan-out |
| planner | the top-level session itself (delegation.md "Plan top-tier, implement small-tier") |
| implementer | a fresh small-tier sub-agent executing THE BRIEF in the assigned worktree |
| verifier | a fresh small-tier sub-agent (never the brief author's model) re-deriving one step; fresh read-only validator sub-agents for per-finding validation |
| reviewer | a fresh read-only sub-agent implementing the [`landing.md`](landing.md) reviewer contract (small default; top for large/complex; mid only in the dual fallback) |
| publisher | the session (or a small-tier delegate) following [`landing.md`](landing.md) |
| coordinator | the session following [`workflow.md`](workflow.md) |

### Codex

| Role | Native definition |
| ---- | ----------------- |
| explorer | `.codex/agents/analyst.toml` (small), `.codex/agents/analyst-top.toml` (top) |
| planner | `.codex/agents/planner.toml` (top) |
| implementer | `.codex/agents/implementer.toml` (small, workspace-write) |
| verifier | `.codex/agents/adversarial-reviewer.toml` (small) |
| reviewer | `.codex/agents/adversarial-reviewer.toml` (small), `.codex/agents/adversarial-reviewer-top.toml` (top), `.codex/agents/adversarial-reviewer-mid.toml` (mid, fallback second pass only) |
| publisher | the session following [`landing.md`](landing.md) |
| coordinator | the session following [`workflow.md`](workflow.md) |

## Decisions

Deviations from the starting six roles, with rationale:

- **quick implementer merged into implementer** as the `light` weight: identical
  permissions, evidence schema, and escalation contract; it differs only in scope cap
  and skipped wrapping stages. The repo already models this as a routing parameter
  (`WEIGHT: light` phases), and Codex defines one implementer role.
- **planner added**: the de-facto top-tier role both vendors already define
  (`.codex/agents/planner.toml`; the Claude session + Reconcile stage). The expensive
  tier needs an explicit contract precisely because it is expensive.
- **code reviewer split into verifier + reviewer**: different outputs (gate record vs
  findings schema) and different tier routing (the verifier is pinned small by owner
  directive; the reviewer escalates to top for large/complex PRs). Both stay
  independent and read-only; Codex serves both from the `adversarial-reviewer` family.
- **code explorer kept** (named `explorer`), covering evidence gathering, triage, and
  investigation fan-outs — the Codex `analyst` family.
- **publisher and coordinator kept**, bound to policy documents rather than a
  dedicated vendor agent: both are procedure-driven small-tier roles a session
  fills by loading one document, and neither vendor needs a separate agent definition
  for them.
- **The shell discovery guard stays.** `scripts/agent/check-agent-config-parity.sh`
  keeps skill/workflow adapter parity, `model-tiers.conf` syntax, and its fast
  pre-commit Codex role→tier pins; `scripts/check_agent_roles.py` owns the
  registry-driven cross-vendor role semantics. The overlapping pins fail loudly on
  divergence — folding them is deliberately deferred until the registry has bedded in.

## Acceptance criteria

- Every registry role has a contract section carrying all eight semantic fields, and
  explicit Claude and Codex bindings that resolve to real files (or `session`).
- `scripts/check_agent_roles.py --all` passes on the tree; it fails loudly when a
  vendor definition drifts from the registry (retiered model pin, sandbox/mutation
  mismatch, orphaned vendor role, missing contract field) while tolerating any
  vendor-native wording difference.
- The check runs if and only if a role surface changes: self-scoped `--staged` in
  pre-commit and `--diff <base>` in CI.

## Out of scope

- Per-role context-slice documents (splitting CLAUDE.md into role-specific required
  reading).
- Effort-level policy: tiers select models; procedures own their effort settings.
- Skill/workflow adapter parity and symlink integrity — already owned by
  `scripts/agent/check-agent-config-parity.sh`.

## Open forks

- None.
