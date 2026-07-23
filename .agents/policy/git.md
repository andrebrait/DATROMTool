# Git — worktrees, hooks, branches, commits

Scope: worktree mechanics, git hooks, branch naming, rebase/diff hygiene, commit style and
attribution. Load when: committing, branching, pushing, or cutting a worktree.

## Worktrees (mandatory for AI agents)

**Every AI agent MUST do all repository work in its own dedicated git worktree** — never the
primary checkout, never shared with another agent (concurrent agents race on the filesystem,
index, `HEAD`, refs). Session layouts (primary checkout vs harness-made session worktrees):
[`sessions.md`](sessions.md).

**Exception — dev-only classes need no PR.** Classes never shipped to users skip the PR
stage: **agent config** (`.claude/`, `.codex/`, `.agents/`), **skills**, and
**documentation-only** changes (`**/*.md`, `AGENTS.md`, `CLAUDE.md`). Each still uses a
worktree but commits/pushes **directly to `master`** (fetch + rebase first). Anything
touching a module `src/` tree (`domain/`, `core/`, `cli/`, `logging/`), tests, `pom.xml`, or
CI uses the full worktree + rebase-only-PR flow.

```sh
git worktree add -b <branch> <path> origin/master   # branch off the latest base
# … work, commit, push, open the PR from inside <path> …
git worktree remove <path>            # run from any directory OUTSIDE <path>
```

- Branch off the **current** base (`git fetch` first); a stale-tip worktree needs a rebase
  before it can land.
- **A rebase needs a visible merge base.** In a shallow checkout, a normal fetch can move
  `origin/master` beyond the retained history while an older worktree still points behind the
  shallow boundary; plain `git rebase origin/master` then mistakes base commits for work and
  replays them. If `git merge-base HEAD origin/master` produces no commit, deepen/unshallow
  the fetch first and retry the ancestry check. Never start the rebase without it.
- **Reuse only YOUR OWN worktree — never adopt one you merely found.** A worktree at the
  conventional path that you did not create this run may belong to a live parallel session:
  `git -C <path> status` — foreign uncommitted changes ⇒ not yours; never `--force`-remove
  it; cut a fresh uniquely-named worktree (suffix `-{epoch}`).
- **Reuse a branch for a follow-up ONLY when no other session owns its PR** (foreign
  commits/pushes, running CI, review replies, an assignee/open-`Fixes #N`-PR you didn't set
  ⇒ another session owns it: wait, cooperate, or start a NEW branch after the merge).
  **Never force-push over another session's in-flight PR.**
- Name the branch for its work item — `issue/{NN}-{slug}` (or `adr/{NN}-{slug}`).
- Gotchas: `git worktree remove` fails from inside the tree — run it from any directory
  outside the tree being removed. `gh pr merge --delete-branch` can't check out a base
  another worktree holds — verify the merge landed, then `git push origin --delete <branch>`.

## Git hooks

Activate once after cloning: `sh scripts/setup-hooks.sh` (sets `core.hooksPath` to
`.githooks`). If `git config core.hooksPath` is not `.githooks`, an agent runs it at session
start (idempotent).

- **`prepare-commit-msg`** — first aborts an agent commit (`CLAUDECODE=1` or
  `CODEX_THREAD_ID` set) in the **primary checkout** (agents commit only in linked worktrees;
  state-checked via `--git-dir` vs `--git-common-dir`, never command text; agent-dedicated
  checkouts opt out via `DATROMTOOL_USER_EMAIL` or
  `git config datromtool.allowprimarycommit true`), then appends the owner's
  `Co-authored-by:` trailer (see Commit style); runs even under `--no-verify`.
