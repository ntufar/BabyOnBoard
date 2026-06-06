package com.example.babyonboard

import android.content.Context
import androidx.room.Room
import com.example.babyonboard.data.db.ContactDao
import com.example.babyonboard.data.db.EventDao
import com.example.babyonboard.data.db.SettingsDao
import com.example.babyonboard.data.db.TripDao
import com.example.babyonboard.data.repository.TripRepositoryImpl
import com.example.babyonboard.domain.repository.TripRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "babyonboard.db"
        ).build()
    }

    @Provides fun provideTripDao(db: AppDatabase): TripDao = db.tripDao()
    @Provides fun provideEventDao(db: AppDatabase): EventDao = db.eventDao()
    @Provides fun provideContactDao(db: AppDatabase): ContactDao = db.contactDao()
    @Provides fun provideSettingsDao(db: AppDatabase): SettingsDao = db.settingsDao()

    @Provides
    @Singleton
    fun provideTripRepository(
        tripDao: TripDao,
        eventDao: EventDao,
        contactDao: ContactDao,
        settingsDao: SettingsDao
    ): TripRepository {
        return TripRepositoryImpl(tripDao, eventDao, contactDao, settingsDao)
    }
}
