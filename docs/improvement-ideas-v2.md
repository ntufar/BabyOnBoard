# Improvement Ideas & New Features — Round 2 (Technical Specifications)

Continues [improvement-ideas.md](improvement-ideas.md) (sections 1–8). Everything below is **new** — found by a second, deeper pass over the code, the manifest, the build setup, and the CI pipeline. Section numbering continues at 9 so cross-references between the two documents stay unambiguous.

Priorities: **P0** correctness/safety of the core product · **P1** high user value · **P2** valuable, not urgent · **P3** exploratory / v1.2+.
Effort: **S** ≤ 1 day · **M** 1–3 days · **L** ≥ 1 week.

## Quick index

| # | Idea | Priority | Effort |
|---|------|----------|--------|
| 9.1 | Cancel pipeline jobs on repeated `onStartCommand` | P0 | S |
| 9.2 | Kill the `tripId = "unknown"` phantom trip (typed start contract) | P0 | S |
| 9.3 | Fix MotionSource double-emission on gyro events | P0 | S |
| 9.4 | Rotate yaw rate out of the device frame | P0 | S |
| 9.5 | DistractionSource false positives (screen-on init, nav apps, periodic re-fire) | P0 | M |
| 9.6 | Auto Backup restores an undecryptable database | P0 | S |
| 9.7 | Remove unused permissions; make "no INTERNET" a provable privacy claim | P0 | S |
| 9.8 | GPS fix timestamps from the fix, not receipt time | P0 | S |
| 9.9 | Gradient: persist it or delete it | P1 | S |
| 10.1 | GAME_ROTATION_VECTOR — dodge in-car magnetic interference | P1 | S |
| 10.2 | Barometer-based gradient & altitude | P2 | M |
| 10.3 | Hardware sensor batching (`maxReportLatencyUs`) | P1 | S |
| 10.4 | Loose-phone / mount detection | P2 | M |
| 10.5 | Sensor capability self-test at onboarding | P1 | S |
| 11.1 | Car-seat recline angle checker | P1 | S |
| 11.2 | Stroller mode | P2 | M |
| 11.3 | Voice coach (TTS, eyes-free feedback) | P2 | M |
| 11.4 | Trip mood tagging (the label source for on-device ML) | P2 | S |
| 11.5 | Age-based car-seat guidance content | P3 | S |
| 11.6 | Hot-weather back-seat escalation | P3 | M |
| 12.1 | Quick Settings tile (start/stop trip) | P1 | S |
| 12.2 | Static app shortcuts | P2 | S |
| 12.3 | Dash mode (glanceable in-car screen) | P2 | M |
| 12.4 | Trip auto-merge across short stops | P2 | S |
| 12.5 | Lifetime stats dashboard | P2 | M |
| 12.6 | GPX export | P3 | S |
| 12.7 | Demo mode (replay a bundled trace) | P2 | S |
| 13.1 | kapt → KSP, Kotlin 2.x, Compose BOM, version catalog | P1 | M |
| 13.2 | Modularization (`:domain`, `:data`, `:sensing`, `:ui`) | P2 | L |
| 13.3 | Static analysis in CI (detekt, ktlint, Konsist) | P1 | S |
| 13.4 | Screenshot tests (Roborazzi) | P2 | M |
| 13.5 | CI hardening (managed-device tests, release-build verification) | P1 | M |
| 13.6 | Local crash journal (no cloud) | P1 | S |
| 13.7 | F-Droid distribution + reproducible builds | P3 | M |
| 13.8 | Edge-to-edge & predictive back audit | P2 | S |
| 14.1 | Merge the `Event` / `TelemetryEvent` duplicate models | P2 | S |
| 14.2 | DB indexes + Paging 3 for history | P1 | S |
| 14.3 | Persist jerk (and gradient) in `MetricSample` | P1 | S |
| 14.4 | Cached trip aggregates | P2 | S |

## 9. Newly identified correctness & safety bugs

All of these are grounded in specific lines of the current code. Like v1 section 1, they are fixes, not enhancements — several silently corrupt the data the user sees.

### 9.1 Cancel pipeline jobs on repeated `onStartCommand` (P0, S)

