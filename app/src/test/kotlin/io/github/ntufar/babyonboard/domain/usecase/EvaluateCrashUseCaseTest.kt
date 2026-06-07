package io.github.ntufar.babyonboard.domain.usecase

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EvaluateCrashUseCaseTest {

    private val useCase = EvaluateCrashUseCase()

    @Test
    fun `no crash when speed history is too short`() {
        val result = useCase.execute(
            speedHistory = listOf(30.0),
            accelHistory = listOf(8.0)
        )
        assertThat(result.isCrashDetected).isFalse()
        assertThat(result.confidence).isEqualTo(0.0f)
    }

    @Test
    fun `no crash when accel history is empty`() {
        val result = useCase.execute(
            speedHistory = listOf(30.0, 28.0, 0.0),
            accelHistory = emptyList()
        )
        assertThat(result.isCrashDetected).isFalse()
    }

    @Test
    fun `no crash when never moving fast`() {
        val result = useCase.execute(
            speedHistory = listOf(10.0, 8.0, 0.0),
            accelHistory = listOf(0.5, 5.0, 8.0)
        )
        assertThat(result.isCrashDetected).isFalse()
    }

    @Test
    fun `no crash when no hard impact`() {
        val result = useCase.execute(
            speedHistory = listOf(50.0, 40.0, 0.0),
            accelHistory = listOf(0.5, 1.0, 2.0)
        )
        assertThat(result.isCrashDetected).isFalse()
    }

    @Test
    fun `no crash when speed does not collapse`() {
        val result = useCase.execute(
            speedHistory = listOf(50.0, 48.0, 45.0),
            accelHistory = listOf(0.5, 5.0, 8.0)
        )
        assertThat(result.isCrashDetected).isFalse()
    }

    @Test
    fun `crash detected when all three conditions are met`() {
        val result = useCase.execute(
            speedHistory = listOf(60.0, 55.0, 3.0),
            accelHistory = listOf(6.0)
        )
        assertThat(result.isCrashDetected).isTrue()
        assertThat(result.confidence).isEqualTo(1.0f)
    }

    @Test
    fun `crash detected with multi-sample histories`() {
        val result = useCase.execute(
            speedHistory = listOf(50.0, 48.0, 45.0, 40.0, 30.0, 20.0, 10.0, 4.0, 2.0, 0.0),
            accelHistory = listOf(0.5, 1.0, 5.0, 8.0, 6.0, 2.0)
        )
        assertThat(result.isCrashDetected).isTrue()
        assertThat(result.confidence).isEqualTo(1.0f)
    }

    @Test
    fun `no crash with long stable speed history`() {
        val speedHistory = (1..20).map { 50.0 }
        val accelHistory = listOf(0.0, 0.0, 0.5, 1.0)
        val result = useCase.execute(speedHistory, accelHistory)
        assertThat(result.isCrashDetected).isFalse()
    }

    @Test
    fun `max speed before last element determines precondition`() {
        val result = useCase.execute(
            speedHistory = listOf(10.0, 15.0, 20.0, 25.0, 30.0, 2.0),
            accelHistory = listOf(1.0, 6.0)
        )
        assertThat(result.isCrashDetected).isTrue()
    }

    @Test
    fun `last speed determines collapse regardless of prior low speeds`() {
        val result = useCase.execute(
            speedHistory = listOf(5.0, 5.0, 5.0, 5.0, 5.0),
            accelHistory = listOf(6.0)
        )
        assertThat(result.isCrashDetected).isFalse()
    }

    @Test
    fun `confidence is zero when crash not detected`() {
        val result = useCase.execute(
            speedHistory = listOf(30.0, 28.0, 26.0),
            accelHistory = listOf(0.0, 0.5, 1.0)
        )
        assertThat(result.isCrashDetected).isFalse()
        assertThat(result.confidence).isEqualTo(0.0f)
    }

    @Test
    fun `vPre uses speed just before last element`() {
        val result = useCase.execute(
            speedHistory = listOf(5.0, 25.0, 2.0),
            accelHistory = listOf(4.0)
        )
        // maxBeforeLast = max(5, 25) = 25 >= 25 ✓
        // vNow = 2.0 < 5.0 ✓
        // peakAccel = 4.0 >= 4.0 ✓
        assertThat(result.isCrashDetected).isTrue()
    }
}
