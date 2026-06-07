package io.github.ntufar.babyonboard.data.repository

import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import io.github.ntufar.babyonboard.AppDatabase
import io.github.ntufar.babyonboard.domain.model.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class TripRepositoryImplTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: TripRepositoryImpl

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = TripRepositoryImpl(
            db.tripDao(), db.eventDao(), db.contactDao(), db.settingsDao(), db.metricSampleDao()
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun aTrip(id: String = UUID.randomUUID().toString()) = Trip(
        id = id, startTs = 1000L, endTs = null,
        distanceM = 5000.0, durationS = 300,
        avgSpeed = 60.0, maxSpeed = 80.0, score = 85,
        babyMode = true
    )

    @Test
    fun `saveTrip and getTripHistory returns saved trips`() = runTest {
        repository.saveTrip(aTrip(id = "1"))
        repository.saveTrip(aTrip(id = "2"))

        val history = repository.getTripHistory()
        assertThat(history).hasSize(2)
        assertThat(history.map { it.id }).containsExactly("2", "1")
    }

    @Test
    fun `updateTrip modifies existing trip`() = runTest {
        repository.saveTrip(aTrip(id = "1"))
        repository.updateTrip(aTrip(id = "1").copy(score = 30, endTs = 9999L))

        val history = repository.getTripHistory()
        assertThat(history[0].score).isEqualTo(30)
        assertThat(history[0].endTs).isEqualTo(9999L)
    }

    @Test
    fun `getTripHistory returns empty list initially`() = runTest {
        val history = repository.getTripHistory()
        assertThat(history).isEmpty()
    }

    @Test
    fun `getSettings returns defaults when no settings saved`() = runTest {
        val settings = repository.getSettings()
        assertThat(settings.autoStart).isFalse()
        assertThat(settings.dndInTrip).isTrue()
        assertThat(settings.emergencyNumber).isEqualTo("112")
        assertThat(settings.units).isEqualTo("km")
        assertThat(settings.retentionDays).isEqualTo(30)
        assertThat(settings.btTriggerDeviceId).isNull()
    }

    @Test
    fun `saveSettings and getSettings round-trip`() = runTest {
        val custom = Settings(
            autoStart = true, btTriggerDeviceId = "bt:123",
            dndInTrip = false, reminderEscalation = 2,
            retentionDays = 7, units = "mi", emergencyNumber = "911"
        )
        repository.saveSettings(custom)

        val loaded = repository.getSettings()
        assertThat(loaded.autoStart).isTrue()
        assertThat(loaded.btTriggerDeviceId).isEqualTo("bt:123")
        assertThat(loaded.dndInTrip).isFalse()
        assertThat(loaded.retentionDays).isEqualTo(7)
        assertThat(loaded.units).isEqualTo("mi")
        assertThat(loaded.emergencyNumber).isEqualTo("911")
    }

    @Test
    fun `saveContact and getContacts round-trip`() = runTest {
        val contact = Contact("c1", "Alice", "+123", ContactRole.EMERGENCY, 1000L)
        repository.saveContact(contact)

        val contacts = repository.getContacts()
        assertThat(contacts).hasSize(1)
        assertThat(contacts[0].name).isEqualTo("Alice")
        assertThat(contacts[0].role).isEqualTo(ContactRole.EMERGENCY)
    }

    @Test
    fun `getContacts returns empty list when none saved`() = runTest {
        val contacts = repository.getContacts()
        assertThat(contacts).isEmpty()
    }

    @Test
    fun `saveEvent persists event`() = runTest {
        repository.saveTrip(aTrip(id = "t1"))
        repository.saveEvent(
            Event("e1", "t1", 1000L, EventType.BRAKE, 0.8f, 4.5, 51.5, -0.1, 0.9f)
        )
    }
}
