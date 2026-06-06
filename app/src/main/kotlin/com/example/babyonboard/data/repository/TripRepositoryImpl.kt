package com.example.babyonboard.data.repository

import com.example.babyonboard.data.db.ContactDao
import com.example.babyonboard.data.db.EventDao
import com.example.babyonboard.data.db.SettingsDao
import com.example.babyonboard.data.db.TripDao
import com.example.babyonboard.data.model.*
import com.example.babyonboard.domain.model.*
import com.example.babyonboard.domain.repository.TripRepository

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
        return tripDao.getAllTrips().map { entity ->
            Trip(entity.id, entity.startTs, entity.endTs, entity.distanceM, entity.durationS, entity
                .avgSpeed, entity.maxSpeed, entity.score, entity.babyMode, entity.routeRef)
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
        val entity = settingsDao.getSettings()
        return entity?.let { s ->
            Settings(
                s.autoStart, s.btTriggerDeviceId, s.dndInTrip,
                s.reminderEscalation, s.retentionDays, s.units,
                s.emergencyNumber
            )
        } ?: Settings(
            autoStart = false,
            btTriggerDeviceId = null,
            dndInTrip = true,
            reminderEscalation = 1,
            retentionDays = 30,
            units = "km",
            emergencyNumber = "112"
        )
    }

    override suspend fun saveContact(contact: Contact) {
        contactDao.insertContact(ContactEntity(
            contact.id, contact.name, contact.phone, contact.role.name,
            contact.consentTs
        ))
    }

    override suspend fun getContacts(): List<Contact> {
        return contactDao.getAllContacts().map { entity ->
            Contact(entity.id, entity.name, entity.phone, ContactRole.valueOf(entity.role), entity.consentTs)
        }
    }
}
