package com.example.ui.sessions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.RoundRecord
import com.example.domain.model.WorkoutSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SessionDetailScreen(
    session: WorkoutSession?,
    rounds: List<RoundRecord>,
    onDeleteSession: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    if (session == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Session not found", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBack) { Text("Back") }
        }
        return
    }

    val formattedDate = SimpleDateFormat("EEEE, MMM dd, yyyy · HH:mm", Locale.getDefault()).format(Date(session.date))

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("session_detail_screen")
    ) {
        // Top App Bar with Back & Delete
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Workout Summary",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.testTag("delete_session_icon")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Session",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hero Card: Total Jumps & Date
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${session.totalJumps}",
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "TOTAL JUMPS",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )

                if (session.correctedTotal != session.detectedTotal) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Detected: ${session.detectedTotal} · Corrected: ${session.correctedTotal}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Key Performance Metrics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val mins = session.activeSec / 60
            val secs = session.activeSec % 60
            DetailMetricTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Timer,
                value = String.format("%02d:%02d", mins, secs),
                label = "Active Time"
            )
            DetailMetricTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Speed,
                value = "${session.avgRate.toInt()}",
                label = "Avg RPM"
            )
            DetailMetricTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Star,
                value = "${session.bestStreak}",
                label = "Best Streak"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Secondary Metrics Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DetailMetricTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.LocalFireDepartment,
                value = session.calories?.let { "${it.toInt()} kcal" } ?: "Unavailable",
                label = "Est. Calories"
            )
            DetailMetricTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Speed,
                value = session.rpe?.let { "$it / 10" } ?: "--",
                label = "RPE Intensity"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Per-Round Breakdown
        if (rounds.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Round Breakdown",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    rounds.forEach { round ->
                        RoundRow(round = round)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Delete Session Button
        OutlinedButton(
            onClick = { showDeleteConfirm = true },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("delete_session_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
        ) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Delete This Workout Session")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Workout Session") },
            text = { Text("Are you sure you want to permanently delete this workout session? Personal records, stats, and adherence will be automatically recalculated.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteSession(session.uuid)
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailMetricTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RoundRow(round: RoundRecord) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Round ${round.roundNo}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "${round.detectedJumps} jumps (${round.activeSec}s)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
