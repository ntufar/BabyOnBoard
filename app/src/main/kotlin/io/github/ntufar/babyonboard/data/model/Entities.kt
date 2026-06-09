package io.github.ntufar.babyonboard.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
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
], indices = [androidx.room.Index("tripId")])
data class EventEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val ts: Long,
    val type: String,
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
], indices = [androidx.room.Index("tripId")])
data class MetricSampleEntity(
    @PrimaryKey val id: String,
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
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val role: String,
    val consentTs: Long
)

@Entity(tableName = "geofences")
data class GeofenceEntity(
    @PrimaryKey val id: String,
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
    val emergencyNumber: String,
    @androidx.room.ColumnInfo(defaultValue = "CAR") val sensitivity: String = "CAR"
)
