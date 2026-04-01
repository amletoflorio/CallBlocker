# Changelog

All notable changes to this project will be documented in this file.

---

## [v1.2.0] - Latest

### New Features
- Added Italian locale (auto-detected from system) via `res/values-it/strings.xml`
- Improved `PhoneUtils.formatForDisplay()` with country-aware formatting (IT, DE, FR, ES, UK, US/CA)

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