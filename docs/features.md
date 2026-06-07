# Feature Implementation Status

Tracking implementation progress against the SRS functional requirements (FRs).

Legend: ✅ Done | 🟡 Partial | 🔴 Not started | ❌ Blocked

---

## 4.1 Driving Telemetry Engine

| FR | Description | Status | Notes |
|----|-------------|--------|-------|
| FR-1 | Continuous sensor sampling & metrics | ✅ | `TelemetryEngine` + `LocationSource`/`MotionSource` |
| FR-2 | Sensor fusion for harsh events | ✅ | Motion + GPS corroboration in `TelemetryEngine.detectEvents()` |
| FR-3 | World-frame processing | ✅ | `TripForegroundService.rotateToWorldFrame()` with rotation matrix |
| FR-4 | Store events with timestamp, type, severity, location, confidence | ✅ | `EventEntity` + `TelemetryEvent` model |
| FR-5 | Per-trip smoothness score (0–100) | ✅ | `TelemetryEngine.calculateScore()` |
| FR-6 | Extended metrics (roughness, gradient, swerve, distraction) | ✅ | `TelemetryEngine` ROUGH/SWERVE detection, gradient calc, `DistractionSource` |
| FR-7 | Speed-limit comparison | 🔴 | Requires external data source; deferred |
| FR-8 | On-device computation | ✅ | No raw sensor data leaves the device |

## 4.2 Baby on Board Mode

| FR | Description | Status | Notes |
|----|-------------|--------|-------|
| FR-9 | Toggle Baby mode | ✅ | `OnboardingScreen` toggle, `Trip.babyMode` field |
| FR-10 | Stricter thresholds in Baby mode | ✅ | `TelemetryEngine` constructor param adjusts thresholds |
| FR-11 | Auto-suggest Baby mode | 🔴 | Deferred (requires geofence/BT learning) |
| FR-12 | Copy: "Baby mode on", never "baby detected" | ✅ | Consistent in UI copy |

## 4.3 Feedback & History

| FR | Description | Status | Notes |
|----|-------------|--------|-------|
| FR-13 | Calm, non-punitive post-trip feedback | ✅ | `TripSummaryScreen` shows score + events |
| FR-14 | Trip history with trend | 🟡 | `TripHistoryScreen` exists (basic list); trend/graph missing |
| FR-15 | Gamification | 🔴 | Deferred to v1.1 |

## 4.4 Distraction Reduction

| FR | Description | Status | Notes |
|----|-------------|--------|-------|
| FR-16 | In-trip DND | 🟡 | `SettingsScreen` has DND toggle; service integration pending |
| FR-17 | Phone handling measurement | 🔴 | Deferred |

## 4.5 Crash Detection & SOS

| FR | Description | Status | Notes |
|----|-------------|--------|-------|
| FR-18 | Best-effort crash detection | ✅ | `EvaluateCrashUseCase` (speed + accel heuristics) |
| FR-19 | Cancellable countdown | ✅ | `SosScreen` with 60s countdown, progress bar, cancel button |
| FR-20 | Contact alert + dial 112 | ✅ | `RaiseSosUseCase.dialEmergencyNumber()` + `SosScreen` triggers `ACTION_DIAL` |
| FR-21 | SOS screen with coordinates | 🟡 | `SosScreen` exists; coordinates display not yet implemented |
| FR-22 | Low-confidence suppression | ✅ | `CrashAssessment.confidence` > 0.5f check in `RaiseSosUseCase` |

## 4.6 Back-Seat Reminder

| FR | Description | Status | Notes |
|----|-------------|--------|-------|
| FR-23 | End-of-trip back-seat reminder | ✅ | `TripViewModel.showBackSeatReminder()` posts notification |
| FR-24 | Escalating reminder | 🔴 | Settings model has `reminderEscalation`; no UI or escalation logic yet |

## 4.7 Safe-Arrival Sharing

| FR | Description | Status | Notes |
|----|-------------|--------|-------|
| FR-25 | Arrival notification to contact | 🔴 | Deferred to v1.1 |
| FR-26 | Opt-in, revocable sharing | 🔴 | Deferred to v1.1 |

## 4.8 Onboarding, Consent, Settings, Data

| FR | Description | Status | Notes |
|----|-------------|--------|-------|
| FR-27 | Onboarding with honest limits | ✅ | `OnboardingScreen` with limits card |
| FR-28 | Settings (mode triggers, contacts, DND, etc.) | ✅ | Full settings: auto-start, DND, units, retention, escalation, contacts |
| FR-29 | Local-first, optional cloud | 🟡 | Local-first done; cloud not implemented |
| FR-30 | Export & delete data | 🔴 | Not yet implemented |

---

## Non-Functional Requirements

| NFR | Description | Status | Notes |
|-----|-------------|--------|-------|
| NFR-1 | Privacy (on-device, GDPR) | 🟡 | On-device processing; consent flow pending |
| NFR-2 | Safety messaging | ✅ | Honest limits in onboarding and spec |
| NFR-3 | Telemetry accuracy | 🟡 | Thresholds are starting points; calibration needed |
| NFR-4 | Battery budget | 🔴 | Adaptive sampling not yet implemented |
| NFR-5 | Reliability (foreground service) | ✅ | `TripForegroundService` with notification |
| NFR-6 | Performance | 🟡 | No UI jank; crash latency TBD |
| NFR-7 | Accessibility | 🔴 | Deferred |
| NFR-8 | Localization | 🔴 | EN only; EL deferred |
| NFR-9 | Security (encrypted DB) | 🟡 | SQLCipher configured in `AppModule`; hardcoded passphrase, no Keystore |
| NFR-10 | Compliance | 🟡 | Permissions declared; Play Data Safety form pending |

---

### Implementation Order (Current Sprint) ✅ All Complete

1. ✅ World-frame rotation transform (`TripForegroundService.rotateToWorldFrame()`)
2. ✅ SQLCipher for Room DB encryption (`AppModule`)
3. ✅ SOS countdown UI (`SosScreen`)
4. ✅ Trip history screen (`TripHistoryScreen`)
5. ✅ Back-seat reminder notification (`TripViewModel.showBackSeatReminder()`)
