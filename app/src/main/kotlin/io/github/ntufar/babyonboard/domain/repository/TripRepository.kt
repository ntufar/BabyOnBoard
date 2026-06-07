package io.github.ntufar.babyonboard.domain.repository

import io.github.ntufar.babyonboard.domain.model.Contact
import io.github.ntufar.babyonboard.domain.model.Event
import io.github.ntufar.babyonboard.domain.model.MetricSample
import io.github.ntufar.babyonboard.domain.model.Settings
import io.github.ntufar.babyonboard.domain.model.Trip

interface TripRepository {
    suspend fun saveTrip(trip: Trip)
    suspend fun saveEvent(event: Event)
    suspend fun saveMetricSample(sample: MetricSample)
    suspend fun getTripHistory(): List<Trip>
    suspend fun updateTrip(trip: Trip)
    suspend fun saveSettings(settings: Settings)
    suspend fun getSettings(): Settings
    suspend fun saveContact(contact: Contact)
    suspend fun getContacts(): List<Contact>
    suspend fun deleteContact(contactId: String)
}
