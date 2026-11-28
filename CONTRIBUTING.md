# Contributing

Thanks for taking the time to contribute. This document covers the things that
are easy to get wrong: which checks must pass, how to run them locally, and how
changes land on `main`.

If you only read one section, read [Before you commit](#before-you-commit).

---

## What is in here

| Path | What it is |
|---|---|
| `backend/` | Fastify + Bun + PostgreSQL API, Drizzle schema, `bun test` suites |
| `app/` | Kotlin Multiplatform client |
| `app/shared/` | Shared Compose UI + data layer (the actual app lives here) |
| `app/android/`, `app/desktop/`, `app/web/`, `app/iosApp/` | Platform runners |
| `docs/` | Long-form docs — `schema.md` is **generated**, everything else is hand-written |
| `CHANGELOG.md` | User-facing release notes; copied into the app at build time |
| `Justfile` | Task runner for every workflow in this file |

---

## Setup

Prerequisites and first-run setup live in the README:
[Quick Start & Development Setup](README.md#-quick-start--development-setup).

Short version, once you have Bun 1.4+, JDK 25+, Podman (or Docker) and `just`:

```bash
bun install --cwd backend          # or: just install
cp backend/.env.example backend/.env   # then fill in JWT_SECRET
just db-up                          # PostgreSQL via Compose
bun run --cwd backend migrate
just dev
```

The toolchain versions are pinned in [`mise.toml`](mise.toml) — Bun 1.4+,
Temurin 25+, and the Android SDK. CI uses **Temurin 25**, so build with 25 to
reproduce it exactly.

---

## Before you commit

`just check` is the single gate for CI's pull-request checks. The other recipes
below are faster loops or extra coverage.

| Command | Covers | When to run |
|---|---|---|
| `just check-fast` | typecheck, eslint, prettier, ktlint, detekt, schema docs, `:shared:jvmTest`, `:android:assembleDebug` | before every commit (~8 s once warm) |
| `just check` | `check-fast` **+** the backend test suite in a throwaway Postgres | before every push / PR (~16 s once warm) |
| `just check-full` | `just check` **+** the WasmJs web bundle | before merging anything touching `app/shared/`, `app/web/`, or backend code that the web build consumes |
| `just check-ui` | ktlint, detekt, desktop, shared/web WasmJs and Android compiles, `:shared:jvmTest` | when touching `app/shared/` UI |

`check-ui` is supplemental: it is not part of `check`, so run it in addition
when you change shared Compose UI. It adds the desktop compile; `check-full`
adds the backend tests and the web bundle.

`just push` runs `just check` and then pushes, so you cannot forget it.

The steps run concurrently and the output is deliberately quiet: a passing run
prints one line, and a failing run prints only the steps that failed.

```bash
just check                      # ✓ check passed (3 steps, 16.0s)
just check-fast -v              # per-step timings, when one step got slow
CHECK_VERBOSE=1 just check      # same, via the environment
```

**If a run feels slow, check memory before blaming the code.** Gradle is
configured with `-Xmx6g`, so it will happily swap an 8 GB machine. Concurrency
is capped at two steps by default (`CHECK_JOBS`); raising it only helps when
there is headroom, and the backend suite is usually the long pole regardless.
`CHECK_GRADLE_PARALLEL=1` enables cross-project Gradle parallelism.

The gate needs bash. On Windows use `just check-seq`, which runs the same steps
sequentially.

### What breaks if you skip a step

Each gate exists because something specific slipped through without it:

- **ktlint** — catches unused imports, import ordering, trailing whitespace and
  blank-line style in Kotlin. Cheap, and purely mechanical: `just lint-fix`
  rewrites it for you.
- **prettier** (`just fmt-check`) — the backend's formatting gate. It runs
  `prettier --check`, so it fails instead of rewriting; `just fmt` fixes it.
- **detekt** — flags swallowed exceptions, complexity, and other defects lint
  cannot see. Not auto-fixable; fix the code or justify a `@Suppress` with a
  reason.
- **`:shared:jvmTest`** — the KMP unit suite in `app/shared/src/commonTest`.
  Note it only compiles the JVM target.
- **`:android:assembleDebug`** — separate on purpose. `jvmTest` never compiles
  `androidMain`, so Android-only breakage passes every other gate and dies here
  (or worse, in the release build).
- **`web-build`** — `deploy-pages.yml` builds this on every push to `main`. If it
  breaks, the site stops deploying even though every PR check was green.
- **schema docs check** — `docs/schema.md` is generated from the Drizzle schema
  and CI fails if it is stale. Regenerate with `just gen-schema` after touching
  `backend/src/drizzle/`.

### What CI runs

`ci.yml` has one filter job and two real jobs. It builds the backend for
`backend/**` and the Kotlin side for `app/**`; both sides also run when a
workflow or `mise.toml` changes.

- **Backend** — typecheck, schema-docs check, `bun test`, eslint.
- **Kotlin** — `ktlintCheck`, `detekt`, `:shared:jvmTest`,
  `:android:assembleDebug`.

Two workflows are **not** part of the pull-request gate: `ios-test.yml` is
manual (`workflow_dispatch`), and `release.yml` runs on published releases. If
you change `iosMain` code, run the iOS job from the Actions tab — a broken iOS
target will not be caught here.

> **Keep this list honest.** `just check` must stay a superset of CI's
> pull-request gate. `check-fast` intentionally skips the DB-backed backend
> suite, so add CI-only backend test steps to `just check`, not `check-fast`.
> Otherwise the local gate silently rots and `main` starts failing.

---

## Style

Formatting is enforced, not discussed. `just check` fails if any file is not
already formatted — formatting is never fixed silently during a check.

```bash
just fmt-check     # verify only: prettier --check + ktlintCheck
just fmt           # rewrite: prettier -w + ktlintFormat
just lint-fix      # rewrite: eslint --fix + ktlintFormat
```

If `just fmt` changes files, commit those separately so the reformat is not
buried inside a behavioural change.

- **Backend (TypeScript)** — eslint + prettier. Types are checked with
  `tsc --noEmit`, which is stricter than `bun build`, so run `just typecheck`.
- **Kotlin** — ktlint for layout, detekt for quality. Prefer reusing the helpers
  in `app/shared/src/commonMain/.../utils/` (date formatting, error mapping)
  over re-implementing them in a screen.
- **Errors** — map failures through `toAppError()` and let `AuthEventManager`
  handle 401s globally. Re-throw `CancellationException`; never swallow it.
- **Never commit secrets.** `backend/.env`, `app/local.properties`, keystores and
  signing passwords are gitignored — keep it that way. CI supplies them as
  Actions secrets.

---

## UI work

Compose rules (width caps, bars, cards, theming) live in
[docs/ui-conventions.md](docs/ui-conventions.md). Read it before changing a
screen; run `just check-ui` for shared UI changes.

---

## Tests

Every bug fix deserves a test that fails without the fix. New behaviour in the
shared client should be testable without a UI: extract the logic (grouping,
filtering, formatting) into a plain function and test *that* — that is why
`RequestFormattingTest` exercises formatting helpers instead of rendering a
screen.

```bash
just app-test                  # KMP suite, JVM target
just test                      # backend suite; starts and tears down its own Postgres
cd app && ./gradlew :shared:jvmTest --tests '*RequestFormattingTest*'   # one class
cd backend && bun test tests/auth.test.ts                    # one file
```

`just test` sets `TEST_DATABASE_URL` for you and removes the container on exit.
If you run `bun test` directly, start `just db-up` first and export
`TEST_DATABASE_URL=postgresql://postgres:postgres@localhost:5432/iiitnr_inventory_test`,
or the DB-backed suites will fail to connect.

---

## Changelog

User-visible changes need a release note in [`CHANGELOG.md`](CHANGELOG.md), in
the same PR as the code. That includes new features, UI/UX changes, fixes and
security changes. Refactors, tests, CI and dependency chores do not need an
entry.

Edit the repository-root `CHANGELOG.md` only. Gradle copies it into the app's
Compose resources at build time; the nested copy is generated and git-ignored.
While a release is in progress, add bullets under `## Unreleased` — the
maintainer renames it to the version number when releasing. Every bullet must
start with a recognized kind (case-insensitive, aliases in the file header):

```markdown
- new: …
- improved: …
- ui: …
- fix: …
- security: …
```

Bullets are displayed grouped in that order (`new`, `improved`, `ui`, `fix`,
`security`); keep related bullets together and write them for users, not as a
commit log.

---

## Commits

Write commits that stand on their own, because history is not squashed — see
[How changes land](#how-changes-land).

- Conventional prefixes, imperative mood, no trailing period:
  `feat(ui): …`, `fix(backend): …`, `chore: …`, `test: …`, `ci: …`, `docs: …`,
  `refactor: …`. Component prefixes (`app:`, `backend:`) are also in the history.
- Subject ≤ ~72 characters; use the body to explain **why**, not what.
- One logical change per commit. A refactor and a behaviour change in the same
  commit are impossible to review and impossible to revert independently.
- Do not commit generated build output, and do not hand-edit `docs/schema.md`.

---

## Pull requests

1. Branch off `main`: `feat/short-description`, `fix/short-description`.
2. Make sure `just check` passes before you push. For UI changes, also run
   `just check-ui` — see [UI work](#ui-work).
3. Add a [`CHANGELOG.md`](CHANGELOG.md) bullet for user-visible changes — see
   [Changelog](#changelog).
4. Open the PR with:
   - **what changed** and **why**,
   - **how you verified it** (commands you ran, screenshots for UI work),
   - anything the reviewer should look at closely, and known gaps.
5. Keep it focused. Unrelated cleanups belong in their own PR.
6. CI must be green before merge. If a job is skipped, say why in the PR.

### Reviewing

Check that the diff actually matches the description, that new logic has a test,
and that no secrets, debug logging, or commented-out code came along for the
ride.

### Dependency updates

Renovate opens dependency PRs from `renovate.json`. GitHub Actions minor,
patch, digest and pin updates are automerged after CI; container images,
backend/Gradle updates and every major update need review. Don't enable
Dependabot version updates alongside it; Dependabot alerts are still useful.

The Mend Renovate GitHub App must be installed on the repository with access to
contents, pull requests, issues and workflows. Without that installation the
config is inert.

---

## How changes land

`main` keeps a **linear history**: no merge commits.

- Land PRs with **Rebase and merge** (or **Squash and merge** for a messy branch).
- **Never** use "Create a merge commit".
- If your branch is behind, rebase it — don't merge `main` into it:

```bash
git fetch origin
git rebase origin/main
git push --force-with-lease
```

Rebasing a branch you own is normal here. Use `--force-with-lease`, never a bare
`--force`, so you cannot clobber someone else's push.

If you are a maintainer updating a contributor's PR: contributors from forks
often allow maintainer edits (GitHub's "Allow edits by maintainers"). Rebase
locally, verify with `just check`, then force-push their branch so the PR updates
in place instead of opening a duplicate.

---

## Releasing

Maintainers only:

```bash
just release 1.18.0    # pushes main, creates a GitHub release with generated notes
just deploy            # rebuild backend container + publish web bundle
```

Bump `VERSION`; the Android and desktop builds and release asset names read it
directly. Keep `backend/package.json` in sync for the backend's package metadata
and version test.

---

## License

By contributing you agree your work is released under the [MIT License](LICENSE).
