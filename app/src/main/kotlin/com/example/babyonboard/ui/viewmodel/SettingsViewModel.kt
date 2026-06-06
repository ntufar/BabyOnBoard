package com.example.babyonboard.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.babyonboard.domain.model.Models.Settings
import com.example.babyonboard.domain.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class SettingsViewModel(private val repository: TripRepository) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main + Job())

    private val _settings = MutableStateFlow<Settings?>(null)
    val settings: StateFlow<Settings?> = _settings

    init {
        viewModelScope.launch {
            _settings.value = repository.getSettings()
        }
    }

    fun updateSettings(newSettings: Settings) {
        viewModelScope.launch {
            repository.saveSettings(newSettings)
            _settings.value = newSettings
        }
    }
}
