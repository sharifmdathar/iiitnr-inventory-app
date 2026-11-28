# AGENTS.md

High-signal notes for coding agents. Run commands from the repository root.
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
  after changing `backend/src/drizzle/`; never hand-edit it.
- Root `CHANGELOG.md` is the only changelog source. Gradle copies it into
  Compose resources at build time; the nested copy under
  `app/shared/src/commonMain/composeResources/files/` is git-ignored. Never
  edit the nested copy.

## Required checks

- `just check-fast` — before each commit: backend static checks, ktlint,
  detekt, JVM tests and the Android debug build.
- `just check` — before pushing: `check-fast` plus the DB-backed backend tests.
- `just check-full` — before merging changes touching `app/shared/`, `app/web/`
  or backend code consumed by the web build: adds the WasmJs web bundle.
- `just check-ui` — required when touching `app/shared/` UI: compiles the
  desktop, shared/web WasmJs and Android targets and reruns JVM tests.
- `just fmt` / `just fmt-check` — format or verify formatting. `just lint-fix`
  auto-fixes eslint and Kotlin formatting.

## Hard rules

- Read `docs/ui-conventions.md` before any Compose UI change.
- Use `LocalIsDarkTheme.current` for dark mode, `ResponsiveContent` and the
  widths in `Responsive.kt` for layout, and `MaterialTheme.inventoryColors` for
  semantic colours. Never hardcode width caps such as `1280.dp`, `1000.dp`,
  `900.dp` or `800.dp`.
- Map failures with `toAppError()`; re-throw `CancellationException`, never
  swallow it.
- Keep pure logic out of composables and add unit tests for it.
- User-facing changes need a bullet under `## Unreleased` in `CHANGELOG.md`
  using `new`, `improved`, `ui`, `fix` or `security`.

## Git and secrets

- Conventional commits, imperative mood, no trailing period; one logical
  change per commit.
- Keep history linear: rebase instead of merging `main`, and never use a merge
  commit.
- Never commit `backend/.env`, `app/local.properties`, keystores or signing
  passwords; CI supplies them as secrets.
- Do not commit generated build output.
