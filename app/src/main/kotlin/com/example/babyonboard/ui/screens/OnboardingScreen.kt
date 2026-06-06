package com.example.babyonboard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.babyonboard.ui.viewmodel.TripViewModel

@Composable
fun OnboardingScreen(viewModel: TripViewModel, onComplete: () -> Unit) {
    var babyMode by remember { mutableStateOf(true) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Welcome to Baby on Board",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Safe-Driving Telemetry for Families",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Honest Limits
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Honest Limits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• We do NOT detect children. Baby mode is user-activated.\n" +
                            "• The hot-car feature is a reminder, not a guarantee.\n" +
                            "• Crash response is best-effort SOS, not a regulated eCall."
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Baby Mode Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = "Enable Baby Mode (Stricter safety thresholds)",
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = babyMode,
                onCheckedChange = { babyMode = it }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Permissions (Simplified for now)
        Button(
            onClick = {
                // Request permissions logic
                onComplete()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Get Started")
        }
    }
}
