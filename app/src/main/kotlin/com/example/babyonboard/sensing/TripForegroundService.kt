package com.example.babyonboard.sensing

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.babyonboard.sensing.engine.RawSensorData
import com.example.babyonboard.sensing.engine.TelemetryEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class TripForegroundService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private lateinit var telemetryEngine: TelemetryEngine
    private val _currentFrame = MutableStateFlow<RawSensorData?>(null)
    
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // In a real app, babyMode would be retrieved from DataStore/Settings
        telemetryEngine = TelemetryEngine(babyMode = true)
        
        serviceScope.launch {
            _currentFrame.collect { data ->
                data?.let {
                    val frame = telemetryEngine.processRawData(it)
                    val events = telemetryEngine.detectEvents(frame)
                    // Persist events and update UI
                    // TODO: Call repository to persist events
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start foreground notification
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
