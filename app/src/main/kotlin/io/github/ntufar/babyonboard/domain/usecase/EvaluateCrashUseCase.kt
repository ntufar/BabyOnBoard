package io.github.ntufar.babyonboard.domain.usecase

class EvaluateCrashUseCase {
    fun execute(speedHistory: List<Double>, accelHistory: List<Double>): CrashAssessment {
        val vPre = speedHistory.lastOrNull() ?: 0.0
        val peakAccel = accelHistory.maxOrNull() ?: 0.0

        val speedCollapsed = speedHistory.takeLast(10).all { it < 5.0 }

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
