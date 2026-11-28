<!--
Release notes bundled with the app and shown in the "What's new" dialog.

This file lives at the repository root on purpose: it is the one place to edit.
At build time Gradle copies it into the app's Compose resources
(:shared:copyChangelogToComposeResources); the copy under
src/commonMain/composeResources is generated and git-ignored. Edit this file only.

While a release is in development, accumulate bullets under `## Unreleased`;
rename it to the version number when cutting the release. The app only ever
shows numeric versions, so `Unreleased` notes stay hidden until renamed.

Every bullet must start with a kind prefix, e.g. `- fix: ...`. Recognized
kinds (case-insensitive): new (also: feat, feature), fix (also: fixed),
improved (also: improvement), ui, security.

Bullets are grouped when shown in the app in this order: new, improved, ui,
fix(ed), security. The parser sorts stably, so writing order within a kind is
preserved; grouping the file the same way just makes it easier to read.
-->
# Changelog

## 1.18.1
- fix: Packaged apps (Windows installers, Linux AppImage) no longer fail at launch with "Failed to launch JVM" — the bundled runtime was missing the java.sql module

## 1.18.0
- new: About & feedback in Profile Screen: reach the team, report bugs, request features, check for updates
- new: Pay fines in the app: pay over UPI, submit your payment proof, and LAs verify it + you're notified of the decision
- new: "What's new" notes appear once after the app updates or on a fresh install (this dialog!)
- improved: Late fees are frozen the moment you return an item
- ui: Profile screen regrouped with a fines entry; avatar in the top bar with logout confirmation
- fix: Rate limiting no longer locks everyone out while browsing component images
- fix: Theme selection and "What's new" progress now persist across launches on Android, desktop and iOS
- fix: CSV export/import works in the web app again
- security: Signup role requests (LA/Faculty) are reviewed by an admin before elevation

## 1.17.9
- new: Fines for everyone: students see their total owed and a per-request breakdown, while LA/Admin get outstanding fines with per-student drill-down
- new: Responsive desktop layouts: master–detail requests, fines and audit log, plus wider component grids
- new: Theme mode (Auto / Light / Dark) with a preference saved on Android, desktop and iOS
- new: Profile redesign with a compact info card and appearance switcher, plus a role filter in user management
- improved: Late fee cap raised from ₹30 to a full four-year programme; the 30-day write-off still applies
- improved: Component CSV export now comes from the backend and covers every component, not just the loaded page
- ui: Renamed "Fulfilled" to "Issued" throughout the app
- fix: Renewal-reminder emails now send for the correct reminder kind
- fix: Fines screens show specific load errors and handle missing item data cleanly
- fix: Request notifications are sent only after the database commit, so failed requests no longer notify
- security: Web sessions keep the token out of localStorage (in-memory token plus an httpOnly cookie); Android backups exclude the token
- security: Closed self-service admin escalation, rate-limit bypasses, and lingering password logins for Google-linked accounts; login is throttled per account
- improved: Request details show absolute due dates (e.g. 20 Aug 2026) instead of "Due in 24d"