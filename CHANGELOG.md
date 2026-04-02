# Changelog

All notable changes to this project will be documented in this file.

---

## [v1.3.0] - Latest

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