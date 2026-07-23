# Coding standards — naming, comments, conventions, linting

Scope: writing code in any language. Load when: any code change, plus the `lang-*.md`
context file for each touched language (`.agents/context/`).

## Naming — follow the established pattern

**A new field, variable, method, class, config key, or CLI option follows the conventions
already in that file (or similar files)** — match the surrounding pattern (casing,
separators, word order, package layout). Java uses `camelCase` members, `PascalCase` types,
`UPPER_SNAKE` constants; picocli options follow the neighbouring option style
(`--long-kebab`). An off-pattern name is a smell even when it works. Before naming a new
public symbol, eyeball ~3 sibling symbols to confirm the name matches the house pattern.

## Comments — constraint, not narration

A comment states a constraint the code cannot show; default budget **≤3 lines**. Design
rationale lives in the architecture notes / the linked issue and the comment carries a
one-line pointer — never a restatement (a contract stored in doc + comment + test is three
copies, two of which drift). One-line regression breadcrumbs stay (`// issue #43: decode
UTF-16 BOM first — else the header sniff false-positives`). **Compression sheds redundancy,
never essential information:** contract facts (params, returns, invariants, defaults, thread
-safety) expressed nowhere else may be reworded tighter, never removed. **Operational headers
of executable scripts are interface documentation, not narration** — usage, options/params,
env vars stay in the header unless the script prints an equivalent `--help`. **Never in
committed comments:** handoff/review archaeology (reviewer names, `PR #N` finding IDs) or
correctness argument aimed at the gate/reviewer — that evidence belongs in the handoff / PR
body, not the tree.

## Code-quality conventions

- **Enums/booleans over magic strings.** Model settings/mode values as a Java `enum` (or a
  sealed type) rather than passing raw strings; predicates return `boolean`. This matches how
  `OutputMode`, `Filter`, and the sorting preferences are already modelled.
- **Short-circuit cheap first.** Put the cheap guard first in `&&`/`||`.
- **String ops over regex in hot loops.** Prefer `String` methods over `Pattern`/`Matcher`
  where equivalent, especially in per-line / per-ROM paths (name parsing runs over large DATs).
- **Immutability by default.** Prefer Guava immutable collections and Lombok `@Value`/`@With`
  for value types, consistent with the codebase; don't expose mutable internal collections.
- **Don't hand-write what Lombok/Jackson/picocli generate.** See
  [`../context/lang-java.md`](../context/lang-java.md).

## Linting

DATROMTool has **no linter/formatter configured yet** — a future Checkstyle or Spotless
addition would wire one; until then **match the surrounding style** and let `mvn verify` (the
compile + JUnit gate) and the cross-OS CI matrix be the authority. When you do touch a file,
keep formatting consistent with its existing indentation and import ordering; do not
reformat untouched regions (gratuitous reformatting bloats the diff — see git.md diff
hygiene).

- **Markdown** (docs): if `markdownlint-cli2` is available, `npx markdownlint-cli2` (`--fix`
  to autofix) — blank line around every heading/list/fence, a language on every fence, single
  trailing newline. It is not yet a hard gate; keep docs clean anyway.
- **Shell** (tooling scripts under `scripts/`, `.claude/hooks/`, `.githooks/`): `sh -n` +
  `shellcheck`; POSIX sh only — see [`../context/lang-shell.md`](../context/lang-shell.md).
