package io.github.ntufar.babyonboard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ntufar.babyonboard.domain.model.Contact
import io.github.ntufar.babyonboard.domain.model.ContactRole
import io.github.ntufar.babyonboard.domain.model.Settings
import io.github.ntufar.babyonboard.domain.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: TripRepository
) : ViewModel() {

    private val _settings = MutableStateFlow<Settings?>(null)
    val settings: StateFlow<Settings?> = _settings

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts

    init {
        viewModelScope.launch {
            _settings.value = repository.getSettings()
            _contacts.value = repository.getContacts()
        }
    }

    fun updateSettings(newSettings: Settings) {
        viewModelScope.launch {
            repository.saveSettings(newSettings)
            _settings.value = newSettings
        }
    }

    fun addContact(name: String, phone: String, role: ContactRole) {
        viewModelScope.launch {
            val contact = Contact(
                id = UUID.randomUUID().toString(),
                name = name,
                phone = phone,
                role = role,
                consentTs = System.currentTimeMillis()
            )
            repository.saveContact(contact)
            _contacts.value = _contacts.value + contact
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            repository.deleteContact(contactId)
            _contacts.value = _contacts.value.filter { it.id != contactId }
        }
    }
}
