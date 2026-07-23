#!/bin/sh
# scripts/lib/git-env-scrub.sh — shared GIT_* context scrubber.
#
# Safe to source: no top-level side effects, no set -e, no traps.
#
# Usage:
#   . "${DIR}/lib/git-env-scrub.sh"
#   scrub_git_env
#
# Why a FUNCTION and not a bare top-level unset:
#   A subprocess cannot unset vars in the PARENT shell. Sourcing a file that calls
#   `unset` top-level WORKS for a sourced file, but collecting the operation into a
#   FUNCTION that the caller invokes means the unset runs in the caller's own shell
#   context — the only place it can affect the caller's git operations.
#
# GIT_* vars exported by the pre-commit hook (and inherited by any child process
# running during a commit) point at the live repo's objects and index — so any git
# command in a child process operates on the REAL repo instead of its own fixture.
# One scrub call per entry-point eliminates the class.

scrub_git_env() {
    unset GIT_DIR GIT_INDEX_FILE GIT_WORK_TREE GIT_PREFIX GIT_OBJECT_DIRECTORY GIT_COMMON_DIR
}
