package io.github.ntufar.babyonboard

import android.content.Context
import androidx.room.Room
import io.github.ntufar.babyonboard.data.db.ContactDao
import io.github.ntufar.babyonboard.data.db.EventDao
import io.github.ntufar.babyonboard.data.db.MetricSampleDao
import io.github.ntufar.babyonboard.data.db.SettingsDao
import io.github.ntufar.babyonboard.data.db.TripDao
import io.github.ntufar.babyonboard.data.repository.TripRepositoryImpl
import io.github.ntufar.babyonboard.domain.repository.TripRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Singleton
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val KEY_ALIAS = "babyonboard_db_key"
    private const val PREFS_NAME = "db_prefs"
    private const val PREFS_KEY = "encrypted_db_passphrase"
    private const val GCM_TAG_LENGTH = 128

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        val passphrase = getDatabasePassphrase(context)
        val factory = SupportFactory(passphrase, null, false)
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "babyonboard.db"
        ).openHelperFactory(factory).build()
    }

    private fun getDatabasePassphrase(context: Context): ByteArray {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
            )
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            keyGenerator.generateKey()
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val secretKey = (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey

        val stored = prefs.getString(PREFS_KEY, null)
        if (stored != null) {
            val combined = Base64.getDecoder().decode(stored)
            val iv = combined.copyOfRange(0, 12)
            val encrypted = combined.copyOfRange(12, combined.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            return cipher.doFinal(encrypted)
        }

        val passphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(passphrase)
        val combined = iv + encrypted
        prefs.edit().putString(PREFS_KEY, Base64.getEncoder().encodeToString(combined)).apply()
        return passphrase
    }

    @Provides fun provideTripDao(db: AppDatabase): TripDao = db.tripDao()
    @Provides fun provideEventDao(db: AppDatabase): EventDao = db.eventDao()
    @Provides fun provideContactDao(db: AppDatabase): ContactDao = db.contactDao()
    @Provides fun provideSettingsDao(db: AppDatabase): SettingsDao = db.settingsDao()
    @Provides fun provideMetricSampleDao(db: AppDatabase): MetricSampleDao = db.metricSampleDao()

    @Provides
    @Singleton
    fun provideTripRepository(
        tripDao: TripDao,
        eventDao: EventDao,
        contactDao: ContactDao,
        settingsDao: SettingsDao,
        metricSampleDao: MetricSampleDao
    ): TripRepository {
        return TripRepositoryImpl(tripDao, eventDao, contactDao, settingsDao, metricSampleDao)
    }
}
