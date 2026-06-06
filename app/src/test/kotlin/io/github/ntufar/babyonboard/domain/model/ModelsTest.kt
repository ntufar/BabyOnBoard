package io.github.ntufar.babyonboard.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ModelsTest {

    // ─── Trip ─────────────────────────────────────────────────────────────

    @Test
    fun `Trip default routeRef is null`() {
        val trip = Trip(
            id = "1", startTs = 0L, endTs = null,
            distanceM = 0.0, durationS = 0,
            avgSpeed = 0.0, maxSpeed = 0.0, score = 100,
            babyMode = false
        )
        assertThat(trip.routeRef).isNull()
    }

    @Test
    fun `Trip copy with endTs`() {
        val trip = Trip(
            id = "1", startTs = 1000L, endTs = null,
            distanceM = 5000.0, durationS = 300,
            avgSpeed = 60.0, maxSpeed = 80.0, score = 85,
            babyMode = true
        )
        val ended = trip.copy(endTs = 4000L)
        assertThat(ended.endTs).isEqualTo(4000L)
    }

    // ─── EventType ────────────────────────────────────────────────────────

    @Test
    fun `EventType contains all expected types`() {
        val types = EventType.values()
        assertThat(types).asList().containsExactly(
            EventType.BRAKE,
            EventType.ACCEL,
            EventType.CORNER,
            EventType.SWERVE,
            EventType.ROUGH,
            EventType.SPEED,
            EventType.PHONE_USE,
            EventType.CRASH
        )
    }

    // ─── Settings ─────────────────────────────────────────────────────────

    @Test
    fun `Settings defaults`() {
        val settings = Settings(
            autoStart = false,
            btTriggerDeviceId = null,
            dndInTrip = true,
            reminderEscalation = 1,
            retentionDays = 30,
            units = "km",
            emergencyNumber = "112"
        )
        assertThat(settings.autoStart).isFalse()
        assertThat(settings.btTriggerDeviceId).isNull()
        assertThat(settings.dndInTrip).isTrue()
        assertThat(settings.emergencyNumber).isEqualTo("112")
    }

    // ─── Contact ──────────────────────────────────────────────────────────

    @Test
    fun `Contact can be EMERGENCY or ARRIVAL`() {
        val emergency = Contact("1", "Alice", "+123", ContactRole.EMERGENCY, 1000L)
        val arrival = Contact("2", "Bob", "+456", ContactRole.ARRIVAL, 2000L)

        assertThat(emergency.role).isEqualTo(ContactRole.EMERGENCY)
        assertThat(arrival.role).isEqualTo(ContactRole.ARRIVAL)
    }

    // ─── Geofence ─────────────────────────────────────────────────────────

    @Test
    fun `Geofence has purpose ARRIVAL or MODE_TRIGGER`() {
        val arrival = Geofence("1", "Home", 0.0, 0.0, 100.0, GeofencePurpose.ARRIVAL)
        val trigger = Geofence("2", "Office", 1.0, 2.0, 50.0, GeofencePurpose.MODE_TRIGGER)

        assertThat(arrival.purpose).isEqualTo(GeofencePurpose.ARRIVAL)
        assertThat(trigger.purpose).isEqualTo(GeofencePurpose.MODE_TRIGGER)
    }

    // ─── TelemetryEvent ──────────────────────────────────────────────────

    @Test
    fun `TelemetryEvent stores event metadata`() {
        val event = TelemetryEvent(
            id = "e1", tripId = "t1", ts = 5000L,
            type = EventType.BRAKE, severity = 0.8f,
            value = 4.5, lat = 51.5, lng = -0.1,
            confidence = 0.9f
        )
        assertThat(event.type).isEqualTo(EventType.BRAKE)
        assertThat(event.severity).isEqualTo(0.8f)
        assertThat(event.value).isEqualTo(4.5)
    }

    // ─── MetricSample ────────────────────────────────────────────────────

    @Test
    fun `MetricSample stores telemetry snapshot`() {
        val sample = MetricSample(
            tripId = "t1", ts = 1000L,
            speed = 50.0, longAccel = 0.5, latAccel = 0.3,
            vertAccel = 9.8, yawRate = 0.1, altitude = 100.0
        )
        assertThat(sample.speed).isEqualTo(50.0)
        assertThat(sample.altitude).isEqualTo(100.0)
    }
}
