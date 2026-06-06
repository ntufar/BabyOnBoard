package com.example.babyonboard.data.model

import androidx.room.Entity
import androidx.room.Id

@Entity(tableName = "trips")
data class TripEntity(
    @Id val id: String,
    val startTs: Long,
    val endTs: Long?,
    val distanceM: Double,
    val durationS: Long,
    val avgSpeed: Double,
    val maxSpeed: Double,
    val score: Int,
    val babyMode: Boolean,
    val routeRef: String?
)

@Entity(tableName = "events", foreignKeys = [
    androidx.room.ForeignKey(
        entity = TripEntity::class,
        parentColumns = ["id"],
        childColumns = ["tripId"],
        onDelete = androidx.room.ForeignKey.CASCADE
    )
])
data class EventEntity(
    @Id val id: String,
    val tripId: String,
    val ts: Long,
    val type: String, // Store as String for simplicity or use TypeConverter
    val severity: Float,
    val value: Double,
    val lat: Double,
    val lng: Double,
    val confidence: Float
)

@Entity(tableName = "metric_samples", foreignKeys = [
    androidx.room.ForeignKey(
        entity = TripEntity::class,
        parentColumns = ["id"],
        childColumns = ["tripId"],
        onDelete = androidx.room.ForeignKey.CASCADE
    )
])
data class MetricSampleEntity(
    @Id val id: String, // Need an ID for MetricSample too
    val tripId: String,
    val ts: Long,
    val speed: Double,
    val longAccel: Double,
    val latAccel: Double,
    val vertAccel: Double,
    val yawRate: Double,
    val altitude: Double
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @Id val id: String,
    val name: String,
    val phone: String,
    val role: String,
    val consentTs: Long
)

@Entity(tableName = "geofences")
data class GeofenceEntity(
    @Id val id: String,
    val label: String,
    val lat: Double,
    val lng: Double,
    val radius: Double,
    val purpose: String
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: String = "default_settings",
    val autoStart: Boolean,
    val btTriggerDeviceId: String?,
    val dndInTrip: Boolean,
    val reminderEscalation: Int,
    val retentionDays: Int,
    val units: String,
    val emergencyNumber: String
)
