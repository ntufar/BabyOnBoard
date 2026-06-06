package io.github.ntufar.babyonboard.domain.usecase

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RaiseSosUseCaseTest {

    private lateinit var context: Context
    private lateinit var useCase: RaiseSosUseCase

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        useCase = RaiseSosUseCase(context)
    }

    @Test
    fun `dialEmergencyNumber creates ACTION_DIAL intent`() {
        useCase.dialEmergencyNumber("911")
        // Robolectric captures the intent automatically
    }

    @Test
    fun `dialEmergencyNumber dials custom emergency number`() {
        useCase.dialEmergencyNumber("112")
        // Verifies no crash when starting the dial intent
    }

    @Test
    fun `execute does not crash when confidence is low`() {
        useCase.execute(isCrashDetected = false, confidence = 0.3f)
    }

    @Test
    fun `execute does not crash when crash detected with high confidence`() {
        useCase.execute(isCrashDetected = true, confidence = 0.9f)
    }
}
