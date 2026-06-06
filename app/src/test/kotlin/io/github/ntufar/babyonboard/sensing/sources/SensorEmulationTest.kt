package io.github.ntufar.babyonboard.sensing.sources

import com.google.common.truth.Truth.assertThat
import io.github.ntufar.babyonboard.sensing.engine.RawSensorData
import org.junit.Test

/**
 * Tests for sensor data emulation and the data structures used to
 * represent real-world driving telemetry.
 *
 * These tests verify that the emulated sensor data matches expected
 * real-world driving patterns.
 */
class SensorEmulationTest {

    // ─── Emulated driving scenarios ──────────────────────────────────────

    /**
     * Generates emulated RawSensorData for a smooth highway driving scenario.
     */
    private fun smoothHighwayData(
        ts: Long = System.currentTimeMillis(),
        speed: Double = 100.0
    ): RawSensorData = RawSensorData(
        timestamp = ts,
        lat = 51.5, lng = -0.1,
        speed = speed,
        latAccel = 0.05,   // minimal lateral movement
        longAccel = 0.1,   // steady acceleration
        vertAccel = 0.02,  // smooth road
        yawRate = 0.01,    // straight line
        altitude = 50.0
    )

    /**
     * Generates emulated RawSensorData for a hard braking event.
     */
    private fun hardBrakeData(
        ts: Long = System.currentTimeMillis(),
        speed: Double = 50.0,
        decel: Double = -4.5
    ): RawSensorData = RawSensorData(
        timestamp = ts,
        lat = 51.5, lng = -0.1,
        speed = speed,
        latAccel = 0.1,
        longAccel = decel,
        vertAccel = 0.05,
        yawRate = 0.02,
        altitude = 50.0
    )

    /**
     * Generates emulated RawSensorData for a sharp cornering event.
     */
    private fun sharpCornerData(
        ts: Long = System.currentTimeMillis(),
        speed: Double = 30.0,
        latG: Double = 4.5,
        yaw: Double = 0.8
    ): RawSensorData = RawSensorData(
        timestamp = ts,
        lat = 51.5, lng = -0.1,
        speed = speed,
        latAccel = latG,
        longAccel = 0.2,
        vertAccel = 0.1,
        yawRate = yaw,
        altitude = 50.0
    )

    /**
     * Generates emulated RawSensorData for a crash scenario:
     * high speed followed by sudden deceleration and speed drop.
     */
    private fun crashScenario(
        phase: Int // 0=before, 1=impact, 2=after
    ): RawSensorData {
        return when (phase) {
            0 -> RawSensorData(timestamp = 0L, lat = 51.5, lng = -0.1,
                speed = 60.0, latAccel = 0.0, longAccel = 0.5,
                vertAccel = 0.0, yawRate = 0.0, altitude = 50.0)
            1 -> RawSensorData(timestamp = 100L, lat = 51.5, lng = -0.1,
                speed = 40.0, latAccel = 0.5, longAccel = -8.0,
                vertAccel = 2.0, yawRate = 0.3, altitude = 50.0)
            2 -> RawSensorData(timestamp = 200L, lat = 51.5, lng = -0.1,
                speed = 2.0, latAccel = 0.0, longAccel = 0.0,
                vertAccel = 0.0, yawRate = 0.0, altitude = 50.0)
            else -> throw IllegalArgumentException("Invalid phase: $phase")
        }
    }

    // ─── Tests ────────────────────────────────────────────────────────────

    @Test
    fun `smooth highway data has minimal acceleration values`() {
        val data = smoothHighwayData()

        assertThat(data.latAccel).isLessThan(0.1)
        assertThat(data.longAccel).isLessThan(0.2)
        assertThat(data.yawRate).isLessThan(0.05)
        // No harsh event thresholds are crossed
        assertThat(data.longAccel).isLessThan(2.5) // baby mode threshold
        assertThat(Math.abs(data.latAccel)).isLessThan(3.0) // baby mode threshold
    }

    @Test
    fun `hard brake data has strong negative longitudinal acceleration`() {
        val data = hardBrakeData()

        assertThat(data.longAccel).isLessThan(0.0)
        assertThat(Math.abs(data.longAccel)).isAtLeast(4.0)
        // This should trigger both baby and normal mode BRAKE detection
    }

    @Test
    fun `sharp corner data has high lateral acceleration and yaw`() {
        val data = sharpCornerData()

        assertThat(Math.abs(data.latAccel)).isAtLeast(4.0)
        assertThat(Math.abs(data.yawRate)).isAtLeast(0.5)
        // This should trigger CORNER event detection
    }

    @Test
    fun `crash impact phase shows extreme deceleration`() {
        val impact = crashScenario(1)

        assertThat(Math.abs(impact.longAccel)).isAtLeast(4.0)
        assertThat(impact.speed).isLessThan(60.0)
    }

    @Test
    fun `crash post-impact shows near-zero speed`() {
        val after = crashScenario(2)

        assertThat(after.speed).isLessThan(5.0)
    }

    @Test
    fun `crash scenario speed progression is decreasing`() {
        val speeds = (0..2).map { crashScenario(it).speed }

        assertThat(speeds[0]).isGreaterThan(speeds[1])
        assertThat(speeds[1]).isGreaterThan(speeds[2])
    }

    @Test
    fun `emulated data preserves timestamp order`() {
        val scenarios = listOf(
            smoothHighwayData(ts = 1000L),
            hardBrakeData(ts = 2000L),
            sharpCornerData(ts = 3000L)
        )
        for (i in 0 until scenarios.size - 1) {
            assertThat(scenarios[i].timestamp).isLessThan(scenarios[i + 1].timestamp)
        }
    }

    @Test
    fun `emulated data has valid coordinate ranges`() {
        val data = smoothHighwayData()

        assertThat(data.lat).isAtLeast(-90.0)
        assertThat(data.lat).isAtMost(90.0)
        assertThat(data.lng).isAtLeast(-180.0)
        assertThat(data.lng).isAtMost(180.0)
        assertThat(data.speed).isAtLeast(0.0)
        assertThat(data.altitude).isAtLeast(0.0)
    }
}
