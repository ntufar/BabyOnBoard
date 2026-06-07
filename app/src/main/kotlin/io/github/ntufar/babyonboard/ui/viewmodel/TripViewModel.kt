package io.github.ntufar.babyonboard.ui.viewmodel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.ntufar.babyonboard.domain.model.EventType
import io.github.ntufar.babyonboard.domain.model.TelemetryEvent
import io.github.ntufar.babyonboard.domain.model.Trip
import io.github.ntufar.babyonboard.domain.repository.TripRepository
import io.github.ntufar.babyonboard.domain.usecase.StartTripUseCase
import io.github.ntufar.babyonboard.sensing.TripForegroundService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TripViewModel @Inject constructor(
    private val repository: TripRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _currentTrip = MutableStateFlow<Trip?>(null)
    val currentTrip: StateFlow<Trip?> = _currentTrip

    private val _tripHistory = MutableStateFlow<List<Trip>>(emptyList())
    val tripHistory: StateFlow<List<Trip>> = _tripHistory

    private val _events = MutableStateFlow<List<TelemetryEvent>>(emptyList())
    val events: StateFlow<List<TelemetryEvent>> = _events

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds

    private val _currentSpeed = MutableStateFlow(0.0)
    val currentSpeed: StateFlow<Double> = _currentSpeed

    private var timerJob: Job? = null
    private var receiverRegistered = false

    /** @VisibleForTesting */
    var startTimerOnTripStart = true

    private val sensorReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                TripForegroundService.ACTION_SPEED_UPDATE -> {
                    val speed = intent.getDoubleExtra(TripForegroundService.EXTRA_SPEED, 0.0)
                    updateTripWithSpeed(speed)
                }
                TripForegroundService.ACTION_EVENT_DETECTED -> {
                    val type = intent.getStringExtra(TripForegroundService.EXTRA_EVENT_TYPE)
                    val value = intent.getDoubleExtra(TripForegroundService.EXTRA_EVENT_VALUE, 0.0)
                    val confidence = intent.getFloatExtra(TripForegroundService.EXTRA_EVENT_CONFIDENCE, 0f)
                    val severity = intent.getFloatExtra(TripForegroundService.EXTRA_EVENT_SEVERITY, 0f)
                    val tripId = intent.getStringExtra(TripForegroundService.EXTRA_TRIP_ID)

                    if (type != null && tripId != null) {
                        val event = TelemetryEvent(
                            id = UUID.randomUUID().toString(),
                            tripId = tripId,
                            ts = System.currentTimeMillis(),
                            type = EventType.valueOf(type),
                            severity = severity,
                            value = value,
                            lat = 0.0,
                            lng = 0.0,
                            confidence = confidence
                        )
                        _events.value = _events.value + event
                        recalculateScore()
                    }
                }
            }
        }
    }

    init {
        loadTripHistory()
    }

    private fun registerSensorReceiver() {
        if (receiverRegistered) return
        try {
            val filter = IntentFilter().apply {
                addAction(TripForegroundService.ACTION_SPEED_UPDATE)
                addAction(TripForegroundService.ACTION_EVENT_DETECTED)
            }
            context.registerReceiver(sensorReceiver, filter, Context.RECEIVER_EXPORTED)
            receiverRegistered = true
        } catch (_: RuntimeException) {
            // Ignore in test environments where Android stubs throw
        }
    }

    private fun loadTripHistory() {
        viewModelScope.launch {
            _tripHistory.value = repository.getTripHistory()
        }
    }

    fun startTrip(babyMode: Boolean) {
        viewModelScope.launch {
            val trip = StartTripUseCase(repository).execute(babyMode)
            _currentTrip.value = trip
            _events.value = emptyList()
            _elapsedSeconds.value = 0L
            _currentSpeed.value = 0.0
            if (startTimerOnTripStart) startTimer()
            registerSensorReceiver()
            startForegroundService(trip.id, babyMode)
        }
    }

    fun endTrip() {
        timerJob?.cancel()
        val wasBabyMode = _currentTrip.value?.babyMode == true
        stopForegroundService()
        viewModelScope.launch {
            val trip = _currentTrip.value ?: return@launch
            val elapsed = _elapsedSeconds.value
            val score = calculateFinalScore()
            val updated = trip.copy(
                endTs = System.currentTimeMillis(),
                durationS = elapsed,
                score = score
            )
            repository.updateTrip(updated)
            _currentTrip.value = updated
            loadTripHistory()
            if (wasBabyMode) {
                showBackSeatReminder()
            }
        }
    }

    private fun showBackSeatReminder() {
        val channelId = "back_seat_reminder"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            channelId, "Back-Seat Reminder", NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Check the Back Seat!")
            .setContentText("Baby mode was active — make sure everyone is out of the car.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        manager.notify(1001, notification)
    }

    fun refreshHistory() {
        loadTripHistory()
    }

    fun loadEventsForTrip(tripId: String, eventsList: List<TelemetryEvent>) {
        _events.value = eventsList
        val trip = _tripHistory.value.find { it.id == tripId }
        _currentTrip.value = trip
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _elapsedSeconds.value += 1
            }
        }
    }

    private fun updateTripWithSpeed(speedKmh: Double) {
        val trip = _currentTrip.value ?: return
        _currentSpeed.value = speedKmh
        val elapsed = _elapsedSeconds.value.coerceAtLeast(1)
        val newDist = trip.distanceM + (speedKmh / 3.6)
        val maxS = maxOf(trip.maxSpeed, speedKmh)
        _currentTrip.value = trip.copy(
            distanceM = newDist,
            maxSpeed = maxS,
            avgSpeed = newDist / elapsed * 3.6
        )
    }

    private fun recalculateScore() {
        val trip = _currentTrip.value ?: return
        val events = _events.value
        val distanceKm = trip.distanceM / 1000.0
        if (distanceKm <= 0.0) return
        val harshCount = events.count { it.severity > 0.5f }
        val eventsPer100km = (harshCount / distanceKm * 100.0).toInt()
        val score = (100 - eventsPer100km * 10).coerceIn(0, 100)
        _currentTrip.value = trip.copy(score = score)
    }

    private fun calculateFinalScore(): Int {
        val trip = _currentTrip.value ?: return 100
        val events = _events.value
        val distanceKm = trip.distanceM / 1000.0
        if (distanceKm <= 0.0) return if (events.isEmpty()) 100 else 50
        val harshCount = events.count { it.severity > 0.5f }
        val eventsPer100km = (harshCount / distanceKm * 100.0).toInt()
        return (100 - eventsPer100km * 10).coerceIn(0, 100)
    }

    private fun startForegroundService(tripId: String, babyMode: Boolean) {
        try {
            val intent = Intent(context, TripForegroundService::class.java).apply {
                putExtra(TripForegroundService.EXTRA_TRIP_ID, tripId)
                putExtra(TripForegroundService.EXTRA_BABY_MODE, babyMode)
            }
            context.startForegroundService(intent)
        } catch (_: RuntimeException) {
            // Ignore in test environments where Android stubs throw
        }
    }

    private fun stopForegroundService() {
        try {
            val intent = Intent(context, TripForegroundService::class.java)
            context.stopService(intent)
        } catch (_: RuntimeException) {
            // Ignore in test environments where Android stubs throw
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        if (receiverRegistered) {
            context.unregisterReceiver(sensorReceiver)
        }
    }
}
