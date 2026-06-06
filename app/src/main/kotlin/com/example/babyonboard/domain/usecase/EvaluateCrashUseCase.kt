package com.example.babyonboard.domain.usecase

import com.example.babyonboard.domain.model.Models.*
import com.example.babyonboard.sensing.engine.TelemetryFrame
import java.util.*

class EvaluateCrashUseCase {
    fun execute(frame: TelemetryFrame, speedHistory: List<Double>, accelHistory: List<Double>): CrashAssessment {
        val vPre = if (speedHistory.isNotEmpty()) speedHistory.last() else 0.0
        val peakAccel = if (accelHistory.isNotEmpty()) accelHistory.maxOrNull() ?: 0.0
        
        // Check if speed collapsed to ~0 for >= 10s
        // This would need more history than just a few samples
        val speedCollapsed = speedHistory.takeLast(10).all { it < 5.0 } // Simplified

        val metPrecondition = vPre >= 25.0
        val metImpact = peakAccel >= 4.0
        val metPost = speedCollapsed

        val confidence = if (metPrecondition && metImpact && metPost) 1.0f else 0.0f
        
        return CrashAssessment(
            isCrashDetected = metPrecondition && metImpact && metPost,
            confidence = confidence
        )
    }
}

data class CrashAssessment(
    val isCrashDetected: Boolean,
    val confidence: Float
)