**Problem.** Every call to `TripForegroundService.onStartCommand()` launches four fresh collector coroutines on `serviceScope` (`TripForegroundService.kt:113–247`) and never cancels the previous ones. A second start intent — user restarts a trip, or the system redelivers with `START_STICKY` — leaves the old collectors running: every motion frame is processed **twice** (duplicate `MetricSample` rows, duplicate events, doubled score deductions), with all copies writing to whatever `tripId` the shared `var` currently holds.

**Design.** Hold a `private var pipelineJob: Job?`; in `onStartCommand`, `pipelineJob?.cancel()` then `pipelineJob = serviceScope.launch { … }` with the collectors as child jobs (`launch` inside a parent `coroutineScope`). Also stop/re-start the sources idempotently (`LocationSource.start()` currently calls `requestLocationUpdates` again without removing the old callback — the fused client replaces it because the callback object is the same, but `MotionSource.start()` registers a second listener set only if `stop()` wasn't called). Add a Robolectric test: two start intents → exactly one sample per synthetic frame.

### 9.2 Kill the `tripId = "unknown"` phantom trip (P0, S)

**Problem.** `tripId = intent?.getStringExtra(EXTRA_TRIP_ID) ?: "unknown"` (`TripForegroundService.kt:80`). With `START_STICKY`, after the process is killed the system restarts the service with a **null intent**: the service then happily records samples and events into trip `"unknown"` with `babyMode = true` — rows no `Trip` ever owns, invisible in history, never purged, and mixing frames from every such restart across the device's lifetime.

**Design.**
1. Introduce a `@Parcelize data class TripStartConfig(tripId: String, babyMode: Boolean, sensitivity: SensitivityMode)` as the single typed start contract (replaces the three string extras).
2. On null intent / missing config: don't guess. Read the persisted session state (v1 2.1's DataStore record). If an `Active` trip exists → resume it with its real config; if not → `stopSelf()` immediately.
3. Delete the `"unknown"` fallback and the `EXTRA_BABY_MODE`/`EXTRA_SENSITIVITY` defaults; a one-shot cleanup in the 4.3 purge job deletes any existing `tripId = "unknown"` rows.

### 9.3 Fix MotionSource double-emission on gyro events (P0, S)

**Problem.** `MotionSource.emitFused()` runs on **both** accelerometer and gyroscope callbacks (`Sources.kt:98,109`). Each gyro event re-emits the *previous accel sample* (`latestAccel.copy(yawRate = …)`) — same acceleration values, same timestamp. Net effect at `SENSOR_DELAY_GAME`: ~100 emissions/s instead of ~50, i.e. **every accel frame processed roughly twice** — doubled Room writes on the SQLCipher DB (compounds v1 3.2), duplicate entries polluting `vertAccelWindow` (the roughness window then spans half the intended time), and duplicate frames hitting the detectors.

**Design.** Emit only from the accelerometer branch (the accel is the pacing signal); the gyro callback just updates `latestGyro`. Alternatively emit gyro-paced frames flagged `accelFresh = false` and have the engine skip detection for them. Guard in the engine regardless: ignore frames whose timestamp equals the previous frame's (belt-and-braces with v1 1.6's `dt ∈ (0, 1 s]`).

### 9.4 Rotate yaw rate out of the device frame (P0, S)

**Problem.** v1 1.1 fixes the accel frame, but the gyro is read as raw device-frame Z (`event.values[2]`, `Sources.kt:106`). That is yaw only when the phone lies flat. Mounted portrait in a cradle, rotation about the car's vertical axis appears on the device **Y** axis — `yawRate` reads ≈ 0, so the CORNER gate (`|yawRate| > 0.1`) suppresses real corner events and SWERVE detection goes blind; phone in a pocket gives an arbitrary mix.

**Design.** Rotate the full gyro vector `(x, y, z)` with the same rotation matrix used for acceleration; take the world-frame Z component as yaw rate. Requires `MotionSource` to keep all three gyro axes (add `gyroX/gyroY` to `RawSensorData` or carry a `DoubleArray`). Fold into the `VehicleFrameTransformer` of v1 1.1 so accel and gyro share one transform and one test suite (replay at flat/portrait/pocket orientations must yield the same yaw trace).

### 9.5 DistractionSource false positives (P0, M)

