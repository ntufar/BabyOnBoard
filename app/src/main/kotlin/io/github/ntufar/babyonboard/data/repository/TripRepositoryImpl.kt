package io.github.ntufar.babyonboard.data.repository

import io.github.ntufar.babyonboard.data.db.ContactDao
import io.github.ntufar.babyonboard.data.db.EventDao
import io.github.ntufar.babyonboard.data.db.MetricSampleDao
import io.github.ntufar.babyonboard.data.db.SettingsDao
import io.github.ntufar.babyonboard.data.db.TripDao
import io.github.ntufar.babyonboard.data.model.*
import io.github.ntufar.babyonboard.domain.model.*
import io.github.ntufar.babyonboard.domain.model.SensitivityMode
import io.github.ntufar.babyonboard.domain.repository.TripRepository
import java.util.UUID

class TripRepositoryImpl(
    private val tripDao: TripDao,
    private val eventDao: EventDao,
    private val contactDao: ContactDao,
    private val settingsDao: SettingsDao,
    private val metricSampleDao: MetricSampleDao
) : TripRepository {

    override suspend fun saveTrip(trip: Trip) {
        tripDao.insertTrip(TripEntity(
            trip.id, trip.startTs, trip.endTs, trip.distanceM,
            trip.durationS, trip.avgSpeed, trip.maxSpeed,
            trip.score, trip.babyMode, trip.routeRef
        ))
    }

    override suspend fun saveMetricSample(sample: MetricSample) {
        metricSampleDao.insertMetricSample(MetricSampleEntity(
            id = UUID.randomUUID().toString(),
            tripId = sample.tripId,
            ts = sample.ts,
            speed = sample.speed,
            longAccel = sample.longAccel,
            latAccel = sample.latAccel,
            vertAccel = sample.vertAccel,
            yawRate = sample.yawRate,
            altitude = sample.altitude
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
            settings.units, settings.emergencyNumber, settings.sensitivity.name
        ))
    }

    override suspend fun getSettings(): Settings {
        val entity = settingsDao.getSettings()
        return entity?.let { s ->
            Settings(
                autoStart = s.autoStart,
                btTriggerDeviceId = s.btTriggerDeviceId,
                dndInTrip = s.dndInTrip,
                reminderEscalation = s.reminderEscalation,
                retentionDays = s.retentionDays,
                units = s.units,
                emergencyNumber = s.emergencyNumber,
                sensitivity = runCatching { SensitivityMode.valueOf(s.sensitivity) }
                    .getOrDefault(SensitivityMode.CAR)
            )
        } ?: Settings(
            autoStart = false,
            btTriggerDeviceId = null,
            dndInTrip = true,
            reminderEscalation = 1,
            retentionDays = 30,
            units = "km",
            emergencyNumber = "112",
            sensitivity = SensitivityMode.CAR
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

    override suspend fun deleteContact(contactId: String) {
        contactDao.deleteContact(contactId)
    }
}
