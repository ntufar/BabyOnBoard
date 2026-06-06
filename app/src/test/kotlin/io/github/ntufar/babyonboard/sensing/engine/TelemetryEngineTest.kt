package io.github.ntufar.babyonboard.sensing.engine

import com.google.common.truth.Truth.assertThat
import io.github.ntufar.babyonboard.domain.model.EventType
import org.junit.Before
import org.junit.Test

class TelemetryEngineTest {

    private lateinit var normalEngine: TelemetryEngine
    private lateinit var babyModeEngine: TelemetryEngine

    @Before
    fun setup() {
        normalEngine = TelemetryEngine(babyMode = false)
        babyModeEngine = TelemetryEngine(babyMode = true)
    }

    // ─── RawSensorData emulation helpers ──────────────────────────────────

    private fun rawData(
        ts: Long = System.currentTimeMillis(),
        longAccel: Double = 0.0,
        latAccel: Double = 0.0,
        vertAccel: Double = 0.0,
        yawRate: Double = 0.0,
        speed: Double = 0.0,
        lat: Double = 0.0,
        lng: Double = 0.0,
        altitude: Double = 0.0
    ): RawSensorData = RawSensorData(
        timestamp = ts, lat = lat, lng = lng, speed = speed,
        latAccel = latAccel, longAccel = longAccel, vertAccel = vertAccel,
        yawRate = yawRate, altitude = altitude
    )

    // ─── processRawData ───────────────────────────────────────────────────

    @Test
    fun `processRawData produces TelemetryFrame with correct fields`() {
        val data = rawData(longAccel = 1.5, latAccel = 0.5, speed = 30.0)

        val frame = normalEngine.processRawData(data)

        assertThat(frame.longAccel).isEqualTo(1.5)
        assertThat(frame.latAccel).isEqualTo(0.5)
        assertThat(frame.speed).isEqualTo(30.0)
        assertThat(frame.jerk).isEqualTo(0.0) // first call, no prev sample
    }

    @Test
    fun `processRawData computes jerk between consecutive samples`() {
        val t0 = 0L
        val t1 = 200L // 200 ms later

        normalEngine.processRawData(rawData(ts = t0, longAccel = 0.0))
        val frame = normalEngine.processRawData(rawData(ts = t1, longAccel = 3.0))

        // jerk = |3.0 - 0.0| / (0.2) = 15.0
        assertThat(frame.jerk).isEqualTo(15.0)
    }

    @Test
    fun `processRawData uses dt in seconds for jerk calculation`() {
        normalEngine.processRawData(rawData(ts = 0L, longAccel = 1.0))
        val frame = normalEngine.processRawData(rawData(ts = 1000L, longAccel = 4.0))

        // jerk = |4.0 - 1.0| / 1.0 = 3.0
        assertThat(frame.jerk).isEqualTo(3.0)
    }

    @Test
    fun `processRawData copies altitude and yawRate`() {
        val data = rawData(altitude = 150.0, yawRate = 0.5)
        val frame = normalEngine.processRawData(data)

        assertThat(frame.altitude).isEqualTo(150.0)
        assertThat(frame.yawRate).isEqualTo(0.5)
    }

    // ─── detectEvents — BRAKE ─────────────────────────────────────────────

    @Test
    fun `detectEvents triggers BRAKE when longAccel below -2_5 in babyMode`() {
        val frame = TelemetryFrame(
            timestamp = 1000L, lat = 0.0, lng = 0.0, speed = 30.0,
            longAccel = -3.0, latAccel = 0.0, vertAccel = 0.0,
            yawRate = 0.0, altitude = 0.0, jerk = 0.0
        )
        val events = babyModeEngine.detectEvents(frame, "trip_1")
        assertThat(events).hasSize(1)
        assertThat(events[0].type).isEqualTo(EventType.BRAKE)
    }

    @Test
    fun `detectEvents does NOT trigger BRAKE in normal mode at 3_0g`() {
        val frame = TelemetryFrame(
            timestamp = 1000L, lat = 0.0, lng = 0.0, speed = 30.0,
            longAccel = -3.0, latAccel = 0.0, vertAccel = 0.0,
            yawRate = 0.0, altitude = 0.0, jerk = 0.0
        )
        val events = normalEngine.detectEvents(frame, "trip_1")
        // Normal threshold is 3.5, so -3.0 should NOT trigger
        assertThat(events).isEmpty()
    }

