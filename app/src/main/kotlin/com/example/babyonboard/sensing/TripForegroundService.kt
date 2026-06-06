package com.example.babyonboard.sensing

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.babyonboard.MainActivity
import com.example.babyonboard.sensing.engine.TelemetryEngine
import com.example.babyonboard.sensing.sources.LocationSource
import com.example.babyonboard.sensing.sources.MotionSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class TripForegroundService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private lateinit var telemetryEngine: TelemetryEngine
    private lateinit var locationSource: LocationSource
    private lateinit var motionSource: MotionSource

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        telemetryEngine = TelemetryEngine(babyMode = true)
        locationSource = LocationSource(this)
        motionSource = MotionSource(this)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        locationSource.start()
        motionSource.start()

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
                val events = telemetryEngine.detectEvents(frame, "trip_${System.currentTimeMillis()}")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
        private const val CHANNEL_ID = "trip_recording"
        private const val NOTIFICATION_ID = 1
    }
}
