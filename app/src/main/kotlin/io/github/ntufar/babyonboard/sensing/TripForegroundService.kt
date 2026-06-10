package io.github.ntufar.babyonboard.sensing

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.ntufar.babyonboard.MainActivity
import io.github.ntufar.babyonboard.domain.model.MetricSample
import io.github.ntufar.babyonboard.domain.model.SensitivityMode
import io.github.ntufar.babyonboard.domain.repository.TripRepository
import io.github.ntufar.babyonboard.domain.usecase.EvaluateCrashUseCase
import io.github.ntufar.babyonboard.sensing.engine.TelemetryEngine
import io.github.ntufar.babyonboard.sensing.sources.DistractionSource
import io.github.ntufar.babyonboard.sensing.sources.LocationSource
import io.github.ntufar.babyonboard.sensing.sources.MotionSource
import kotlinx.coroutines.*
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class TripForegroundService : Service() {

    @Inject lateinit var tripRepository: TripRepository
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var telemetryEngine: TelemetryEngine
    private lateinit var locationSource: LocationSource
    private lateinit var motionSource: MotionSource
    private lateinit var distractionSource: DistractionSource
    private var tripId: String = ""

    private val speedHistory = mutableListOf<Double>()
    private val accelHistory = mutableListOf<Double>()
    private val crashUseCase = EvaluateCrashUseCase()
    private var crashAlerted = false

    @Volatile private var latestLocation: io.github.ntufar.babyonboard.sensing.engine.RawSensorData? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> distractionSource.onScreenOn()
                Intent.ACTION_SCREEN_OFF -> distractionSource.onScreenOff()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        locationSource = LocationSource(this)
        motionSource = MotionSource(this)
        distractionSource = DistractionSource(this)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val babyMode = intent?.getBooleanExtra(EXTRA_BABY_MODE, true) ?: true
        val sensitivityName = intent?.getStringExtra(EXTRA_SENSITIVITY) ?: SensitivityMode.CAR.name
        val sensitivity = runCatching { SensitivityMode.valueOf(sensitivityName) }
            .getOrDefault(SensitivityMode.CAR)
        tripId = intent?.getStringExtra(EXTRA_TRIP_ID) ?: "unknown"
        crashAlerted = false

        telemetryEngine = TelemetryEngine(babyMode = babyMode, sensitivity = sensitivity)
        telemetryEngine.resetWindows()
        latestLocation = null
        locationSource.start()
        motionSource.start()
        distractionSource.start()

        createNotificationChannel()
        try {
            startForeground(
                NOTIFICATION_ID, buildNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
            sendDebugLog("Service started (location type)")
        } catch (e: SecurityException) {
            // Location permission not granted; start without the location type so
            // motion-only recording (Walking indoors) still works.
            startForeground(NOTIFICATION_ID, buildNotification())
            sendDebugLog("Service started (no location permission: ${e.message})")
        }

        // Update latest GPS location whenever it arrives
        serviceScope.launch {
            locationSource.locationFlow.collect { loc ->
                latestLocation = loc
            }
        }

        var motionEventCount = 0

        // Process every motion event immediately, using the last known location if available
        serviceScope.launch {
            motionSource.sensorFlow.collect { motion ->
                val (worldLong, worldLat, worldVert) = if (motion.rotationVector != null) {
                    rotateToWorldFrame(
                        motion.longAccel, motion.latAccel, motion.vertAccel,
                        motion.rotationVector!!
                    )
                } else {
                    Triple(motion.longAccel, motion.latAccel, motion.vertAccel)
                }
                val loc = latestLocation
                val data = if (loc != null) {
                    loc.copy(
                        latAccel = worldLat,
                        longAccel = worldLong,
                        vertAccel = worldVert,
                        yawRate = motion.yawRate,
                        timestamp = motion.timestamp
                    )
                } else {
                    motion.copy(
                        lat = 0.0, lng = 0.0, speed = 0.0,
                        latAccel = worldLat,
                        longAccel = worldLong,
                        vertAccel = worldVert
                    )
                }

                motionEventCount++
                if (motionEventCount == 1) {
                    sendDebugLog("First motion event: speed=${data.speed} longAccel=${data.longAccel} hasGps=${loc != null}")
                }

                val frame = telemetryEngine.processRawData(data)
                serviceScope.launch {
                    runCatching {
                        tripRepository.saveMetricSample(MetricSample(
                            tripId = tripId,
                            ts = frame.timestamp,
                            speed = frame.speed,
                            longAccel = frame.longAccel,
                            latAccel = frame.latAccel,
                            vertAccel = frame.vertAccel,
                            yawRate = frame.yawRate,
                            altitude = frame.altitude
                        ))
                    }.onFailure { e ->
                        sendDebugLog("saveMetricSample failed: ${e.javaClass.simpleName}: ${e.message}")
                    }
                }
                val events = telemetryEngine.detectEvents(frame, tripId)

                val extendedEvents = telemetryEngine.detectExtendedEvents(frame, tripId)
                val allEvents = events + extendedEvents

                telemetryEngine.calculateGradient(data.altitude, data.lat, data.lng)

                val speedKmh = data.speed * 3.6
                distractionSource.tick(speedKmh)

                speedHistory.add(data.speed)
                accelHistory.add(abs(data.longAccel))
                if (speedHistory.size > 30) speedHistory.removeAt(0)
                if (accelHistory.size > 30) accelHistory.removeAt(0)

                if (!crashAlerted && speedHistory.size >= 5) {
                    val assessment = crashUseCase.execute(speedHistory, accelHistory)
                    if (assessment.isCrashDetected) {
                        crashAlerted = true
                        val crashIntent = Intent(ACTION_CRASH_DETECTED).apply {
                            putExtra(EXTRA_CONFIDENCE, assessment.confidence)
                            putExtra(EXTRA_LAT, data.lat)
                            putExtra(EXTRA_LNG, data.lng)
                        }
                        sendBroadcast(crashIntent)
                    }
                }

                val speedIntent = Intent(ACTION_SPEED_UPDATE).apply {
                    putExtra(EXTRA_SPEED, speedKmh)
                    putExtra(EXTRA_LONG_ACCEL, frame.longAccel)
                    putExtra(EXTRA_VERT_ACCEL, frame.vertAccel)
                    putExtra(EXTRA_TRIP_ID, tripId)
                }
                sendBroadcast(speedIntent)

                for (event in allEvents) {
                    val eventIntent = Intent(ACTION_EVENT_DETECTED).apply {
                        putExtra(EXTRA_EVENT_TYPE, event.type.name)
                        putExtra(EXTRA_EVENT_VALUE, event.value)
                        putExtra(EXTRA_EVENT_CONFIDENCE, event.confidence)
                        putExtra(EXTRA_EVENT_SEVERITY, event.severity)
                        putExtra(EXTRA_TRIP_ID, tripId)
                    }
                    sendBroadcast(eventIntent)
                }
            }
        }

        serviceScope.launch {
            distractionSource.distractionFlow.collect {
                val eventIntent = Intent(ACTION_EVENT_DETECTED).apply {
                    putExtra(EXTRA_EVENT_TYPE, "PHONE_USE")
                    putExtra(EXTRA_EVENT_VALUE, 1.0)
                    putExtra(EXTRA_EVENT_CONFIDENCE, 0.8f)
                    putExtra(EXTRA_EVENT_SEVERITY, 0.7f)
                    putExtra(EXTRA_TRIP_ID, tripId)
                }
                sendBroadcast(eventIntent)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        locationSource.stop()
        motionSource.stop()
        distractionSource.stop()
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: IllegalArgumentException) {
            // Ignore if not registered
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Trip Recording",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Baby On Board")
            .setContentText("Trip recording active")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun rotateToWorldFrame(
        longAccel: Double, latAccel: Double, vertAccel: Double,
        rotationVector: FloatArray
    ): Triple<Double, Double, Double> {
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector)
        val device = floatArrayOf(longAccel.toFloat(), latAccel.toFloat(), vertAccel.toFloat())
        val world = FloatArray(3)
        world[0] = rotationMatrix[0] * device[0] + rotationMatrix[1] * device[1] + rotationMatrix[2] * device[2]
        world[1] = rotationMatrix[3] * device[0] + rotationMatrix[4] * device[1] + rotationMatrix[5] * device[2]
        world[2] = rotationMatrix[6] * device[0] + rotationMatrix[7] * device[1] + rotationMatrix[8] * device[2]
        return Triple(world[0].toDouble(), world[1].toDouble(), world[2].toDouble())
    }

    companion object {
        const val CHANNEL_ID = "trip_recording"
        const val NOTIFICATION_ID = 1
        const val EXTRA_TRIP_ID = "trip_id"
        const val EXTRA_BABY_MODE = "baby_mode"
        const val EXTRA_SENSITIVITY = "sensitivity"
        const val EXTRA_SPEED = "speed"
        const val EXTRA_EVENT_TYPE = "event_type"
        const val EXTRA_EVENT_VALUE = "event_value"
        const val EXTRA_EVENT_CONFIDENCE = "event_confidence"
        const val EXTRA_EVENT_SEVERITY = "event_severity"
        const val EXTRA_LONG_ACCEL = "long_accel"
        const val EXTRA_VERT_ACCEL = "vert_accel"
        const val ACTION_SPEED_UPDATE = "io.github.ntufar.babyonboard.SPEED_UPDATE"
        const val ACTION_EVENT_DETECTED = "io.github.ntufar.babyonboard.EVENT_DETECTED"
        const val ACTION_CRASH_DETECTED = "io.github.ntufar.babyonboard.CRASH_DETECTED"
        const val ACTION_DEBUG_LOG = "io.github.ntufar.babyonboard.DEBUG_LOG"
        const val EXTRA_CONFIDENCE = "confidence"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LNG = "lng"
        const val EXTRA_DEBUG_MESSAGE = "debug_message"
    }

    private fun sendDebugLog(message: String) {
        sendBroadcast(Intent(ACTION_DEBUG_LOG).apply {
            putExtra(EXTRA_DEBUG_MESSAGE, message)
        })
    }
}