    @Test
    fun `detectEvents triggers BRAKE in normal mode at 4_0g`() {
        val frame = TelemetryFrame(
            timestamp = 1000L, lat = 0.0, lng = 0.0, speed = 30.0,
            longAccel = -4.0, latAccel = 0.0, vertAccel = 0.0,
            yawRate = 0.0, altitude = 0.0, jerk = 0.0
        )
        val events = normalEngine.detectEvents(frame, "trip_1")
        assertThat(events).hasSize(1)
        assertThat(events[0].type).isEqualTo(EventType.BRAKE)
        assertThat(events[0].value).isEqualTo(4.0)
        assertThat(events[0].confidence).isEqualTo(0.9f)
        assertThat(events[0].severity).isEqualTo(0.8f)
    }

    @Test
    fun `detectEvents does not trigger BRAKE at low speed`() {
        val frame = TelemetryFrame(
            timestamp = 1000L, lat = 0.0, lng = 0.0, speed = 3.0,
            longAccel = -4.0, latAccel = 0.0, vertAccel = 0.0,
            yawRate = 0.0, altitude = 0.0, jerk = 0.0
        )
        val events = normalEngine.detectEvents(frame, "trip_1")
        assertThat(events).isEmpty()
    }

    // ─── detectEvents — ACCEL ─────────────────────────────────────────────

    @Test
    fun `detectEvents triggers ACCEL when longAccel above threshold`() {
        val frame = TelemetryFrame(
            timestamp = 1000L, lat = 0.0, lng = 0.0, speed = 30.0,
            longAccel = 4.0, latAccel = 0.0, vertAccel = 0.0,
            yawRate = 0.0, altitude = 0.0, jerk = 0.0
        )
        val events = normalEngine.detectEvents(frame, "trip_1")
        assertThat(events).hasSize(1)
        assertThat(events[0].type).isEqualTo(EventType.ACCEL)
    }

    @Test
    fun `detectEvents triggers ACCEL in babyMode at lower threshold`() {
        val frame = TelemetryFrame(
            timestamp = 1000L, lat = 0.0, lng = 0.0, speed = 30.0,
            longAccel = 2.6, latAccel = 0.0, vertAccel = 0.0,
            yawRate = 0.0, altitude = 0.0, jerk = 0.0
        )
        val events = babyModeEngine.detectEvents(frame, "trip_1")
        assertThat(events).hasSize(1)
        assertThat(events[0].type).isEqualTo(EventType.ACCEL)
    }

    @Test
    fun `detectEvents does not trigger ACCEL below threshold`() {
        val frame = TelemetryFrame(
            timestamp = 1000L, lat = 0.0, lng = 0.0, speed = 30.0,
            longAccel = 2.0, latAccel = 0.0, vertAccel = 0.0,
            yawRate = 0.0, altitude = 0.0, jerk = 0.0
        )
        val events = normalEngine.detectEvents(frame, "trip_1")
        assertThat(events).isEmpty()
    }

    // ─── detectEvents — CORNER ────────────────────────────────────────────

    @Test
    fun `detectEvents triggers CORNER when latAccel above threshold`() {
        val frame = TelemetryFrame(
            timestamp = 1000L, lat = 0.0, lng = 0.0, speed = 30.0,
            longAccel = 0.0, latAccel = 4.5, vertAccel = 0.0,
            yawRate = 0.5, altitude = 0.0, jerk = 0.0
        )
        val events = normalEngine.detectEvents(frame, "trip_1")
        assertThat(events).hasSize(1)
        assertThat(events[0].type).isEqualTo(EventType.CORNER)
    }

    @Test
    fun `detectEvents triggers CORNER in babyMode at lower threshold`() {
        val frame = TelemetryFrame(
            timestamp = 1000L, lat = 0.0, lng = 0.0, speed = 30.0,
            longAccel = 0.0, latAccel = 3.0, vertAccel = 0.0,
            yawRate = 0.5, altitude = 0.0, jerk = 0.0
        )
        val events = babyModeEngine.detectEvents(frame, "trip_1")
        assertThat(events).hasSize(1)
        assertThat(events[0].type).isEqualTo(EventType.CORNER)
    }

    @Test
    fun `detectEvents ignores CORNER when yawRate is near zero`() {
        val frame = TelemetryFrame(
            timestamp = 1000L, lat = 0.0, lng = 0.0, speed = 30.0,
            longAccel = 0.0, latAccel = 4.5, vertAccel = 0.0,
            yawRate = 0.05, altitude = 0.0, jerk = 0.0
        )
        val events = normalEngine.detectEvents(frame, "trip_1")
        assertThat(events).isEmpty()
    }

