# Feature Implementation Status

Tracking implementation progress against the SRS functional requirements (FRs).

Legend: ✅ Done | 🟡 Partial | 🔴 Not started | ❌ Blocked

---

## 4.1 Driving Telemetry Engine

| FR | Description | Status | Notes |
|----|-------------|--------|-------|
| FR-1 | Continuous sensor sampling & metrics | ✅ | `TelemetryEngine` + `LocationSource`/`MotionSource` |
| FR-2 | Sensor fusion for harsh events | ✅ | Motion + GPS corroboration in `TelemetryEngine.detectEvents()` |
| FR-3 | World-frame processing | 🟡 | Rotation vector available but frame transform is placeholder |
| FR-4 | Store events with timestamp, type, severity, location, confidence | ✅ | `EventEntity` + `TelemetryEvent` model |
| FR-5 | Per-trip smoothness score (0–100) | ✅ | `TelemetryEngine.calculateScore()` |
| FR-6 | Extended metrics (roughness, gradient, swerve, distraction) | 🔴 | Not in MVP scope |
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
| FR-14 | Trip history with trend | 🔴 | `TripDao` supports queries; no history screen yet |
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
| FR-19 | Cancellable countdown | 🔴 | SOS UI not yet implemented |
| FR-20 | Contact alert + dial 112 | 🟡 | `RaiseSosUseCase.dialEmergencyNumber()` implemented |
| FR-21 | SOS screen with coordinates | 🔴 | Not yet implemented |
| FR-22 | Low-confidence suppression | 🟡 | `CrashAssessment.confidence` tracked; suppression logic placeholder |

## 4.6 Back-Seat Reminder

| FR | Description | Status | Notes |
|----|-------------|--------|-------|
| FR-23 | End-of-trip back-seat reminder | 🔴 | Not yet implemented |
| FR-24 | Escalating reminder | 🔴 | Settings model has `reminderEscalation`; no UI yet |

## 4.7 Safe-Arrival Sharing

| FR | Description | Status | Notes |
|----|-------------|--------|-------|
| FR-25 | Arrival notification to contact | 🔴 | Deferred to v1.1 |
| FR-26 | Opt-in, revocable sharing | 🔴 | Deferred to v1.1 |

## 4.8 Onboarding, Consent, Settings, Data

| FR | Description | Status | Notes |
|----|-------------|--------|-------|
| FR-27 | Onboarding with honest limits | ✅ | `OnboardingScreen` with limits card |
| FR-28 | Settings (mode triggers, contacts, DND, etc.) | 🟡 | `SettingsScreen` partial; contacts/retention missing |
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
| NFR-9 | Security (encrypted DB) | 🔴 | Room DB created; SQLCipher not yet configured |
| NFR-10 | Compliance | 🟡 | Permissions declared; Play Data Safety form pending |

---

### Implementation Order (Current Sprint)

1. Complete world-frame rotation transform (`TelemetryEngine`)
2. Add SQLCipher for Room DB encryption
3. Implement SOS countdown UI
4. Build trip history screen
5. Back-seat reminder notification
