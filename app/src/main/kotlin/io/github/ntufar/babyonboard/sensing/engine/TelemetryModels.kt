package io.github.ntufar.babyonboard.sensing.engine

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

data class TripScore(
    val score: Int,
    val totalEvents: Int,
    val harshEvents: List<io.github.ntufar.babyonboard.domain.model.TelemetryEvent>
)
