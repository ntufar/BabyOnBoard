# Changelog

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