**Problem.** Three compounding issues in `Sources.kt:125–160`:
1. `start()` unconditionally sets `screenOn = true`. Typical trip start: user taps Start, pockets the phone, screen times out — but until a `SCREEN_OFF` broadcast arrives the tick loop logs a PHONE_USE event **every 5 seconds while driving**.
2. `tick()` re-fires for as long as the screen is on: using Google Maps navigation in a mount = a PHONE_USE event every 5 s for the whole trip, nuking the score of exactly the safety-conscious parent following navigation.
3. Screen-on ≠ handling: Always-On Display and lock-screen glances count the same as active phone handling (FR-17 explicitly wants *handling*).

**Design.**
- Initialise from truth: `screenOn = powerManager.isInteractive` in `start()`.
- Count **episodes, not ticks**: one PHONE_USE event per screen-on episode that begins (or persists) above 5 km/h, with `durationMs` accumulated until screen-off — severity scales with duration (10 s glance ≠ 5 min of scrolling). Pairs with the episode state machine of v1 1.2.
- Exemptions: keyguard still locked (`KeyguardManager.isKeyguardLocked` — AOD/lock-screen glance → ignore or half-weight); Baby on Board itself in foreground (dash mode 12.3 must not be a penalty — expose foreground state to the service via the v1 4.2 bus).
- Optional "navigation exemption" toggle in Settings, honest copy: "screen-on while a navigation app is active isn't counted" (detecting *which* app needs `PACKAGE_USAGE_STATS`; ship the toggle as time-boxed exemption first — screen on within 10 s of trip start in a mount — and evaluate the permission later).
- True *handling* signal (FR-17): while screen is on, if the accel variance profile shows the phone is hand-held (constant micro-motion, orientation changes) vs. rigid in a mount → only hand-held counts full severity. Feeds on 10.4's mount detection.

### 9.6 Auto Backup restores an undecryptable database (P0, S)

**Problem.** `AndroidManifest.xml` sets `android:allowBackup="true"` with no backup rules. Android Auto Backup includes the Room database files — but the SQLCipher passphrase lives in AndroidKeystore (features.md #1), which is **hardware-bound and never backed up**. After a device migration or reinstall-with-restore, the app gets the encrypted DB bytes without the key: SQLCipher open throws, and depending on where that happens the app crash-loops on first launch on the new phone.

**Design.**
1. Add `android:fullBackupContent` (pre-12) and `android:dataExtractionRules` (12+) excluding `databases/` and the DataStore file — or set `allowBackup="false"` outright (simplest, matches local-first posture; users get real portability via 4.4 export instead).
2. Defensive open: wrap DB creation in `AppModule` with a `runCatching`; on `SQLiteException`/cipher failure show a one-time "data from your old phone can't be unlocked on this device" screen offering reset (delete + recreate) — never a silent crash loop.
3. Regression test: Robolectric with a garbage DB file on disk → app reaches onboarding with the recovery prompt.

### 9.7 Remove unused permissions; make "no INTERNET" provable (P0, S)

**Problem.** The manifest requests `INTERNET`, `ACCESS_BACKGROUND_LOCATION`, and `ACTIVITY_RECOGNITION`, none of which the code uses. Each is a liability: background location triggers Play's strictest review track (with a required video demo) *for a feature that doesn't exist yet* (v1 2.2 is unbuilt); ACTIVITY_RECOGNITION is a user-visible runtime permission that onboarding never requests; and INTERNET quietly contradicts the product's central promise.

**Design.**
- Drop all three now; re-add each in the PR that actually ships its feature (2.2 → background location + activity recognition; 6.3 osmdroid → INTERNET, or better: put INTERNET only in a `maps` product flavor so the default build stays network-incapable).
- Turn the absence into a feature: onboarding and the privacy dashboard (v1 4.5) state "this app cannot send data anywhere — it doesn't even hold the Android internet permission", verifiable by anyone on the Play listing's permission list. Almost no telemetry app can say this; it's the cheapest trust-builder available.
- Add a CI check (13.3) that fails if `INTERNET` appears in the merged manifest of the default flavor (`./gradlew :app:processDebugManifest` + grep), so a dependency can't sneak it back in.

### 9.8 GPS fix timestamps from the fix, not receipt time (P0, S)

**Problem.** `LocationSource` stamps every fix `System.currentTimeMillis()` at callback delivery (`Sources.kt:33`), discarding `location.time` / `location.elapsedRealtimeNanos`. Fused-provider fixes can be delivered late or batched; the receipt stamp misplaces the fix relative to motion samples, which corrupts exactly the speed-vs-accel corroboration that v1 1.4 and 5.1 build.

**Design.** Carry `location.elapsedRealtimeNanos` as the canonical fix time — the same clock v1 1.6 adopts for motion samples, so the two streams become directly comparable with no epoch conversion. Also propagate `location.time` for display/persistence. Staleness checks (v1 1.8) then compare fix age on one monotonic clock.

### 9.9 Gradient: persist it or delete it (P1, S)

**Problem.** `telemetryEngine.calculateGradient(...)` is called every frame (`TripForegroundService.kt:191`) and its return value is **discarded**. It burns a haversine per frame, mutates engine state, and produces nothing — FR-6's "gradient" is currently decorative. It's also called at motion rate (~50 Hz) with GPS positions that change at 1 Hz, so `dist` is ~0 and the `> 10 m` guard means it almost never yields a value anyway.

**Design.** Move the call to the GPS fix path (once per fix, where positions actually change). Persist the result: `MetricSample.gradientPct: Double?` (migration via v1 8.3). Consumers: hill-aware scoring context in `TripScorer` (a hard brake on a 12 % descent is different), and a "hilly route" chip in the trip summary. If no consumer ships within a release or two, delete the function — dead computation in the hot path is the worst of both worlds. Better input signal: 10.2's barometer.

## 10. Sensing quality — round 2

### 10.1 GAME_ROTATION_VECTOR to dodge in-car magnetic interference (P1, S)

**Problem.** The frame transform relies on `TYPE_ROTATION_VECTOR`, which fuses the **magnetometer** — and a car is a magnetometer's nightmare: speaker magnets, the wireless-charging coil under the phone mount, steel bodywork. A corrupted magnetic heading silently skews the rotation matrix, and with it every "world-frame" acceleration the engine sees.

**Design.** Prefer `TYPE_GAME_ROTATION_VECTOR` (gyro + accel only, no magnetometer): device-to-world *tilt* is exact, and the arbitrary yaw offset doesn't matter because vehicle heading comes from GPS bearing anyway (v1 1.1 projects onto heading — the magnetometer's only contribution was absolute yaw, which 1.1 stops needing). Fallback chain: GAME_ROTATION_VECTOR → ROTATION_VECTOR → accel-only gravity alignment (low-pass the accelerometer for the gravity vector, mark `frameConfidence = LOW`). Log which source was active per trip for field debugging (8.6).

