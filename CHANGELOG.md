# Changelog

All notable changes to this project will be documented in this file.

---

## [v1.8.0] - Latest

### New Features

#### Home screen widgets
- **Toggle widget (2×1)** — one-tap on/off button on the home screen; no need to open the app. Uses `suspendUntil` to mirror the exact toggle logic of the app itself
- **Status widget (2×2)** — informational widget showing current protection state, whitelist contact count, and the last blocked call (number + timestamp)
- Both widgets are compatible with MIUI/HyperOS launchers (RemoteViews-safe layouts)

#### Full settings export / import
- **Export all settings** — serialises every user-configurable preference to a single JSON file; suitable for migration to a new device or reinstall
- **Import all settings** — restores the full configuration from a previously exported JSON file
- Available in Settings → Backup, with dedicated Export / Import buttons (separate from the whitelist backup)

#### SIM badge in call log list
- Each entry in the blocked calls list now shows a small SIM badge (SIM1 / SIM2) inline with the date, when the SIM could be determined

### Improvements
- Home screen title redesigned: **"Call"** in white, **"Blocker"** in emerald green — more distinctive and on-brand
- Toggle circle now shows a **pulsing glow ring** when protection is active (infinite animation via `rememberInfiniteTransition`)
- Status banners (SIM badge, suspended, schedule) now **animate in and out** with `AnimatedVisibility` (fade + vertical expand/shrink)
- **Boot setting** moved from its own tab into the Backup tab as a card — the tab row is shorter and better organised
- Export settings card in Backup now uses the correct button labels ("Export" / "Import") instead of reusing the whitelist export strings
- App icon redesigned: **shield + phone handset**, dark Slate→Emerald gradient background
- Launcher background color updated to `#020617` (Slate950) to match the app dark theme

### Bug Fixes
- **Fix: widget load error on MIUI** — `android:background` with a custom shape drawable on the `RemoteViews` root is not supported on MIUI; replaced with a plain solid color (`#E5020617`)
- **Fix: `android:description` and `android:targetCellWidth/Height`** attributes in `appwidget-provider` XML caused load errors on MIUI (API 31+ only); removed
- **Fix: widget toggle not syncing with app state** — the widget previously wrote `callProtectionEnabled = false`; the app reads `suspendUntil`. Widget now uses `suspendUntil = Long.MAX_VALUE` (off) / `cancelSuspend()` (on), matching the app's own toggle logic exactly
- **Fix: StatusWidget crash on load** — `countSync()` and `getLastBlockedSync()` are synchronous Room queries that must not run on the main thread; widget now dispatches DB work to a background `Thread`
- **Fix: call passed through when protection appeared active** — `callProtectionEnabled` was left as `false` in SharedPreferences by an older widget version; the service now relies solely on `isSuspended`; `callProtectionEnabled` is deprecated, always returns `true`, setter is no-op
- **Fix: SIM slot always null on MIUI** — `buildSimMapFromCallLog` now handles negative `subscription_id` values written by MIUI (e.g. `-979867179`); added Strategy 2 (ordering-based mapping when ICCID is empty) and Strategy 3 (process-of-elimination deduction from partial map)
- **Fix: notification icon** — uses correct alpha-mask format (`#FF000000`) on all Android versions; the previous `#FFFFFFFF` path defeated the mask mechanism
- **Fix: `ic_launcher_background.xml` drawable** — removed to eliminate resource name conflict with `@color/ic_launcher_background` referenced by the adaptive icon
- **Fix: unescaped apostrophes in Italian strings** — `l'ultima` and `dell'app` caused `AAPT Failed to compile values` error
- **Fix: `BackupTab` missing `@Composable` annotation** — caused compile error
- **Fix: `ContactDao.countSync()`** — used wrong table name `contacts` instead of `allowed_contacts`
- **Fix: `StatusWidgetProvider`** — referenced non-existent field `timestamp` on `BlockedCallEntity`; correct field is `blockedAt`
- **Fix: `AppPreferences` export/import methods** — were placed outside the class body (after the closing `}`), causing unresolved references to all `KEY_*` constants

