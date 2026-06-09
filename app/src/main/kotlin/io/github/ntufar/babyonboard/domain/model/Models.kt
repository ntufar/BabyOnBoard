package io.github.ntufar.babyonboard.domain.model

import java.time.Instant

data class Trip(
    val id: String,
    val startTs: Long,
    val endTs: Long?,
    val distanceM: Double,
    val durationS: Long,
    val avgSpeed: Double,
    val maxSpeed: Double,
    val score: Int,
    val babyMode: Boolean,
    val routeRef: String? = null
)

data class Event(
    val id: String,
    val tripId: String,
    val ts: Long,
    val type: EventType,
    val severity: Float,
    val value: Double,
    val lat: Double,
    val lng: Double,
    val confidence: Float
)

data class TelemetryEvent(
    val id: String,
    val tripId: String,
    val ts: Long,
    val type: EventType,
    val severity: Float,
    val value: Double,
    val lat: Double,
    val lng: Double,
    val confidence: Float
)

enum class EventType {
    BRAKE, ACCEL, CORNER, SWERVE, ROUGH, SPEED, PHONE_USE, CRASH
}

data class MetricSample(
    val tripId: String,
    val ts: Long,
    val speed: Double,
    val longAccel: Double,
    val latAccel: Double,
    val vertAccel: Double,
    val yawRate: Double,
    val altitude: Double
)

data class Contact(
    val id: String,
    val name: String,
    val phone: String,
    val role: ContactRole,
    val consentTs: Long
)

enum class ContactRole {
    EMERGENCY, ARRIVAL
}

data class Geofence(
    val id: String,
    val label: String,
    val lat: Double,
    val lng: Double,
    val radius: Double,
    val purpose: GeofencePurpose
)

enum class GeofencePurpose {
    ARRIVAL, MODE_TRIGGER
}

enum class SensitivityMode {
    CAR, BUS, TRAIN, WALKING;

    val minSpeedMs: Double get() = when (this) {
        CAR     -> 5.0
        BUS     -> 2.0
        TRAIN   -> 0.5
        WALKING -> 0.0
    }

    val longAccelThreshold: Double get() = when (this) {
        CAR     -> 3.5
        BUS     -> 2.5
        TRAIN   -> 2.0
        WALKING -> 1.5
    }

    val latAccelThreshold: Double get() = when (this) {
        CAR     -> 4.0
        BUS     -> 3.0
        TRAIN   -> 2.5
        WALKING -> 1.5
    }

    val roughnessThreshold: Double get() = when (this) {
        CAR     -> 2.0
        BUS     -> 1.5
        TRAIN   -> 1.0
        WALKING -> 0.7
    }
}

data class Settings(
    val autoStart: Boolean,
    val btTriggerDeviceId: String?,
    val dndInTrip: Boolean,
    val reminderEscalation: Int,
    val retentionDays: Int,
    val units: String,
    val emergencyNumber: String,
    val sensitivity: SensitivityMode = SensitivityMode.CAR
)
