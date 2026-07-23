# Delegation — tiers, brief/handoff/gate, canonical gates

Scope: delegating any step to a sub-agent, and validating what comes back. Load when:
planning, spawning, or gating delegated work (ticket packets and ad-hoc alike).

## Plan top-tier, implement small-tier

Provider-neutral procedures name three capability tiers — **top / mid / small**
(deliberately disjoint from the effort-level words, so "high" always means an effort value).
The machine-readable mapping is [`../model-tiers.conf`](../model-tiers.conf): **top** =
`claude-fable-5` / `gpt-5.6-sol`; **mid** = `claude-opus-4-8` / `gpt-5.6-terra`; **small** =
`claude-sonnet-5` / `gpt-5.6-luna`. A tier selects the model, not the effort knob. The role
families built on these tiers (explorer, planner, implementer, verifier, reviewer, publisher,
coordinator) are specified with their vendor bindings in [`agent-roles.md`](agent-roles.md).

Substantial coding work is **planned and gated by the top tier** (falling back to mid when
top is unavailable) and **implemented by small-tier** sub-agents: the planner splits the task
into steps, a small-tier implementer executes each, an **independent small-tier verifier
gates every step** (never the brief author's model), and the planner validates the returned
records before the next — that per-step gating is what makes a cheaper implementer safe.
Ticket execution follows the fresh-session workflow ([`workflow.md`](workflow.md)); for
ad-hoc coding, follow the same shape. The higher model may implement a fix **directly** when
it is relatively small and doable in one step — and always handles **docs / config / settings
/ skills** directly. Delegation is for non-trivial, multi-step `src/`/tests/CI work.

- **The per-step verifier is always small tier** — never the top-tier model that authored the
  brief; a different model reads with different blind spots, and the step gate doesn't need
  the top tier. The top model's cross-referencing is reserved for the **whole-PR review**
  (the adversarial reviewer on a large/complex PR, [`landing.md`](landing.md)).
- **An implementer may re-delegate when a subtask genuinely splits** — but **accountability
  never splits**: the spawning agent verifies nested work itself before it enters its handoff,
  every handoff/gate field stays the spawner's to fill, and a nested delegate's defect is the
  spawner's defect at the gate above.
- **The planner's brief follows the delegation contract below** — a vague or wrong brief is a
  planner bug.
- **Mode propagation to delegates is mechanical** — the `SubagentStart` hook
  (`.claude/settings.json`) injects the ponytail + caveman capsule into every spawned
  sub-agent; briefs add a mode line only for a non-default level.
- **The small tier follows every directive of the canonical policy (AGENTS.md + its routed
  files).** The implementer is cheaper, not exempt. Run at effort `xhigh` or better, stated
  explicitly in every spawn.

## The delegation contract (brief → handoff → gate)

Three fixed artifacts govern **every** delegated step. The design principle: **cheap models
reliably fill required fields and reliably drop optional virtues**, so every check is a named
field in an artifact, and **an empty or missing field is a gate failure** — never a judgment
call. This contract exists because prose-only gates demonstrably fail: post-hoc audits keep
finding reproducible defects in work that passed every prose gate and review.

### THE BRIEF (planner → implementer) — mandatory sections

1. **Objective** — the one outcome, tied to the work item.
2. **Required reading** — `file:line` refs (identifiers, not pasted bodies — the implementer
   reads just-in-time in its own fresh context); the prior step's handoff.
3. **Coverage matrix** — when the change touches anything with siblings (every caller of a
   touched method, every branch of a touched conditional, every archive format, every
   No-Intro token class, every `SubComparator` in a sort chain, every CLI option in a mixin):
   the planner enumerates ALL rows **from the source** — grep output, the structure's own
   definition — **never from memory**. Each row maps to a test or an explicit justified
   deferral. A brief saying "all X" without the enumerated list is invalid.
4. **Hostile-input rows** — for any new/changed parser, regex, or input guard the planner
   supplies the adversarial input set with expected outcomes: punycode/IDN labels, empty
   input, header/no-header, quotes + regex metacharacters, tabs and consecutive spaces,
   oversized values, wrong/mixed encoding, malformed archives, truncated DAT XML.
5. **Constraints** — the do-NOT-touch list, plus the **never-weaken rule**: a brief may never
   weaken a canonical-policy mandate. In particular, red→green is **test-first**
   (testing.md #1): the reproduction test authored and executed RED before any production
   edit, frozen byte-identical, re-run GREEN unchanged after — **executed runs with output
   pasted**, never "reasoned through". Comments follow "Comments — constraint, not narration".
6. **Verification** — the canonical gates (table below) plus per-item acceptance checks, each
   a runnable command with its expected observable (the shape "WHEN `<command/input>` THEN
   `<observable>`"), mapping 1:1 to the tests the step ships.
7. **ESCALATE contract** — if any factual claim in the brief is contradicted by the code or a
   live probe, **STOP and return a structured blocker**; never silently patch the plan. An
   environmental claim tagged ASSUMED is probed before anything is built on it. Same rule when
   the fix requires **inventing a mechanism the brief never named** — escalate, or at minimum
   return DONE-WITH-DEVIATION, never plain DONE.
8. **Implementer scope — trust the brief, don't re-investigate it.** The brief embeds its
   evidence; the implementer's reading scope is the brief + its named refs + the code it edits.
   ESCALATE (item 7) is reactive: an *encountered* contradiction triggers it; proactively
   auditing the brief does not.

### THE HANDOFF (implementer → planner) — fixed fields, missing field = gate reject

- **Verdict**: DONE / DONE-WITH-DEVIATION / BLOCKED.
- **What changed**: files + a one-line why each; the commit hash.
- **Gates**: the exact commands run + pasted output tails (pass/fail counts) — never bare
  claims.
- **Red→green proof** (behaviour-changing steps): the reproduction test's FAILING output —
  executed BEFORE any production edit — AND its PASSING output after, both pasted from
  executed runs, plus the test file's `git hash-object` at red time (must equal the committed
  file).
- **Coverage matrix**: every brief row ticked with its test, or its stated deferral.
- **Deviations / judgment calls** (or "none"); **carry-forward** for the next step.

### THE GATE (planner, after every step) — mechanical, evidenced, artifact-producing

The producer never grades its own work; the gate **re-derives**, it never merely re-reads.
Every item is mandatory; a skipped item is recorded as SKIPPED with the reason. **Terse
prose, full checks** — brevity applies to wording, never to which checks run.

1. **Re-run the canonical gates yourself** — `scripts/agent/run-gates.sh --diff <base>`
   (table below; "touched" is computed from the diff's file types).
2. **Re-execute the red proof yourself** for behaviour changes — never accept the handoff's
   claim. `scripts/agent/verify-red-proof.sh --worktree <path> --test-cmd '<cmd>' --src
   <path>... --hash <test>=<red-time-sha>... [--base-ref <pre-fix-commit>]` reverts the src
   paths, requires the test to FAIL, restores, requires PASS, and enforces the freeze hash.
3. **Read the full diff** (`git show` — never `--stat` alone) and tick **every** action-plan
   item and coverage-matrix row against what the diff actually does. A mechanism in the diff
   the brief never named = STOP: write hostile-input rows for it and land their tests before
   PASS.
4. **Test honesty**: no weakened/removed assertions; every "does NOT contain X" assertion has
   an X-shaped fixture that could make it fail (vacuity check); no red run manufactured by
   mocking a fault production cannot produce; real failure modes exercised through the
   production surface (an on-disk corrupt archive, not an injected exception).
5. **Conventions**: each new public symbol listed beside ~3 sibling symbols proving the name
   matches the house pattern; comments/docs mentioning touched symbols reconciled with the new
   reality; any comment/doc claim naming a sibling file or house convention verified by grep;
   added comments respect the comment budget.
6. **Write the gate record** — a fixed-field block: commands + results, red/green evidence,
   per-item diff verdicts, matrix confirmation, the SKIPPED list.

### Canonical gates (single source of truth — briefs and gates reference THIS table)

Mechanical runner: `scripts/agent/run-gates.sh [--diff <base>]` (`--plan` to preview) —
change the table and the runner together.

| Touched | Gates (all must pass) |
| ------- | --------------------- |
| Java (`*.java`) or `pom.xml` | `mvn -B verify` (compile + JUnit reactor) |
| Shell (`*.sh`) | `sh -n` per file · `shellcheck` (scope: `scripts/`, `.claude/hooks/`, `.githooks/`) |
| Markdown (`*.md`) | `npx markdownlint-cli2` (when available; SKIPPED-tool-missing otherwise) |
| GUI (planned) | GUI-level coverage exists for the change (test mandate #4) — wire this row when the GUI module lands |

## Validating workflow records

**Validate, don't re-derive**: the independent verifier just re-ran the gates, re-executed
the red proof, and read the full diff, with pasted evidence — the calling session skips only
a redundant third derivation on top of it.

- Every fixed field non-empty and internally consistent — a missing/empty field rejects the
  record, never a judgment call.
- Every evidence entry is an executed command + pasted output, not prose.
- Spot-read the load-bearing diff hunks the verdicts rest on.
- Reject a record with any failed or missing item; rejection means HALT (or one corrected
  re-run) — never patch the record yourself.

## Agent-ops scripts (`scripts/agent/`)

Mechanical procedures live once, tested, in `scripts/agent/`: `work-branch.sh` (branch
sanitiser + worktree cutter), `run-gates.sh` (canonical-gates runner), `verify-red-proof.sh`
(red→green re-execution + freeze hash), `wait-checks.sh` (CI wait), `wait-reviewer.sh`
(reviewer-wait state machine). Shared contract in `scripts/agent/agent_env.sh`.

- **Portability contract.** All network access rides the `gh`/`git` CLIs. `gh` absent → the
  script exits **3** with a `GH-UNAVAILABLE` message: the agent falls back to `mcp__github__*`
  tools with wakeup-paced checks (waits.md rule #4). Any other missing tool → exit **4**
  (`TOOL-MISSING`). Exit **2** = usage/precondition, **1** = the check itself failed, **0** =
  verdict reached.
- **Hook-context safety.** Git-touching scripts scrub the hook-exported
  `GIT_DIR`/`GIT_INDEX_FILE`/… via `scripts/lib/git-env-scrub.sh` (`scrub_git_env`) —
  inherited hook env otherwise aims fixture git ops at the live repository.
- **Agent-maintained.** When an environment change breaks one of these scripts, the agent
  fixes it **in the same session** and lands it via the normal flow — never works around it
  silently in a transcript.
