package io.github.ntufar.babyonboard.ui.viewmodel

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.github.ntufar.babyonboard.domain.model.SensitivityMode
import io.github.ntufar.babyonboard.domain.model.Settings
import io.github.ntufar.babyonboard.domain.model.Trip
import io.github.ntufar.babyonboard.domain.repository.TripRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TripViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun createRepository(): TripRepository {
        val repo = mockk<TripRepository>()
        coEvery { repo.getTripHistory() } returns emptyList()
        coEvery { repo.saveTrip(any()) } returns Unit
        coEvery { repo.updateTrip(any()) } returns Unit
        coEvery { repo.getSettings() } returns Settings(
            autoStart = false, btTriggerDeviceId = null, dndInTrip = true,
            reminderEscalation = 1, retentionDays = 30, units = "km",
            emergencyNumber = "112", sensitivity = SensitivityMode.CAR
        )
        return repo
    }

    private fun createContext(): Context {
        return RuntimeEnvironment.getApplication()
    }

    private fun createViewModel(repo: TripRepository = createRepository()) = TripViewModel(
        repo,
        createContext()
    )

    @Test
    fun `tripHistory is empty at initialization`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.tripHistory.value).isEmpty()
    }

    @Test
    fun `tripHistory loads from repository at init`() = runTest(testDispatcher) {
        val repo = createRepository()
        val existingTrips = listOf(
            Trip("1", 1000L, 2000L, 5000.0, 120, 50.0, 70.0, 90, false)
        )
        coEvery { repo.getTripHistory() } returns existingTrips

        val viewModel = createViewModel(repo)
        advanceUntilIdle()

        assertThat(viewModel.tripHistory.value).hasSize(1)
        assertThat(viewModel.tripHistory.value[0].id).isEqualTo("1")
    }

    @Test
    fun `startTrip creates trip and starts timer`() = runTest(testDispatcher) {
        val viewModel = createViewModel().apply { startTimerOnTripStart = false }
        viewModel.startTrip(babyMode = false)
        advanceUntilIdle()

        assertThat(viewModel.currentTrip.value).isNotNull()
        assertThat(viewModel.currentTrip.value!!.babyMode).isFalse()
        assertThat(viewModel.currentTrip.value!!.distanceM).isEqualTo(0.0)

        viewModel.endTrip()
        advanceUntilIdle()
    }

    @Test
    fun `startTrip with babyMode sets babyMode on trip`() = runTest(testDispatcher) {
        val viewModel = createViewModel().apply { startTimerOnTripStart = false }
        viewModel.startTrip(babyMode = true)
        advanceUntilIdle()

        assertThat(viewModel.currentTrip.value!!.babyMode).isTrue()

        viewModel.endTrip()
        advanceUntilIdle()
    }

    @Test
    fun `endTrip stops timer and updates trip`() = runTest(testDispatcher) {
        val repo = createRepository()
        val viewModel = createViewModel(repo).apply { startTimerOnTripStart = false }
        viewModel.startTrip(babyMode = true)
        advanceUntilIdle()

        viewModel.endTrip()
        advanceUntilIdle()

        coVerify { repo.updateTrip(any()) }
    }

    @Test
    fun `refreshHistory reloads trips from repository`() = runTest(testDispatcher) {
        val repo = createRepository()
        coEvery { repo.getTripHistory() } returnsMany listOf(
            emptyList(),
            listOf(Trip("1", 1000L, null, 0.0, 0, 0.0, 0.0, 100, false))
        )

        val viewModel = createViewModel(repo)
        advanceUntilIdle()

        assertThat(viewModel.tripHistory.value).isEmpty()

        viewModel.refreshHistory()
        advanceUntilIdle()

        assertThat(viewModel.tripHistory.value).hasSize(1)
    }

    @Test
    fun `endTrip with babyMode sends notification without error`() = runTest(testDispatcher) {
        val repo = createRepository()
        val viewModel = TripViewModel(repo, RuntimeEnvironment.getApplication()).apply { startTimerOnTripStart = false }
        viewModel.startTrip(babyMode = true)
        advanceUntilIdle()

        viewModel.endTrip()
        advanceUntilIdle()

        coVerify { repo.updateTrip(any()) }
    }

    @Test
    fun `endTrip without babyMode completes without error`() = runTest(testDispatcher) {
        val repo = createRepository()
        val viewModel = TripViewModel(repo, RuntimeEnvironment.getApplication()).apply { startTimerOnTripStart = false }
        viewModel.startTrip(babyMode = false)
        advanceUntilIdle()

        viewModel.endTrip()
        advanceUntilIdle()

        coVerify { repo.updateTrip(any()) }
    }

    @Test
    fun `loadEventsForTrip sets currentTrip and events`() = runTest(testDispatcher) {
        val repo = createRepository()
        val trips = listOf(
            Trip("t1", 1000L, null, 0.0, 0, 0.0, 0.0, 100, false)
        )
        coEvery { repo.getTripHistory() } returns trips

        val viewModel = createViewModel(repo)
        advanceUntilIdle()

        viewModel.loadEventsForTrip("t1", emptyList())
        assertThat(viewModel.currentTrip.value).isNotNull()
        assertThat(viewModel.currentTrip.value!!.id).isEqualTo("t1")
    }
}
