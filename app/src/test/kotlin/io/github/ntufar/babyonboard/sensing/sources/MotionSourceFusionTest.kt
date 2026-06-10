package io.github.ntufar.babyonboard.sensing.sources

import com.google.common.truth.Truth.assertThat
import io.github.ntufar.babyonboard.sensing.engine.RawSensorData
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MotionSourceFusionTest {

    private fun makeMotionSource(): MotionSource =
        MotionSource(RuntimeEnvironment.getApplication())

    private fun setPrivate(target: Any, fieldName: String, value: Any?) {
        val field = generateSequence(target.javaClass) { it.superclass }
            .flatMap { it.declaredFields.asSequence() }
            .first { it.name == fieldName }
        field.isAccessible = true
        field.set(target, value)
    }

    private fun callEmitFused(source: MotionSource) {
        val method = source.javaClass.declaredMethods.first { it.name == "emitFused" }
        method.isAccessible = true
        method.invoke(source)
    }

    private fun sampleAccel() = RawSensorData(
        timestamp = 1000L,
        lat = 0.0, lng = 0.0, speed = 0.0,
        latAccel = 0.1, longAccel = 0.5, vertAccel = -9.8,
        yawRate = 0.0, altitude = 0.0
    )

    @Test
    fun `emitFused emits with zero yawRate when gyro is absent`() = runTest {
        val source = makeMotionSource()
        setPrivate(source, "latestAccel", sampleAccel())
        // latestGyro intentionally left null

        val emission = async { source.sensorFlow.first() }
        yield() // let the async collector subscribe before we emit

        callEmitFused(source)

        val result = emission.await()
        assertThat(result.yawRate).isEqualTo(0.0)
        assertThat(result.longAccel).isWithin(1e-6).of(0.5)
    }

    @Test
    fun `emitFused uses gyro yawRate when gyro data is available`() = runTest {
        val source = makeMotionSource()
        val gyroData = sampleAccel().copy(yawRate = 1.23)
        setPrivate(source, "latestAccel", sampleAccel())
        setPrivate(source, "latestGyro", gyroData)

        val emission = async { source.sensorFlow.first() }
        yield()

        callEmitFused(source)

        val result = emission.await()
        assertThat(result.yawRate).isWithin(1e-6).of(1.23)
    }

    @Test
    fun `emitFused does not emit when latestAccel is null`() = runTest {
        val source = makeMotionSource()
        // both accel and gyro null

        val collected = mutableListOf<RawSensorData>()
        val job = backgroundScope.async { source.sensorFlow.collect { collected.add(it) } }
        yield()

        callEmitFused(source)
        yield()

        assertThat(collected).isEmpty()
        job.cancel()
    }
}
