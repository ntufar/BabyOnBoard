package io.github.ntufar.babyonboard.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ntufar.babyonboard.domain.model.EventType
import io.github.ntufar.babyonboard.domain.model.TelemetryEvent
import io.github.ntufar.babyonboard.ui.viewmodel.TripViewModel
import kotlin.math.abs

@Composable
fun LiveTripScreen(
    viewModel: TripViewModel,
    onTripEnded: () -> Unit
) {
    val trip by viewModel.currentTrip.collectAsState()
    val elapsed by viewModel.elapsedSeconds.collectAsState()
    val speed by viewModel.currentSpeed.collectAsState()
    val events by viewModel.events.collectAsState()
    val speedHistory by viewModel.speedHistory.collectAsState()
    val accelHistory by viewModel.accelHistory.collectAsState()
    val vertAccelHistory by viewModel.vertAccelHistory.collectAsState()
    val latAccelHistory by viewModel.latAccelHistory.collectAsState()
    val debugLogs by viewModel.debugLogs.collectAsState()

    val harshEvents = events.count { it.severity > 0.5f }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Trip Recording",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = formatDuration(elapsed),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Duration",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${speed.toInt()}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "km/h",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${trip?.score ?: 100}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = if ((trip?.score ?: 100) >= 80) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Score",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Distance: ${(trip?.distanceM ?: 0.0).toInt()} m",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Max Speed: ${(trip?.maxSpeed ?: 0.0).toInt()} km/h",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Harsh Events: $harshEvents",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (harshEvents > 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Live Telemetry",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (speedHistory.size >= 2 || accelHistory.size >= 2) {
                    if (speedHistory.size >= 2) {
                        SparklineChart(
                            data = speedHistory,
                            label = "Speed (km/h)",
                            lineColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                        )
                    }
                    if (accelHistory.size >= 2) {
                        Spacer(modifier = Modifier.height(12.dp))
                        SparklineChart(
                            data = accelHistory,
                            label = "Long. Accel (m/s²)",
                            lineColor = MaterialTheme.colorScheme.tertiary,
                            zeroLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                        )
                    }
                    if (latAccelHistory.size >= 2) {
                        Spacer(modifier = Modifier.height(12.dp))
                        SparklineChart(
                            data = latAccelHistory,
                            label = "Lat. Accel (m/s²)",
                            lineColor = MaterialTheme.colorScheme.secondary,
                            zeroLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                        )
                    }
                    if (vertAccelHistory.size >= 2) {
                        Spacer(modifier = Modifier.height(12.dp))
                        SparklineChart(
                            data = vertAccelHistory,
                            label = "Vert. Accel (m/s²)",
                            lineColor = MaterialTheme.colorScheme.error,
                            zeroLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Waiting for sensor data…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (events.isNotEmpty()) {
            val lastEvents = events.takeLast(5)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Recent Events",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    lastEvents.forEach { event ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = event.type.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "%.1fg".format(event.value),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (trip?.babyMode == true) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "Baby Mode Active - Stricter Thresholds",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        if (debugLogs.isNotEmpty()) {
            var expanded by remember { mutableStateOf(true) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Debug Log (${debugLogs.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        TextButton(onClick = { expanded = !expanded }) {
                            Text(if (expanded) "Hide" else "Show")
                        }
                    }
                    if (expanded) {
                        val listState = rememberLazyListState()
                        LaunchedEffect(debugLogs.size) {
                            if (debugLogs.isNotEmpty()) listState.animateScrollToItem(debugLogs.size - 1)
                        }
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                        ) {
                            items(debugLogs) { line ->
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                viewModel.endTrip()
                onTripEnded()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.Close, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("End Trip")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SparklineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color.Blue,
    label: String = "",
    zeroLine: Boolean = false
) {
    if (data.size < 2) return
    Column(modifier = modifier) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        val strokeWidthPx = 4f
        val minVal = data.min()
        val maxVal = data.max()
        Canvas(modifier = Modifier.fillMaxSize()) {
            val range = maxVal - minVal
            val effectiveMin = if (range < 0.1f) minVal - 0.5f else minVal
            val effectiveMax = if (range < 0.1f) maxVal + 0.5f else maxVal
            val effectiveRange = effectiveMax - effectiveMin

            fun yFor(v: Float): Float =
                size.height - ((v - effectiveMin) / effectiveRange) * size.height

            if (zeroLine && effectiveMin < 0f && effectiveMax > 0f) {
                val zeroY = yFor(0f)
                drawLine(
                    color = lineColor.copy(alpha = 0.3f),
                    start = Offset(0f, zeroY),
                    end = Offset(size.width, zeroY),
                    strokeWidth = strokeWidthPx
                )
            }

            val path = Path()
            data.forEachIndexed { index, value ->
                val x = if (data.size > 1) index.toFloat() / (data.size - 1) * size.width else 0f
                val y = yFor(value)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = lineColor, style = Stroke(width = strokeWidthPx))
        }
    }
}

@Composable
fun EventCard(event: TelemetryEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (event.type) {
                EventType.BRAKE, EventType.CORNER -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = event.type.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "%.1fg | %.0f%% confidence".format(event.value, event.confidence * 100),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Surface(
                color = when {
                    event.severity > 0.7f -> MaterialTheme.colorScheme.error
                    event.severity > 0.4f -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                },
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "%.1f".format(event.severity),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s)
    else "%02d:%02d".format(m, s)
}
