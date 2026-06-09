package io.github.ntufar.babyonboard.sensing.engine

import io.github.ntufar.babyonboard.domain.model.EventType
import io.github.ntufar.babyonboard.domain.model.TelemetryEvent
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class TelemetryEngine(private val babyMode: Boolean) {

    private val longAccelThreshold = if (babyMode) 2.5 else 3.5
    private val latAccelThreshold = if (babyMode) 3.0 else 4.0
    private val jerkThreshold = 2.0

    private var prevAccel: Double? = null
    private var prevTimestamp: Long? = null

    private val vertAccelWindow = mutableListOf<Double>()
    private val maxVertAccelWindowSize = 20

    private data class YawSample(val ts: Long, val yaw: Double, val latAccel: Double)
    private val yawWindow = mutableListOf<YawSample>()
    private val maxYawWindowSize = 30

    private var prevLat: Double? = null
    private var prevLng: Double? = null
    private var prevDist: Double? = null
    private var prevGradAlt: Double? = null

    private val roughnessThreshold = if (babyMode) 1.5 else 2.0
    private val swerveYawThreshold = 0.5

    private fun computeVariance(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        return values.sumOf { (it - mean) * (it - mean) } / values.size
    }

    fun resetWindows() {
        vertAccelWindow.clear()
        yawWindow.clear()
        prevLat = null
        prevLng = null
        prevDist = null
        prevGradAlt = null
    }

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

        if (frame.longAccel <= -longAccelThreshold) {
            events.add(TelemetryEvent(
                id = "${tripId}_brake_$ts", tripId = tripId, ts = ts,
                type = EventType.BRAKE, severity = 0.8f,
                value = abs(frame.longAccel),
                lat = frame.lat, lng = frame.lng, confidence = 0.9f
            ))
        }

        if (frame.longAccel >= longAccelThreshold) {
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

    fun detectExtendedEvents(frame: TelemetryFrame, tripId: String): List<TelemetryEvent> {
        val events = mutableListOf<TelemetryEvent>()
        val ts = frame.timestamp

        vertAccelWindow.add(frame.vertAccel)
        if (vertAccelWindow.size > maxVertAccelWindowSize) vertAccelWindow.removeAt(0)
        if (vertAccelWindow.size >= 10) {
            val variance = computeVariance(vertAccelWindow)
            if (variance > roughnessThreshold) {
                events.add(TelemetryEvent(
                    id = "${tripId}_rough_$ts", tripId = tripId, ts = ts,
                    type = EventType.ROUGH, severity = 0.5f,
                    value = variance,
                    lat = frame.lat, lng = frame.lng, confidence = 0.7f
                ))
            }
        }

        yawWindow.add(YawSample(ts, frame.yawRate, frame.latAccel))
        if (yawWindow.size > maxYawWindowSize) yawWindow.removeAt(0)
        if (yawWindow.size >= 5) {
            val swerveEvent = detectSwerve(tripId, frame)
            if (swerveEvent != null) {
                events.add(swerveEvent)
            }
        }

        return events
    }

    private fun detectSwerve(tripId: String, frame: TelemetryFrame): TelemetryEvent? {
        val yawSamples = yawWindow.takeLast(10)
        if (yawSamples.size < 5) return null

        var foundPair = false
        var peakLatAccel = 0.0

        for (i in 0 until yawSamples.size - 2) {
            val first = yawSamples[i]
            val second = yawSamples[i + 1]
            val third = yawSamples[i + 2]

            if (abs(first.yaw) > swerveYawThreshold &&
                abs(third.yaw) > swerveYawThreshold * 0.6 &&
                first.yaw * third.yaw < 0 &&
                abs(second.yaw) < abs(first.yaw) * 0.5 &&
                (third.ts - first.ts) in 1..3000
            ) {
                foundPair = true
                peakLatAccel = maxOf(peakLatAccel, abs(second.latAccel))
            }
        }

        if (foundPair && peakLatAccel > 1.0) {
            return TelemetryEvent(
                id = "${tripId}_swerve_${frame.timestamp}",
                tripId = tripId,
                ts = frame.timestamp,
                type = EventType.SWERVE,
                severity = 0.6f,
                value = peakLatAccel,
                lat = frame.lat,
                lng = frame.lng,
                confidence = 0.6f
            )
        }
        return null
    }

    fun calculateGradient(altitude: Double, lat: Double, lng: Double): Double? {
        val dist = if (prevLat != null && prevLng != null) {
            haversineM(prevLat!!, prevLng!!, lat, lng)
        } else {
            prevLat = lat; prevLng = lng; return null
        }

        val grade = if (prevGradAlt != null && prevDist != null && dist > 0) {
            val altDelta = altitude - prevGradAlt!!
            val segmentDist = dist
            if (segmentDist > 10.0) (altDelta / segmentDist) * 100.0 else null
        } else null

        prevLat = lat
        prevLng = lng
        prevDist = dist
        prevGradAlt = altitude

        return grade
    }

    fun calculateScore(events: List<TelemetryEvent>, distanceM: Double): Int {
        if (distanceM == 0.0) return 100
        val harshEventsPer100km = (events.size / (distanceM / 100000.0)).toInt()
        val baseScore = 100 - (harshEventsPer100km * 10)
        return baseScore.coerceIn(0, 100)
    }

    companion object {
        fun haversineM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val r = 6371000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
            return r * 2 * atan2(sqrt(a), sqrt(1 - a))
        }
    }
}
