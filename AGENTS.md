# DATROMTool — agent bootstrap (canonical)

This file is the **canonical, vendor-neutral agent policy bootstrap** for
`andrebrait/DATROMTool`. Claude Code loads it through the thin `CLAUDE.md` adapter
(`@AGENTS.md` import); Codex reads it natively. Detailed policy lives in `.agents/policy/`,
domain context in `.agents/context/` — loaded per the routing table below, never all at once.
Shared behavior changes land in those files, never in a vendor copy.

## What DATROMTool is

A platform-independent Java tool for ROM management and DAT-file manipulation — successor to
[1g1r-romset-generator](https://github.com/andrebrait/1g1r-romset-generator), inspired by
[SabreTools](https://github.com/SabreTools/SabreTools) and
[Retool](https://github.com/unexpectedpanda/retool). It parses DAT files (primarily Logiqx
XML), extracts metadata from ROM names via the No-Intro naming convention, detects
divergences between parsed and declared data, filters/sorts games (1G1R), and reorganizes
ROMs across archive formats. Today it is a CLI; a **GUI and further features are planned** —
policy is written so it carries forward as the surface area grows.

## Working principles — don't guess

- **Never assume** — read the source of truth, investigate the live state, and confirm a
  genuine fork before building. A clean grep of one file is not proof; a plausible memory is
  not a fact.
- **Ambiguity:** pick the obvious option and proceed when there is one; `AskUserQuestion`
  only when the choice is genuinely the user's (unclear intent, diverging defensible
  approaches, architecturally significant change). Applies to autonomous flows too.
- **Evidence:** a claim without a run artifact is ASSUMED; environmental claims written into
  artifacts are probed in-session first; no self-exemption from a MUST rule without quoting
  the authorizing user message; debugging lists ≥2 hypotheses + a discriminating probe
  before any fix edit.

## Never-list (hard invariants)

- All repository code work happens in a dedicated git worktree — cut via
  `scripts/agent/work-branch.sh <issue|adr> <NN> [title...] --worktree`; never hand-derive
  the branch slug, never work in the primary checkout.
- Dev-only classes (agent config under `.claude/`/`.codex/`/`.agents/`, skills,
  documentation-only `**/*.md`, `AGENTS.md`, `CLAUDE.md`) commit directly to `master` after
  fetch + rebase, still from a worktree; anything touching `src/` (module `src/` trees under
  `domain/`, `core/`, `cli/`, `logging/`), tests, `pom.xml`, or CI takes the full
  rebase-only-PR flow with independent review.
- Merge PRs by rebase only; history stays strictly linear; rebase onto the latest `master`
  before every push, PR, or CI dispatch; clean the diff before you push.
- Push every green, final commit to its remote branch immediately; work never stays only on
  a local branch. Dev-only commits push to `master`; code branches push to their own remote
  branch.
- Landing a change is not committing it: it means commit, push, open a non-draft PR, address
  every review round, and rebase-merge it (dev-only classes land at the push to `master`).
  Report work as landed only after that completes; otherwise report its real state.
- A behaviour change needs its test-first red→green proof: reproduction test executed RED
  before any production edit, frozen byte-identical, re-run GREEN unchanged — executed
  runs, never reasoned through.
- Every change ships WITH its tests; no coverage theater (every test carries an assertion
  that fails on regression). A GUI change (once the GUI module lands) carries GUI-level
  coverage, not only unit tests.
- No orphaned waits: harness-tracked work gets no timer; every untracked wait has a hard
  cap + deadline and dies with its task.
- `--no-verify` is for humans, not agents. Never weaken a canonical mandate without quoted
  user authorization.
- Read the whole GitHub issue (title, body, every comment) before working it.

Enforcement is mechanical where possible: `.githooks/` (prepare-commit-msg, pre-push), CI
(`.github/workflows/maven.yaml`), and `scripts/agent/run-gates.sh` are authoritative;
lifecycle hooks carry the communication-mode capsules.

## Routing table — read on trigger, not up front

| Task touches | Read first |
| ------------ | ---------- |
| delegating any step; validating a handoff | `.agents/policy/delegation.md` |
| a ticket / fresh-session execution | `.agents/policy/workflow.md` (roles: `agent-roles.md`) |
| waiting on anything external | `.agents/policy/waits.md` |
| committing, branching, worktrees, attribution | `.agents/policy/git.md` |
| session layouts, managed-remote, resume | `.agents/policy/sessions.md` |
| landing a PR, review findings | `.agents/policy/landing.md` |
| a GitHub issue (triage gates, lifecycle) | `.agents/policy/issues.md` |
| writing/changing tests; running suites | `.agents/policy/testing.md` |
| writing code (any language) | `.agents/policy/coding.md` + `.agents/context/lang-<java\|shell>.md` per touched language |
| module layout, DAT pipeline, patterns | `.agents/context/architecture.md` |
| release, tags, GitHub Actions packaging | `.agents/context/release.md` |
| Codex-specific surfaces / noun translation | `.agents/context/codex-adapter.md` (Codex sessions, at start) |

Delegation shape: substantial coding work is planned/gated by the **top tier**, implemented
by **small-tier** sub-agents, every step gated by an independent small-tier verifier via the
brief → handoff → gate contract; the top tier handles small one-step fixes and
docs/config/settings/skills directly. Tiers top/mid/small map to models in
`.agents/model-tiers.conf` (disjoint from effort words — "high" is always an effort value).

Test law (five principles, full text in `testing.md`): red-before/green-after test-first
proof · every change ships with its tests · no coverage theater · front-end/GUI changes need
front-end/GUI tests · tests document intent.

## Repository structure

```text
DATROMTool/
├── domain/    # Data models + domain logic (datafile/logiqx Logiqx XML model; detector/; serialization/)
├── core/      # Business logic (GameFilterer, GameParser, GameSorter, io/ scanner+copier, command/OneGameOneRom)
├── cli/       # picocli CLI — DatRomCommand entry point + option/ converter/ argument/
├── logging/   # Logback + jansi config
├── test-data/ # Fixtures for filesystem-dependent tests
├── .agents/   # policy/ + context/ + skills/ + model-tiers.conf + plugins/  (this bootstrap's home)
├── .claude/   # Claude adapter: settings.json, hooks/, skills/ (symlinked from .agents/skills/)
├── .codex/    # Codex adapter: config.toml, hooks.json, agents/*.toml
├── scripts/   # Dev tooling: agent/ ops, git hooks setup, token-savior/rtk launchers
└── .githooks/ # prepare-commit-msg + pre-push (activate via scripts/setup-hooks.sh)
```

Maven multi-module (Java 25, Maven 3.9+). Build: `mvn clean compile test-compile`; test:
`mvn verify`; package CLI: `mvn package -DskipTests` →
`cli/target/cli-<version>-jar-with-dependencies.jar` (entry point
`io.github.datromtool.cli.DatRomCommand`). **`master` is the single mainline** (no
`main`/`devel` split). Releases tag `vX.Y.Z` (stable) / `vX.Y.Z-rc<N>` (prerelease) — see
`.agents/context/release.md`.

## Communication

Session-start hooks activate ponytail (build lazy) + caveman (talk terse); the capsules are
the mechanism. Two style exceptions get normal professional grammar: external/public-facing
text (issues, PR bodies, commits) and documentation. Commits:
`<scope>: <imperative summary>`. While working an issue/PR, prefix replies with the
one-line status marker `<emoji> ***ID***(***#PR***): ***Title***` (~28 chars; 📝 authoring ·
🏗️ implementing · 🤔 investigating · 🛠️ fixing · 👀 awaiting review · ⏳ awaiting CI ·
🏁 merged/cleanup); omit on plain conversational turns.

## Vendor adapters

Vendor-specific surfaces live in each vendor's own adapter, never in this neutral file:

- **Claude Code** → `CLAUDE.md` (imports this file via `@AGENTS.md`; holds Claude-only
  surfaces — hooks in `.claude/settings.json`, skills at `.claude/skills/` symlinked from
  `.agents/skills/`, git-hook marker `CLAUDECODE=1`, coauthor identity
  `Claude <noreply@anthropic.com>`).
- **Codex** → `.agents/context/codex-adapter.md`. Codex reads this bootstrap natively but not
  that file; **read it at session start** for the canonical-noun → Codex translation table and
  Codex specifics (subagents, attribution, resume, hook/marker surfaces; marker
  `CODEX_THREAD_ID`).

@RTK.md

Respond terse like smart caveman. All technical substance stay. Only fluff die. Drop
articles/filler/pleasantries/hedging; fragments OK; technical terms exact; code unchanged.
Auto-clarity for security warnings, irreversible actions, confusion — resume after.
Code/commits/PRs written normal. Stop: "stop caveman" / "normal mode".
