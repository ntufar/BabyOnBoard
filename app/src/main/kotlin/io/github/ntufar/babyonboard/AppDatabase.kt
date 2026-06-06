package io.github.ntufar.babyonboard

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.ntufar.babyonboard.data.db.ContactDao
import io.github.ntufar.babyonboard.data.db.EventDao
import io.github.ntufar.babyonboard.data.db.SettingsDao
import io.github.ntufar.babyonboard.data.db.TripDao
import io.github.ntufar.babyonboard.data.model.ContactEntity
import io.github.ntufar.babyonboard.data.model.EventEntity
import io.github.ntufar.babyonboard.data.model.GeofenceEntity
import io.github.ntufar.babyonboard.data.model.MetricSampleEntity
import io.github.ntufar.babyonboard.data.model.SettingsEntity
import io.github.ntufar.babyonboard.data.model.TripEntity

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
