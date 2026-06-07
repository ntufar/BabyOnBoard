# Changelog

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
