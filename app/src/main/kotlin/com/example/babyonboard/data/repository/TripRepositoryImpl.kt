package com.example.babyonboard.data.repository

import com.example.babyonboard.data.db.Daos.*
import com.example.babyonboard.data.model.Entities.*
import com.example.babyonboard.domain.model.Models.*
import com.example.babyonboard.domain.repository.TripRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TripRepositoryImpl(
    private val tripDao: TripDao,
    private val eventDao: EventDao,
    private val contactDao: ContactDao,
    private val settingsDao: SettingsDao
) : TripRepository {

    override suspend fun saveTrip(trip: Trip) {
        tripDao.insertTrip(TripEntity(
            trip.id, trip.startTs, trip.endTs, trip.distanceM,
            trip.durationS, trip.avgSpeed, trip.maxSpeed,
            trip.score, trip.babyMode, trip.routeRef
        ))
    }

    override suspend fun saveEvent(event: Event) {
        eventDao.insertEvent(EventEntity(
            event.id, event.tripId, event.ts, event.type.name,
            event.severity, event.value, event.lat, event.lng,
            event.confidence
        ))
    }

    override suspend fun getTripHistory(): List<Trip> {
        return tripDao.getAllTrips().map {
            Trip(it.id, it.startTs, it.endTs, it.distanceM, it.durationS, it
                .avgSpeed, it.maxSpeed, it.score, it.babyMode, it.routeRef)
        }
    }

    override suspend fun updateTrip(trip: Trip) {
        tripDao.updateTrip(TripEntity(
            trip.id, trip.startTs, trip.endTs, trip.distanceM,
            trip.durationS, trip.avgSpeed, trip.maxSpeed,
            trip.score, trip.babyMode, trip.routeRef
        ))
    }

    override suspend fun saveSettings(settings: Settings) {
        settingsDao.insertSettings(SettingsEntity(
            "default_settings", settings.autoStart, settings.btTriggerDeviceId,
            settings.dndInTrip, settings.reminderEscalation, settings.retentionDays,
            settings.units, settings.emergencyNumber
        ))
    }

    override suspend fun getSettings(): Settings {
        val entity = settingsDao.getSettings() ?: throw Exception("Settings not found")
        return Settings(
            entity.autoStart, entity.btTriggerDeviceId, entity.dndInTrip,
            entity.reminderEscalation, entity.retentionDays, entity.units,
            entity.emergencyNumber
        )
    }

    override suspend fun saveContact(contact: Contact) {
        contactDao.insertContact(ContactEntity(
            contact.id, contact.name, contact.phone, contact.role.name,
            contact.consentTs
        ))
    }

    override suspend fun getContacts(): List<Contact> {
        return contactDao.getAllContacts().map {
            Contact(it.id, it.name, it.phone, ContactRole.valueOf(it.role), it.consentTs)
        }
    }
}
