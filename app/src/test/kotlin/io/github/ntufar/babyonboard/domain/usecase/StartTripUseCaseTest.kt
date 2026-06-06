package io.github.ntufar.babyonboard.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.github.ntufar.babyonboard.domain.model.Settings
import io.github.ntufar.babyonboard.domain.model.Trip
import io.github.ntufar.babyonboard.domain.repository.TripRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class StartTripUseCaseTest {

    private val repository: TripRepository = mockk()

    @Test
    fun `execute creates trip with UUID as id`() = runTest {
        coEvery { repository.saveTrip(any()) } returns Unit

        val useCase = StartTripUseCase(repository)
        val trip = useCase.execute(babyMode = true)

        assertThat(trip.id).isNotEmpty()
        // UUID format: 8-4-4-4-12 hex chars
        assertThat(trip.id).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    }

    @Test
    fun `execute sets startTs to current time`() = runTest {
        coEvery { repository.saveTrip(any()) } returns Unit

        val useCase = StartTripUseCase(repository)
        val before = System.currentTimeMillis()
        val trip = useCase.execute(babyMode = false)
        val after = System.currentTimeMillis()

        assertThat(trip.startTs).isAtLeast(before)
        assertThat(trip.startTs).isAtMost(after)
    }

    @Test
    fun `execute sets endTs to null`() = runTest {
        coEvery { repository.saveTrip(any()) } returns Unit

        val useCase = StartTripUseCase(repository)
        val trip = useCase.execute(babyMode = false)

        assertThat(trip.endTs).isNull()
    }

    @Test
    fun `execute initialises metrics to zero`() = runTest {
        coEvery { repository.saveTrip(any()) } returns Unit

        val useCase = StartTripUseCase(repository)
        val trip = useCase.execute(babyMode = false)

        assertThat(trip.distanceM).isEqualTo(0.0)
        assertThat(trip.durationS).isEqualTo(0)
        assertThat(trip.avgSpeed).isEqualTo(0.0)
        assertThat(trip.maxSpeed).isEqualTo(0.0)
        assertThat(trip.score).isEqualTo(100)
    }

    @Test
    fun `execute sets babyMode from parameter`() = runTest {
        coEvery { repository.saveTrip(any()) } returns Unit

        val useCase = StartTripUseCase(repository)
        val tripTrue = useCase.execute(babyMode = true)
        val tripFalse = useCase.execute(babyMode = false)

        assertThat(tripTrue.babyMode).isTrue()
        assertThat(tripFalse.babyMode).isFalse()
    }

    @Test
    fun `execute persists trip to repository`() = runTest {
        coEvery { repository.saveTrip(any()) } returns Unit

        val useCase = StartTripUseCase(repository)
        useCase.execute(babyMode = true)

        coVerify(exactly = 1) { repository.saveTrip(any<Trip>()) }
    }

    @Test
    fun `execute routeRef is null`() = runTest {
        coEvery { repository.saveTrip(any()) } returns Unit

        val useCase = StartTripUseCase(repository)
        val trip = useCase.execute(babyMode = false)

        assertThat(trip.routeRef).isNull()
    }
}