### 10.2 Barometer-based gradient & altitude (P2, M)

GPS altitude is noisy (±10–20 m); the pressure sensor resolves ~0.1 m of elevation change. Where `TYPE_PRESSURE` exists (most mid/high-end phones): sample at 5 Hz, EMA-smooth, convert relative altitude via the barometric formula (absolute calibration unnecessary — gradient only needs deltas), and derive `gradientPct = Δh / Δdistance` over a 50 m rolling window. Replaces the GPS-altitude gradient from 9.9 when available. Bonus: pressure spikes are a known crash side-channel (airbag deployment causes a measurable cabin pressure pulse) — record the trace around impact candidates as an extra confidence input for v1 5.1, cost-free.

### 10.3 Hardware sensor batching (P1, S)

`registerListener` currently uses the 2-arg form — every sample wakes the AP immediately. Passing `maxReportLatencyUs` (e.g. 1 s in CRUISE, 0 in ALERT) lets the sensor hub batch samples in hardware FIFO and deliver them in bursts, cutting wakeups dramatically — the single cheapest battery win available, and it composes with (rather than replaces) v1 3.1's adaptive rates. Detection latency is unaffected where it matters because ALERT mode sets latency 0. Requires 1.6's sensor-event timestamps first (batched events arrive together; wall-clock stamping would collapse them onto one instant — another reason 1.6 is a prerequisite for everything).

### 10.4 Loose-phone / mount detection (P2, M)

A phone loose on the passenger seat measures its own sliding, not the car. Detect rigidity: rolling 2 s window of high-pass (> 5 Hz) accel energy plus rotation-vector angular velocity — mounted phones show low high-frequency energy and near-zero orientation drift; hand-held or loose phones don't. Output a per-frame `mountState ∈ {MOUNTED, LOOSE, HANDHELD}`: LOOSE down-weights event confidence (v1 1.4's frame-confidence input) and triggers a once-per-trip gentle tip ("secure the phone for accurate scoring"); HANDHELD while moving feeds 9.5's distraction severity. Pure signal processing, no new sensors or permissions; validate on replay traces (v1 8.1) recorded in all three states.

