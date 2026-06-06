package io.github.ntufar.babyonboard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ntufar.babyonboard.domain.model.Settings
import io.github.ntufar.babyonboard.domain.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: TripRepository
) : ViewModel() {

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
