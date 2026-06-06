package com.example.babyonboard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.babyonboard.domain.model.Models.*

@Composable
fun TripSummaryScreen(trip: Trip, events: List<TelemetryEvent>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Trip Summary",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Safe-Driving Score",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${trip.score} / 100",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Avg Speed", style = MaterialTheme.typography.bodyLarge)
                Text("${trip.avgSpeed} km/h", style = MaterialTheme.typography.headlineSmall)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Distance", style = MaterialTheme.typography.bodyLarge)
                Text("${trip.distanceM.toInt()} m", style = MaterialTheme.typography.headlineSmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Harsh Events Detected (${events.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(events) { event ->
                ListItem(
                    headlineContent = { Text("${event.type}: ${event.value.toInt()}") },
                    supportingContent = { Text("Confidence: ${(event.confidence * 100).toInt()}%") },
                    trailingContent = {
                        val color = if (event.severity > 0.7f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        Surface(color = color, shape = MaterialTheme.shapes.small) {
                            Text(
                                text = event.severity.toString(),
                                modifier = Modifier.padding(4.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                )
            }
        }
    }
}
