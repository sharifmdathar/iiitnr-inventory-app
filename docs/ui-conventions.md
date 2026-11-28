# UI & Responsive Layout Conventions

The client is one Compose codebase running on phones, desktop and the web, so
these rules are what keep a screen looking the same everywhere. The design
system lives in `app/shared/src/commonMain/.../ui/` — extend it there rather
than re-inventing pieces per screen.

## Width caps

Two constants in `ui/components/common/Responsive.kt` cover every screen:

| Constant | Width | Use for |
|---|---|---|
| `WideContentWidth` | `1280.dp` | adaptive grids, and `Expanded` (`>= 900.dp`) Master–Detail layouts |
| `NarrowContentWidth` | `900.dp` | single-column screens, and the `AppTopBar` of focused screens |

- Wrap responsive screens in `ResponsiveContent { widthClass -> … }` and branch
  on `ScreenWidthClass.Compact` / `Medium` / `Expanded`.
- Focused-screen bodies stay narrower than the bar above them: the login form
  is `400.dp`, registration is `420.dp`, and the profile body is `560.dp`,
  centred inside `NarrowContentWidth`.
- Do **not** hardcode `1280.dp`, `1000.dp`, `900.dp` or `800.dp` caps in a
  screen. If a new tier is genuinely needed, add it to `Responsive.kt` first.

## Headers, dividers and footers

- Pass `maxContentWidth` to `AppTopBar` and `PaginationBar` so titles, actions
  and page controls line up with the content grid; the bar and footer
  backgrounds still span the full window.
- Bar titles use `colorScheme.onSurface`. `primary` is for card and list-item
  titles, not for the bar.
- Put a full-width
  `HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)` below a
  fixed header block (search + filters) and below `AppTopBar`, before the
  scrollable content. The divider stays edge-to-edge even when the content above
  it is capped.

## Cards, tints and badges

- **Neutral cards** — `surfaceContainerLow`, `elevation = 0.dp`, and
  `BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)`. No drop
  shadows; the hairline border does the separating.
- **Semantic-tinted cards** — blend the hue onto the surface instead of stacking
  alpha:

  ```kotlin
  lerp(
      start = MaterialTheme.colorScheme.surfaceContainerLow,
      stop = semanticColor,
      fraction = if (LocalIsDarkTheme.current) 0.14f else 0.07f,
  )
  ```

  Use `0.22f` dark / `0.12f` light when the row is selected (`RequestCard`).
  Borders are the semantic hue at 35% alpha and `1.dp` normally, then the full
  hue at `2.dp` when selected (`RequestCard`, `AuditLogCard`); neutral
  selectable cards select with `1.5.dp` `primary`.
- **Badges and pills** — `0.24f` alpha in dark, `0.14f` in light.

## Theming

Never call `isSystemInDarkTheme()` inside a composable. Read
`LocalIsDarkTheme.current`; the client's only `isSystemInDarkTheme()` call lives
in `ThemeMode.resolveIsDark()`, which resolves the persisted `ThemeMode`. The
Android runner resolves that mode once and passes it to
`IIITNRInventoryAppTheme(darkTheme = ...)`; the theme wrapper does not call it
again. Semantic colours come from `MaterialTheme.inventoryColors`.

## Controls and ergonomics

- Filters are borderless `TextButton` pills — selected is `primary` (or the
  relevant semantic colour), unselected is `onSurfaceVariant`. No `FilterChip`.
- Dropdowns use `ExposedDropdownMenu` with
  `Modifier.exposedDropdownSize(matchAnchorWidth = true)`, so the menu matches
  the field instead of shrinking to its widest item.
- Images use `SubcomposeAsyncImage` with a category/icon placeholder for
  `loading`, for `error`, and for a null URL — never a broken-image glyph.
- Icon-only actions get a `TooltipBox`; clickable elements get
  `Modifier.handCursor()`.
- Never stretch a button full-width on `Expanded`; buttons belong in a dialog or
  inside a capped column.

## UI verification

`just check` stays the required gate, but it compiles only the JVM and Android
targets. For anything touching `app/shared/` UI, also run:

```bash
just check-ui
```

It runs ktlint and detekt, compiles the desktop, shared WasmJs, web WasmJs and
Android targets, and reruns `:shared:jvmTest`. That catches a Compose API that
exists on the JVM but not in the web toolchain, which `jvmTest` alone misses.

`just check-ui` is a focused UI gate, not a replacement for `just check-full`:
`check-full` adds the backend tests and the deployed web bundle, while
`check-ui` adds the desktop compile. For UI work, run `just check` plus
`just check-ui`; run `just check-full` too before merging anything touching
`app/shared/` or `app/web/`.
