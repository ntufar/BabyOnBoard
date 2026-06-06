package com.example.babyonboard.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.babyonboard.domain.model.Models.*
import com.example.babyonboard.domain.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class TripViewModel(private val repository: TripRepository) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main + Job())

    private val _currentTrip = MutableStateFlow<Trip?>(null)
    val currentTrip: StateFlow<Trip?> = _currentTrip

    fun startTrip(babyMode: Boolean) {
        viewModelScope.launch {
            val trip = StartTripUseCase(repository).execute(babyMode)
            _currentTrip.value = trip
        }
    }

    fun endTrip(trip: Trip) {
        viewModelScope.launch {
            repository.updateTrip(trip.copy(endTs = System.currentTimeMillis()))
        }
    }
}
