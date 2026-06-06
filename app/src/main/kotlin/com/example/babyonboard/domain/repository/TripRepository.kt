package com.example.babyonboard.domain.repository

import com.example.babyonboard.domain.model.Contact
import com.example.babyonboard.domain.model.Event
import com.example.babyonboard.domain.model.Settings
import com.example.babyonboard.domain.model.Trip

interface TripRepository {
    suspend fun saveTrip(trip: Trip)
    suspend fun saveEvent(event: Event)
    suspend fun getTripHistory(): List<Trip>
    suspend fun updateTrip(trip: Trip)
    suspend fun saveSettings(settings: Settings)
    suspend fun getSettings(): Settings
    suspend fun saveContact(contact: Contact)
    suspend fun getContacts(): List<Contact>
}
