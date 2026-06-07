package io.github.ntufar.babyonboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.ntufar.babyonboard.domain.model.Trip
import io.github.ntufar.babyonboard.sensing.TripForegroundService
import io.github.ntufar.babyonboard.ui.screens.*
import io.github.ntufar.babyonboard.ui.theme.BabyOnBoardTheme
import io.github.ntufar.babyonboard.ui.viewmodel.SettingsViewModel
import io.github.ntufar.babyonboard.ui.viewmodel.TripViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BabyOnBoardTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val tripViewModel: TripViewModel = hiltViewModel()
                    val settingsViewModel: SettingsViewModel = hiltViewModel()

                    var babyMode by remember { mutableStateOf(true) }
                    var emergencyNumber by remember { mutableStateOf("112") }

                    val sosReceiver = remember {
                        object : BroadcastReceiver() {
                            override fun onReceive(context: Context, intent: Intent) {
                                if (intent.action == TripForegroundService.ACTION_CRASH_DETECTED) {
                                    navController.navigate("sos") {
                                        popUpTo(0) { inclusive = false }
                                    }
                                }
                            }
                        }
                    }

                    DisposableEffect(Unit) {
                        val filter = IntentFilter(TripForegroundService.ACTION_CRASH_DETECTED)
                        registerReceiver(sosReceiver, filter, Context.RECEIVER_EXPORTED)
                        onDispose { unregisterReceiver(sosReceiver) }
                    }

                    LaunchedEffect(Unit) {
                        settingsViewModel.settings.collect { s ->
                            s?.let { emergencyNumber = it.emergencyNumber }
                        }
                    }

                    NavHost(navController = navController, startDestination = "onboarding") {
                        composable("sos") {
                            SosScreen(
                                emergencyNumber = emergencyNumber,
                                onCancel = { navController.popBackStack() },
                                onCountdownFinished = { navController.popBackStack() }
                            )
                        }

                        composable("onboarding") {
                            OnboardingScreen(
                                onComplete = { mode ->
                                    babyMode = mode
                                    tripViewModel.startTrip(mode)
                                    navController.navigate("live_trip") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("trip_history") {
                            TripHistoryScreen(
                                viewModel = tripViewModel,
                                onStartTrip = {
                                    tripViewModel.startTrip(babyMode)
                                    navController.navigate("live_trip")
                                },
                                onTripSelected = { trip ->
                                    navController.navigate("trip_summary/${trip.id}")
                                },
                                onSettings = {
                                    navController.navigate("settings")
                                }
                            )
                        }

                        composable("live_trip") {
                            LiveTripScreen(
                                viewModel = tripViewModel,
                                onTripEnded = {
                                    val trip = tripViewModel.currentTrip.value
                                    if (trip != null) {
                                        navController.navigate("trip_summary/${trip.id}") {
                                            popUpTo("trip_history") { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        composable("trip_summary/{tripId}") { backStackEntry ->
                            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable

                            LaunchedEffect(tripId) {
                                tripViewModel.loadEventsForTrip(tripId, emptyList())
                            }

                            val currentTrip by tripViewModel.currentTrip.collectAsState()
                            val events by tripViewModel.events.collectAsState()

                            currentTrip?.let { trip ->
                                TripSummaryScreen(
                                    trip = trip,
                                    events = events,
                                    onBackToHistory = {
                                        navController.navigate("trip_history") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }

                        composable("settings") {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
