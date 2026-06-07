package io.github.ntufar.babyonboard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.ntufar.babyonboard.domain.model.EventType
import io.github.ntufar.babyonboard.domain.model.TelemetryEvent
import io.github.ntufar.babyonboard.domain.model.Trip
import kotlin.math.roundToInt

@Composable
fun TripSummaryScreen(
    trip: Trip,
    events: List<TelemetryEvent>,
    units: String = "km",
    onBackToHistory: () -> Unit
) {
    val speedUnit = if (units == "mi") "mph" else "km/h"
    val distUnit = if (units == "mi") "mi" else "m"
    val displaySpeed = if (units == "mi") trip.avgSpeed * 0.621371 else trip.avgSpeed
    val displayDist = if (units == "mi") trip.distanceM * 0.000621371 else trip.distanceM.toDouble()
    val distFormat = if (units == "mi") "%.2f".format(displayDist) else "${trip.distanceM.toInt()}"

    val harshEvents = events.count { it.severity > 0.5f }
    val breakdown = computeScoreBreakdown(events, trip.score)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Trip Summary",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    trip.score >= 80 -> MaterialTheme.colorScheme.primaryContainer
                    trip.score >= 60 -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.errorContainer
                }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Safe-Driving Score",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${trip.score} / 100",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = when {
                        trip.score >= 80 -> MaterialTheme.colorScheme.primary
                        trip.score >= 60 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }
        }

        ScoreBreakdownCard(breakdown)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Avg Speed", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = "%.1f".format(displaySpeed),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(speedUnit, style = MaterialTheme.typography.labelSmall)
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Distance", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = distFormat,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(distUnit, style = MaterialTheme.typography.labelSmall)
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Duration", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = formatDurationShort(trip.durationS),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (harshEvents == 0) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Harsh Events: $harshEvents",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (events.isNotEmpty()) {
            Text(
                text = "Events",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    EventCard(event)
                }
            }
        }

        if (trip.babyMode) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Recorded in Baby Mode",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center
                )
            }
        }

        Button(
            onClick = onBackToHistory,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Default.Home, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Back to History")
        }
    }
}

private fun formatDurationShort(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%dh %02dm".format(h, m)
    else "%dm %02ds".format(m, s)
}

internal data class EventTypeDeduction(
    val type: EventType,
    val count: Int,
    val deductionPts: Int
)

internal fun computeScoreBreakdown(
    events: List<TelemetryEvent>,
    score: Int
): List<EventTypeDeduction> {
    val harsh = events.filter { it.severity > 0.5f }
    if (harsh.isEmpty()) return emptyList()
    val totalDeduction = (100 - score).coerceIn(0, 100)
    val harshCount = harsh.size
    return harsh
        .groupBy { it.type }
        .map { (type, typeEvents) ->
            val pts = ((typeEvents.size.toFloat() / harshCount) * totalDeduction).roundToInt()
            EventTypeDeduction(type, typeEvents.size, pts)
        }
        .filter { it.deductionPts > 0 }
        .sortedByDescending { it.deductionPts }
}

@Composable
private fun ScoreBreakdownCard(breakdown: List<EventTypeDeduction>) {
    if (breakdown.isEmpty()) return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Score Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            breakdown.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${item.count}× ${eventTypeLabel(item.type)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "−${item.deductionPts} pts",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun eventTypeLabel(type: EventType) = when (type) {
    EventType.BRAKE -> "hard brake"
    EventType.ACCEL -> "hard acceleration"
    EventType.CORNER -> "sharp corner"
    EventType.SWERVE -> "swerve"
    EventType.ROUGH -> "rough road"
    EventType.SPEED -> "speeding"
    EventType.PHONE_USE -> "phone use"
    EventType.CRASH -> "crash"
}
