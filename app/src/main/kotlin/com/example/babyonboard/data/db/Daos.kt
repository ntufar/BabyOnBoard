package com.example.babyonboard.data.db

import androidx.room.*
import com.example.babyonboard.data.model.*

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
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 'default_settings'")
    suspend fun getSettings(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: SettingsEntity)
}
