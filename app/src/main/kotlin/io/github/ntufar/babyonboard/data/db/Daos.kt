package io.github.ntufar.babyonboard.data.db

import androidx.room.*
import io.github.ntufar.babyonboard.data.model.*

@Dao
interface TripDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity)

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Query("SELECT * FROM trips ORDER BY startTs DESC")
    suspend fun getAllTrips(): List<TripEntity>
}

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)

    @Query("SELECT * FROM events WHERE tripId = :tripId")
    suspend fun getEventsForTrip(tripId: String): List<EventEntity>
}

@Dao
interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Query("SELECT * FROM contacts")
    suspend fun getAllContacts(): List<ContactEntity>

    @Query("DELETE FROM contacts WHERE id = :contactId")
    suspend fun deleteContact(contactId: String)
}

@Dao
interface MetricSampleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetricSample(sample: MetricSampleEntity)

    @Query("SELECT * FROM metric_samples WHERE tripId = :tripId ORDER BY ts ASC")
    suspend fun getSamplesForTrip(tripId: String): List<MetricSampleEntity>
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 'default_settings'")
    suspend fun getSettings(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: SettingsEntity)
}
