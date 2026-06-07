package io.github.ntufar.babyonboard.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.ntufar.babyonboard.domain.model.Trip
import io.github.ntufar.babyonboard.ui.viewmodel.TripViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripHistoryScreen(
    viewModel: TripViewModel,
    onStartTrip: () -> Unit,
    onTripSelected: (Trip) -> Unit,
    onSettings: () -> Unit
) {
    val trips by viewModel.tripHistory.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshHistory()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Trip History")
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onStartTrip,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Start Trip") }
            )
        }
    ) { padding ->
        if (trips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "No trips yet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap the button below to start your first trip",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "trend_chart") {
                    ScoreTrendChart(trips = trips)
                }
                items(trips, key = { trip -> trip.id }) { trip ->
                    TripCard(
                        trip = trip,
                        onClick = { onTripSelected(trip) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreTrendChart(trips: List<Trip>) {
    val dataPoints = trips.sortedBy { it.startTs }.takeLast(10)
    if (dataPoints.size < 2) return

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val errorColor = MaterialTheme.colorScheme.error
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val avgScore = dataPoints.map { it.score }.average().toInt()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Score Trend",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "avg $avgScore/100",
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                val padL = 8.dp.toPx()
                val padR = 8.dp.toPx()
                val padT = 8.dp.toPx()
                val padB = 8.dp.toPx()
                val chartW = size.width - padL - padR
                val chartH = size.height - padT - padB
                val n = dataPoints.size

                // Threshold guide lines at 60 and 80
                val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
                listOf(60f to tertiaryColor, 80f to primaryColor).forEach { (threshold, color) ->
                    val y = padT + chartH * (1f - threshold / 100f)
                    drawLine(
                        color = color.copy(alpha = 0.25f),
                        start = Offset(padL, y),
                        end = Offset(size.width - padR, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dash
                    )
                }

                // Compute screen positions
                val pts = dataPoints.mapIndexed { i, trip ->
                    val x = padL + i * chartW / (n - 1).toFloat()
                    val y = padT + chartH * (1f - trip.score / 100f)
                    Offset(x, y)
                }

                // Connecting line
                val linePath = Path().apply {
                    pts.forEachIndexed { i, pt ->
                        if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                    }
                }
                drawPath(
                    path = linePath,
                    color = onSurfaceVariant.copy(alpha = 0.4f),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Data point dots
                dataPoints.forEachIndexed { i, trip ->
                    val dotColor = when {
                        trip.score >= 80 -> primaryColor
                        trip.score >= 60 -> tertiaryColor
                        else -> errorColor
                    }
                    drawCircle(color = dotColor, radius = 4.dp.toPx(), center = pts[i])
                    drawCircle(
                        color = dotColor.copy(alpha = 0.2f),
                        radius = 7.dp.toPx(),
                        center = pts[i]
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "oldest",
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariant
                )
                Text(
                    text = "${dataPoints.size} trips",
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariant
                )
                Text(
                    text = "latest",
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TripCard(
    trip: Trip,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormat.format(Date(trip.startTs)),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = when {
                        trip.score >= 80 -> MaterialTheme.colorScheme.primary
                        trip.score >= 60 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "${trip.score}/100",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Distance", "${trip.distanceM.toInt()} m")
                StatItem("Duration", formatDuration(trip.durationS))
                StatItem("Max Speed", "${trip.maxSpeed.toInt()} km/h")
            }

            if (trip.babyMode) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Baby Mode",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%dh %02dm".format(h, m)
    else "%dm %02ds".format(m, s)
}
