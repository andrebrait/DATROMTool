# Testing — how to satisfy the mandate, and the test environment

Scope: writing/changing tests and running the suites. Load when: any change ships tests
(every change does) or a suite runs locally.

## Test coverage (mandatory) — the five principles

Tests are how a change proves itself. **Five non-negotiable principles govern every change —
unit, integration, or GUI. Each is a hard gate: a change that violates any one is NOT done,
no matter what the line-coverage number says.**

1. **A test is EVIDENCE the change works — for a behaviour change it MUST fail before and pass
   after, and the proof is TEST-FIRST.** Author the reproduction test(s) **before touching
   production code**, at full suite quality (every standard here applies — they ship in the
   suite and double as the defect's in-suite reproduction), and execute them on the untouched
   code: they **FAIL for the exact reason the change addresses**. From that red run the tests
   are **frozen** — byte-identical until green (a temporary `@Disabled` while developing is
   fine, but the committed file matches the red-run content exactly; record `git hash-object`
   of each test file at red time). After the change the SAME tests, **zero edits**, **PASS** —
   one green run proving both that the tests test the condition and that the fix works. Only
   then write the further tests the change needs. A test written after the fix, or edited
   between red and green, is evidence of nothing. **Two exceptions:** behaviour-**PRESERVING**
   work (refactors, prep) pins the *existing* behaviour as an oracle and stays green across
   the change — still mandatory; and **brand-new code with no pre-existing behaviour to be
   wrong** needs no red run against the void — the only possible red there is a missing
   symbol/class, an *existence* test, itself coverage theater. Its tests still ship with it
   asserting real behaviour, and any change it makes to EXISTING observable behaviour still
   gets its red-first proof.
2. **Every change ships WITH its tests.** "The existing suite still passes" is **not**
   coverage of a new change.
3. **NEVER coverage theater.** A test must *validate* the code, not merely *execute* it — it
   carries an assertion that would **fail on a regression**. Green at 100% line coverage with
   no failable assertion is **rejected**.
4. **Front-end / GUI changes REQUIRE front-end / GUI tests.** DATROMTool is CLI-only today;
   once the planned GUI module lands, a change observable only through the GUI carries
   GUI-level coverage, not just unit tests on the logic behind it. For the CLI, a change to
   observable command behaviour (options parsed, exit codes, output format) carries a test at
   the CLI boundary (a picocli-level or end-to-end invocation test), not only a core-logic
   unit test.
5. **Tests express the change's INTENT — they are documentation, not just coverage.** Name and
   comments state the intended outcome being pinned, never the mechanics of how it is coded.

## Satisfying the principles

- **Branch coverage — test every condition, not one side.** A boolean gets off *and* on (plus
  any third state); every `if`/`switch`/pattern-match branch and documented input class gets
  its own assertion.
- **Assert the before-state in transition tests.** A test that flips a toggle asserts the
  *original* result first, so green proves the flip **caused** the change — never just the
  final state.
- **Self-encapsulated — never order-dependent.** Shared fixtures are fine; no test may depend
  on a sibling running first. Reset per-test state explicitly (`@BeforeEach`); a class-scoped
  baseline is NOT per-test isolation.
- **Specify complex behaviour BDD-style; keep trivial tests trivial.** Non-trivial behaviour
  (region/language precedence, sorting order, multi-step scan→match→copy flows) gets
  Given–When–Then structure in name/comments; use `@ParameterizedTest` for input-class matrices.
- **Synchronize — a duration is never an assertion.** A test waits by consuming the event it
  needs (a latch, an observed condition, a `Future.get()`), never "the work completed within N
  seconds" and never a fixed sleep as coordination. The only time bound allowed is a generous
  salvage cap whose sole job is reaping a stuck run; its expiry reports "stuck/environment",
  loudly and distinguishably from the behaviour under test. Widening a deadline is never a
  flake fix — the deadline is doing assertion work and no constant is large enough.
- **On failure, print expected vs actual — no guessing.** Every assertion puts the comparison
  on the terminal (AssertJ / JUnit assertions with messages); a bare boolean matcher is not
  acceptable.

## Running tests

```sh
mvn verify                          # full suite (from repo root; run after any change)
mvn test -Dtest=ByteSizeTest        # single test class
mvn test -Dtest=ByteSizeTest#method # single method
mvn -pl core verify -am             # one module + its upstream deps
```

- **Base classes for filesystem fixtures:** `ArchiveContentsDependantTest` /
  `TestDirDependantTest` — extend these for tests needing archive/dir fixtures; test data
  lives in `test-data/`.
- **JUnit 5 + junit-pioneer + Mockito.** Use `@ParameterizedTest`, `@TempDir`, and
  junit-pioneer extensions rather than hand-rolled fixtures where they fit.
- A local-only failure: diagnose before dismissing — if it is genuinely pre-existing on
  `master`, **file a tracking issue**; never leave it as folklore. The cross-OS CI matrix
  (ubuntu/macos/windows × Java 25) is the final authority — a path/encoding/locale assumption
  that passes on one OS can fail on another.
