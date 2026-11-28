# AGENTS.md

You are a careful coding agent working in this repository. Use the available
repository tools (`read`, `edit`, `write`, `glob`, `grep`, `bash`) and run
commands from the repository root. Last reviewed: 2026-11-28.

For the full workflow, see [CONTRIBUTING.md](CONTRIBUTING.md); for Compose UI
rules, read [docs/ui-conventions.md](docs/ui-conventions.md) before editing a
screen.

## Architecture and generated files

- `backend/` — Fastify + Bun + Drizzle/PostgreSQL API. Backend tests live in
  `backend/tests/`.
- `app/shared/` — the actual Compose Multiplatform client (UI, data, platform
  storage). Platform modules (`android/`, `desktop/`, `web/`, `iosApp/`) are
  thin runners.
- `docs/schema.md` is generated from the Drizzle schema. Run `just gen-schema`
  after changing `backend/src/drizzle/`; hand-edit it only as a last resort,
  and fix the generator first so the next run reproduces the change.
- Root `CHANGELOG.md` is the only changelog source. Gradle copies it into
  Compose resources at build time; the nested copy under
  `app/shared/src/commonMain/composeResources/files/` is git-ignored. Edit the
  nested copy only by rerunning the copy task; do not commit it.

## Required checks

- `just check-fast` — before each commit: backend static checks, ktlint,
  detekt, JVM (Java Virtual Machine) tests and the Android debug build.
- `just check` — before pushing: `check-fast` plus the DB-backed backend tests.
- `just check-full` — before merging changes touching `app/shared/`, `app/web/`
  or backend code consumed by the web build: adds the WasmJs web bundle.
- `just check-ui` — required when touching `app/shared/` UI: compiles the
  desktop, shared/web WasmJs and Android targets and reruns JVM tests.
- `just fmt` / `just fmt-check` — format or verify formatting.
- `just lint-fix` — auto-fix eslint and Kotlin formatting.

## Hard rules

- Read `docs/ui-conventions.md` before any Compose UI change.
- Use `LocalIsDarkTheme.current` for dark mode, `ResponsiveContent` and the
  widths in `Responsive.kt` for layout, and `MaterialTheme.inventoryColors` for
  semantic colours. Prefer those constants; avoid hardcoding width caps such as
  `1280.dp`, `1000.dp`, `900.dp` or `800.dp`. If a new tier is genuinely
  needed, add it to `Responsive.kt` first.
- When mapping an API failure, use `toAppError()`; re-throw
  `CancellationException` after cleanup rather than swallowing it.
- Keep pure logic out of composables and add unit tests for it.
- User-facing changes need a bullet under `## Unreleased` in `CHANGELOG.md`
  using `new`, `improved`, `ui`, `fix` or `security`.

## Git and secrets

- Conventional commits, imperative mood, no trailing period; one logical
  change per commit.
- Keep history linear: rebase instead of merging `main`; if a merge commit is
  unavoidable, document the exception in the PR.
- Do not commit `backend/.env`, `app/local.properties`, keystores or signing
  passwords; CI supplies them as secrets. If one is committed by mistake,
  rotate it and rewrite history before merging.
- Do not commit generated build output.
