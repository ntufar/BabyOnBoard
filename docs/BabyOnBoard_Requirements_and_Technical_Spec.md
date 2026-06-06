# Baby on Board — Requirements & Technical Specification

**Product:** Baby on Board — Safe-Driving Telemetry for Families
**Platform:** Android (phone only; no CAN bus, OBD, or external/in-cabin sensors)
**Document type:** Software Requirements Specification (Part A) + Technical Specification (Part B)
**Version:** 0.2 (draft)
**Date:** June 2026

---

## Honest scope statement (read first)

Baby on Board uses **only the sensors already in the phone** (GNSS/GPS, accelerometer, gyroscope, rotation-vector, barometer where present, microphone, Bluetooth, connectivity). Its **core feature is driving telemetry**: measuring how the car is actually driven — speed, hard braking, hard acceleration, cornering, smoothness/jerk, and a wider set of derived metrics — and turning that into calm, useful feedback for a caregiver carrying a child. This is something a phone genuinely does well.

Three limits are stated up front and honoured throughout:

1. **The name is a brand, not a sensor claim.** "Baby on Board" references the familiar car sign and the *use context* the parent sets. The app does **not** detect a child; Baby mode is user-activated. UX copy must never imply the phone senses a baby.
2. **The hot-car feature is a reminder, not protection.** It reminds the caregiver to check the back seat; it cannot detect a child left behind.
3. **Crash response is an SOS, not eCall.** The app cannot transmit the regulated eCall data set to emergency services. It does best-effort crash *detection* then a user-cancellable SOS (alert contacts + dial 112).

The telemetry engine is the part the phone can do well and is therefore the product's centre of gravity. Detection-style claims are deliberately avoided.

---

# PART A — Requirements (SRS)

## 1. Purpose & Goals

Baby on Board measures real driving behaviour from phone sensors and gives parents/caregivers a clear, non-judgemental picture of how smooth and safe each drive with their child was — while quietly providing distraction reduction, a best-effort crash SOS, and an end-of-trip back-seat reminder. The strategic intent is to validate demand for a family safe-driving product on software alone, before any hardware investment.

**Primary success metrics (MVP):**
- **Telemetry quality:** harsh-event detection precision/recall against labelled drives; speed accuracy vs. reference.
- **Activation:** % of installs completing onboarding and recording ≥1 scored trip.
- **Retention:** 30- and 90-day retention (the core hypothesis — families keep it).
- **Behaviour change:** harsh-event rate per 100 km trend over first 8 weeks.
- **Willingness-to-pay:** opt-in rate to a premium tier.

## 2. Personas

- **Primary — New parent (Maria, 31):** drives a 6-month-old to nursery; wants reassurance and gentle feedback on her smoothness, not scolding.
- **Secondary — Co-caregiver / grandparent:** occasional driver of the child; less tech-savvy (accessibility matters).
- **Tertiary — Safety-conscious partner:** wants an "arrived safely" notification and to see the smoothness trend.

## 3. Use cases (high level)

- UC1: Start a trip (manual/auto) → the **telemetry engine records the drive**.
- UC2: Drive with phone notifications suppressed; minimal, non-distracting cues.
- UC3: End trip → calm smoothness summary (speed profile, harsh events, jerk) + back-seat reminder.
- UC4: Review trip history and a safe-driving trend over time.
- UC5: Probable crash → SOS countdown → contacts alerted + 112 dial unless cancelled.
- UC6: Share an "arrived safely" notification with a nominated contact.
- UC7: Manage consent, data, contacts, privacy.

## 4. Functional Requirements

MoSCoW priority: **(M)** must, **(S)** should, **(C)** could.

