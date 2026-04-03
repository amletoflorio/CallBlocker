# Changelog

All notable changes to this project will be documented in this file.

---

## [v1.4.0] - Latest

### New Features
- **Dual SIM support** — choose which SIM card(s) to protect: SIM 1, SIM 2, or both; selector is hidden automatically on single-SIM devices
- **SIM badge on Home screen** — shows which SIM(s) are currently protected when the service is active and a dual-SIM device is detected
- **Language selector** — choose between system default, English, and Italian; the app applies the change immediately via `Activity.recreate()` without requiring a manual restart
- **What's new screen** — in-app changelog accessible from the Info tab and from the top bar icon in Settings; shows full version history with color-coded NEW / FIX / IMPROVE badges
- **Dedicated Updates tab** — moved update check out of the Info tab into its own tab in Settings, with auto-check every 24 h via `UpdateCheckWorker` (WorkManager) and an opt-in notification when a new version is available on GitHub

### Improvements
- Settings screen restores the previously selected tab after a language change (no longer resets to the Protection tab)

### Configuration
- Added `READ_PHONE_STATE` permission to `AndroidManifest.xml` (used for dual-SIM detection via `SubscriptionManager`)
- Added `LocaleHelper` utility for locale wrapping in `attachBaseContext`
- Added `UpdateCheckWorker` with network constraint and automatic retry on error
- Bumped `versionCode` to 4 and `versionName` to `1.4.0`

---

## [v1.3.0]

### New Features
- **Automatic backup with WorkManager** — periodic JSON export every 1, 7, or 30 days (configurable), saved to `Documents/CallBlocker/` on the device
- **"Last export" label** — the Backup tab now shows the date and time of the last successful automatic backup
- **Silent backup notification** — a low-priority notification is sent when the automatic backup completes, showing the number of contacts exported
- **Update check via GitHub Releases** — opt-in toggle in the Info tab; tapping "Check now" calls the GitHub API, compares the tag with the current version, and shows a dialog with a download link if a newer release is available
- **Dynamic version display** — the Info tab now reads the version from `BuildConfig.VERSION_NAME` instead of a static string, so it always matches the actual build

### Configuration
- Added `INTERNET` permission to `AndroidManifest.xml` (used only for the opt-in update check)
- Added `androidx.work:work-runtime-ktx:2.9.0` dependency for WorkManager
- Enabled `buildConfig = true` in `buildFeatures` (required for `BuildConfig.VERSION_NAME`)
- Bumped `versionCode` to 3 and `versionName` to `1.3.0`

---

## [v1.2.0]

### New Features
- Added Italian locale (auto-detected from system) via `res/values-it/strings.xml`
- Improved `PhoneUtils.formatForDisplay()` with country-aware formatting (IT, DE, FR, ES, UK, US/CA)
- Possibility to edit whitelist contacts

### Refactoring
- Extracted all hardcoded strings to `res/values/strings.xml` (EN default)
- Migrated all UI screens and ViewModel to `stringResource` / `context.getString()`

---

## [v1.1.0]

### Bug Fixes
- Fixed toggle state not updating on resume
- Fixed toggle now suspends/resumes protection instead of revoking role

### Refactoring
- Refactored settings screen with tab layout (Protection / Backup / Info)

### New Features
- Added in-app snackbar feedback for export/import operations
- Added GitHub link in Info tab

---

## [v1.0.0]

### New Features
- Initial release