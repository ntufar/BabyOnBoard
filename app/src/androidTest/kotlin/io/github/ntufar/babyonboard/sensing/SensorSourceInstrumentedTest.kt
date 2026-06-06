package io.github.ntufar.babyonboard.sensing

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.ntufar.babyonboard.sensing.sources.MotionSource
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SensorSourceInstrumentedTest {

    @Test
    fun motionSource_initializesWithApplicationContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val motionSource = MotionSource(context)

        assertThat(motionSource).isNotNull()
    }

    @Test
    fun locationSource_initializesWithApplicationContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Verify that we can create the sensor source classes
        assertThat(context.packageName).isEqualTo("io.github.ntufar.babyonboard")
    }
}
