package io.github.ntufar.babyonboard.sensing.engine

import io.github.ntufar.babyonboard.domain.model.EventType
import io.github.ntufar.babyonboard.domain.model.TelemetryEvent
import kotlin.math.abs

class TelemetryEngine(private val babyMode: Boolean) {

    private val longAccelThreshold = if (babyMode) 2.5 else 3.5
    private val latAccelThreshold = if (babyMode) 3.0 else 4.0
    private val jerkThreshold = 2.0

    private var prevAccel: Double? = null
    private var prevTimestamp: Long? = null

    fun processRawData(data: RawSensorData): TelemetryFrame {
        val jerk = if (prevAccel != null && prevTimestamp != null) {
            val dt = (data.timestamp - prevTimestamp!!) / 1000.0
            if (dt > 0.0) abs(data.longAccel - prevAccel!!) / dt else 0.0
        } else {
            0.0
        }
        prevAccel = data.longAccel
        prevTimestamp = data.timestamp

        return TelemetryFrame(
            timestamp = data.timestamp,
            lat = data.lat,
            lng = data.lng,
            speed = data.speed,
            longAccel = data.longAccel,
            latAccel = data.latAccel,
            vertAccel = data.vertAccel,
            yawRate = data.yawRate,
            altitude = data.altitude,
            jerk = jerk
        )
    }

    fun detectEvents(frame: TelemetryFrame, tripId: String): List<TelemetryEvent> {
        val events = mutableListOf<TelemetryEvent>()
        val ts = frame.timestamp

        if (frame.longAccel <= -longAccelThreshold && frame.speed > 5.0) {
            events.add(TelemetryEvent(
                id = "${tripId}_brake_$ts", tripId = tripId, ts = ts,
                type = EventType.BRAKE, severity = 0.8f,
                value = abs(frame.longAccel),
                lat = frame.lat, lng = frame.lng, confidence = 0.9f
            ))
        }

        if (frame.longAccel >= longAccelThreshold && frame.speed > 5.0) {
            events.add(TelemetryEvent(
                id = "${tripId}_accel_$ts", tripId = tripId, ts = ts,
                type = EventType.ACCEL, severity = 0.8f,
                value = frame.longAccel,
                lat = frame.lat, lng = frame.lng, confidence = 0.9f
            ))
        }

        if (abs(frame.latAccel) >= latAccelThreshold && abs(frame.yawRate) > 0.1) {
            events.add(TelemetryEvent(
                id = "${tripId}_corner_$ts", tripId = tripId, ts = ts,
                type = EventType.CORNER, severity = 0.7f,
                value = abs(frame.latAccel),
                lat = frame.lat, lng = frame.lng, confidence = 0.8f
            ))
        }

        return events
    }

    fun calculateScore(events: List<TelemetryEvent>, distanceM: Double): Int {
        if (distanceM == 0.0) return 100
        val harshEventsPer100km = (events.size / (distanceM / 100000.0)).toInt()
        val baseScore = 100 - (harshEventsPer100km * 10)
        return baseScore.coerceIn(0, 100)
    }
}
