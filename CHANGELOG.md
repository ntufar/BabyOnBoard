# Changelog

## [0.0.22] - 2026-06-10

### Fixed
- **"Waiting for sensor data" stays indefinitely indoors**: `MotionSource.emitFused()` previously required both accelerometer *and* gyroscope data before emitting. On devices without a gyroscope (or where gyro is slow to warm up), `sensorFlow` never emitted and the Live Telemetry card stayed on the "Waiting for sensor data…" placeholder forever. Gyro is now optional — the flow emits on every accelerometer frame with `yawRate = 0.0` when no gyro data is available. Also added null-guards in `MotionSource.start()` so registering absent sensors doesn't cause an NPE.

## [0.0.21] - 2026-06-10

### Fixed
- **Unit test failure**: `TripViewModelTest` mocks now stub `getSettings()` so tests pass after `startTrip` was updated to read sensitivity from settings.

## [0.0.20] - 2026-06-10

### Changed
- No functional changes; version bump to align release track.

## [0.0.19] - 2026-06-10

### Added
- **Sensitivity mode setting**: New `SensitivityMode` enum (Car / Bus / Train / Walking) in Settings controls both the minimum-speed gate and the acceleration thresholds used by `TelemetryEngine`. Walking mode has no speed floor, Car mode keeps the original 5 m/s guard. Stored in the `settings` table via Room migration 1→2.

## [0.0.18] - 2026-06-10

### Fixed
- **Live telemetry graphs not visible without driving**: The "Live Telemetry" card is now always shown during a trip; it displays a "Waiting for sensor data…" placeholder until data arrives instead of being hidden entirely.
- **No events or graph data when walking**: Removed the `speed > 5.0 m/s` guard from all event detectors (brake, accel, corner, rough road, swerve) so the telemetry pipeline responds to accelerometer input regardless of GPS speed.

## [0.0.17] - 2026-06-10

### Fixed
- **No data recorded without GPS fix**: `TripForegroundService` previously used `combine()` on location and motion flows, which required both to emit before any data was processed. Motion events are now processed immediately on every sensor frame using the last known location as context; if no GPS fix has arrived yet, event detection still runs with coordinates defaulting to 0,0. This fixes recording on trains and in environments with poor GPS coverage.
- **GPS-only location provider**: `LocationSource` now uses `FusedLocationProviderClient` instead of raw `GPS_PROVIDER`, enabling location from GPS, Wi-Fi, and cell towers so the pipeline receives position data in tunnels, stations, and other GPS-challenged environments.

### Added
- **Live telemetry graphs**: `LiveTripScreen` now shows a rolling sparkline chart of speed (km/h) and longitudinal acceleration (m/s²) while a trip is in progress, rendered via Compose Canvas with a zero-reference line on the acceleration chart.

## [0.0.16] - 2026-06-09

### Added
- **Distraction tracking receiver**: Registered a dynamic `BroadcastReceiver` in `TripForegroundService` that listens for screen on/off events (`ACTION_SCREEN_ON`/`ACTION_SCREEN_OFF`) and forwards them to `DistractionSource` to accurately track phone use events.
- **Service Unit Tests**: Added `TripForegroundServiceTest` containing comprehensive unit tests for screen receiver registration, unregistration, and event handling.

### Fixed
- **TripSummaryScreen unit conversion**: Updated the post-trip summary to display distance in kilometers (`km`) instead of meters (`m`) when using metric units, and formatted both metric and imperial (miles) distance values to a consistent 2 decimal places.
- **MainActivity settings propagation**: Pass the active unit setting (`units`) to `TripSummaryScreen` so display units are dynamically collected and rendered correctly.

## [0.0.15] - 2026-06-09

### Fixed
- **SQLiteNotADatabaseException for passphrase mismatch**: Force Room to open the database immediately and catch SQLiteNotADatabaseException to delete database files and rebuild when the key is out of sync or file is corrupt.

## [0.0.14] - 2026-06-08

### Fixed
- **Pre-encryption database cleanup**: Detect plain SQLite database files (from before SQLCipher was introduced) and delete them before opening, ensuring Room creates a fresh encrypted database instead of failing.

## [0.0.13] - 2026-06-08

### Fixed
- **SQLCipher native library registration**: Replaced missing loadLibs() with System.loadLibrary("sqlcipher") to ensure the native library is registered before database first open.

## [0.0.12] - 2026-06-08

