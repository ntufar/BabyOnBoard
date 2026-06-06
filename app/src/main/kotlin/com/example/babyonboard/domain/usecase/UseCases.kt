package com.example.babyonboard.domain.usecase

import com.example.babyonboard.domain.model.Models.*
import com.example.babyonboard.sensing.engine.TelemetryFrame
import com.example.babyonboard.sensing.engine.TelemetryEvent

class StartTripUseCase(private val repository: TripRepository) {
    suspend fun execute(babyMode: Boolean): Trip {
        // Logic to initialize trip and start recording
        // For now, return a dummy trip
        return Trip(
            id = "temp_id",
            startTs = System.currentTimeMillis(),
            endTs = null,
            distanceM = 0.0,
            durationS = 0,
            avgSpeed = 0.0,
            maxSpeed = 0.0,
            score = 100,
            babyMode = babyMode
        )
    }
}

class ProcessSensorWindowUseCase {
    // Logic to process raw sensor data into a TelemetryFrame
}

class DetectEventUseCase(private val engine: TelemetryEngine) {
    fun execute(frame: TelemetryFrame): List<TelemetryEvent> {
        return engine.detectEvents(frame)
    }
}

class ScoreTripUseCase(private val engine: TelemetryEngine) {
    fun execute(events: List<TelemetryEvent>, distanceM: Double): Int {
        return engine.calculateScore(events, distanceM)
    }
}

class EvaluateCrashUseCase {
    fun execute(frame: TelemetryFrame, speedHistory: List<Double>): Boolean {
        // Best-effort crash detection heuristic
        // 1. v_pre >= 25 km/h
        // 2. peak |a| >= 4g
        // 3. speed collapse to 0 for > 10s
        return false // Placeholder
    }
}

class RaiseSosUseCase {
    fun execute(isCrashDetected: Boolean) {
        // Logic to trigger SOS countdown and alerts
    }
}
