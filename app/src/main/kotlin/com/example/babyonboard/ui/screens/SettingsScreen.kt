package com.example.babyonboard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.babyonboard.domain.model.Settings

@Composable
fun SettingsScreen(settings: Settings, onSettingsUpdate: (Settings) -> Unit) {
    var dndEnabled by remember { mutableStateOf(settings.dndInTrip) }
    var emergencyNumber by remember { mutableStateOf(settings.emergencyNumber) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text("Do Not Disturb during trip")
            Switch(
                checked = dndEnabled,
                onCheckedChange = { 
                    dndEnabled = it
                    onSettingsUpdate(settings.copy(dndInTrip = it))
                }
            )
        }

        OutlinedTextField(
            value = emergencyNumber,
            onValueChange = {
                emergencyNumber = it
                onSettingsUpdate(settings.copy(emergencyNumber = it))
            },
            label = { Text("Emergency Number") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                onSettingsUpdate(settings.copy(dndInTrip = dndEnabled, emergencyNumber = emergencyNumber))
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Save Settings")
        }
    }
}
