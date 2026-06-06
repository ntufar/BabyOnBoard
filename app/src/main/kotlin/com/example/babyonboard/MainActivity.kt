package com.example.babyonboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import com.example.babyonboard.ui.screens.*
import com.example.babyonboard.ui.theme.BabyOnBoardTheme
import com.example.babyonboard.ui.viewmodel.TripViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BabyOnBoardTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val viewModel = TripViewModel(TripRepositoryImpl(MockTripDao(), MockEventDao(), MockContactDao(), MockSettingsDao()))
                    var currentScreen by remember { mutableStateOf("onboarding") }
                    var tripData by remember { mutableStateOf<com.example.babyonboard.domain.model.Models.Trip?>(null) }
                    var events by remember { mutableStateOf(emptyList<com.example.babyonboard.domain.model.Models.TelemetryEvent>()) }

                    when (currentScreen) {
                        "onboarding" -> {
                            OnboardingScreen(viewModel) {
                                currentScreen = "summary"
                                // Mock trip data for summary
                                tripData = com.example.babyonboard.domain.model.Models.Trip(
                                    "1", System.currentTimeMillis(), System.currentTimeMillis(), 
                                    10000.0, 600, 45.0, 80.0, 92, true, null
                                )
                                events = listOf(
                                    com.example.babyonboard.domain.model.Models.TelemetryEvent(
                                        "e1", "1", System.currentTimeMillis(), 
                                        com.example.babyonboard.domain.model.Models.EventType.BRAKE, 0.8f, 4.5, 10.0, 10.0, 0.9f
                                    )
                                )
                            }
                        }
                        "summary" -> {
                            tripData?.let {
                                TripSummaryScreen(it, events)
                            }
                        }
                        "settings" -> {
                            SettingsScreen(
                                com.example.babyonboard.domain.model.Models.Settings(
                                    true, null, true, 1, 30, "km", "112"
                                )
                            ) { _ -> }
                        }
                    }
                }
            }
        }
    }
}

// Mocks for immediate compilation
class MockTripDao : com.example.babyonboard.data.db.TripDao {
    override suspend fun insertTrip(trip: com.example.babyonboard.data.model.Entities.TripEntity) {}
    override suspend fun updateTrip(trip: com.example.babyonboard.data.model.Entities.TripEntity) {}
    override suspend fun getAllTrips(): List<com.example.babyonboard.data.model.Entities.TripEntity> = emptyList()
}
class MockEventDao : com.example.babyonboard.data.db.EventDao {
    override suspend fun insertEvent(event: com.example.babyonboard.data.model.Entities.EventEntity) {}
    override suspend fun getEventsForTrip(tripId: String): List<com.example.babyonboard.data.model.Entities.EventEntity> = emptyList()
}
class MockContactDao : com.example.babyonboard.data.db.ContactDao {
    override suspend fun insertContact(contact: com.example.babyonboard.data.model.Entities.ContactEntity) {}
    override suspend fun getContacts(): List<com.example.babyonboard.data.model.Entities.ContactEntity> = emptyList()
}
class MockSettingsDao : com.example.babyonboard.data.db.SettingsDao {
    override suspend fun getSettings(): com.example.babyonboard.data.model.Entities.SettingsEntity? = null
    override suspend fun insertSettings(settings: com.example.babyonboard.data.model.Entities.SettingsEntity) {}
}
