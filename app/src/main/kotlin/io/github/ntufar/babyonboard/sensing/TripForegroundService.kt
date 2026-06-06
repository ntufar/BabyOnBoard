package io.github.ntufar.babyonboard.sensing

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import io.github.ntufar.babyonboard.MainActivity
import io.github.ntufar.babyonboard.sensing.engine.TelemetryEngine
import io.github.ntufar.babyonboard.sensing.sources.LocationSource
import io.github.ntufar.babyonboard.sensing.sources.MotionSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine

class TripForegroundService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private lateinit var telemetryEngine: TelemetryEngine
    private lateinit var locationSource: LocationSource
    private lateinit var motionSource: MotionSource
    private var tripId: String = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        locationSource = LocationSource(this)
        motionSource = MotionSource(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val babyMode = intent?.getBooleanExtra(EXTRA_BABY_MODE, true) ?: true
        tripId = intent?.getStringExtra(EXTRA_TRIP_ID) ?: "unknown"

        telemetryEngine = TelemetryEngine(babyMode = babyMode)
        locationSource.start()
        motionSource.start()

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        serviceScope.launch {
            locationSource.locationFlow.combine(motionSource.sensorFlow) { loc, motion ->
                loc.copy(
                    latAccel = motion.latAccel,
                    longAccel = motion.longAccel,
                    vertAccel = motion.vertAccel,
                    yawRate = motion.yawRate
                )
            }.collect { data ->
                val frame = telemetryEngine.processRawData(data)
                val events = telemetryEngine.detectEvents(frame, tripId)

                val speedIntent = Intent(ACTION_SPEED_UPDATE).apply {
                    putExtra(EXTRA_SPEED, data.speed * 3.6)
                    putExtra(EXTRA_TRIP_ID, tripId)
                }
                sendBroadcast(speedIntent)

                for (event in events) {
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

        return START_STICKY
    }

    override fun onDestroy() {
        locationSource.stop()
        motionSource.stop()
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

    companion object {
        const val CHANNEL_ID = "trip_recording"
        const val NOTIFICATION_ID = 1
        const val EXTRA_TRIP_ID = "trip_id"
        const val EXTRA_BABY_MODE = "baby_mode"
        const val EXTRA_SPEED = "speed"
        const val EXTRA_EVENT_TYPE = "event_type"
        const val EXTRA_EVENT_VALUE = "event_value"
        const val EXTRA_EVENT_CONFIDENCE = "event_confidence"
        const val EXTRA_EVENT_SEVERITY = "event_severity"
        const val ACTION_SPEED_UPDATE = "io.github.ntufar.babyonboard.SPEED_UPDATE"
        const val ACTION_EVENT_DETECTED = "io.github.ntufar.babyonboard.EVENT_DETECTED"
    }
}
