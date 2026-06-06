package com.example.babyonboard.sensing.engine

import com.example.babyonboard.domain.model.EventType
import kotlin.math.abs

class TelemetryEngine(private val babyMode: Boolean) {

    private val longAccelThreshold = if (babyMode) 2.5 else 3.5
    private val latAccelThreshold = if (babyMode) 3.0 else 4.0
    private val jerkThreshold = 2.0 // Example

    fun processRawData(data: RawSensorData): TelemetryFrame {
        // 1. Frame Transform (Simplified for MVP: assuming data is already somewhat aligned or using raw for now)
        // In a real implementation, this would use rotation matrices from rotation vector.
        
        // 2. Filter & Compute Jerk
        // For MVP, jerk = d(accel)/dt. We'll need a buffer for this.
        val jerk = 0.0 // Placeholder for actual jerk calculation over a window

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

    fun detectEvents(frame: TelemetryFrame): List<TelemetryEvent> {
        val events = mutableListOf<TelemetryEvent>()
        
        // Hard Braking
        if (frame.longAccel <= -longAccelThreshold && frame.speed > 5.0) {
            events.add(TelemetryEvent(
                frame.timestamp,
                EventType.BRAKE,
                0.8f,
                abs(frame.longAccel),
                frame.lat,
                frame.lng,
                0.9f
            ))
        }

        // Hard Acceleration
        if (frame.longAccel >= longAccelThreshold && frame.speed > 5.0) {
            events.add(TelemetryEvent(
                frame.timestamp,
                EventType.ACCEL,
                0.8f,
                frame.longAccel,
                frame.lat,
                frame.lng,
                0.9f
            ))
        }

        // Hard Cornering
        if (abs(frame.latAccel) >= latAccelThreshold && abs(frame.yawRate) > 0.1) {
            events.add(TelemetryEvent(
                frame.timestamp,
                EventType.CORNER,
                0.7f,
                abs(frame.latAccel),
                frame.lat,
                frame.lng,
                0.8f
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
