package com.example.ui.workout

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.workout.WorkoutPhase
import com.example.domain.workout.WorkoutState
import com.example.service.WorkoutForegroundService

import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun WorkoutScreen(
    onFinishWorkout: (WorkoutState) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val workoutState by WorkoutForegroundService.serviceState.collectAsStateWithLifecycle()

    // Keep screen on during active workout
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Auto-navigate to summary when workout finishes
    LaunchedEffect(workoutState.phase) {
        if (workoutState.phase == WorkoutPhase.FINISHED) {
            onFinishWorkout(workoutState)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("workout_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Phase & Round Status
        TopPhaseBanner(
            phase = workoutState.phase,
            currentRound = workoutState.currentRound,
            totalRounds = workoutState.totalRounds,
            restRemainingSeconds = workoutState.restRemainingSeconds,
            activeSeconds = workoutState.activeSeconds
        )

        // Center: Huge Countdown OR Huge Jump Counter + Streaks
        if (workoutState.phase == WorkoutPhase.COUNTDOWN) {
            CountdownDisplay(seconds = workoutState.countdownSeconds)
        } else {
            CenterJumpCounter(
                phase = workoutState.phase,
                totalRounds = workoutState.totalRounds,
                currentRound = workoutState.currentRound,
                totalJumps = workoutState.totalJumps,
                roundJumps = workoutState.roundJumps,
                currentStreak = workoutState.currentStreak,
                bestStreak = workoutState.bestStreak,
                lastCompletedRoundSummary = workoutState.lastCompletedRoundSummary
            )
        }

        // Live Secondary Metrics Grid
        LiveMetricsBar(
            currentCadenceJpm = workoutState.currentCadenceJpm,
            activeSeconds = workoutState.activeSeconds,
            elapsedSeconds = workoutState.elapsedSeconds,
            roundTargetSeconds = workoutState.roundTargetSeconds,
            targetJumps = workoutState.targetJumps,
            estimatedCalories = workoutState.estimatedCalories
        )

        // Hands-Free Bottom Controls (Pause, Stop, Manual +/- Correction)
        BottomWorkoutControls(
            isPaused = workoutState.phase == WorkoutPhase.PAUSED,
            onPause = { sendServiceAction(context, WorkoutForegroundService.ACTION_PAUSE) },
            onResume = { sendServiceAction(context, WorkoutForegroundService.ACTION_RESUME) },
            onStop = {
                sendServiceAction(context, WorkoutForegroundService.ACTION_FINISH)
                onFinishWorkout(workoutState)
            },
            onAdjustJumps = { delta ->
                val intent = Intent(context, WorkoutForegroundService::class.java).apply {
                    action = WorkoutForegroundService.ACTION_ADJUST_JUMPS
                    putExtra(WorkoutForegroundService.EXTRA_DELTA_JUMPS, delta)
                }
                context.startService(intent)
            }
        )
    }
}

private fun sendServiceAction(context: Context, action: String) {
    val intent = Intent(context, WorkoutForegroundService::class.java).apply {
        this.action = action
    }
    context.startService(intent)
}

@Composable
private fun TopPhaseBanner(
    phase: WorkoutPhase,
    currentRound: Int,
    totalRounds: Int,
    restRemainingSeconds: Int,
    activeSeconds: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 16.dp)
    ) {
        val (bannerText, containerColor, textColor) = when (phase) {
            WorkoutPhase.COUNTDOWN -> Triple(
                "PREPARE",
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.onSecondaryContainer
            )
            WorkoutPhase.WARMUP -> Triple(
                "WARMUP",
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.onPrimaryContainer
            )
            WorkoutPhase.RESTING -> Triple(
                "REST · ${restRemainingSeconds}s",
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.onTertiaryContainer
            )
            WorkoutPhase.PAUSED -> Triple(
                "PAUSED",
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.onErrorContainer
            )
            WorkoutPhase.FINISHED -> Triple(
                "COMPLETE",
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.onPrimaryContainer
            )
            else -> Triple(
                "ROUND $currentRound OF $totalRounds",
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = containerColor
        ) {
            Text(
                text = bannerText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = textColor,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        WorkoutTimerText(activeSeconds = activeSeconds)
    }
}

@Composable
private fun WorkoutTimerText(activeSeconds: Int) {
    val mins = activeSeconds / 60
    val secs = activeSeconds % 60
    Text(
        text = String.format("%02d:%02d", mins, secs),
        style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        ),
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun CountdownDisplay(seconds: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(vertical = 32.dp)
    ) {
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn()
        ) {
            Text(
                text = if (seconds > 0) "$seconds" else "START!",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 110.sp,
                    lineHeight = 110.sp,
                    fontWeight = FontWeight.Black
                ),
                color = if (seconds > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Counting will start after cue",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CenterJumpCounter(
    phase: WorkoutPhase,
    totalRounds: Int,
    currentRound: Int,
    totalJumps: Int,
    roundJumps: Int,
    currentStreak: Int,
    bestStreak: Int,
    lastCompletedRoundSummary: com.example.domain.workout.CompletedRoundData?
) {
    val isMultiRound = totalRounds > 1
    val displayJumps = if (isMultiRound) roundJumps else totalJumps
    val jumpLabel = if (isMultiRound) "ROUND $currentRound JUMPS" else stringResource(R.string.unit_jumps).uppercase()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        BigJumpCounter(displayJumps = displayJumps, jumpLabel = jumpLabel)

        // On-screen Round Summary Card when resting
        if (phase == WorkoutPhase.RESTING && lastCompletedRoundSummary != null) {
            RoundSummaryCard(summary = lastCompletedRoundSummary)
        }

        Spacer(modifier = Modifier.height(12.dp))

        val pillText = if (isMultiRound) {
            "Total: $totalJumps · Streak: $currentStreak"
        } else {
            "Streak: $currentStreak (Best: $bestStreak)"
        }
        StreakPill(pillText = pillText)
    }
}

@Composable
private fun BigJumpCounter(displayJumps: Int, jumpLabel: String) {
    Text(
        text = "$displayJumps",
        style = MaterialTheme.typography.displayLarge.copy(
            fontSize = 104.sp,
            lineHeight = 104.sp,
            fontWeight = FontWeight.Black
        ),
        color = MaterialTheme.colorScheme.primary
    )
    Text(
        text = jumpLabel,
        style = MaterialTheme.typography.titleMedium.copy(
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun RoundSummaryCard(summary: com.example.domain.workout.CompletedRoundData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Round ${summary.round} Summary",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${summary.jumps}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "Jumps",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${summary.jpm}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "JPM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
                if (summary.bestStreak > 0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${summary.bestStreak}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Best Streak",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakPill(pillText: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = pillText,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun LiveMetricsBar(
    currentCadenceJpm: Float,
    activeSeconds: Int,
    elapsedSeconds: Int,
    roundTargetSeconds: Int,
    targetJumps: Int,
    estimatedCalories: Float?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LiveMetricBox(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Speed,
                value = "${currentCadenceJpm.toInt()}",
                label = "JPM"
            )
            val mins = activeSeconds / 60
            val secs = activeSeconds % 60
            LiveMetricBox(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Timer,
                value = String.format("%02d:%02d", mins, secs),
                label = "Active Time"
            )
            LiveMetricBox(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Timer,
                value = "${elapsedSeconds / 60}m ${elapsedSeconds % 60}s",
                label = "Total Elapsed"
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LiveMetricBox(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Speed,
                value = if (roundTargetSeconds > 0) "${roundTargetSeconds}s" else "$targetJumps jumps",
                label = "Round Target"
            )
            LiveMetricBox(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.LocalFireDepartment,
                value = estimatedCalories?.let { "${it.toInt()} kcal" } ?: "Unavailable",
                label = "Calories (Est)"
            )
        }
    }
}

@Composable
private fun LiveMetricBox(
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
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
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
private fun BottomWorkoutControls(
    isPaused: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onAdjustJumps: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Manual +/- Correction Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { onAdjustJumps(-1) },
                shape = CircleShape,
                modifier = Modifier.size(48.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "-1 jump",
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = "Manual Correction",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = { onAdjustJumps(1) },
                shape = CircleShape,
                modifier = Modifier.size(48.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "+1 jump",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Large Primary Workout Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { if (isPaused) onResume() else onPause() },
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .testTag("workout_pause_resume_button"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Resume" else "Pause",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(38.dp)
                )
            }

            Button(
                onClick = onStop,
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .testTag("workout_stop_button"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop Workout",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
    }
}
