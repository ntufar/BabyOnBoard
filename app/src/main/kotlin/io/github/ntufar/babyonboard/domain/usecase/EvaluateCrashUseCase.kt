package io.github.ntufar.babyonboard.domain.usecase

class EvaluateCrashUseCase {
    fun execute(speedHistory: List<Double>, accelHistory: List<Double>): CrashAssessment {
        if (speedHistory.size < 3 || accelHistory.isEmpty()) {
            return CrashAssessment(isCrashDetected = false, confidence = 0.0f)
        }

        val maxSpeedBefore = speedHistory.dropLast(1).maxOrNull() ?: 0.0
        val vNow = speedHistory.last()
        val peakAccel = accelHistory.maxOrNull() ?: 0.0

        val wasMovingFast = maxSpeedBefore >= 25.0
        val hardImpact = peakAccel >= 4.0
        val speedCollapsed = vNow < 5.0

        val detected = wasMovingFast && hardImpact && speedCollapsed

        return CrashAssessment(
            isCrashDetected = detected,
            confidence = if (detected) 1.0f else 0.0f
        )
    }
}

data class CrashAssessment(
    val isCrashDetected: Boolean,
    val confidence: Float
)