### Configuration
- Added `widget/ToggleWidgetProvider.kt` and `widget/StatusWidgetProvider.kt`
- Added `res/layout/widget_toggle.xml` and `res/layout/widget_status.xml`
- Added `res/xml/toggle_widget_info.xml` and `res/xml/status_widget_info.xml`
- Added `BlockedCallDao.countBlockedSync()`, `getLastBlockedSync()`, `ContactDao.countSync()` for widget DB access
- Bump `versionCode` to 8, `versionName` to `1.8.0`

---

## [v1.7.0]

### Bug Fixes
- Fixed: SIM slot not shown in blocked call detail screen — now displays which SIM received the call
- Fixed: notification icon inconsistency — monochrome (negative) version is now used in both the status bar and expanded notification, eliminating launcher icon bleed-through
- Fixed: label "Ririleva SIM" renamed to "Rileva SIM" / "Aggiorna SIM" throughout the UI

### New Features

#### STIR/SHAKEN — Call attestation
- **Call attestation reading from `Call.Details`** — Android exposes the verification level directly via `getCallerNumberVerificationStatus()`, no external APIs required. Possible values: `VERIFICATION_STATUS_PASSED` (number verified by carrier), `VERIFICATION_STATUS_FAILED` (verification failed, likely spoofing), `VERIFICATION_STATUS_NOT_VERIFIED` (carrier does not support the standard)
- **Attestation-based blocking** — new optional rule in `CallBlockerService`: automatically block calls with `FAILED` attestation regardless of the whitelist. Configurable via toggle in Settings, per SIM
- **Attestation badge in call log** — colored icon next to each call: green (verified), red (failed), gray (not verified). The number detail screen includes a plain-language explanation for non-technical users
- **Prominent disclaimer in Settings** — informs the user that Italian carriers do not implement STIR/SHAKEN uniformly, so `NOT_VERIFIED` does not necessarily mean spam

#### Outgoing call auto-whitelist ("I called first")
- **Outgoing call logic** — if the user has called a number in the last X hours (configurable: 1 / 6 / 24 / 48), that number is automatically allowed even if it is not in the contacts or whitelist. Reads outgoing call log via `CallLog.Calls.OUTGOING_TYPE` with the `READ_CALL_LOG` permission already present in the Manifest
- **Settings configuration** — toggle "Allow calls from numbers I have dialed" with a slider for the time window. A dedicated badge is shown in the call log when a call is allowed for this reason

#### Scheduled blocking rules
- **Schedule screen in Settings** — two distinct modes: *Auto-enable blocking* (e.g. nights, weekends) and *Auto-disable blocking* (e.g. working hours, to avoid missing work calls)
- **Granular configuration** — day-of-week selector (M/T/W/T/F/S/S) + start and end time per rule. Multiple rules supported (e.g. "disable Mon–Fri 9:00–18:00" + "enable always Sat–Sun"). Each rule is configurable per SIM or for both
- **`AlarmManager` implementation** — uses `setExactAndAllowWhileIdle()` to schedule activation and deactivation. Rules are persisted in a dedicated Room table (`ScheduleRuleEntity`)
- **HomeScreen status indicator** — when blocking is active or inactive due to a scheduled rule, a label is shown below the main toggle (e.g. "Auto-disabled until 18:00") so the user understands why the state changed without touching anything
- **Manual override** — tapping the toggle while a scheduled rule is active shows a dialog: "Disable this rule only for today or permanently?"

### Configuration
- Added Room table `ScheduleRuleEntity` with dedicated non-destructive migration
- Bump `versionCode` to 7, `versionName` to `1.7.0`

---

## [v1.6.0]

