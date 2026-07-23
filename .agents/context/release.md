# Releases & versioning

Scope: tag scheme and the release pipeline. Load when: cutting a release or touching
`.github/workflows/maven.yaml`.

Base branch is **`master`**. Releases are driven entirely by pushing a git **tag**.

## Tag scheme

- **Stable:** `vX.Y.Z` → GitHub Release (not prerelease).
- **Prerelease:** the tag **contains `-rc`** (e.g. `vX.Y.Z-rc1`) → GitHub Release marked
  prerelease.

The release job decides prerelease via `contains(github.ref, '-rc')` (literal substring on the
ref). Maven's `tagNameFormat` is `v@{project.version}`. The **pre-push hook enforces the tag
scheme locally**, so a malformed tag is refused before it ever reaches CI.

## Pipeline (`.github/workflows/maven.yaml`)

The `CI` workflow runs on pushes/PRs to `master` and on `v*` tags:

- **build** job — matrix `ubuntu-latest` × `macos-latest` × `windows-latest`, Java 25
  (temurin): `mvn … clean compile test-compile -U`, then `mvn … verify -U`.
- **release** job — gated on `startsWith(github.ref, 'refs/tags/v')`, `needs: build`, runs on
  `ubuntu-latest`:
  1. `mvn … package -DskipTests` (builds each module's `*-jar-with-dependencies.jar`).
  2. Renames each module fat jar to `DATROMTool-<name>.jar`, then zips each with `LICENSE` and
     `README.md`.
  3. Publishes a GitHub Release with the `build/*.zip` artifacts;
     `prerelease: ${{ contains(github.ref, '-rc') }}`.

**Pushing the tag is what triggers the release** — there is no separate dispatch. Tag a
commit, push it to origin, and the workflow builds, tests, packages, and publishes.