### 10.5 Sensor capability self-test at onboarding (P1, S)

minSdk 26 spans devices with no gyroscope, no rotation vector, or no barometer — on those, swerve detection, frame rotation, and (future) baro-gradient silently degrade today. Onboarding step "checking your phone's sensors": enumerate `getDefaultSensor` for each type, show a checklist, and persist a `DeviceCapabilities` record that the engine reads to select fallbacks explicitly (no gyro → disable SWERVE + CORNER yaw gate, say so in the trip summary footer: "cornering detection unavailable on this device"). Honest limits are already the product's voice (FR-27); this extends them to hardware. Also surfaces in the debug console (8.6).

## 11. Baby-specific differentiators

The competition (insurer telematics apps) can't credibly build these; they're what makes this app *Baby* on Board rather than generic telemetry.

### 11.1 Car-seat recline angle checker (P1, S)

Newborn seats must recline within a band (typically 30–45° from vertical; rear-facing convertibles have their own bands) — wrong angles are a genuine airway-safety issue, and parents check with a bubble-level app or a ball on a string today. The phone already is an inclinometer: a screen where the parent holds the phone flat against the seat-back, live angle from the gravity vector (`atan2` on low-passed accelerometer, exactly the math the engine already has), a green band for the target range, haptic tick when entering it, per-seat target presets ("newborn 30–45°", "toddler rear-facing", "custom" with the seat manual's figure). Zero new permissions, one screen, huge on-brand value — likely the most shareable single feature in the app. Disclaimer copy per NFR-2: "always confirm with your seat's own indicator and manual."

### 11.2 Stroller mode (P2, M)

`SensitivityMode.WALKING` already exists in the model with tuned thresholds (`Models.kt:82–112`) — the sensing stack is one UI toggle away from scoring *pram walks*: kerb impacts, rough pavement, jerky stops. Design: a "Stroller" trip type (distinct from a sensitivity tweak) that (a) uses WALKING thresholds, (b) skips GPS-speed gating (indoor/underpass walks; distance from step-cadence estimate or GPS when available), (c) scores comfort only — no BRAKE/ACCEL framing, only ROUGH + jerk ("smooth stroll: 94") — and (d) excludes these trips from driving history/streaks (`Trip.kind: DRIVE | STROLL` column, migration via 8.3). Nobody else has "how smooth was the walk to the park"; it doubles the app's usable occasions per day and works for non-drivers.

### 11.3 Voice coach — TTS, eyes-free (P2, M)

Post-trip feedback arrives when the drive no longer matters; in-trip visual feedback is a distraction engine. Voice is the correct in-trip channel: on-device `TextToSpeech`, utterances only at natural moments (trip start "Baby mode on — drive gently", one calm note after a severe episode "that was a firm stop — all okay?", arrival summary "Smooth trip — 94"). Hard limits: ≤ 3 utterances per trip, min 5 min spacing, never during an active episode (wait for calm), `AudioManager` focus with `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` so navigation/music ducks briefly, and a Baby-mode "quiet mode" (baby sleeping — haptic-only via the notification). All strings localized (v1 6.9) and non-punitive (FR-13). Off by default; enable from Settings.

### 11.4 Trip mood tagging (P2, S)

One-tap annotation on the trip summary: "How was the ride for the baby?" → 😴 slept / 🙂 calm / 😢 fussy (skippable, never required). Schema: `Trip.moodTag: String?`. Three payoffs: (1) it's the **label source** v1 7.8's ML scoring needs — perceived-comfort labels tied to full telemetry aggregates, collected for free; (2) it validates the Baby Comfort Index (7.4) — if 😴 trips don't score higher, the index is wrong; (3) history filtering ("show fussy trips") reveals patterns to the parent (fussy trips cluster at 17:00 — nap timing, not driving). Correlation shown only after ≥ 10 tagged trips to avoid junk conclusions.

### 11.5 Age-based car-seat guidance content (P3, S)

Optional child birth date (local only, obviously) unlocks stage-aware content: rear-facing duration reminders per local guidance, seat-transition checklists ("approaching 13 kg — check your seat's rear-facing limit"), winter-coat warning in cold months (bulky coats under harness straps are a known hazard), and the 11.1 angle presets auto-selected by stage. Static bundled content, reviewed by a car-seat technician, localized with 6.9; a quarterly app update refreshes it. No network, no accounts. Positions the app as the *car-seat companion*, widening it beyond telemetry.

### 11.6 Hot-weather back-seat escalation (P3, M)

Vehicular heatstroke is the nightmare scenario behind FR-23/24. Without claiming child detection (the app must never imply it — FR-12's discipline applies doubly): escalate the existing back-seat reminder ladder (v1 6.2) faster when heat risk is plausible. Signal sources in order of preference: `TYPE_AMBIENT_TEMPERATURE` sensor (rare), else a conservative month × local-time heuristic (device locale/timezone, e.g. May–Sep 10:00–18:00), user-overridable. Effect: in Baby mode + heat-risk window, the ladder starts at step 1 and reaches the full-screen alarm one step sooner; copy says "hot day — double-check the back seat", nothing more. Small, honest, and potentially the most consequential feature in the app.

## 12. UX — round 2

### 12.1 Quick Settings tile (P1, S)

`TileService` ("Start trip" / "Trip active — tap to stop"): swipe down, tap, drive. Tap forwards to `TileService.startActivityAndCollapse` → MainActivity with an auto-start extra (an activity start satisfies the FGS-from-foreground rule, same trick as v1 7.10's widget). Active state mirrors the session state from v1 2.1's `TripSessionManager`. This is the cheapest fix for the "parents won't remember to press Start" problem until full auto-start (2.2) ships.

### 12.2 Static app shortcuts (P2, S)

`shortcuts.xml`: "Start trip", "Start Baby-mode trip", "Trip history". Long-press the launcher icon → one tap into a recording. Pairs with 12.1; both are an afternoon of work combined.

### 12.3 Dash mode (P2, M)

A deliberate in-car screen for mounted phones: landscape, `FLAG_KEEP_SCREEN_ON`, near-black background with large high-contrast numerals (speed, elapsed, live score ring), automatic night palette, **no touch targets during motion** except a large End button gated by speed < 5 km/h. On a severe event: a brief soft amber glow, never text to read. Crucially, dash mode registers itself as exempt foreground for 9.5's distraction logic — the app must not penalize its own screen. Entry: automatic when a trip is active and orientation goes landscape while charging, or manual from the live screen.

### 12.4 Trip auto-merge across short stops (P2, S)

Fuel stop or nursery drop-off splits one journey into two trips, halving distances and doubling per-trip event rates (which the v1 1.9 scorer normalizes by distance — short fragments score erratically). At finalisation, if the previous trip ended < 10 min ago and < 200 m away (haversine on last/first samples): offer "continue previous trip?" on the summary, or auto-merge when the gap < 3 min. Merge = same `tripId` reused on resume (cleanest), or a `mergedIntoTripId` column with aggregates recomputed. Stop intervals excluded from duration/avg-speed; summary shows a stop marker on the route map (v1 6.3).

### 12.5 Lifetime stats dashboard (P2, M)

A "Stats" tab computed entirely from existing tables: total km/trips/hours, score distribution histogram, weekday × hour-of-day heatmap of average score (surfaces "Friday 17:00 school run is your roughest window"), event-type mix over time, longest smooth streak, per-month distance. All Room aggregate queries (`GROUP BY strftime`); Canvas charts in the existing `ScoreTrendChart` style. Deep-links from the weekly summary notification (v1 6.5).

### 12.6 GPX export (P3, S)

Extend the 4.4 exporter with GPX 1.1: `<trkpt lat lon><ele><time><extensions>` per `MetricSample`, events as `<wpt>` waypoints. Parents can view routes in any mapping tool without the app growing a map dependency; also the interchange format reviewers/testers will ask for. ~50 lines on top of 4.4's plumbing.

### 12.7 Demo mode (P2, S)

Onboarding ends at a wall: the user can't see the product's value until they've driven. Ship one bundled anonymized trace (a real 10-min drive with a few events) and a "See a demo trip" button that replays it **through the real pipeline** — the v1 8.1 `TraceReplayer` pointed at the production engine, writing to a `Trip` flagged `isDemo = true` (excluded from history aggregates, deletable in one tap). The user sees a genuine summary, score breakdown, and route map in 30 seconds. Also doubles as a manual smoke test of the whole pipeline on every device — and gives Play reviewers a way to exercise the app without a car.

## 13. Platform, build & tooling — round 2

### 13.1 kapt → KSP, Kotlin 2.x, Compose BOM, version catalog (P1, M)

The build is on Kotlin 1.9-era Compose (compiler ext 1.5.5), kapt for Room+Hilt, hand-pinned dependency versions in Groovy DSL. Migration order: (1) Room + Hilt to **KSP** (both fully support it; kapt is in maintenance and typically 2× slower — biggest CI win available); (2) Kotlin 2.x + the Compose compiler Gradle plugin (removes `composeOptions` pinning forever, brings strong skipping mode); (3) `libs.versions.toml` version catalog + `gradle/versions` update PRs via Renovate/Dependabot; (4) Compose BOM instead of per-artifact 1.5.4 pins (Material3 1.2.0 → current fixes real a11y and text-field bugs relevant to 6.8); (5) Gradle configuration cache on. Do this *before* the section-1 engine work lands, while merge conflicts are cheap.

### 13.2 Modularization (P2, L)

Split `:app` into `:core:domain` (pure Kotlin — use cases, models, `TelemetryEngine`/`TripScorer` after v1 1.1 moves the transform), `:core:data` (Room, repositories), `:sensing` (service + sources), `:app` (UI + DI wiring). Payoffs specific to this codebase: the domain module gets JVM-only unit tests with no Robolectric (the 8.1/8.2 replay suite runs in milliseconds), Konsist/lint can enforce "domain depends on nothing Android" mechanically, and CI caches per-module. Do after 13.1 (KSP first — kapt makes multi-module builds crawl).

### 13.3 Static analysis in CI (P1, S)

CI today runs `lint`, `test`, `assembleDebug` — no style or architecture gates. Add: **ktlint** (format), **detekt** (complexity/smells, with baseline file so it lands green), **Konsist** tests asserting the architecture doc's rules ("classes in `domain` import nothing from `android.*`", "ViewModels don't touch DAOs"), and the 9.7 merged-manifest permission check. All as one `check` job before build; each is ~30 lines of config.

### 13.4 Screenshot tests — Roborazzi (P2, M)

The UI is pure Compose — ideal for JVM screenshot tests (no emulator): golden images for OnboardingScreen, TripSummaryScreen (with a fixture trip: events, breakdown, both unit systems), SosScreen mid-countdown, TripHistoryScreen with the trend chart, light+dark, default+200 % font scale (feeds 6.8). Catches the class of regression unit tests never see (the v0.0.27 "duration text wrapping" fix was exactly this class). `verifyRoborazziDebug` in the CI check job; `recordRoborazziDebug` to bless changes.

### 13.5 CI hardening (P1, M)

- **Instrumented lane:** Gradle Managed Devices (`gradle-managed-device` ATD image) running the 8.4 service tests + a boot-record-finalise smoke on API 26 (min) and 35 (target) — the two API levels where FGS behavior actually differs.
- **Release verification:** CI currently only builds debug. Add `assembleRelease` (with a throwaway CI keystore) so R8 breakage (v1 8.5) is caught in PR, not at publish; diff the merged release manifest against a committed golden to catch permission creep (9.7).
- **APK size budget:** fail if the release APK grows > 10 % in one PR (guards against an accidental map/tile-library import breaking the lightweight posture).
- **Trace-corpus job:** once 8.1 lands, a dedicated job replaying the golden corpus with expectations — separate from `test` so a tuning PR shows exactly which traces shifted.

### 13.6 Local crash journal — no cloud (P1, S)

No crash reporting exists, and the privacy posture forbids Crashlytics. Local-only alternative: `Thread.setDefaultUncaughtExceptionHandler` (chaining to the previous handler) writes timestamp + stack + app version + trip-active flag to a size-capped ring of files in `filesDir/crashlog/`; on next launch, a gentle card "the app had a problem — include the report when you email support?" → share sheet with the text file (user reads exactly what leaves the device — nothing does otherwise). Also captures the service being killed mid-trip (pairs with 2.3's `wasInterrupted`). Surface the ring in the debug console (8.6).

### 13.7 F-Droid distribution + reproducible builds (P3, M)

The app is open source (LICENSE in repo), local-only, and — after 9.7 — network-incapable: the exact profile the F-Droid audience seeks, and privacy-conscious parents overlap heavily with it. Requirements: no proprietary dependencies in the F-Droid flavor — the single blocker is `play-services-location`; add a `foss` flavor swapping `FusedLocationProviderClient` for plain `LocationManager` behind the existing `LocationSource` interface (~a day, and it also de-risks the Play-services dependency generally). Then a reproducible-build recipe (`fdroiddata` metadata, pinned NDK-free build). The Play build keeps fused location.

### 13.8 Edge-to-edge & predictive back audit (P2, S)

targetSdk 35 force-enables edge-to-edge — verify every screen against status/navigation-bar insets (`Modifier.safeDrawingPadding()` where missing, especially the SOS countdown whose Cancel button must never sit under a gesture area), and opt in to predictive back (`android:enableOnBackInvokedCallback`) checking the LiveTripScreen's back-handling (pairs with v1 features.md #10's cancel-trip confirmation).

## 14. Data layer — round 2

### 14.1 Merge the duplicate event models (P2, S)

`Event` and `TelemetryEvent` in `Models.kt` are field-for-field identical; the codebase maps between them for no benefit. Keep one domain `Event` (gaining `durationMs` from v1 1.2 and `mountState`/`frameConfidence` provenance from 10.4/1.4); delete the other. Mechanical refactor, but do it before section-1 work multiplies the mapping sites.

### 14.2 DB indexes + Paging 3 for history (P1, S)

`MetricSample` at 10 Hz persisted (v1 3.2) is ~36 k rows/hour; events and samples are always queried by `(tripId, ts)`. Verify/add composite indexes on both tables (`@Entity(indices = …)` — a migration, so batch with 8.3), and an index on `Trip.startTs` for history ordering. Convert `TripHistoryScreen`'s query to Paging 3 (`PagingSource` from Room) so a year of trips doesn't load into memory to render ten rows. Measure before/after with a 100-trip fixture DB.

### 14.3 Persist jerk (and gradient) in `MetricSample` (P1, S)

The engine computes jerk per frame (`TelemetryEngine.kt:57`) — the spec's headline comfort metric and the input v1 1.9's `jerkPenalty` and 7.4's Comfort Index need — but `MetricSample` doesn't store it, so historical trips can never be re-scored on jerk. Add `jerk: Double` (+ `gradientPct: Double?` from 9.9) columns now, in the same 8.3 migration wave, even before the scorer lands: data collected in the meantime becomes retroactively usable. Storage cost at 10 Hz is trivial next to the existing columns.

### 14.4 Cached trip aggregates (P2, S)

Score breakdown (#26), speed profile (6.4), and comfort index (7.4) all recompute from thousands of samples on every summary open. At finalisation, compute once and store on `Trip`: per-type event counts + severity sums, jerk histogram (8 buckets), speed variance, battery delta (v1 3.1), as a small JSON column (`aggregatesJson`). Summary screens read the JSON; drill-down views still hit samples. Also the exact feature vector 7.8's ML wants — one computation, three consumers.

## Suggested sequencing (merged with v1)

The v1 sequencing stands; round-2 items slot in as follows:

1. **Immediately, alongside v1 "trustworthy engine":** 9.1–9.4, 9.8 (they corrupt the same data the section-1 fixes clean up — fixing detectors on top of doubled frames is wasted work), 9.6, 9.7, and 13.1 (build modernization before the big engine refactor, not during).
2. **With "never lose a trip":** 9.2's session-state integration, 14.1–14.3 batched into the first 8.3 migration, 13.3, 13.5.
3. **With "crash & care":** 9.5 (distraction correctness), 10.1, 10.5.
4. **With "delight":** 12.1, 12.2, 12.7, 11.1 (the angle checker is a standalone win — shippable any sprint), 13.4, 13.6.
5. **v1.1:** 11.3, 11.4, 12.3, 12.4, 12.5, 10.3, 10.4, 14.4.
6. **v1.2 exploration:** 10.2, 11.2, 11.5, 11.6, 12.6, 13.2, 13.7.

Round 2's headline, mirroring v1's: **9.1–9.6 are silent data-corruption and data-loss bugs, not polish** — duplicated pipelines, phantom trips, double-rate frames, device-frame yaw, a distraction detector that punishes navigation, and a backup that restores unreadable data. They cost days total and protect everything else built on top. Among the features, 11.1 (angle checker) and 12.7 (demo mode) deliver the most value per line of code, and 9.7 (provably no network) is the cheapest differentiator in the entire backlog.
