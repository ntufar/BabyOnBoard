package com.example.babyonboard.sensing.sources

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.example.babyonboard.sensing.engine.RawSensorData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class LocationSource(context: Context) : LocationListener {
    private val locationManager = context.getSystemService(LocationManager::class.java)
    private val _locationFlow = MutableSharedFlow<RawSensorData>(extraBufferCapacity = 10)
    val locationFlow: SharedFlow<RawSensorData> = _locationFlow

    fun start() {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, this)
    }

    override fun onLocationChanged(location: Location) {
        _locationFlow.tryEmit(RawSensorData(
            timestamp = System.currentTimeMillis(),
            lat = location.latitude,
            lng = location.longitude,
            speed = location.speed,
            latAccel = 0.0, // Placeholder
            longAccel = 0.0, // Placeholder
            vertAccel = 0.0, // Placeholder
            yawRate = 0.0, // Placeholder
            altitude = location.altitude
        ))
    }
}

class MotionSource(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val _sensorFlow = MutableSharedFlow<RawSensorData>(extraBufferCapacity = 100)
    val sensorFlow: SharedFlow<RawSensorData> = _sensorFlow

    fun start() {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val data = when (event.sensor.type) {
            Sensor.TYPE_LINEAR_ACCELERATION -> RawSensorData(
                timestamp = System.currentTimeMillis(),
                lat = 0.0, lng = 0.0, speed = 0.0,
                latAccel = event.values[1],
                longAccel = event.values[0],
                vertAccel = event.values[2],
                yawRate = 0.0, altitude = 0.0
            )
            Sensor.TYPE_GYROSCOPE -> RawSensorData(
                timestamp = System.currentTimeMillis(),
                lat = 0.0, lng = 0.0, speed = 0.0,
                latAccel = 0.0, longAccel = 0.0,
                vertAccel = 0.0,
                yawRate = event.values[2], altitude = 0.0
            )
            else -> null
        }
        data?.let { _sensorFlow.tryEmit(it) }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