    // ─── detectEvents — multiple simultaneous ─────────────────────────────

    @Test
    fun `detectEvents fires BRAKE and CORNER when both conditions met`() {
        val frame = TelemetryFrame(
            timestamp = 1000L, lat = 0.0, lng = 0.0, speed = 30.0,
            longAccel = -4.0, latAccel = 4.5, vertAccel = 0.0,
            yawRate = 0.5, altitude = 0.0, jerk = 0.0
        )
        val events = normalEngine.detectEvents(frame, "trip_1")
        assertThat(events).hasSize(2)
        assertThat(events.map { it.type }).containsExactly(EventType.BRAKE, EventType.CORNER)
    }

    // ─── detectEvents — event id format ───────────────────────────────────

    @Test
    fun `detectEvents assigns id with tripId prefix and timestamp`() {
        val frame = TelemetryFrame(
            timestamp = 5000L, lat = 0.0, lng = 0.0, speed = 30.0,
            longAccel = -4.0, latAccel = 0.0, vertAccel = 0.0,
            yawRate = 0.0, altitude = 0.0, jerk = 0.0
        )
        val events = normalEngine.detectEvents(frame, "abc123")
        assertThat(events[0].id).startsWith("abc123_brake_")
    }

    // ─── calculateScore ───────────────────────────────────────────────────

    @Test
    fun `calculateScore returns 100 when distance is zero`() {
        val score = normalEngine.calculateScore(emptyList(), distanceM = 0.0)
        assertThat(score).isEqualTo(100)
    }

    @Test
    fun `calculateScore returns 100 for no events over any distance`() {
        val score = normalEngine.calculateScore(emptyList(), distanceM = 50000.0)
        assertThat(score).isEqualTo(100)
    }

    @Test
    fun `calculateScore deducts 10 per harsh event per 100km`() {
        val events = listOf(
            TelemetryEventStub(severity = 0.8f, value = 4.0),
            TelemetryEventStub(severity = 0.9f, value = 5.0)
        )
        // 2 events over 100km → 2 * 10 = 20 deduction → 80
        val score = normalEngine.calculateScore(events(2), distanceM = 100000.0)
        // events list maps through the stubs
        val actual = normalEngine.calculateScore(
            events.map { it.toTelemetryEvent("t", "t") },
            distanceM = 100000.0
        )
        assertThat(actual).isEqualTo(80)
    }

    @Test
    fun `calculateScore clamps to 0 minimum`() {
        val events = (1..20).map {
            TelemetryEventStub(severity = 0.9f, value = 5.0).toTelemetryEvent("t", "t")
        }
        val score = normalEngine.calculateScore(events, distanceM = 1000.0)
        assertThat(score).isEqualTo(0)
    }

    @Test
    fun `calculateScore returns 100 for events within 100km when none`() {
        val score = normalEngine.calculateScore(emptyList(), distanceM = 50000.0)
        assertThat(score).isEqualTo(100)
    }

    // ─── scores match babyMode thresholds ─────────────────────────────────

    @Test
    fun `babyMode engine triggers more events for same sensor input`() {
        val frame = TelemetryFrame(
            timestamp = 1000L, lat = 0.0, lng = 0.0, speed = 30.0,
            longAccel = -3.0, latAccel = 0.0, vertAccel = 0.0,
            yawRate = 0.0, altitude = 0.0, jerk = 0.0
        )
        val normalEvents = normalEngine.detectEvents(frame, "t1")
        val babyEvents = babyModeEngine.detectEvents(frame, "t1")

        assertThat(normalEvents).isEmpty()
        assertThat(babyEvents).isNotEmpty()
    }
}

/**
 * Helper to create TelemetryEvent-like data for score calculations.
 */
private data class TelemetryEventStub(
    val severity: Float,
    val value: Double,
    val confidence: Float = 0.9f,
    val type: EventType = EventType.BRAKE
) {
    fun toTelemetryEvent(id: String, tripId: String) =
        io.github.ntufar.babyonboard.domain.model.TelemetryEvent(
            id = "${id}_$type",
            tripId = tripId,
            ts = System.currentTimeMillis(),
            type = type,
            severity = severity,
            value = value,
            lat = 0.0,
            lng = 0.0,
            confidence = confidence
        )
}

private fun events(count: Int): List<io.github.ntufar.babyonboard.domain.model.TelemetryEvent> =
    (1..count).map { i ->
        TelemetryEventStub(
            severity = 0.8f,
            value = 4.0
        ).toTelemetryEvent("e$i", "t")
    }
