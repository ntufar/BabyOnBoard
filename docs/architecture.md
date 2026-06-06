# Architecture

## Overview

Native Android, Clean Architecture + MVVM. The sensing/telemetry pipeline is the heart of the system.

```
┌─────────────────────────────────────────────────────────┐
│                        UI Layer                         │
│  Compose Screens ───► ViewModels ───► StateFlow         │
│  (Onboarding, TripSummary, Settings)                    │
└─────────────────────┬───────────────────────────────────┘
                      │ calls
┌─────────────────────▼───────────────────────────────────┐
│                     Domain Layer                         │
│  Use Cases ───► Domain Models ───► Repository Interface │
│  (StartTripUseCase, EvaluateCrashUseCase, etc.)         │
└─────────────────────┬───────────────────────────────────┘
                      │ implements
┌─────────────────────▼───────────────────────────────────┐
│                      Data Layer                          │
│  RepositoryImpl ───► Room DAOs ───► Entities            │
│  (TripRepositoryImpl, AppDatabase)                      │
└─────────────────────┬───────────────────────────────────┘
                      │ feeds
┌─────────────────────▼───────────────────────────────────┐
│                    Sensing Layer                         │
│  TripForegroundService                                  │
│    ├── LocationSource (GPS)                             │
│    ├── MotionSource (Accelerometer + Gyroscope)         │
│    ├── TelemetryEngine                                  │
│    │    ├── processRawData() ───► TelemetryFrame        │
│    │    ├── detectEvents() ───► List<TelemetryEvent>    │
│    │    └── calculateScore() ───► Int                   │
│    └── sensor fusion via flow combine                   │
└─────────────────────────────────────────────────────────┘
```

## Layer Responsibilities

### UI (`com.example.babyonboard.ui`)
- **Screens:** Jetpack Compose (Material 3)
- **ViewModels:** `@HiltViewModel`, expose `StateFlow` to screens
- **Theme:** Material 3 dark/light color schemes

### Domain (`com.example.babyonboard.domain`)
- **Models:** `Trip`, `Event`, `TelemetryEvent`, `Settings`, `Contact`, etc.
- **Use cases:** Business logic (pure Kotlin, no Android dependencies)
- **Repository interface:** Abstraction for data layer

### Data (`com.example.babyonboard.data`)
- **Room database:** `AppDatabase` with 6 entities
- **DAOs:** `TripDao`, `EventDao`, `ContactDao`, `SettingsDao`
- **Repository impl:** Maps between domain models and Room entities

### Sensing (`com.example.babyonboard.sensing`)
- **TripForegroundService:** Android foreground service orchestrating all sensors
- **LocationSource:** GPS location updates via `LocationManager`
- **MotionSource:** Accelerometer + gyroscope via `SensorManager`
- **TelemetryEngine:** Core signal processing — frame transform, event detection, scoring

## Telemetry Pipeline

```
RawSensorData (GPS + accel + gyro)
    │
    ▼
TelemetryEngine.processRawData()
    │  └── compute jerk = d(accel)/dt
    ▼
TelemetryFrame (aligned sensor frame)
    │
    ▼
TelemetryEngine.detectEvents()
    │  └── threshold: BRAKE, ACCEL, CORNER
    ▼
List<TelemetryEvent>
    │
    ▼
TelemetryEngine.calculateScore()
    │  └── harsh events per 100 km → 0–100 score
    ▼
TripScore
```

## Crash Detection

```
EvaluateCrashUseCase
  Input: speedHistory, accelHistory
  1. v_pre ≥ 25 km/h
  2. peak |accel| ≥ 4g
  3. speed collapsed to < 5 km/h for ≥ 10s
  Output: CrashAssessment (isCrashDetected, confidence)
```

## Dependency Injection (Hilt)

```
SingletonComponent
  ├── AppDatabase (Room)
  │   ├── TripDao
  │   ├── EventDao
  │   ├── ContactDao
  │   └── SettingsDao
  └── TripRepository
      └── TripRepositoryImpl
```

ViewModels use `@HiltViewModel` with constructor injection.

## Data Flow Example — Trip Recording

```
1. User taps "Get Started" → OnboardingScreen
2. MainActivity navigates to summary (demo flow)
3. TripForegroundService starts:
   a. Creates notification channel + foreground notification
   b. Starts LocationSource (GPS at 1Hz)
   c. Starts MotionSource (accel + gyro at SENSOR_DELAY_GAME)
   d. Combines location + motion flows
   e. Each combined frame → TelemetryEngine.processRawData()
   f. → TelemetryEngine.detectEvents()
   g. Events persisted via Room
4. Trip ends → score calculated → summary displayed
```

## Key Technical Decisions

| Decision | Rationale |
|----------|-----------|
| Native Android over cross-platform | Foreground service + high-rate sensors + permissions are the riskiest part; Android-only makes native the low-risk choice |
| Room + DataStore | Room for structured trip/event data; DataStore for preferences |
| Hilt over manual DI | Standard for Android; reduces boilerplate |
| Coroutines + Flow | First-class async on Android; StateFlow for UI state |
| Foreground Service type `location` | Required for background GPS; must persist notification |
| Separate domain model from entities | Clean Architecture — data layer can change without affecting domain |
