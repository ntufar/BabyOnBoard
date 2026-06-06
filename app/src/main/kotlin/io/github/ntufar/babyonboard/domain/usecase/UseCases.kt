package io.github.ntufar.babyonboard.domain.usecase

import io.github.ntufar.babyonboard.domain.model.Trip
import io.github.ntufar.babyonboard.domain.repository.TripRepository

class StartTripUseCase(private val repository: TripRepository) {
    suspend fun execute(babyMode: Boolean): Trip {
        return Trip(
            id = "temp_id",
            startTs = System.currentTimeMillis(),
            endTs = null,
            distanceM = 0.0,
            durationS = 0,
            avgSpeed = 0.0,
            maxSpeed = 0.0,
            score = 100,
            babyMode = babyMode
        )
    }
}