### New Features
- **Advanced retry rule** — new option in Settings → Protection: if a blocked number calls X times (2–10, configurable) within Y minutes (5 / 10 / 30 / 60), the call is allowed through and the number is added to a temporary whitelist; a high-priority notification informs the user
- **Temporary whitelist** — contacts added automatically by the retry rule have an `expiresAt` timestamp; they are shown with a badge in the whitelist UI and removed automatically once expired
- **Boot receiver** — new option in Settings → Protection: "Reactivate on boot" re-enables call protection and clears any active suspension automatically after the device restarts (requires enabling autostart in MIUI / OEM battery settings)
- **Ko-fi donation button** — added in Settings → Info to support the project

### Bug Fixes
- Fixed: outgoing calls are no longer logged or blocked — `CallBlockerService` now checks `DIRECTION_OUTGOING` (API 29+) and exits early before any logging or blocking logic
- Fixed: Room database migration crash caused by `DEFAULT` values written into the SQLite catalogue by `ALTER TABLE ADD COLUMN`; migration 6→7 now recreates the `blocked_calls` table with the exact schema Room expects

### Improvements
- SIM slot is now shown correctly for each individual blocked attempt in the log

### Configuration
- Bumped `versionCode` to 6, `versionName` to `1.6.0`

---

## [v1.5.0]

### New Features
- **Rich blocked call log** — each number in the log is now tappable and opens a detail screen with full information
- **Call detail screen** — shows formatted number, country and operator (detected offline from prefix table, no network calls), number type (mobile / landline / VoIP / toll-free), total blocked attempts, and the date/time of each individual attempt
- **SIM per attempt** — each call attempt in the detail screen shows which SIM it arrived on (dual-SIM devices only)
- **Quick "Add to whitelist"** — button directly on the call detail screen to whitelist a number without navigating to the contacts screen
- **"Search online" button** — opens the browser with the number as a search query; no data is sent automatically, the user decides
- **Automatic log cleanup** — new option in Settings → Protection to automatically delete log entries older than 7, 30, or 90 days (default: never)
- **Suspension banner on Home** — when protection is suspended from Settings, the Home screen now shows a banner with the expiry date/time without requiring an app restart

### Bug Fixes
- Fixed Home screen toggle not reflecting suspension changes made in Settings without reopening the app
- Fixed number formatting and country/operator detection for numbers stored without leading `+` (pre-existing log entries)
- Fixed call detail showing 0 attempts for numbers stored without `+` prefix (query now normalises both sides)

### Improvements
- Settings tab selection is now remembered across sessions and app restarts (no longer resets to Protection tab on reopen)
- Responsive layout on Home screen: both portrait and landscape are fully scrollable and adapt correctly when a large system font size is set
- Landscape mode is now a two-column layout instead of a non-scrollable single column

### Configuration
- Bumped Room database to version 3 with non-destructive migration (adds `simSlot` column to `blocked_calls`)
- Bumped `versionCode` to 5, `versionName` to `1.5.0`

---

## [v1.4.0]

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
- Bumped `versionCode` to 4, `versionName` to `1.4.0`

---

## [v1.3.0]

### New Features
- **Automatic backup with WorkManager** — periodic JSON export every 1, 7, or 30 days (configurable), saved to a user-chosen folder
- **"Last export" label** — the Backup tab now shows the date and time of the last successful automatic backup
- **Silent backup notification** — a low-priority notification is sent when the automatic backup completes, showing the number of contacts exported
- **Update check via GitHub Releases** — opt-in toggle in the Info tab; tapping "Check now" calls the GitHub API, compares the tag with the current version, and shows a dialog with a download link if a newer release is available
- **Dynamic version display** — the Info tab now reads the version from `BuildConfig.VERSION_NAME` instead of a static string, so it always matches the actual build

### Configuration
- Added `INTERNET` permission to `AndroidManifest.xml` (used only for the opt-in update check)
- Added `androidx.work:work-runtime-ktx` dependency for WorkManager
- Enabled `buildConfig = true` in `buildFeatures` (required for `BuildConfig.VERSION_NAME`)
- Bumped `versionCode` to 3, `versionName` to `1.3.0`

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
