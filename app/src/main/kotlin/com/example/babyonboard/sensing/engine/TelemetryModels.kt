package com.example.babyonboard.sensing.engine

import com.example.babyonboard.domain.model.EventType

data class RawSensorData(
    val timestamp: Long,
    val lat: Double,
    val lng: Double,
    val speed: Double,
    val latAccel: Double,
    val longAccel: Double,
    val vertAccel: Double,
    val yawRate: Double,
    val altitude: Double,
    val rotationVector: FloatArray? = null
)

data class TelemetryFrame(
    val timestamp: Long,
    val lat: Double,
    val lng: Double,
    val speed: Double,
    val longAccel: Double,
    val latAccel: Double,
    val vertAccel: Double,
    val yawRate: Double,
    val altitude: Double,
    val jerk: Double
)

data class TelemetryEvent(
    val timestamp: Long,
    val type: EventType,
    val severity: Float,
    val value: Double,
    val lat: Double,
    val lng: Double,
    val confidence: Float
)

data class TripScore(
    val score: Int,
    val totalEvents: Int,
    val harshEvents: List<TelemetryEvent>
)
