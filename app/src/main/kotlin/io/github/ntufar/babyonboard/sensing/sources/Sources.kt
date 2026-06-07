package io.github.ntufar.babyonboard.sensing.sources

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import io.github.ntufar.babyonboard.sensing.engine.RawSensorData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class LocationSource(private val context: Context) : LocationListener {
    private val locationManager = context.getSystemService(LocationManager::class.java)
    private val _locationFlow = MutableSharedFlow<RawSensorData>(extraBufferCapacity = 10)
    val locationFlow: SharedFlow<RawSensorData> = _locationFlow

    fun start() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, this)
        }
    }

    fun stop() {
        locationManager.removeUpdates(this)
    }

    override fun onLocationChanged(location: Location) {
        _locationFlow.tryEmit(RawSensorData(
            timestamp = System.currentTimeMillis(),
            lat = location.latitude,
            lng = location.longitude,
            speed = location.speed.toDouble(),
            latAccel = 0.0,
            longAccel = 0.0,
            vertAccel = 0.0,
            yawRate = 0.0,
            altitude = location.altitude
        ))
    }
}

class MotionSource(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val _sensorFlow = MutableSharedFlow<RawSensorData>(extraBufferCapacity = 100)
    val sensorFlow: SharedFlow<RawSensorData> = _sensorFlow

    @Volatile
    private var latestAccel: RawSensorData? = null
    @Volatile
    private var latestGyro: RawSensorData? = null
    @Volatile
    private var latestRotation: FloatArray? = null

    fun start() {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, rotation, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                latestAccel = RawSensorData(
                    timestamp = System.currentTimeMillis(),
                    lat = 0.0, lng = 0.0, speed = 0.0,
                    latAccel = event.values[1].toDouble(),
                    longAccel = event.values[0].toDouble(),
                    vertAccel = event.values[2].toDouble(),
                    yawRate = 0.0, altitude = 0.0,
                    rotationVector = latestRotation
                )
                emitFused()
            }
            Sensor.TYPE_GYROSCOPE -> {
                latestGyro = RawSensorData(
                    timestamp = System.currentTimeMillis(),
                    lat = 0.0, lng = 0.0, speed = 0.0,
                    latAccel = 0.0, longAccel = 0.0,
                    vertAccel = 0.0,
                    yawRate = event.values[2].toDouble(), altitude = 0.0,
                    rotationVector = latestRotation
                )
                emitFused()
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                latestRotation = event.values.clone()
            }
        }
    }

    private fun emitFused() {
        val accel = latestAccel ?: return
        val gyro = latestGyro ?: return
        _sensorFlow.tryEmit(accel.copy(yawRate = gyro.yawRate, rotationVector = latestRotation))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
