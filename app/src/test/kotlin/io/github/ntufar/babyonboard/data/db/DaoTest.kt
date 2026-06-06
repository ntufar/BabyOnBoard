package io.github.ntufar.babyonboard.data.db

import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import io.github.ntufar.babyonboard.AppDatabase
import io.github.ntufar.babyonboard.data.model.ContactEntity
import io.github.ntufar.babyonboard.data.model.EventEntity
import io.github.ntufar.babyonboard.data.model.SettingsEntity
import io.github.ntufar.babyonboard.data.model.TripEntity
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
class DaoTest {

    private lateinit var db: AppDatabase
    private lateinit var tripDao: TripDao
    private lateinit var eventDao: EventDao
    private lateinit var contactDao: ContactDao
    private lateinit var settingsDao: SettingsDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        tripDao = db.tripDao()
        eventDao = db.eventDao()
        contactDao = db.contactDao()
        settingsDao = db.settingsDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun aTrip(id: String = UUID.randomUUID().toString()) = TripEntity(
        id = id, startTs = 1000L, endTs = null,
        distanceM = 5000.0, durationS = 300,
        avgSpeed = 60.0, maxSpeed = 80.0, score = 85,
        babyMode = true, routeRef = null
    )

    @Test
    fun `tripDao insert and getAll returns trips ordered by startTs desc`() = runTest {
        tripDao.insertTrip(aTrip(id = "a").copy(startTs = 1000L))
        tripDao.insertTrip(aTrip(id = "b").copy(startTs = 2000L))

        val trips = tripDao.getAllTrips()
        assertThat(trips).hasSize(2)
        assertThat(trips[0].id).isEqualTo("b")
        assertThat(trips[1].id).isEqualTo("a")
    }

    @Test
    fun `tripDao update modifies existing trip`() = runTest {
        tripDao.insertTrip(aTrip(id = "1"))
        tripDao.updateTrip(aTrip(id = "1").copy(score = 50, endTs = 5000L))

        val trips = tripDao.getAllTrips()
        assertThat(trips[0].score).isEqualTo(50)
        assertThat(trips[0].endTs).isEqualTo(5000L)
    }

    @Test
    fun `tripDao getAll returns empty list when no trips`() = runTest {
        val trips = tripDao.getAllTrips()
        assertThat(trips).isEmpty()
    }

    @Test
    fun `eventDao insert and getByTripId`() = runTest {
        tripDao.insertTrip(aTrip(id = "t1"))
        tripDao.insertTrip(aTrip(id = "t2"))

        eventDao.insertEvent(
            EventEntity("e1", "t1", 1000L, "BRAKE", 0.8f, 4.0, 0.0, 0.0, 0.9f)
        )
        eventDao.insertEvent(
            EventEntity("e2", "t1", 2000L, "ACCEL", 0.7f, 3.5, 1.0, 1.0, 0.8f)
        )
        eventDao.insertEvent(
            EventEntity("e3", "t2", 1500L, "CORNER", 0.6f, 3.0, 2.0, 2.0, 0.7f)
        )

        val t1Events = eventDao.getEventsForTrip("t1")
        assertThat(t1Events).hasSize(2)

        val t2Events = eventDao.getEventsForTrip("t2")
        assertThat(t2Events).hasSize(1)
        assertThat(t2Events[0].type).isEqualTo("CORNER")
    }

    @Test
    fun `eventDao getByTripId returns empty when no events`() = runTest {
        tripDao.insertTrip(aTrip(id = "t1"))
        val events = eventDao.getEventsForTrip("t1")
        assertThat(events).isEmpty()
    }

    @Test
    fun `contactDao insert and getAll`() = runTest {
        contactDao.insertContact(
            ContactEntity("c1", "Alice", "+123", "EMERGENCY", 1000L)
        )
        contactDao.insertContact(
            ContactEntity("c2", "Bob", "+456", "ARRIVAL", 2000L)
        )

        val contacts = contactDao.getAllContacts()
        assertThat(contacts).hasSize(2)
        assertThat(contacts.map { it.name }).containsExactly("Alice", "Bob")
    }

    @Test
    fun `contactDao getAll returns empty when no contacts`() = runTest {
        val contacts = contactDao.getAllContacts()
        assertThat(contacts).isEmpty()
    }

    @Test
    fun `settingsDao getSettings returns null when not set`() = runTest {
        val settings = settingsDao.getSettings()
        assertThat(settings).isNull()
    }

    @Test
    fun `settingsDao insert and get`() = runTest {
        settingsDao.insertSettings(
            SettingsEntity(id = "default_settings", autoStart = false, btTriggerDeviceId = null,
                dndInTrip = true, reminderEscalation = 1, retentionDays = 30,
                units = "km", emergencyNumber = "112")
        )

        val loaded = settingsDao.getSettings()
        assertThat(loaded).isNotNull()
        assertThat(loaded!!.dndInTrip).isTrue()
        assertThat(loaded.emergencyNumber).isEqualTo("112")
    }

    @Test
    fun `settingsDao insert replaces existing settings`() = runTest {
        settingsDao.insertSettings(
            SettingsEntity(id = "default_settings", autoStart = false, btTriggerDeviceId = null,
                dndInTrip = true, reminderEscalation = 1, retentionDays = 30,
                units = "km", emergencyNumber = "112")
        )
        settingsDao.insertSettings(
            SettingsEntity(id = "default_settings", autoStart = true, btTriggerDeviceId = null,
                dndInTrip = false, reminderEscalation = 2, retentionDays = 7,
                units = "mi", emergencyNumber = "911")
        )

        val loaded = settingsDao.getSettings()
        assertThat(loaded!!.autoStart).isTrue()
        assertThat(loaded.dndInTrip).isFalse()
        assertThat(loaded.retentionDays).isEqualTo(7)
        assertThat(loaded.units).isEqualTo("mi")
        assertThat(loaded.emergencyNumber).isEqualTo("911")
    }

    @Test
    fun `events are scoped to their trip`() = runTest {
        tripDao.insertTrip(aTrip(id = "t1"))
        eventDao.insertEvent(
            EventEntity("e1", "t1", 1000L, "BRAKE", 0.8f, 4.0, 0.0, 0.0, 0.9f)
        )

        val tripEvents = eventDao.getEventsForTrip("t1")
        assertThat(tripEvents).hasSize(1)

        val otherEvents = eventDao.getEventsForTrip("nonexistent")
        assertThat(otherEvents).isEmpty()
    }
}
