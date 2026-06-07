# Feature Implementation Status

Tracking implementation progress against the SRS functional requirements (FRs).

Legend: [x] Done | [-] Partial | [ ] Not started

---

## 4.1 Driving Telemetry Engine

- [x] FR-1 Continuous sensor sampling & metrics — `TelemetryEngine` + `LocationSource`/`MotionSource`
- [x] FR-2 Sensor fusion for harsh events — Motion + GPS corroboration in `TelemetryEngine.detectEvents()`
- [x] FR-3 World-frame processing — `TripForegroundService.rotateToWorldFrame()` with rotation matrix
- [x] FR-4 Store events with timestamp, type, severity, location, confidence — `EventEntity` + `TelemetryEvent` model
- [x] FR-5 Per-trip smoothness score (0–100) — `TelemetryEngine.calculateScore()`
- [x] FR-6 Extended metrics (roughness, gradient, swerve, distraction) — `TelemetryEngine` ROUGH/SWERVE detection, gradient calc, `DistractionSource`
- [ ] FR-7 Speed-limit comparison — Requires external data source; deferred
- [x] FR-8 On-device computation — No raw sensor data leaves the device

## 4.2 Baby on Board Mode

- [x] FR-9 Toggle Baby mode — `OnboardingScreen` toggle, `Trip.babyMode` field
- [x] FR-10 Stricter thresholds in Baby mode — `TelemetryEngine` constructor param adjusts thresholds
- [ ] FR-11 Auto-suggest Baby mode — Deferred (requires geofence/BT learning)
- [x] FR-12 Copy: "Baby mode on", never "baby detected" — Consistent in UI copy

## 4.3 Feedback & History

- [x] FR-13 Calm, non-punitive post-trip feedback — `TripSummaryScreen` shows score + events
- [-] FR-14 Trip history with trend — `TripHistoryScreen` exists (basic list); trend/graph missing
- [ ] FR-15 Gamification — Deferred to v1.1

## 4.4 Distraction Reduction

- [-] FR-16 In-trip DND — `SettingsScreen` has DND toggle; service integration pending
- [ ] FR-17 Phone handling measurement — Deferred

## 4.5 Crash Detection & SOS

- [x] FR-18 Best-effort crash detection — `EvaluateCrashUseCase` (speed + accel heuristics)
- [x] FR-19 Cancellable countdown — `SosScreen` with 60s countdown, progress bar, cancel button
- [x] FR-20 Contact alert + dial 112 — `RaiseSosUseCase.dialEmergencyNumber()` + `SosScreen` triggers `ACTION_DIAL`
- [-] FR-21 SOS screen with coordinates — `SosScreen` exists; coordinates display not yet implemented
- [x] FR-22 Low-confidence suppression — `CrashAssessment.confidence` > 0.5f check in `RaiseSosUseCase`

## 4.6 Back-Seat Reminder

- [x] FR-23 End-of-trip back-seat reminder — `TripViewModel.showBackSeatReminder()` posts notification
- [ ] FR-24 Escalating reminder — Settings model has `reminderEscalation`; no UI or escalation logic yet

## 4.7 Safe-Arrival Sharing

- [ ] FR-25 Arrival notification to contact — Deferred to v1.1
- [ ] FR-26 Opt-in, revocable sharing — Deferred to v1.1

## 4.8 Onboarding, Consent, Settings, Data

- [x] FR-27 Onboarding with honest limits — `OnboardingScreen` with limits card
- [x] FR-28 Settings (mode triggers, contacts, DND, etc.) — Full settings: auto-start, DND, units, retention, escalation, contacts
- [-] FR-29 Local-first, optional cloud — Local-first done; cloud not implemented
- [ ] FR-30 Export & delete data — Not yet implemented

---

## Non-Functional Requirements

- [-] NFR-1 Privacy (on-device, GDPR) — On-device processing; consent flow pending
- [x] NFR-2 Safety messaging — Honest limits in onboarding and spec
- [-] NFR-3 Telemetry accuracy — Thresholds are starting points; calibration needed
- [ ] NFR-4 Battery budget — Adaptive sampling not yet implemented
- [x] NFR-5 Reliability (foreground service) — `TripForegroundService` with notification
- [-] NFR-6 Performance — No UI jank; crash latency TBD
- [ ] NFR-7 Accessibility — Deferred
- [ ] NFR-8 Localization — EN only; EL deferred
- [-] NFR-9 Security (encrypted DB) — SQLCipher configured in `AppModule`; hardcoded passphrase, no Keystore
- [-] NFR-10 Compliance — Permissions declared; Play Data Safety form pending

---

## Implementation Order (Current Sprint) — All Complete

1. [x] World-frame rotation transform (`TripForegroundService.rotateToWorldFrame()`)
2. [x] SQLCipher for Room DB encryption (`AppModule`)
3. [x] SOS countdown UI (`SosScreen`)
4. [x] Trip history screen (`TripHistoryScreen`)
5. [x] Back-seat reminder notification (`TripViewModel.showBackSeatReminder()`)

---

## Proposed Improvements

Prioritized by impact vs. effort.

### Critical Fixes (security + correctness)

- [ ] #1 Replace hardcoded DB passphrase with Android Keystore (`AppModule.kt:26`)
- [ ] #2 Wire up DistractionSource — call `onScreenOn()/onScreenOff()` in `TripForegroundService`
- [ ] #3 Implement data retention cleanup — schedule coroutine to delete trips older than `retentionDays`
- [ ] #4 Add CALL_PHONE permission + auto-dial in SOS flow; display coordinates on SosScreen
- [ ] #5 TripSummaryScreen: fix unit conversion (show km not m for metric, consistent decimal places)

### Medium Impact / Moderate Effort

- [ ] #6 Data export (JSON/CSV) — one button in Settings to serialize trips+events to shareable file
- [ ] #7 Trip history trends graph — speed profile, score over time, or weekly score trend
- [ ] #8 In-trip Do Not Disturb — wire `NotificationManager.setInterruptionFilter()` on trip start
- [ ] #9 Back-seat reminder with escalation — incrementing notification urgency from `reminderEscalationLevel`
- [ ] #10 Cancel trip confirmation dialog — allow back-navigation from LiveTripScreen without ending trip

### High Value / Larger Features (v1.1+)

- [ ] #11 Gamification / streaks — safest-driver streaks, "smooth trip" badges, weekly goals
- [ ] #12 Speed-limit comparison — overlay OSM/Mapbox speed-limit data on current speed
- [ ] #13 Adaptive sensor sampling — reduce sampling rate when smooth, increase during harsh events
- [ ] #14 Auto-suggest Baby Mode — heuristic from time-of-day, trip frequency, past harsh events
- [ ] #15 Accessibility pass — content descriptions, minimum touch targets, TalkBack testing

### Testing & Quality

- [ ] #16 SettingsViewModel tests
- [ ] #17 TripForegroundService tests
- [ ] #18 Add migration strategy to AppDatabase before next schema change

### Architecture (debt/quality)

- [ ] #19 Wire up MetricSample writing — entity and DAO exist but nothing inserts samples
- [ ] #20 Enable ProGuard/R8 for release (`minifyEnabled false` today)
