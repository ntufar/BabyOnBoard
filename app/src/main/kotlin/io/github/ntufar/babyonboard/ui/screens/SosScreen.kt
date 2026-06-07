package io.github.ntufar.babyonboard.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SosScreen(
    emergencyNumber: String = "112",
    onCancel: () -> Unit,
    onCountdownFinished: () -> Unit
) {
    val context = LocalContext.current
    var countdown by remember { mutableStateOf(60) }
    val animatedProgress by animateFloatAsState(
        targetValue = countdown / 60f,
        label = "countdown"
    )

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$emergencyNumber")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        onCountdownFinished()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "CRASH DETECTED",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )

            Text(
                text = "SOS will be sent in",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Text(
                text = "$countdown",
                style = MaterialTheme.typography.displayLarge,
                fontSize = 72.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.error
            )

            LinearProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(8.dp),
                color = MaterialTheme.colorScheme.error,
                trackColor = MaterialTheme.colorScheme.errorContainer
            )

            Text(
                text = "Calling $emergencyNumber",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cancel SOS",
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
