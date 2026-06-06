package com.example.babyonboard

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.babyonboard.data.db.ContactDao
import com.example.babyonboard.data.db.EventDao
import com.example.babyonboard.data.db.SettingsDao
import com.example.babyonboard.data.db.TripDao
import com.example.babyonboard.data.model.ContactEntity
import com.example.babyonboard.data.model.EventEntity
import com.example.babyonboard.data.model.GeofenceEntity
import com.example.babyonboard.data.model.MetricSampleEntity
import com.example.babyonboard.data.model.SettingsEntity
import com.example.babyonboard.data.model.TripEntity

@Database(
    entities = [TripEntity::class, EventEntity::class, MetricSampleEntity::class,
                ContactEntity::class, GeofenceEntity::class, SettingsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun eventDao(): EventDao
    abstract fun contactDao(): ContactDao
    abstract fun settingsDao(): SettingsDao
}
