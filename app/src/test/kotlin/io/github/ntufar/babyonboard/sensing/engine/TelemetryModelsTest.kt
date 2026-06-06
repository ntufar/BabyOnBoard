package io.github.ntufar.babyonboard.sensing.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TelemetryModelsTest {

    @Test
    fun `RawSensorData default rotationVector is null`() {
        val data = RawSensorData(
            timestamp = 1000L,
            lat = 51.5, lng = -0.1, speed = 30.0,
            latAccel = 0.0, longAccel = 0.0, vertAccel = 0.0,
            yawRate = 0.0, altitude = 100.0
        )
        assertThat(data.rotationVector).isNull()
    }

    @Test
    fun `RawSensorData accepts explicit rotationVector`() {
        val rv = floatArrayOf(1.0f, 0.0f, 0.0f, 0.5f)
        val data = RawSensorData(
            timestamp = 1000L,
            lat = 51.5, lng = -0.1, speed = 30.0,
            latAccel = 0.0, longAccel = 0.0, vertAccel = 0.0,
            yawRate = 0.0, altitude = 100.0,
            rotationVector = rv
        )
        assertThat(data.rotationVector).isEqualTo(rv)
    }

    @Test
    fun `TelemetryFrame contains all processed fields`() {
        val frame = TelemetryFrame(
            timestamp = 2000L,
            lat = 48.8, lng = 2.3, speed = 50.0,
            longAccel = 0.5, latAccel = 0.3, vertAccel = 9.8,
            yawRate = 0.2, altitude = 50.0, jerk = 2.5
        )
        assertThat(frame.lat).isEqualTo(48.8)
        assertThat(frame.jerk).isEqualTo(2.5)
        assertThat(frame.timestamp).isEqualTo(2000L)
    }

    @Test
    fun `TripScore stores score summary`() {
        val events = listOf(
            io.github.ntufar.babyonboard.domain.model.TelemetryEvent(
                id = "e1", tripId = "t1", ts = 1000L,
                type = io.github.ntufar.babyonboard.domain.model.EventType.BRAKE,
                severity = 0.8f, value = 4.0,
                lat = 0.0, lng = 0.0, confidence = 0.9f
            )
        )
        val score = TripScore(score = 85, totalEvents = 1, harshEvents = events)
        assertThat(score.score).isEqualTo(85)
        assertThat(score.totalEvents).isEqualTo(1)
        assertThat(score.harshEvents).hasSize(1)
    }
}