- **`pre-push`** — enforces the release tag scheme (`vX.Y.Z` stable / `vX.Y.Z-rc<N>`
  prerelease; any other `v*` tag is rejected) and denies an agent (`CLAUDECODE=1` or
  `CODEX_THREAD_ID` set) branch push that would rewrite remote history the agent never
  fetched (the advertised remote oid must equal the remote-tracking ref —
  `--force-with-lease`'s check enforced by effect).

No pre-commit linter hook yet: DATROMTool has no linter/formatter configured (a future
Checkstyle/Spotless addition would wire one). The gate is `mvn verify`, run yourself while
iterating; CI (`.github/workflows/maven.yaml`) is the hard gate.

## Rebase and diff hygiene

**Rebase onto the latest `master` before every push, PR, or CI dispatch.** `master` advances
out of band: `git fetch origin` + `git rebase origin/master`, `--force-with-lease` if
rewritten; never reconcile with a merge commit. A stale base re-runs bugs the base already
fixed and sends you chasing a phantom regression; a freshly-rebased branch that still fails
is genuinely your bug.

**Clean the diff before you push/PR.** `git diff origin/master...HEAD` and reduce it to only
what the change requires — strip debug logging, dead/commented-out experiments,
churned-then-reverted code, introduced-then-unused symbols, gratuitous reformatting, scratch
files. Cheapest before the PR exists.

**Push as soon as a commit is green and final.** A commit that exists only on this
workstation is invisible and unbacked-up work; never let commits pile up locally waiting for
some later batch push. Dev-only commits push straight to `master`; code branches push to
their own remote branch and carry on into the PR flow ([`landing.md`](landing.md)).

## Branch naming (issues and ADRs)

**Issue** `issue/{NN}-{slug}`, **ADR** `adr/{NN}-{slug}`; `{slug}` derives from the title by
this **mandatory** sanitiser:

1. Lowercase.
2. Strip emojis + every non-ASCII char; drop anything not `[a-z0-9]`.
3. Collapse each removed/non-alphanumeric run to a single `-`; trim leading/trailing `-`.
4. Truncate ≤30 chars at a `-` boundary (never trailing `-`).
5. Empty slug → omit it (bare `issue/{NN}` / `adr/{NN}`).

Output is `[a-z0-9-]` only. **Never hand-derive it**: `scripts/agent/work-branch.sh
<issue|adr> <NN> [title...]` implements the sanitiser; `--worktree` also cuts the worktree at
an absolute path off `origin/master`. **On collision** with an *unrelated* branch, append
`-{epoch}` (epoch seconds). Example: issue #43 "TLD-Allow KeyError on …" →
`issue/43-tld-allow-keyerror-on`.

## Commit style

`<scope>: <imperative summary>` (follow the existing log — e.g. `core: fix region
precedence in GameSorter`, `ci: pin temurin 25`). No trailing period; body optional for
non-obvious changes. Scope is usually the module (`domain`/`core`/`cli`/`logging`) or an
area (`ci`, `build`, `docs`).

**Attribution:** keep the human owner visible and earn a GitHub **Verified** badge. On a box
with the **user's own signing key**, the user authors/commits/signs as themselves; credit the
active AI client with a `Co-authored-by:` trailer only when its provider adapter defines a
verified, GitHub-recognized identity. Claude's adapter uses `Claude <noreply@anthropic.com>`;
Codex has no verified coauthor identity — disclose it in the PR footer, never fabricate or
borrow another provider's identity. The owner is Andre Brait
`<andrebrait@gmail.com>` (@andrebrait).

## Author, committer, and signing (full text)

Two environments, two attribution shapes — both keep the human owner visible and earn a
GitHub **Verified** badge. Pick by whether the box has the user's own signing key.

**User's personal environment, signing with the user's own key** (`commit.gpgsign = true`, or
a configured `user.signingkey`): do **not** override the local identity — the user authors,
commits, and signs as themselves (Verified as the user). A non-user client is then not the
committer, so credit it via the trailer: **add `Co-authored-by: Claude <…>` as the final
line(s)** using Claude's GitHub-recognized identity (an unrecognized email credits no one);
Codex work discloses in the PR footer instead. Leave the user's `-S` in place; do not add
`--author=`. `.githooks/prepare-commit-msg` injects the owner's `Co-authored-by:` trailer
automatically (resolving `coauthor.email`/`coauthor.name`, else `$DATROMTOOL_USER_EMAIL`,
else the commit author) and is a no-op when the human is already the committer or already
credited.

**Agent / managed-remote environment (no user signing key on the box):** committer = signer =
the client's GitHub identity (the account whose verified email owns the registered signing
key — GitHub binds the Verified badge and commit credit to the committer); author = the human
owner, set explicitly (`--author=` / `GIT_AUTHOR_*`); the human is credited via the
`Co-authored-by:` trailer the hook injects. Sign every commit (`-S`). This mode needs the
client's committer email verified on its GitHub account holding the registered signing key;
until provisioned, commits land correctly attributed but read *Unverified*.