### 4.1 Driving telemetry engine — THE CORE FEATURE
- **FR-1 (M):** The app shall continuously sample phone sensors during a trip and compute the metrics defined in **Appendix A (Telemetry & Derived Metrics)** — at minimum: speed (instant/avg/max), hard braking, hard acceleration, hard cornering, and smoothness/jerk.
- **FR-2 (M):** All harsh-event detection shall use **sensor fusion** — a motion-sensor signature must be corroborated by a GPS signal (e.g., hard brake = longitudinal deceleration spike *and* speed drop) before being logged as an event.
- **FR-3 (M):** Motion data shall be processed in the **vehicle/world frame**, not the raw phone frame, so detection is independent of how the phone is held or mounted (Appendix A, B-§3).
- **FR-4 (M):** Each detected event shall be stored with timestamp, type, severity, location, and a **confidence value**.
- **FR-5 (M):** The app shall compute a per-trip **smoothness/safe-driving score (0–100)** from the metric set, normalising harsh events per 100 km so trip length does not bias the score.
- **FR-6 (S):** The app shall compute the extended metrics where the hardware supports them — road roughness, gradient/hills, yaw-based swerve detection, night/risky-hour driving, and a phone-distraction measure (Appendix A).
- **FR-7 (S):** Where reliable speed-limit data is available, the app shall flag speeding; absent that data (MVP default), it shall not display any speed-limit comparison and shall score on absolute-speed bands and smoothness only.
- **FR-8 (M):** Telemetry computation shall run **on-device**; no raw sensor stream leaves the phone.

### 4.2 Baby on Board mode (context wrapper)
- **FR-9 (M):** The user shall toggle Baby mode; state is clearly visible during a trip.
- **FR-10 (M):** In Baby mode, telemetry thresholds shall be **stricter/gentler** (lower harsh-event thresholds, heavier jerk weighting) because smoothness matters most for an infant.
- **FR-11 (S):** Baby mode may be auto-suggested by a learned trigger (designated car Bluetooth, time-of-day, or home↔nursery geofence) — always confirmable, never silently assumed.
- **FR-12 (M):** Copy shall read "Baby mode on" (user-set), never "baby detected".

### 4.3 Feedback & history
- **FR-13 (M):** Post-trip feedback shall be **calm and non-punitive** (speed profile, event list, smoothness score, trend) — never real-time scolding while a child is aboard. Any in-trip cue shall be ambient/audio-minimal.
- **FR-14 (M):** The app shall keep a trip history with per-trip detail and an over-time safe-driving trend.
- **FR-15 (C):** Gentle gamification (streaks, "12 smooth trips") to support retention.

### 4.4 Distraction reduction
- **FR-16 (M):** During a trip the app shall offer to enable Do Not Disturb (requires Notification Policy access), suppressing non-essential notifications.
- **FR-17 (S):** The app shall measure phone handling/use while moving (screen-on, unlocks, foreground app, active calls) and surface it in the trip summary — not as a distracting real-time alert.

### 4.5 Crash detection & SOS
- **FR-18 (M):** The app shall run best-effort crash detection (deceleration + speed-collapse fusion; B-§5), described to the user as best-effort with known error modes.
- **FR-19 (M):** On a probable crash, a loud, cancellable countdown (default 60 s) shall precede any outbound action.
- **FR-20 (M):** If not cancelled, the app shall alert nominated contacts (location + "Baby on Board" flag) and prompt one-tap dial to **112** (auto-dial only if pre-authorised).
- **FR-21 (M):** The SOS screen shall show coordinates + plain-language location for the user to read aloud.
- **FR-22 (S):** Low-confidence events (e.g., a dropped stationary phone) shall be suppressed.

### 4.6 Back-seat reminder (hot-car backstop)
- **FR-23 (M):** At trip end in Baby mode, a prominent "Check the back seat" reminder shall fire (notification + optional alarm), positioned explicitly as a backstop, not a guarantee.
- **FR-24 (S):** The reminder shall escalate until acknowledged (configurable).

### 4.7 Safe-arrival sharing
- **FR-25 (S):** The user may have an "arrived safely" message sent to a nominated contact on reaching a chosen destination (geofence).
- **FR-26 (M):** Sharing is opt-in per recipient, disclosed to the recipient, and revocable. No covert location sharing.

