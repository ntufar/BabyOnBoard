package io.github.ntufar.babyonboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.ntufar.babyonboard.domain.model.EventType
import io.github.ntufar.babyonboard.domain.model.Settings
import io.github.ntufar.babyonboard.domain.model.TelemetryEvent
import io.github.ntufar.babyonboard.domain.model.Trip
import io.github.ntufar.babyonboard.ui.screens.*
import io.github.ntufar.babyonboard.ui.theme.BabyOnBoardTheme
import io.github.ntufar.babyonboard.ui.viewmodel.TripViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BabyOnBoardTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val viewModel: TripViewModel = hiltViewModel()
                    var currentScreen by remember { mutableStateOf("onboarding") }
                    var tripData by remember { mutableStateOf<Trip?>(null) }
                    var events by remember { mutableStateOf(emptyList<TelemetryEvent>()) }

                    when (currentScreen) {
                        "onboarding" -> {
                            OnboardingScreen(
                                onComplete = {
                                    currentScreen = "summary"
                                    tripData = Trip(
                                        "1", System.currentTimeMillis(), System.currentTimeMillis(),
                                        10000.0, 600, 45.0, 80.0, 92, true, null
                                    )
                                    events = listOf(
                                        TelemetryEvent(
                                            "e1", "1", System.currentTimeMillis(),
                                            EventType.BRAKE, 0.8f, 4.5, 10.0, 10.0, 0.9f
                                        )
                                    )
                                }
                            )
                        }
                        "summary" -> {
                            tripData?.let { TripSummaryScreen(it, events) }
                        }
                        "settings" -> {
                            SettingsScreen(
                                Settings(true, null, true, 1, 30, "km", "112")
                            ) { _ -> }
                        }
                    }
                }
            }
        }
    }
}
