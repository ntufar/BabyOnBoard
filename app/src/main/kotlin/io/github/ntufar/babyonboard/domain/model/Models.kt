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

data class Settings(
    val autoStart: Boolean,
    val btTriggerDeviceId: String?,
    val dndInTrip: Boolean,
    val reminderEscalation: Int,
    val retentionDays: Int,
    val units: String,
    val emergencyNumber: String
)
