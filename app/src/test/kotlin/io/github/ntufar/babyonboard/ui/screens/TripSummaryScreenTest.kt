package io.github.ntufar.babyonboard.ui.screens

import com.google.common.truth.Truth.assertThat
import io.github.ntufar.babyonboard.domain.model.EventType
import io.github.ntufar.babyonboard.domain.model.TelemetryEvent
import org.junit.Test

class TripSummaryScreenTest {

    private fun event(type: EventType, severity: Float = 0.8f) = TelemetryEvent(
        id = "${type.name}_${System.nanoTime()}",
        tripId = "trip1",
        ts = 0L,
        type = type,
        severity = severity,
        value = 1.0,
        lat = 0.0,
        lng = 0.0,
        confidence = 0.9f
    )

    @Test
    fun `empty events returns empty breakdown`() {
        val result = computeScoreBreakdown(emptyList(), score = 100)
        assertThat(result).isEmpty()
    }

    @Test
    fun `no harsh events returns empty breakdown`() {
        val events = listOf(
            event(EventType.ROUGH, severity = 0.5f),
            event(EventType.ROUGH, severity = 0.3f)
        )
        val result = computeScoreBreakdown(events, score = 100)
        assertThat(result).isEmpty()
    }

    @Test
    fun `perfect score with harsh events returns empty breakdown`() {
        val events = listOf(event(EventType.BRAKE))
        val result = computeScoreBreakdown(events, score = 100)
        assertThat(result).isEmpty()
    }

    @Test
    fun `single event type returns one entry with full deduction`() {
        val events = listOf(
            event(EventType.BRAKE),
            event(EventType.BRAKE),
            event(EventType.BRAKE)
        )
        val result = computeScoreBreakdown(events, score = 70)
        assertThat(result).hasSize(1)
        assertThat(result[0].type).isEqualTo(EventType.BRAKE)
        assertThat(result[0].count).isEqualTo(3)
        assertThat(result[0].deductionPts).isEqualTo(30)
    }

    @Test
    fun `multiple event types split deduction proportionally`() {
        val events = listOf(
            event(EventType.BRAKE),
            event(EventType.BRAKE),
            event(EventType.ACCEL)
        )
        // score = 40, totalDeduction = 60, harshCount = 3
        // BRAKE: 2/3 * 60 = 40, ACCEL: 1/3 * 60 = 20
        val result = computeScoreBreakdown(events, score = 40)
        val brakeEntry = result.find { it.type == EventType.BRAKE }
        val accelEntry = result.find { it.type == EventType.ACCEL }

        assertThat(brakeEntry).isNotNull()
        assertThat(brakeEntry!!.count).isEqualTo(2)
        assertThat(brakeEntry.deductionPts).isEqualTo(40)

        assertThat(accelEntry).isNotNull()
        assertThat(accelEntry!!.count).isEqualTo(1)
        assertThat(accelEntry.deductionPts).isEqualTo(20)
    }

    @Test
    fun `result is sorted by deduction descending`() {
        val events = listOf(
            event(EventType.ACCEL),
            event(EventType.BRAKE),
            event(EventType.BRAKE),
            event(EventType.BRAKE)
        )
        val result = computeScoreBreakdown(events, score = 0)
        assertThat(result.first().type).isEqualTo(EventType.BRAKE)
        assertThat(result.last().type).isEqualTo(EventType.ACCEL)
    }

    @Test
    fun `score clamped at 0 uses full 100 point deduction`() {
        val events = listOf(event(EventType.BRAKE))
        val result = computeScoreBreakdown(events, score = 0)
        assertThat(result[0].deductionPts).isEqualTo(100)
    }

    @Test
    fun `non-harsh events are excluded from breakdown`() {
        val events = listOf(
            event(EventType.BRAKE, severity = 0.8f),
            event(EventType.ROUGH, severity = 0.5f)
        )
        val result = computeScoreBreakdown(events, score = 50)
        assertThat(result.none { it.type == EventType.ROUGH }).isTrue()
        assertThat(result.any { it.type == EventType.BRAKE }).isTrue()
    }
}