### Fixed
- **16 KB page size compatibility** — upgraded SQLCipher from `net.zetetic:android-database-sqlcipher:4.5.4` to `net.zetetic:sqlcipher-android:4.9.0`; the new artifact ships binaries compiled with `p_align=0x4000` (16 KB), eliminating the "LOAD segment not aligned" warning on Android 15+ devices; removed the now-unnecessary `patchelf` post-processing step from both the Gradle build and CI release workflow

## [0.0.11] - 2026-06-08

### Fixed
- **SQLCipher dependency** — downgraded from 4.6.1 to 4.5.4, the latest version available on Maven Central; 4.6.1 is only published to repo.zetetic.net which is unreachable from CI

## [0.0.10] - 2026-06-08

### Fixed
- **16 KB memory page support** — updated SQLCipher from 4.5.0 to 4.5.4 and Play Services Location from 21.1.0 to 21.3.0; fixed Gradle alignment task to run inside `afterEvaluate` so AGP tasks are registered before lookup (the original `configureEach` approach silently skipped patchelf on every build)

## [0.0.9] - 2026-06-08

### Added
- **Score breakdown card** — `TripSummaryScreen` now shows a per-event-type deduction breakdown (e.g. "3× hard brake −30 pts") so drivers can see exactly what cost them points; computed by `computeScoreBreakdown()` as a proportional split of total deduction across harsh event types; 8 unit tests added in `TripSummaryScreenTest`
- **Prominent location disclosure** — `OnboardingScreen` now shows a "Data We Collect & Why" card before the permission dialog explaining what GPS/sensor data is collected, that it stays on-device, and is never shared; user must tick an acknowledgement checkbox before the "Grant Permissions" button activates; addresses Google Play Prominent Disclosure and Consent Requirement

## [0.0.8] - 2026-06-07

### Added
- **MetricSample writing** — every sensor frame is now persisted to the `metric_samples` table; `TripRepository` exposes `saveMetricSample()`, `TripRepositoryImpl` maps the domain model to `MetricSampleEntity` with a generated UUID, and `TripForegroundService` calls it on each telemetry frame via an injected `TripRepository` (service annotated `@AndroidEntryPoint`)

### Fixed
- `TripRepositoryImpl` constructor arity mismatch — `AppModule` was passing a stale 5-argument call after an earlier refactor removed `metricSampleDao`; constructor and provider are now in sync

### Changed
- DB passphrase bytes zeroed immediately after `SupportFactory` consumes them, closing the plaintext heap window
- AndroidKeystore AES key upgraded from default 128-bit to 256-bit

## [0.0.7] - 2026-06-07

### Fixed
- **16 KB memory page alignment** — `libsqlcipher.so` now properly aligned for Android 15+ compatibility; alignment now runs after `stripReleaseDebugSymbols` (which previously undid the patchelf changes)

### Changed
- Release workflow installs `patchelf` before building
- `alignNativeLibs` depends on strip task instead of merge task, so alignment survives stripping
- Build fails on missing patchelf for release variants

## [0.0.6] - 2026-06-07

### Fixed
- **16 KB memory page alignment** — `patchelf` installed in CI release workflow; build fails on missing patchelf for release builds

## [0.0.5] - 2026-06-07

### Added
- **Store listing assets** — launcher icons for all densities (mdpi–xxxhdpi), 512×512 Play Store icon, 1024×500 feature graphic
- **Play Store description** — plain text full description compliant with Google Play metadata policy
- **Google Play badge** — added to README and web page, linking to store listing
- **Website badge** — added to README badge row, linking to GitHub Pages

### Changed
- Updated web page version to v0.0.5
- Fixed screenshot filename typo (screensot → screenshot)
- Updated targetSdk badge from 34 to 35 in README

## [0.0.4] - 2026-06-07

### Added
- **Extended driving metrics** — road roughness detection (vertical accel variance), swerve detection (opposite yaw pattern), gradient calculation (altitude delta/distance), phone distraction tracking (screen-on while moving)
- `TelemetryEngine.detectExtendedEvents()` — emits `ROUGH` and `SWERVE` events
- `TelemetryEngine.calculateGradient()` — computes % grade from GPS altitude
- `DistractionSource` — tracks screen-on state and emits `PHONE_USE` events when device is interacted with while driving
- **16 KB page size alignment** — `patchelf` Gradle task aligns `libsqlcipher.so` LOAD segments for Android 15+ compatibility
- **App version display** — version label (`v0.0.4`) shown in Settings screen footer
- **End Trip navigation fix** — final score/duration computed synchronously so `trip_summary` renders correct data immediately
- **Trip summary trip ID guard** — `loadEventsForTrip` no longer overwrites `_currentTrip` when it already matches the target trip

