---
name: debug
description: >
  Structured hypothesis-ledger debugging (CLAUDE.md "Evidence rules"): reproduce
  first, list competing hypotheses, run the discriminating probe for each, and
  only fix a CONFIRMED cause — never "try a fix and see". Args: <symptom, error
  message, or failing command>. Use when the user says "debug this", "why is X
  failing", "investigate this error", or invokes /debug.
---

You are debugging under the CLAUDE.md hypothesis-ledger rule: **no fix edit before a
CONFIRMED hypothesis**. Committing to a path without discriminating evidence is the failure
mode this skill kills — a plausible story is not a diagnosis.

Args: `{{ args }}`

## Step 1 — Reproduce (or gather the artifacts)

Run the failing thing yourself and capture the exact output — the error line, the wrong
value, the diff between expected and actual. Not reproducible off-appliance? Collect the real
artifacts instead (logs, the smoke-diagnostics snapshot, `config.xml` section, the pasted
user report) and say precisely what could not be reproduced and why. **No reproduction and no
artifacts ⇒ stop and ask for them** — do not theorize from the symptom description alone.

## Step 2 — The ledger

Maintain this table in your working notes and carry it into the final report:

```text
OBSERVATIONS (pasted output only, no interpretation)
  O1: <command> → <exact output line(s)>
HYPOTHESES (≥2, each with a mechanism)
  H1: <cause> — would explain O1 because <mechanism>; predicts <observable>
  H2: <cause> — …
PROBES (one per live hypothesis — the cheapest command whose output separates them)
  P1 (tests H1 vs H2): <command> → EXPECTED-if-H1: <x> / EXPECTED-if-H2: <y>
  P1 ACTUAL: <pasted output> → verdict: H1 CONFIRMED / REFUTED / …
```

Rules:

- **≥2 hypotheses before the first probe.** One hypothesis is a conclusion wearing a lab
  coat. If you genuinely cannot form a second, write down why — that reasoning is itself
  checkable.
- **Probes discriminate; they don't confirm.** A probe whose output would look the same under
  both hypotheses is not a probe. Prefer reading the effective live state (CLAUDE.md
  "Investigate, don't assume": the tool's own CLI, the included files, the chroot-relative
  path) over re-reading the code you already believe you understand.
- **Every environmental claim gets probed**, not remembered — default shells, tool exit-code
  semantics, platform behaviour (the false-"pipefail" and #902 class).
- All hypotheses refuted → the evidence has told you something: write new hypotheses FROM the
  probe outputs and continue the ledger. Never fall through to "just try changing X".

## Step 3 — Fix only after CONFIRMED

- State the confirmed root cause in one sentence, citing the probe that proved it.
- Root cause, not symptom (ponytail): grep every caller/sibling of the faulty path — the
  #858→#900 chain was five symptom-fixes of one cause. Enumerate the sibling axes before
  choosing where the fix goes.
- The fix follows the test mandate: pin with a test that **fails on the broken code**
  (executed, output recorded) and passes after. Land per the normal flow (worktree; PR per
  `.agents/policy/landing.md` for code, direct `master` for dev-only classes) — or hand the
  confirmed diagnosis to a delegated implementer if the user only asked for the investigation.

## Step 4 — Report

Final report = verdict (root cause + the confirming probe), the full ledger, the fix (or the
recommended next step), and any ASSUMED facts that remain unverified.