### 4.8 Onboarding, consent, settings, data
- **FR-27 (M):** Onboarding shall explain the honest limits before requesting permissions; permissions requested contextually/incrementally.
- **FR-28 (M):** Settings shall control mode triggers, contacts, DND, reminder behaviour, retention, units, emergency number, and account/export/delete.
- **FR-29 (M):** The app shall work **local-first** (no account) for the MVP core; optional cloud backup is explicit opt-in, encrypted, exportable.
- **FR-30 (M):** The user shall be able to export and permanently delete all data in-app (GDPR).

## 5. Non-Functional Requirements

- **NFR-1 Privacy (M):** On-device processing by default; nothing transmitted without consent. GDPR-compliant (lawful basis, minimisation, purpose limitation, retention, DSAR/erasure); in-app data dashboard.
- **NFR-2 Safety messaging (M):** No feature presented as a guarantee of child safety; backstop/best-effort framing mandatory in UX, store listing, policy. No marketing on tragedy statistics.
- **NFR-3 Telemetry accuracy (M):** Metrics carry confidence; thresholds calibrated against labelled drives; the app asserts no certainty it lacks.
- **NFR-4 Battery (M):** Bounded, documented budget (target ≤6–8%/hr active driving) via adaptive sampling and halting collection when stationary.
- **NFR-5 Reliability (M):** Monitoring survives backgrounding/screen-off via foreground service; recovers after process death.
- **NFR-6 Performance (M):** No UI jank; crash decision latency < 2 s after the event window.
- **NFR-7 Accessibility (S):** WCAG-aligned; large targets; TalkBack; minimal in-trip interaction.
- **NFR-8 Localization (S):** EN + EL at launch; EU-language ready; locale-correct emergency number (112 default).
- **NFR-9 Security (M):** Encrypted local DB (SQLCipher/Jetpack Security); TLS for any sync; OWASP MASVS.
- **NFR-10 Compliance (M):** Meet Google Play Location/Foreground-Service/sensitive-permission policies; truthful Data Safety form.

## 6. Assumptions, Constraints, Dependencies

- **Assumption:** Phone orientation is arbitrary (pocket/holder/mount) → pipeline must be orientation-independent.
- **Constraint:** Phone-only sensors; no vehicle data; no occupancy/child-presence detection.
- **Constraint:** Background location is restricted and Play-reviewed (B-§7); design minimises reliance on it.
- **Dependency (optional):** Speed-limit/road data (HERE/TomTom/OSM) only if FR-7 speed-limit comparison is enabled; omitted in MVP.
- **Dependency:** Google Play Services (FusedLocationProvider, Activity Recognition).
- **Hardware variance:** Not all phones have a gyroscope or barometer; the app must degrade gracefully.

---

# PART B — Technical Specification

## 1. Architecture overview

Native Android, Clean architecture / MVVM. The **sensing/telemetry pipeline is the heart of the system**:

- **UI:** Jetpack Compose + ViewModels (StateFlow).
- **Domain:** use-cases (StartTrip, ProcessSensorWindow, DetectEvent, ScoreTrip, EvaluateCrash, RaiseSos) — pure Kotlin, unit-testable.
- **Data:** Room (encrypted), DataStore, repositories.
- **Sensing:** `TripForegroundService` orchestrating `LocationSource`, `MotionSource`, `ActivityRecognitionSource`, `BluetoothCarSource`, feeding a `TelemetryEngine` (frame-transform → filters → detectors → scorer) and a `TripStateMachine`.
- **DI:** Hilt. **Async:** Coroutines/Flow. **Deferred work:** WorkManager.

Native Kotlin chosen over cross-platform because reliable foreground-service + high-rate sensor + permissions is the riskiest part; Android-only scope makes native the low-risk choice.

## 2. Technology stack

