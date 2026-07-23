# Shell — language context

Scope: writing or changing POSIX shell in the repo's agent/runtime **tooling** scripts
(token-savior, rtk, agent-ops, git hooks). Load when: any touched `*.sh` file.

- POSIX sh only (`#!/bin/sh`); no bash-isms (`[[`, arrays, `$RANDOM`). Quote all expansions.
- **POSIX-compliant means correct under strict-POSIX SEMANTICS (ash/dash), not merely free of
  bash-isms** — e.g. a redirection error on a special built-in (`:`, `exec`, `set`) exits a
  non-interactive ash/dash shell entirely while bash continues. bash-as-sh passing is not
  proof of correctness.
- Absolute paths for add-on/privileged binaries; base utilities may be bare.
- Prefer shell built-ins over spawning processes: parameter expansion (`${var#prefix}`,
  `${var%suffix}`, `${var:-default}`) and `case` over calling out to `grep`/`sed`/`awk` for
  simple string work.
