# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Compile (fast check, no APK)
./gradlew :app:compileDebugKotlin

# Unit tests (JVM, no device needed)
./gradlew test

# Single test class
./gradlew test --tests "io.github.ntufar.babyonboard.ui.viewmodel.TripViewModelTest"

# Instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint

# Debug APK
./gradlew assembleDebug
```

All CI checks (`lint` → `test` → `assembleDebug`) run on push via `.github/workflows/ci.yml`.

## Architecture

Clean Architecture with four layers; dependencies flow inward:

```
ui/  →  domain/  ←  data/
         ↑
       sensing/
```

- **`domain/`** — pure Kotlin: `Trip`, `TelemetryEvent`, `Contact`, `Settings` models; `TripRepository` interface; `StartTripUseCase`, `EvaluateCrashUseCase`, `RaiseSosUseCase`.
- **`data/`** — Room + SQLCipher: entities in `data/model/Entities.kt`, DAOs in `data/db/Daos.kt`, `TripRepositoryImpl` maps domain ↔ entity.
- **`sensing/`** — `TripForegroundService` (foreground location service) fuses GPS + accelerometer + gyroscope through `TelemetryEngine`. Emits local broadcasts (`ACTION_SPEED_UPDATE`, `ACTION_EVENT_DETECTED`, `ACTION_CRASH_DETECTED`) that `TripViewModel` and `MainActivity` consume.
- **`ui/`** — Jetpack Compose screens + two `@HiltViewModel`s: `TripViewModel` (trip lifecycle, score, events) and `SettingsViewModel` (contacts, settings).

Navigation is a single `NavHost` in `MainActivity` with routes: `onboarding → live_trip → trip_summary/{tripId} → trip_history → settings`, plus `sos` navigated to on crash broadcast.

## Key Design Decisions

**Score formula** (implemented in both `TelemetryEngine` and `TripViewModel`): `score = (100 − harshEventsPer100km × 10).coerceIn(0, 100)`. Harsh = `severity > 0.5f`. The same formula exists in two places intentionally — `TelemetryEngine` is the authoritative real-time engine; `TripViewModel.recalculateScore()` recomputes from the events list received via broadcast.

**Baby Mode thresholds** are stricter (e.g. BRAKE at −2.5 vs −3.5 m/s²) and are applied inside `TelemetryEngine.detectEvents()`. The `babyMode` flag travels from `OnboardingScreen` → `TripViewModel.startTrip()` → `TripForegroundService` extras → `TelemetryEngine`.

**SQLCipher key management**: AES-256-GCM passphrase generated once, encrypted with Android Keystore (alias `babyonboard_db_key`), stored in `SharedPreferences`. Key setup is in `AppModule.kt`.

**Sensor data flow**: `LocationSource` + `MotionSource` emit `RawSensorData` Flows that `TripForegroundService` merges. The service rotates accelerometer data to world frame via `SensorManager.getRotationMatrixFromVector`, then passes `TelemetryFrame`s to `TelemetryEngine`.

## Testing Conventions

Unit tests use JUnit 4 + MockK + Google Truth. Room tests use an in-memory database. `TripViewModelTest` sets `viewModel.startTimerOnTripStart = false` to avoid background coroutine timers in tests. Robolectric (4.11.1) is available for Android API tests without a device.

There is no mock for the database in integration-style tests — `TripRepositoryImplTest` uses a real in-memory Room instance.

## Feature Tracking

`docs/features.md` tracks FR status with `[x]` done, `[-]` partial, `[ ]` open. Update it when implementing features.

## After Implementing a Feature or Major Change

When a feature is complete or a significant change is made, do all of the following before considering the task done:

1. **Write tests** — unit tests covering the new behaviour; follow the conventions in the Testing Conventions section above.
2. **Update `docs/features.md`** — mark the relevant FR(s) `[x]` and update the implementation note (file/class reference).
3. **Update `README.md`** — reflect any new capabilities in the Key Features list, Tech Stack table, or Roadmap checklist as appropriate.
4. **Update `CHANGELOG.md`** — add an entry under the current version (or a new version block) describing what was Added, Changed, or Fixed.
5. **Update the web page** — keep `web/` in sync with any user-facing feature additions or version bumps.