### Changed
- `TelemetryEngine.kt` — added rolling windows for vertical accel (roughness) and yaw/lat accel (swerve), `resetWindows()`, `haversineM()` companion
- `Sources.kt` — added `DistractionSource` class
- `TripForegroundService.kt` — wires extended events, distraction source, gradient calculation into sensor pipeline
- `TripViewModel.kt` — `endTrip()` computes final state synchronously; `loadEventsForTrip()` preserves existing trip if ID matches
- `SettingsScreen.kt` — added version label at bottom
- `app/build.gradle` — added `patchelf` alignment task for 16 KB page size, version bumped to 0.0.4

## [0.0.3] - 2026-06-07

### Added
- **Full Settings screen** — auto-start trip toggle, units selector (km/mi), data retention period (7–90 days), reminder escalation level (1–3 alerts)
- **Emergency Contacts management** — add/delete contacts with name, phone, and role (emergency/arrival) via dialog; persisted in Room DB
- `ContactDao.deleteContact()` — remove contacts by ID
- `SettingsViewModel` — exposes `contacts` StateFlow, `addContact()`, `deleteContact()`

### Changed
- `SettingsScreen.kt` — complete rewrite: scrollable `LazyColumn` with grouped sections (Trip & Recording, Safety & Alerts, Data Management, Emergency Contacts)
- `TripRepository` — added `deleteContact()` interface method
- `TripRepositoryImpl` — wired `deleteContact()` to `ContactDao`

## [0.0.1] - 2026-06-07

### Added
- **Trip History screen** — browse past trips from Room DB with score badges, distance, duration, and max speed
- **Live Trip recording screen** — real-time display of elapsed time, current speed, safe-driving score, distance, max speed, and harsh event count
- **Trip Summary screen** — post-trip overview with color-coded score, stats cards, event log with severity badges
- **Settings screen** — Do Not Disturb toggle and emergency number configuration, persisted to Room DB via SettingsViewModel
- **Onboarding flow** — welcome screen with Honest Limits card, Baby Mode toggle, and runtime permission requests (location, activity recognition, notifications) via `rememberLauncherForActivityResult`
- **Compose Navigation** — proper `NavHost`-based routing between all screens
- **Foreground service integration** — `TripForegroundService` starts/stops with trip lifecycle, broadcasts real-time sensor data (speed, harsh events) back to the ViewModel
- **Real-time scoring** — score is calculated dynamically based on harsh event frequency per 100 km
- **UUID-based trip IDs** — `StartTripUseCase` now generates `UUID.randomUUID()` instead of hardcoded `"temp_id"`, and persists immediately to Room
- **Baby Mode** — stricter event detection thresholds (2.5g brake/accel, 3.0g corner) when enabled; shown as badge on LiveTrip and Summary screens
- **Auto-mirrored icons** — uses `Icons.AutoMirrored.Filled.ArrowBack` and `Icons.AutoMirrored.Filled.List` for proper RTL support
- **Test suite** — 40 unit tests across ViewModel, TelemetryEngine, use cases, domain models, and sensor emulation
- **GitHub Actions CI** — `ci.yml` runs lint + unit tests + debug APK on push/PR; `release.yml` builds signed release APK on tags
- **GitHub Pages deployment** — `deploy-web.yml` auto-deploys landing page on push to master

### Changed
- `MainActivity.kt` — replaced `mutableStateOf("onboarding")` string-based screen switching with `NavHost` composable
- `TripViewModel.kt` — complete rewrite: manages trip lifecycle, timer, live speed/event state, registers `BroadcastReceiver` for foreground service data, calculates scores; receiver registration moved out of `init` for testability
- `TripForegroundService.kt` — accepts `babyMode` and `tripId` via intent extras; broadcasts speed updates and detected events instead of dropping them
- `OnboardingScreen.kt` — added real permission requests with grant-status display
- `SettingsScreen.kt` — now backed by `SettingsViewModel` with Room persistence
- `TripSummaryScreen.kt` — redesigned with Material 3 cards, score color-coding, and navigation buttons
- `app/build.gradle` — version bumped to `0.0.1`
- Gradle wrapper upgraded to 8.13

### Fixed
- Removed all hardcoded dummy trip and event data from `MainActivity`
- Deprecated `Icons.Default.ArrowBack`/`List` replaced with auto-mirrored variants
- `IntentFilter.addAction()` and `startForegroundService()` wrapped in try-catch for test environment compatibility
