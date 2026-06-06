package com.example.babyonboard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.babyonboard.domain.model.Trip
import com.example.babyonboard.domain.repository.TripRepository
import com.example.babyonboard.domain.usecase.StartTripUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TripViewModel @Inject constructor(
    private val repository: TripRepository
) : ViewModel() {

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