| Concern | Choice |
|---|---|
| Language / UI | Kotlin / Jetpack Compose (Material 3) |
| Architecture / DI | MVVM + Clean / Hilt |
| Async | Coroutines + Flow |
| Persistence | Room + SQLCipher; DataStore |
| Location | FusedLocationProviderClient |
| Motion | SensorManager (linear accel, gyroscope, rotation vector, barometer) |
| Activity | ActivityRecognition / ActivityTransition |
| Geofencing | GeofencingClient |
| Background | Foreground Service (type `location`) + WorkManager |
| Maps (optional) | Maps SDK; speed-limit via HERE/TomTom/OSM |
| min/target SDK | minSdk 26; targetSdk = latest required by Play (34+) |

## 3. Telemetry pipeline (core)

1. **Acquire:** GPS at adaptive 1 Hz (active) with accuracy/speed-accuracy; motion sensors at ~50 Hz (no `HIGH_SAMPLING_RATE_SENSORS` needed — that's only >200 Hz).
2. **Frame transform:** use `TYPE_ROTATION_VECTOR`/gravity to rotate phone-frame acceleration into the world/vehicle frame; or estimate the driving axis via PCA over a rolling window. **This step is mandatory** — without it, longitudinal/lateral separation is meaningless.
3. **Filter:** low-pass to remove vibration noise; separate longitudinal, lateral, vertical components; compute jerk = d(accel)/dt.
4. **Detect:** threshold + windowed detectors per metric (Appendix A), each requiring GPS corroboration where applicable.
5. **Score:** per-trip composite, harsh-events normalised per 100 km; stricter weights in Baby mode.
6. **Persist:** events + trip aggregates to encrypted Room.

**Speed truth:** always GPS Doppler `location.speed`. **Never integrate the accelerometer for speed** (drifts within seconds); accelerometer is for event detection only.

## 4. Trip state machine

```
IDLE ─(manual start | IN_VEHICLE | car BT connect)─► ACTIVE
ACTIVE ─(telemetry sampling + detectors)─► ACTIVE
ACTIVE ─(probable crash)─► CRASH_REVIEW ─(timeout)─► SOS / ─(cancel)─► ACTIVE
ACTIVE ─(manual stop | STILL > N min | BT disconnect)─► ENDING
ENDING ─(finalise score, persist)─► POST_TRIP (summary + back-seat reminder) ─► IDLE
```
State persisted to survive process death.

## 5. Crash-detection algorithm (best-effort)

**Inputs:** world-frame `|a|`, GPS speed `v`, speed history.
**Heuristic (tunable, calibrate in beta):**
1. Pre-condition `v_pre` ≥ ~25 km/h.
2. Impact: peak `|a|` ≥ ~4 g within a short window.
3. Post: speed collapses to ~0 and stays low ≥ ~10 s.
4. Confidence from how strongly all three are met.
**Trigger →** loud cancellable countdown (60 s) → SOS on timeout.
**Limits (disclosed):** false positives (drops, potholes, sport braking), false negatives (low-speed/oblique). System-level detection uses richer fusion; this is explicitly best-effort. Countdown + low-confidence suppression keep false alarms tolerable.

## 6. Scoring model

- Event severity from longitudinal/lateral magnitude beyond thresholds (separate Normal vs Baby thresholds).
- Trip score = f(harsh-event rate/km, peak severities, jerk, speed variance), normalised 0–100.
- Baby mode penalises jerk more heavily.
- Speeding contributes only if reliable limit data is present; otherwise excluded with a visible note.
- Rule-based for MVP; optional on-device TFLite refinement later.

## 7. Background execution & permissions (current Android constraints)

- **Android 14+ (API 34+):** every foreground service must declare a `foregroundServiceType` and request the matching `FOREGROUND_SERVICE_*` permission, or the system throws `SecurityException`. Trip monitoring uses type **`location`** → `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_LOCATION` (normal perms) **plus** runtime `ACCESS_FINE_LOCATION`.
- A location FGS **cannot start from the background** without `ACCESS_BACKGROUND_LOCATION`. To minimise reliance (and Play-review friction), prefer **user-initiated** trip start or start the FGS as a continuation of a user-visible Activity-Recognition / car-BT event. Request background location only if background auto-start is required, with a clear multi-step rationale (foreground first, then justify background).
- **Google Play** reviews background/FGS location against a "minimum scope" test — request foreground over background wherever possible; complete Play Console FGS + location declarations truthfully.
- **Android 15+:** `dataSync` FGS capped ~6 h and can't launch from `BOOT_COMPLETED` — do **not** model trip monitoring as `dataSync`; `location` FGS may run with a persistent notification. If using `SYSTEM_ALERT_WINDOW` for the crash overlay, Android 15 needs a currently-visible overlay to start an FGS from background → prefer a full-screen-intent notification.
- **Other perms:** `ACTIVITY_RECOGNITION` (auto trip detect), `BLUETOOTH_CONNECT` (car trigger), `POST_NOTIFICATIONS` (13+), `ACCESS_NOTIFICATION_POLICY` (DND), optional `CALL_PHONE`/`SEND_SMS` for SOS (prefer `ACTION_DIAL` to avoid `CALL_PHONE`). Avoid `RECORD_AUDIO` in MVP.
- A persistent, honest FGS notification ("Baby on Board is monitoring this trip") runs whenever monitoring is active.

### Permissions matrix (MVP)
| Permission | Type | Why | When requested |
|---|---|---|---|
| ACCESS_FINE_LOCATION | runtime | speed/route/crash location | trip onboarding |
| FOREGROUND_SERVICE / _LOCATION | normal | run trip monitor | manifest |
| ACCESS_BACKGROUND_LOCATION | runtime | only if background auto-start | separate, justified step |
| ACTIVITY_RECOGNITION | runtime | auto trip detect | when enabling auto-start |
| BLUETOOTH_CONNECT | runtime | car association trigger | when setting BT trigger |
| POST_NOTIFICATIONS | runtime | reminders, SOS, status | onboarding |
| ACCESS_NOTIFICATION_POLICY | special | in-trip DND | when enabling DND |
| CALL_PHONE *(optional)* | runtime | one-tap 112 | SOS setup; prefer ACTION_DIAL |
| SEND_SMS *(optional)* | runtime | auto-alert contacts | SOS setup |

## 8. Data model (Room, simplified)

- **Trip**(id, startTs, endTs, distanceM, durationS, avgSpeed, maxSpeed, score, babyMode, routeRef?)
- **Event**(id, tripId FK, ts, type[BRAKE|ACCEL|CORNER|SWERVE|ROUGH|SPEED|PHONE_USE|CRASH], severity, value, lat, lng, confidence)
- **MetricSample**(optional, downsampled) (tripId FK, ts, speed, longAccel, latAccel, vertAccel, yawRate, altitude)
- **Contact**(id, name, phone, role[EMERGENCY|ARRIVAL], consentTs)
- **Geofence**(id, label, lat, lng, radius, purpose[ARRIVAL|MODE_TRIGGER])
- **Settings**(autoStart, btTriggerDeviceId?, dndInTrip, reminderEscalation, retentionDays, units, emergencyNumber)

Encrypted at rest; raw high-rate samples kept only transiently / downsampled; retention auto-purge.

## 9. Notifications, SOS UX, DND

- Ongoing FGS status notification during trips.
- Back-seat reminder: high-priority + optional alarm; escalates per settings.
- Crash flow: full-screen intent over lock screen, large CANCEL, alarm, countdown; on timeout → SOS screen with coordinates + dial 112 + contact alert.
- DND via `setInterruptionFilter` after Notification Policy access; restore at trip end.

## 10. Privacy & security

Local-first; no network in MVP core. Optional sync opt-in, TLS, encrypted. In-app data dashboard (view/export JSON-CSV/delete-all). GDPR consent (granular), documented lawful basis, DPIA (location + child-safety). Truthful Play Data Safety form. No covert tracking; arrival sharing consensual and disclosed.

## 11. Testing strategy

- **Unit:** detectors/scoring against recorded sensor traces (golden braking/cornering/crash datasets).
- **Sensor-replay harness:** record real drives (timestamped sensor+GPS CSV), replay through `TelemetryEngine` for deterministic regression — the key tool for tuning thresholds safely.
- **Instrumented:** permission flows, FGS lifecycle, process-death recovery, DND restore.
- **Field beta:** real parents; measure detection precision/recall, crash false-positive rate, battery, retention.
- **Edge cases:** phone drop, tunnel/GPS loss, mounted vs pocket, passenger/bus rides (must not misfire).

## 12. Roadmap

- **MVP:** full telemetry engine (speed, brake/accel/corner, jerk, smoothness score), Baby mode thresholds, trip history/trend, distraction DND, crash detection + SOS, back-seat reminder, local-first, EN/EL. No speed-limit data, no cloud, no ML.
- **v1.1:** extended metrics (roughness, gradient, swerve, distraction index), arrival sharing, gamification, optional encrypted cloud backup, speed-limit scoring via data partner.
- **v1.2:** on-device ML scoring (TFLite), insurer/family-plan exploration, iOS port if validated.
- **Later (hardware bridge):** optional cabin sensor for true child-presence detection — reconnects to the broader device business case.

## 13. Open questions / risks

- Harsh-event detection accuracy across phone models/mounts — calibrate in beta.
- Crash false-positive rate before any auto-dial default.
- `CALL_PHONE`/`SEND_SMS` (sensitive) vs `ACTION_DIAL` + tap.
- Background auto-start vs Play-review cost — MVP may be user-initiated only.
- Battery at 1 Hz GPS + 50 Hz motion — validate adaptive sampling.
- Driver-vs-passenger ambiguity — affects any future insurer use; disclose openly.
- Liability framing of SOS and reminder — legal review of disclaimers.

---

# APPENDIX A — Telemetry & Derived Metrics

Implementation reference for the sensing layer. Thresholds are **starting points to calibrate against labelled drives**, not final values. "Fusion" means the listed sensors must agree before an event is logged. All metrics computed on-device.

### Longitudinal (speed & smoothness)
| Metric | Source sensor(s) | Detection method | Threshold / algorithm (tunable) | Confidence / notes |
|---|---|---|---|---|
| Speed (instant/avg/max) | GNSS | Read `location.speed` (Doppler); reject low-accuracy fixes | Use fixes with good `speedAccuracy`; ignore < ~3 km/h | High above walking pace |
| Speed consistency | GNSS | Rolling std-dev of speed | Derived; lower = smoother | Med-high |
| Hard braking | Accelerometer (long.) + GNSS | World-frame longitudinal decel spike **and** corroborating speed drop | decel ≥ ~3.5–4.5 m/s² (Baby: ~2.5–3.0) | Med-high; needs frame transform |
| Hard acceleration | Accelerometer (long.) + GNSS | Longitudinal accel spike + speed rise | accel ≥ ~3.0–3.5 m/s² (Baby: ~2.0–2.5) | Med-high |
| Jerk (smoothness) | Accelerometer | d(accel)/dt over window | |jerk| beyond comfort band | Med; **best infant-comfort metric**, heavily weighted in Baby mode |
| Idling / stop count | GNSS (+ accel) | Speed ≈ 0 sustained | speed < ~2 km/h for > ~10 s | Med |

### Lateral & rotational (handling)
| Metric | Source sensor(s) | Detection method | Threshold / algorithm | Confidence / notes |
|---|---|---|---|---|
| Hard cornering | Accelerometer (lat.) + Gyroscope | Lateral accel beyond threshold, confirmed by yaw rate | lat accel ≥ ~3.5–4.0 m/s² (Baby lower) + yaw present | Med |
| Yaw rate / turn detection | Gyroscope (z) | Angular velocity threshold/integration | Turn vs harsh-turn bands | High (turn); Med (harsh) |
| Swerve / sharp lane change | Gyroscope + accelerometer | Paired opposite yaw + lateral signature within short window | Pattern match | Low-Med (harder; tune carefully) |

### Road & environment
| Metric | Source sensor(s) | Detection method | Threshold / algorithm | Confidence / notes |
|---|---|---|---|---|
| Road roughness / potholes | Accelerometer (vert.) | Vertical-axis variance / peak count | Variance beyond baseline | Med; relevant to ride quality |
| Road gradient / hills | Barometer + GNSS altitude | Altitude delta over distance | % grade | Med; **barometer not on all phones** → degrade gracefully |
| Heading / heading-change rate | GNSS bearing | Bearing delta over time | Derived | High |

### Contextual & behavioural
| Metric | Source sensor(s) | Detection method | Threshold / algorithm | Confidence / notes |
|---|---|---|---|---|
| Trip distance / duration / route | GNSS + clock | Accumulate accepted fixes (Haversine) | Derived | High |
| Trip frequency / time-of-day | Clock + trip log | Aggregate | Derived | High |
| Night / risky-hour driving | System clock (+ light sensor) | Time-window classification | Local sunset/sunrise or fixed band | High |
| Phone distraction | Screen state, unlocks, foreground app, call state, touch events | Detect interaction while moving | Any screen-on/handling during ACTIVE trip | High; **one of the most predictive risk signals** |
| Harsh-event rate per 100 km | Derived | Normalise event counts by distance | Derived | High; the fair scoring base |

### Efficiency (rough estimates only)
| Metric | Source sensor(s) | Detection method | Threshold / algorithm | Confidence / notes |
|---|---|---|---|---|
| Eco / driving-style score | Accelerometer + GNSS | Composite of accel/decel smoothness + speed bands | Heuristic | Med; directional |
| Approx fuel / CO₂ | GNSS + style model | Distance × style-adjusted model | Estimate | Low; **no engine data** — clearly label as estimate |

### Safety
| Metric | Source sensor(s) | Detection method | Threshold / algorithm | Confidence / notes |
|---|---|---|---|---|
| Crash detection | Accelerometer + GNSS | Impact peak + speed collapse fusion (B-§5) | `v_pre` ≥ 25 km/h, peak ≥ ~4 g, speed→0 ≥ 10 s | Best-effort; disclose error modes |
| Speed-limit compliance | GNSS + **external map data** | Compare speed to limit | Requires speed-limit data source | N/A in MVP; omit comparison if data absent |

### Not obtainable from a phone (state honestly; needs OBD/CAN or cabin sensors)
Engine RPM, real fuel level/consumption, throttle position, brake-pedal pressure, steering angle, seatbelt status, true odometer, tyre pressure, and **vehicle occupancy / child presence**. These define the boundary between this app and the broader hardware device concept.

### Cross-cutting implementation notes
- **Orientation transform is mandatory** before any longitudinal/lateral logic (B-§3).
- **GPS is the speed source of truth**; accelerometer is for events only — never integrate accel for speed.
- **Require sensor agreement** (motion + GPS) to log harsh events; this is the main false-positive control.
- **Driver vs passenger** cannot be reliably determined; acceptable for family use, a validity caveat for any insurer use.
- **Graceful degradation:** check sensor availability at runtime (gyroscope/barometer may be absent) and disable dependent metrics cleanly.
- **GPS gaps** (tunnels/urban canyons): handle with short interpolation/hold; do not fabricate events during signal loss.

---

*This document specifies an Android-only, phone-sensor-only MVP whose core is a driving-telemetry engine. Claims are bounded to what the hardware supports: the name is a brand (not detection), Baby mode is user-set, the back-seat feature is a reminder, and crash response is a best-effort SOS (not eCall). Platform/permission details reflect current Google Play and Android (API 34+/15) requirements and should be re-verified against the latest Android developer documentation at implementation time.*
