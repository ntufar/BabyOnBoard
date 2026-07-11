# Improvement Ideas & New Features — Technical Specifications

> **Round 2:** a second pass with newly found bugs and additional features (sections 9–14) lives in [improvement-ideas-v2.md](improvement-ideas-v2.md).

Extensive, code-grounded backlog of improvements and new features. Each item states the motivation, a concrete technical design against the current codebase, affected files, and a priority/effort estimate. Cross-references `features.md` item numbers (`#n`) where one exists.

Priorities: **P0** correctness/safety of the core product · **P1** high user value · **P2** valuable, not urgent · **P3** exploratory / v1.2+.
Effort: **S** ≤ 1 day · **M** 1–3 days · **L** ≥ 1 week.

## Quick index

| # | Idea | Priority | Effort |
|---|------|----------|--------|
| 1.1 | Vehicle-frame projection (heading-aligned long/lat axes) | P0 | M |
| 1.2 | Event debouncing / episode state machine | P0 | M |
| 1.3 | Low-pass filtering before detection | P0 | S |
| 1.4 | Computed severity & confidence (not hardcoded) | P0 | S |
| 1.5 | GPS fix quality gating | P0 | S |
| 1.6 | Use sensor-event timestamps, not wall clock | P0 | S |
| 1.7 | Remove accelerometer speed integration; GPS-gap policy | P0 | S |
| 1.8 | GPS staleness detection | P0 | S |
| 1.9 | Weighted, jerk-aware scoring model v2 | P1 | M |
| 1.10 | Per-vehicle baseline learning (auto-calibration) | P2 | L |
| 2.1 | Full trip state machine with persistence | P0 | L |
| 2.2 | Auto trip start/stop (Activity Recognition + car Bluetooth) | P1 | L |
| 2.3 | Orphaned-trip recovery (#21) | P0 | S |
| 2.4 | Partial WakeLock (#22) | P0 | S |
| 2.5 | "Stop Trip" notification action (#23) | P1 | S |
| 2.6 | Persist events from the service, not the UI | P0 | M |
| 3.1 | Adaptive sensor sampling (#13) | P1 | M |
| 3.2 | Batched MetricSample writes | P1 | S |
| 3.3 | Low-battery in-trip warning (#24) | P2 | S |
| 4.1 | Scope service broadcasts to the app package | P0 | S |
| 4.2 | Replace broadcasts with an injected event bus | P1 | M |
| 4.3 | Data retention auto-purge (#3) | P0 | S |
| 4.4 | Data export JSON/CSV + delete-all (#6, FR-30) | P1 | M |
| 4.5 | Consent & privacy dashboard (NFR-1) | P1 | M |
| 5.1 | Time-based crash evaluation window | P0 | M |
| 5.2 | SOS coordinates + plain-language location (#4, FR-21) | P1 | S |
| 5.3 | SMS contact alerting with consent | P1 | M |
| 5.4 | Crash full-screen intent hardening | P1 | S |
| 6.1 | In-trip Do Not Disturb (#8, FR-16) | P1 | S |
| 6.2 | Back-seat reminder escalation (#9, FR-24) | P1 | S |
| 6.3 | Route map on trip summary (#27) | P1 | M |
| 6.4 | Speed profile chart per trip (#7) | P2 | S |
| 6.5 | Weekly summary notification (#28) | P2 | S |
| 6.6 | Contextual post-trip tips (#34) | P2 | S |
| 6.7 | Gamification: streaks & badges (#11, FR-15) | P2 | M |
| 6.8 | Accessibility pass (#15, NFR-7) | P1 | M |
| 6.9 | Localization EN/EL (NFR-8) | P1 | M |
| 7.1 | Arrival sharing via geofence (FR-25/26) | P2 | L |
| 7.2 | Baby-mode auto-suggest (FR-11, #14/#33) | P2 | M |
| 7.3 | Speed-limit comparison via OSM (FR-7, #12) | P3 | L |
| 7.4 | Baby Comfort Index | P2 | M |
| 7.5 | Driving profiles (#29) | P3 | M |
| 7.6 | Per-route roughness comparison (#31) | P3 | M |
| 7.7 | Cabin noise comfort metric (#30) | P3 | M |
| 7.8 | On-device ML scoring (TFLite) | P3 | L |
| 7.9 | E2E-encrypted cloud backup (FR-29) | P3 | L |
| 7.10 | Home-screen widget (Glance) | P3 | S |
| 7.11 | Wear OS companion | P3 | L |
| 8.1 | Sensor-replay harness (B-§11) | P0 | L |
| 8.2 | Detector & scoring unit tests on golden traces | P0 | M |
| 8.3 | Room migration strategy (#18) | P1 | S |
| 8.4 | Service/ViewModel instrumented tests (#16/#17) | P1 | M |
| 8.5 | R8/ProGuard for release (#20) | P1 | S |
| 8.6 | In-app debug telemetry console | P2 | M |

## 1. Telemetry engine — correctness & signal processing

These are the highest-leverage items: the current engine detects events from raw, unfiltered, per-frame data, which affects every score the user ever sees.

### 1.1 Vehicle-frame projection (P0, M)

**Problem.** `TripForegroundService.rotateToWorldFrame()` rotates device-frame acceleration into the **world frame (East/North/Up)**, then treats `world[0]` as longitudinal and `world[1]` as lateral. Those axes are east/north — they only coincide with the car's axes when driving due east. A hard brake heading north shows up entirely in "latAccel", so BRAKE events are missed and CORNER events fabricated depending on compass heading. FR-3 ("vehicle frame") is only half-implemented.

**Design.**
1. Keep the existing rotation to ENU (correct — it removes device orientation).
2. Obtain vehicle heading `θ` from GPS: `location.bearing` when `hasBearing()` and speed ≥ ~2 m/s (bearing is noise when slow). Propagate through `RawSensorData` (add `bearingDeg: Double?`).
3. Project horizontal world acceleration onto the heading: `a_long = a_east·sin θ + a_north·cos θ`, `a_lat = a_east·cos θ − a_north·sin θ` (θ from north, clockwise). `a_vert = world[2]` unchanged.
4. Between GPS fixes, dead-reckon heading with the gyro (`θ += yawRate·dt`), reset on each good fix.
5. Fallback when bearing is unavailable > 5 s: PCA-over-rolling-window driving-axis estimation, or mark frames `frameConfidence = LOW` and suppress long/lat detection (roughness stays valid).

**Touch points.** `TripForegroundService` (move the transform into the engine), `TelemetryEngine` (new pure-Kotlin `VehicleFrameTransformer`), `RawSensorData`/`TelemetryFrame`.

**Acceptance.** A replayed hard-brake trace (see 8.1) yields the same BRAKE detection at 8 compass headings, phone flat/pocket/mounted.

### 1.2 Event debouncing / episode state machine (P0, M)

**Problem.** `TelemetryEngine.detectEvents()` emits an event for **every frame** over threshold. At `SENSOR_DELAY_GAME` (~50 Hz), one 2-second hard brake produces ~100 BRAKE events, each stored and each deducting score — trip scores collapse after a single genuine event.

**Design.** Convert each detector into a state machine producing one event per *episode*:

```
IDLE ──(|a| ≥ T_start for ≥ 300 ms)──► ACTIVE (accumulate peak, duration)
ACTIVE ──(|a| < T_end = 0.7·T_start for ≥ 500 ms)──► emit single event → IDLE
```

- Hysteresis (`T_end < T_start`) prevents flapping at the threshold.
- The emitted event carries `value = peak`, `durationMs`, and severity per 1.4.
- Minimum inter-event gap per type (e.g. 3 s) as a final guard.
- Applies to BRAKE/ACCEL/CORNER; ROUGH needs a cooldown (currently re-fires every frame while variance stays high); SWERVE needs the same (`yawWindow` keeps matching the same pair for up to 10 frames).

**Data model.** Add `durationMs: Long` to `TelemetryEvent`/`EventEntity` (Room migration, see 8.3).

### 1.3 Low-pass filtering before detection (P0, S)

**Problem.** Raw linear-acceleration samples go straight into threshold checks. Engine/road vibration and phone rattle in a cradle spike above 3.5 m/s² for a frame or two. Spec B-§3 mandates filtering; nothing filters today.

**Design.** Per-axis single-pole IIR low-pass (EMA), cutoff ~2 Hz, for event detection: `y[n] = α·x[n] + (1−α)·y[n−1]`, `α = dt/(RC+dt)`, `RC = 1/(2π·f_c)`. Keep an *unfiltered* vertical channel for the roughness detector (vibration is its signal). Compute jerk from the filtered longitudinal channel. Expose cutoff as a constructor param so 8.1 replay can tune it.

### 1.4 Computed severity & confidence (P0, S)

**Problem.** Severity is hardcoded (`0.8f` brake/accel, `0.7f` corner) and confidence likewise, so FR-4's fields carry no information and scoring can't distinguish a firm stop from an emergency stop.

**Design.**
- `severity = ((peak − threshold)/(severeLevel − threshold)).coerceIn(0f, 1f)` with per-type `severeLevel` (e.g. brake severe at 7 m/s²; Baby mode lowers both anchors).
- `confidence` from corroboration: GPS present & fresh (+0.3), speed change consistent with the accel integral over the episode (+0.4), frame confidence from 1.1 (+0.3). Events below 0.5 are stored but excluded from the score and flagged in the UI.
- Speed corroboration (FR-2) becomes real: for a BRAKE episode require the speed drop to be consistent with `∫a dt`; today speed is only a minimum gate.

### 1.5 GPS fix quality gating (P0, S)

**Problem.** `LocationSource` forwards every fix; `location.accuracy`, `speedAccuracyMetersPerSecond`, and `hasSpeed()` are ignored. Urban-canyon multipath produces teleporting fixes → phantom distance and bogus speed corroboration.

**Design.** In `onLocationResult`: drop fixes with `accuracy > 30 m`; use `location.speed` only when `hasSpeed()`; propagate `speedAccuracyMetersPerSecond` into `RawSensorData.speedAccuracy`; distance accumulation skips segments where either endpoint was low-quality or implied speed > 60 m/s (jump rejection).

### 1.6 Sensor-event timestamps (P0, S)

**Problem.** `MotionSource` stamps samples with `System.currentTimeMillis()` at delivery time. Sensor batching and handler latency make dt jitter badly; jerk (= Δa/Δt) inherits that jitter, and wall clock can step (NTP), producing negative dt.

**Design.** Use `SensorEvent.timestamp` (nanos, `elapsedRealtimeNanos` clock) as canonical sample time; convert to epoch ms only at persistence (anchor computed once at service start). Guard `dt ∈ (0, 1 s]` in jerk computation.

### 1.7 Remove accelerometer speed integration (P0, S)

**Problem.** During GPS loss the service integrates longitudinal accel into `deadReckoningSpeedMs`. The spec is explicit: *never integrate the accelerometer for speed* — bias drifts within seconds, then that fake speed gates event detection and feeds crash evaluation.

**Design.** On GPS loss: hold last known speed ≤ 5 s with decaying confidence, then `speed = null` (nullable, or `speedValid: Boolean`). While invalid: suppress speed-gated detectors and crash evaluation, emit a `GAP` marker (pairs with #25), exclude the interval from distance.

### 1.8 GPS staleness detection (P0, S)

**Problem.** `latestLocation` is written per fix and read forever. After the *first* fix, `loc != null` is permanently true — the GPS-lost branch can never execute again, and a 10-minute-old tunnel fix still supplies "current" speed and coordinates to detectors, crash assessment, and stored events.

**Design.** Store `(fix, receivedAtElapsedMs)`; valid only if age < 3 s — the prerequisite for 1.7's gap policy actually triggering. Also register `LocationCallback.onLocationAvailability` and clear on provider loss.

### 1.9 Scoring model v2 (P1, M)

**Problem.** `calculateScore()` = `100 − 10·(events per 100 km)`, all event types equal, integer-truncated, unstable for short trips (a 500 m nursery run with 1 event → 200/100 km → score 0). Jerk — per the spec the best infant-comfort metric and the FR-10 differentiator — is computed but never used; Baby mode only lowers thresholds, it doesn't reweight.

**Design.** Pure-Kotlin `TripScorer` (moved out of `TelemetryEngine` so persisted trips can be re-scored):

```
score = 100 − Σ_type w_type·f(rate_type) − w_jerk·jerkPenalty − w_var·speedVarPenalty
rate_type   = Σ severity_i / max(distanceKm, 2.0)     // 2 km floor stabilises short trips
f(rate)     = k·rate / (1 + rate/r_sat)               // saturating, no cliff to 0
jerkPenalty = time fraction with |jerk| > comfort band (from MetricSample aggregates)
```

Default weights: BRAKE 12, ACCEL 8, CORNER 10, SWERVE 14, ROUGH 3, PHONE_USE 10, jerk 15, speed-variance 5. Baby mode: jerk weight ×2, comfort band −30 %. Persist a `ScoreBreakdown` JSON on `Trip` so `ScoreBreakdownCard` (#26) shows real per-component deductions instead of a proportional split.

### 1.10 Per-vehicle baseline learning (P2, L)

Fixed thresholds treat a soft-sprung SUV and a stiff hatchback identically. During the first ~10 trips per detected car (keyed by BT device id from 7.2, else "default vehicle"), record the p95 long/lat accel and vertical variance; set thresholds as `max(specFloor, p95 × 1.4)`. New `VehicleProfile` Room entity `(btDeviceId?, label, p95Long, p95Lat, vertBaseline, tripCount)`. Show "calibrating (trip 3/10)" until stable. Never raise thresholds above the spec's Baby-mode ceilings.

## 2. Trip lifecycle & reliability

### 2.1 Full trip state machine (P0, L)

**Problem.** B-§4 specifies `IDLE → ACTIVE → ENDING → POST_TRIP` with persistence, but the implementation is ad-hoc intents; nothing represents ENDING and state does not survive process death (NFR-5 half-met).

**Design.** `TripStateMachine` (pure Kotlin, `StateFlow<TripState>`) owned by a Hilt-singleton `TripSessionManager`, consumed by both service and ViewModels (also removes the broadcast dependency, see 4.2). States: `Idle`, `Active(tripId, startTs, babyMode)`, `CrashReview(tripId, assessment)`, `Ending(tripId)`, `PostTrip(tripId)`. Persist state + tripId to DataStore (Proto) on every transition; on start, restore: an `Active` state with a live service resumes; without one, finalise via `Ending` from persisted `MetricSample`s (subsumes 2.3).

### 2.2 Automatic trip start/stop (P1, L)

The single biggest retention feature: parents will not remember to press Start.

- **Detection:** `ActivityRecognitionClient.requestActivityTransitionUpdates` for `IN_VEHICLE ENTER/EXIT` (permission requested only when the user enables auto-start — FR-27), plus `BluetoothDevice.ACTION_ACL_CONNECTED` matched against `Settings.btTriggerDeviceId` (`BLUETOOTH_CONNECT`).
- **Start path & Play policy:** a location FGS can't start from background without `ACCESS_BACKGROUND_LOCATION`. Two tiers: (a) default — high-priority "Looks like you're driving — start trip?" notification whose tap starts the FGS from foreground (no background-location permission); (b) opt-in "fully automatic" tier requesting background location with the B-§7 multi-step rationale.
- **Stop:** `IN_VEHICLE EXIT`, or speed < 2 km/h for 5 min, or BT disconnect → `Ending` with a 2-min grace window (traffic lights, pickup).
- **Passenger guard:** if the trip matches transit heuristics (long dwell every ~500 m) or the user picked BUS/TRAIN sensitivity, suggest "not driving?" rather than auto-scoring — protects the score from bus rides.

### 2.3 Orphaned-trip recovery (#21) (P0, S)

On app start, query for trips with `endTs == null` and no running session. Finalise: `endTs = last MetricSample.ts`, recompute distance/avg/max from samples, score via `TripScorer`, mark `wasInterrupted = true` (new column) so the summary says "recording was interrupted". Run as a one-shot WorkManager job from `BabyOnBoardApplication.onCreate`.

### 2.4 Partial WakeLock (#22) (P0, S)

Acquire `PARTIAL_WAKE_LOCK` in `onStartCommand`, release in `onDestroy`, tag `babyonboard:trip`, safety timeout ~4 h re-armed on each fix. Without it, Doze on long motorway trips throttles sensor callbacks exactly when crash detection matters most. Additionally offer the battery-optimisation-exemption settings deeplink once, with an explanation (the direct request intent is Play-sensitive).

### 2.5 "Stop Trip" notification action (#23) (P1, S)

`NotificationCompat.Action` → `PendingIntent.getService` → `ACTION_STOP_TRIP` in `onStartCommand`, routed through the state machine (`Ending`) so finalisation and the back-seat reminder still run. Also: live speed/duration in the notification throttled to 1/min, Baby-mode colorized accent.

### 2.6 Persist events from the service (P0, M)

**Problem.** The service persists `MetricSample`s but only *broadcasts* detected events for the UI to persist. With the screen off (the normal driving case) the activity is dead, so events exist only as broadcasts nobody receives; the final score reflects whatever the ViewModel happened to see.

**Design.** `tripRepository.saveEvent(event)` directly in the service collect loop. The UI subscribes to Room (`EventDao.observeByTrip` Flow) for the live event feed — Room becomes the single source of truth, and the live screen survives process recreation for free.

## 3. Battery & performance (NFR-4)

### 3.1 Adaptive sensor sampling (#13) (P1, M)

Three profiles switched by state + recent activity:

| Profile | Condition | GPS | Motion |
|---|---|---|---|
| CRUISE | no event in 60 s, speed stable | 1 Hz | `SENSOR_DELAY_UI` (~16 Hz) |
| ALERT | episode active or jerk elevated | 1 Hz, min interval 500 ms | `SENSOR_DELAY_GAME` (~50 Hz) |
| STATIONARY | speed < 2 km/h for 60 s | 0.2 Hz | accel only, 5 Hz (wake trigger) |

`MotionSource.setRate(profile)` re-registers listeners; `LocationSource.setRate()` swaps `LocationRequest`s. Thresholds are rate-independent after 1.3's filtering (validate via 8.1 at both rates). Log per-trip battery delta (`BatteryManager` at start/end) into `Trip` to track the ≤ 6 %/hr target in the field.

### 3.2 Batched MetricSample writes (P1, S)

Every motion frame currently runs `saveMetricSample()` in its own coroutine — ~50 single-row transactions/s on a SQLCipher DB (each paying cipher cost). Buffer and flush every 5 s or 250 samples via one `insertAll` transaction; downsample to 10 Hz at write time (engine still sees full rate — the spec calls for downsampled persistence anyway). Flush on `onDestroy` and state transitions.

### 3.3 Low-battery warning (#24) (P2, S)

Watch `ACTION_BATTERY_LOW`/`BATTERY_CHANGED` in the service; below `Settings.lowBatteryThresholdPct` (new, default 15) post one high-priority notification ("crash detection may stop if the phone dies") and switch to CRUISE profile to stretch runtime.

## 4. Security, privacy & data governance

### 4.1 Scope broadcasts to the package (P0, S)

**Problem.** `sendBroadcast(Intent(ACTION_SPEED_UPDATE…))` etc. are implicit broadcasts: **any installed app** can read live location, speed, and crash events — and any app can *spoof* `ACTION_CRASH_DETECTED` to trigger the SOS flow.

**Fix (one line each, do immediately):** `intent.setPackage(packageName)` on every `sendBroadcast` in `TripForegroundService`, and register receivers with `Context.RECEIVER_NOT_EXPORTED` (API 33+).

### 4.2 Replace broadcasts with an injected flow bus (P1, M)

Hilt `@Singleton TelemetrySessionBus` exposing `SharedFlow<TelemetryUpdate>` (sealed: `SpeedUpdate`, `EventDetected`, `CrashDetected`, `DebugLog`) shared by service and ViewModels. Keep only the crash full-screen-intent notification on the system path (it must work with no UI process). Deletes the string-typed `EXTRA_*` constants and the PHONE_USE re-encoding.

### 4.3 Retention auto-purge (#3, FR-28) (P0, S)

Daily WorkManager job (`requiresDeviceIdle`) running `tripDao.deleteOlderThan(now − retentionDays)`, cascading to events and samples (verify `ON DELETE CASCADE` on the FKs; add in the 8.3 migration if missing). Metric samples get a shorter independent cap (e.g. 30 days) since they dominate storage.

### 4.4 Data export & delete-all (#6, FR-30 — GDPR) (P1, M)

- **Export:** trips + events (+ optional samples) via `kotlinx.serialization` to JSON and CSV, zipped into `cacheDir`, shared via `FileProvider` + `ACTION_SEND` (no storage permission). WorkManager with progress notification for large datasets.
- **Delete-all:** type-to-confirm → `database.clearAllTables()` + DataStore reset + cancel scheduled work. Both are prerequisites for a truthful Play Data Safety form (NFR-10).

### 4.5 Consent & privacy dashboard (P1, M)

Single screen: what is collected (live row counts per table), where it lives (device only), per-collection toggles (samples, distraction, event locations), export/delete buttons. Onboarding gains an explicit consent step persisted as `consentVersion + timestamp`; bump and re-prompt when scope changes. Groundwork for any future cloud/insurer feature.

## 5. Crash detection & SOS

### 5.1 Time-based crash evaluation window (P0, M)

**Problem.** `speedHistory`/`accelHistory` keep the last **30 samples**. At ~50 Hz that's ~0.6 s of history — but the spec's heuristic needs `v_pre` from *before* impact and a speed collapse sustained **≥ 10 s**. The collapse condition can never be evaluated over a real 10 s window, `v_pre` is read from within the impact itself, and every motion frame duplicates the same GPS speed, so the "speed history" is mostly one value repeated.

**Design.** Ring buffer of `(elapsedMs, speed, |a|)` covering 30 s, appending speed only on *new* GPS fixes. `EvaluateCrashUseCase` becomes staged:

```
phase 1  IMPACT:    peak |a| ≥ 4 g within any 500 ms window
phase 2  PRE:       median speed in [t_impact−8 s, t_impact−2 s] ≥ 25 km/h
phase 3  COLLAPSE:  all speeds in [t_impact+2 s, t_impact+12 s] < 5 km/h
confidence = w1·impactMargin + w2·preMargin + w3·collapseCompleteness
```

Decision fires ~12 s after impact (the spec's "< 2 s" is *after the event window*). Suppress when `speedValid == false` (1.7) — no fabricated crashes in tunnels. Unit-test against synthetic and recorded traces: phone drop at standstill, pothole at 80 km/h, hard stop from 100, simulated crash profile.

### 5.2 SOS coordinates + plain-language location (#4, FR-21) (P1, S)

`SosScreen` gets lat/lng (already in the crash intent): show `DD°MM.mmm'` + decimal in big monospace, plus a reverse-geocoded one-liner via async `Geocoder` with timeout and offline fallback to coordinates-only — never block the countdown on geocoding. "Copy" and "Share location" buttons. The countdown must run in a service/AlarmManager context, not composable state (see 5.4).

### 5.3 SMS contact alerting (P1, M)

FR-20's "alert contacts" is currently only the dial prompt. Opt-in `SEND_SMS` (requested at SOS setup only): on expiry send each `ContactRole.EMERGENCY` contact `"Possible crash detected for <name>. Location: https://maps.google.com/?q=<lat>,<lng> — Baby mode was ON. Automated message from Baby on Board."` via `SmsManager` with sent/delivered PendingIntents; per-contact delivery status on the SOS screen. Fallback if declined: prefilled `ACTION_SENDTO smsto:` intent. Log the outcome as an `Event(type=CRASH)`.

### 5.4 Crash flow hardening (P1, S)

- `setFullScreenIntent` + `USE_FULL_SCREEN_INTENT`, `CATEGORY_ALARM`, `USAGE_ALARM` audio (bypasses DND — which the app itself may have enabled via 6.1).
- Countdown runs in the foreground service (state `CrashReview`), so a locked phone still escalates.
- Cancel re-enters `Active` and records a suppressed-crash event (confidence + "cancelled by user") — this is the data that calibrates the false-positive rate flagged in B-§13.

## 6. UX & feedback

### 6.1 In-trip Do Not Disturb (#8, FR-16) (P1, S)

On start, if enabled and policy access granted: save `currentInterruptionFilter`, set `INTERRUPTION_FILTER_PRIORITY`; restore at `Ending` **and** in `onDestroy`, and persist the pre-trip filter to DataStore so an app kill doesn't strand the user in DND. Settings toggle deep-links to `ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS` when access is missing.

### 6.2 Back-seat reminder escalation (#9, FR-24) (P1, S)

Ladder driven by `Settings.reminderEscalation` (0–3), scheduled with `setExactAndAllowWhileIdle`:

| Step | Delay | Behaviour |
|---|---|---|
| 0 | trip end | high-priority notification "Check the back seat 👶" |
| 1 | +60 s unacked | repost with alarm-stream sound + vibration |
| 2 | +120 s | full-screen intent + looping alarm tone |
| 3 | +180 s | optional SMS to the ARRIVAL contact |

"I checked" action cancels the ladder; acknowledgment latency stored per trip. Steps ≥ 2 only in Baby mode.

### 6.3 Route map in trip summary (#27) (P1, M)

Polyline from `MetricSample` coordinates. Recommendation: **osmdroid** (no API key, matches the local-first/privacy posture; Maps SDK would be the first Google network dependency). Douglas-Peucker downsampling (ε ≈ 10 m); event markers colour-coded by type, severity-scaled; tap → detail bottom sheet. Settings toggle "Show maps (downloads tiles from OpenStreetMap)" keeps the no-network default honest.

### 6.4 Speed profile chart (#7) (P2, S)

Reuse the `ScoreTrendChart` Canvas approach: speed vs elapsed time from `MetricSample`, event ticks, Baby-mode comfort-band shading for jerk excursions. Downsample to ≤ 300 points with min/max preservation so peaks don't vanish.

### 6.5 Weekly summary notification (#28) (P2, S)

Weekly WorkManager (Sunday 18:00 local): trip count, avg score, delta vs prior week, best trip; one notification deep-linking to a `WeeklySummaryScreen`. Copy stays non-punitive per FR-13 ("Score dipped 4 points — mostly one rough Thursday trip").

### 6.6 Contextual post-trip tips (#34) (P2, S)

Static `Map<EventType, List<TipTemplate>>` (no network): dominant event type picks a templated tip ("3 hard brakes — try lifting off earlier when you see brake lights"). One tip max; suppress when score ≥ 90 (praise instead); rotate templates; localise via 6.9.

### 6.7 Gamification (#11, FR-15) (P2, M)

Local-only `Badge` entity `(id, achievedTs, tripId?)`, rules evaluated at finalisation: `SmoothStreak(n)`, `ZeroHarshWeek`, `EarlyBirdCalm` (score ≥ 85 before 08:00 — the stressed nursery run), `FirstHundredKm`. Streak chip on `TripHistoryScreen`; gentle streak loss ("streak paused — every trip is a fresh start"), never shame (FR-13/NFR-2).

### 6.8 Accessibility pass (#15, NFR-7) (P1, M)

`contentDescription` on all icon buttons and charts (alt-text summarising the trend); 48 dp touch targets; `liveRegion` semantics on the SOS countdown so TalkBack announces remaining seconds; 200 % font-scale audit; contrast check of score colours. Add `enableAccessibilityChecks()` Compose tests to CI.

### 6.9 Localization EN + EL (NFR-8) (P1, M)

Extract remaining hardcoded strings (service notification, SOS copy, tips) to resources; `values-el/`; plurals; per-locale emergency-number default table (112 EU-wide, `Settings.emergencyNumber` override kept). Safety-critical copy human-reviewed, not machine-translated — the NFR-2 wording is load-bearing.

## 7. New features (v1.1+)

### 7.1 Arrival sharing (FR-25/26) (P2, L)

`GeofencingClient` fence per `Geofence(purpose=ARRIVAL)`, registered at trip start and removed at trip end (no persistent background geofences → lighter Play review). On ENTER during a trip: "Arrived — tell <contact>?" → one tap sends the prefilled SMS. Consent per FR-26: disclosure SMS at setup ("reply STOP to opt out"), `Contact.consentTs` recorded, revocation deletes the pairing; fully automatic sending only after explicit per-recipient opt-in.

### 7.2 Baby-mode auto-suggest (FR-11, #14/#33) (P2, M)

Deterministic first (no ML): saved car-BT connect, `MODE_TRIGGER` geofence exit (home → nursery), and time-of-day pattern (≥ 3 of the last 5 same-weekday trips at this hour had Baby mode on). Any trigger → suggestion chip "Enable Baby mode for this trip?" — always confirmable, never silent (FR-11's hard rule). Store trigger provenance on the trip for precision measurement.

### 7.3 Speed-limit comparison via OSM (FR-7, #12) (P3, L)

Local-first: download regional OSM extracts of `maxspeed`-tagged ways (Geofabrik) on Wi-Fi into an R-tree-indexed SQLite sidecar; map-match GPS points (heading-consistent, ≤ 25 m). Emit `SPEED` events only at high limit-confidence (tagged, matched, sustained 10 s over limit +10 %). Where no data: show nothing (FR-7's default). Gate behind a build flag; validate map-matching on replay traces first.

### 7.4 Baby Comfort Index (P2, M)

Distinct from the safety score: comfort = what the infant feels. Composite 0–100 from jerk distribution (dominant), vertical roughness exposure, cornering accumulation (`∫|a_lat|dt`), and cabin noise if 7.7 lands. Shown only in Baby-mode trips as a second ring ("Smooth ride: 92 — great for sleeping 😴"). Computed by `TripScorer` from existing aggregates; no new sensing. Strong differentiator — nobody frames telemetry as *infant comfort*.

### 7.5 Driving profiles (#29) (P3, M)

`Profile` entity `(id, name, sensitivity, babyModeDefault, sosContactIds, btDeviceId?)`; `Trip.profileId` FK; selector on the start screen (default = last used or BT-matched); history filterable per profile — solves "grandma's trips wreck my streak".

### 7.6 Per-route roughness comparison (#31) (P3, M)

Geohash (precision 7, ~150 m) each ROUGH event; per-cell rolling stats `(geohash, count, meanVariance, lastSeen)`. Summary: "This route was 20 % rougher than usual" + roughest-segment chip. Local only; feeds a future "smoothest route to nursery" suggestion.

### 7.7 Cabin noise comfort (#30) (P3, M)

`AudioRecord` RMS amplitude only — per-second approximate dB level, PCM discarded immediately, nothing written (documented in the privacy dashboard; `RECORD_AUDIO` requested contextually with "level only" copy). Minute-level noise percentile stored as a metric; feeds 7.4. Off by default; the spec avoids `RECORD_AUDIO` in MVP, so this is v1.2+ opt-in.

### 7.8 On-device ML scoring (TFLite) (P3, L)

Once 8.1 exists and beta trips get 1-tap "was this trip smooth?" labels: train a small model mapping per-trip aggregate features (event rates by type/severity, jerk histogram, speed variance, roughness) → perceived smoothness. TFLite at finalisation, blended `0.7·rules + 0.3·ml` during validation. Aggregate features only, on-device (FR-8) — never a raw-sensor cloud model.

### 7.9 E2E-encrypted cloud backup (FR-29) (P3, L)

Explicit opt-in; key from a user passphrase (Argon2id) — provider-blind. The 4.4 export bundle encrypted AES-GCM, uploaded to the user's own Google Drive `appDataFolder` (no first-party server to run). Restore validates schema version against 8.3 migrations. Requires 4.4 + 4.5 first.

### 7.10 Home-screen widget (P3, S)

Glance widget: last score + weekly sparkline + "Start trip" button (launches MainActivity with an auto-start intent — an activity start satisfies the FGS foreground-start requirement). Updated from the trip finaliser via `updateAll()`.

### 7.11 Wear OS companion (P3, L)

Live speed/score tile; wrist vibration for the back-seat reminder (harder to miss than a phone in a bag); SOS cancel from the wrist. `MessageClient` transport; the phone stays the sensor/compute node. Only after retention data justifies platform spread.

## 8. Testing & tooling

### 8.1 Sensor-replay harness (B-§11) (P0, L)

**The key enabler for every threshold change in section 1** — currently the only way to verify a tuning change is to drive a car.

- **Record:** debug-only toggle writing raw pre-transform `RawSensorData` (rotation vector + GPS interleaved) to gzipped JSONL in `getExternalFilesDir("traces")`, one file per trip.
- **Replay:** JVM test fixture `TraceReplayer(file).playInto(engine)` — pure Kotlin; requires the frame transform moved out of the Service into the engine (part of 1.1). Deterministic timestamps from the trace.
- **Golden corpus:** committed traces under `app/src/test/resources/traces/` — hard brake, hard corner, pothole run, phone-drop-while-parked, bus ride, simulated crash — each with a YAML expectations file (`expected events, score range`). CI fails when tuning shifts results outside expectations.
- **Anonymisation:** offset lat/lng by a random fixed delta before committing.

### 8.2 Detector & scoring unit tests (P0, M)

Property-style tests: no event below threshold; exactly one event per synthetic episode after 1.2; severity monotone in peak; `TripScorer` monotone decreasing in event rate, short-trip floor, Baby-mode jerk weighting observable; `EvaluateCrashUseCase` phases independently falsifiable; `VehicleFrameTransformer` round-trips at 8 headings. Locks in section 1 before beta calibration.

### 8.3 Room migration strategy (#18) (P1, S)

Sections 1–7 add columns/entities. Before the *first* one ships: versioned `Migration`s, `exportSchema = true` with committed schemas, `MigrationTestHelper` test on the SQLCipher factory. Never `fallbackToDestructiveMigration` — this DB is the user's entire history.

### 8.4 Service & ViewModel tests (#16/#17) (P1, M)

Robolectric: start intent → notification exists; stop action → `Ending`; destroy → DND restored (6.1). `SettingsViewModel` round-trips; `TripSessionManager` transition-table test covering every B-§4 transition plus illegal-transition rejection.

### 8.5 Release hardening (#20) (P1, S)

`minifyEnabled true` + `shrinkResources`; keep rules for Room/SQLCipher/Hilt; release-build instrumented smoke test (start trip, record 30 s, finalise). Baseline profile module later for cold start.

### 8.6 In-app debug console (P2, M)

The `ACTION_DEBUG_LOG` broadcasts hint at debugging pain. Debug-build screen: ring buffer of the last 500 engine log lines (via the 4.2 bus), live frame values (accel components, heading, frame confidence), current sampling profile, and an "export trace" button wired to 8.1. Removes `adb logcat` from test drives.

## Suggested sequencing

1. **"Trustworthy engine" (P0 core):** 4.1 → 1.6 → 1.5 → 1.8 → 1.3 → 1.2 → 1.1 → 1.4 → 1.7 → 2.6, with 8.1/8.2 built alongside (record traces *before* changing detectors so before/after is measurable).
2. **"Never lose a trip":** 2.1, 2.3, 2.4, 2.5, 3.2, 4.3, 8.3.
3. **"Crash & care":** 5.1–5.4, 6.1, 6.2.
4. **"Delight":** 1.9, 6.3–6.6, 4.4, 4.5, 6.8, 6.9.
5. **v1.1:** 2.2, 3.1, 6.7, 7.1, 7.2, 7.4.
6. **v1.2 exploration:** rest of section 7.

The single most important takeaway: items **1.1, 1.2, 1.5–1.8, and 5.1 are correctness fixes, not enhancements** — today's scores and crash decisions are computed from heading-confused axes, unfiltered spikes, per-frame duplicate events, and a 0.6-second "10-second" window. Fixing those, validated by the replay harness, is worth more than any new feature.
