package io.github.ntufar.babyonboard

import android.content.Context
import androidx.room.Room
import io.github.ntufar.babyonboard.data.db.ContactDao
import io.github.ntufar.babyonboard.data.db.EventDao
import io.github.ntufar.babyonboard.data.db.SettingsDao
import io.github.ntufar.babyonboard.data.db.TripDao
import io.github.ntufar.babyonboard.data.repository.TripRepositoryImpl
import io.github.ntufar.babyonboard.domain.repository.TripRepository
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
