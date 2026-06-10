package io.github.ntufar.babyonboard.sensing.sources

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import io.github.ntufar.babyonboard.sensing.engine.RawSensorData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class LocationSource(private val context: Context) {
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val _locationFlow = MutableSharedFlow<RawSensorData>(extraBufferCapacity = 10)
    val locationFlow: SharedFlow<RawSensorData> = _locationFlow

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
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

    fun start() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                .setMinUpdateIntervalMillis(500L)
                .build()
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        }
    }

    fun stop() {
        fusedClient.removeLocationUpdates(locationCallback)
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
        if (accelerometer != null) sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        if (gyro != null) sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME)
        if (rotation != null) sensorManager.registerListener(this, rotation, SensorManager.SENSOR_DELAY_GAME)
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
        _sensorFlow.tryEmit(accel.copy(yawRate = latestGyro?.yawRate ?: 0.0, rotationVector = latestRotation))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

class DistractionSource(context: Context) {
    private val _distractionFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 10)
    val distractionFlow: SharedFlow<Unit> = _distractionFlow
    private var screenOn = false
    private var lastDistractionTs = 0L
    private val debounceMs = 5000L

    fun start() {
        screenOn = true
    }

    fun onScreenOn() {
        screenOn = true
        val now = System.currentTimeMillis()
        if (now - lastDistractionTs > debounceMs) {
            lastDistractionTs = now
            _distractionFlow.tryEmit(Unit)
        }
    }

    fun onScreenOff() {
        screenOn = false
    }

    fun tick(speedKmh: Double) {
        if (screenOn && speedKmh > 5.0) {
            val now = System.currentTimeMillis()
            if (now - lastDistractionTs > debounceMs) {
                lastDistractionTs = now
                _distractionFlow.tryEmit(Unit)
            }
        }
    }

    fun stop() {}
}
