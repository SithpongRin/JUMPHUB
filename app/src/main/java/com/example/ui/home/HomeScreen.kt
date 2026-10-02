package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.sync.SyncState
import com.example.domain.model.UserActivePlan

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onStartWorkout: () -> Unit,
    onOpenPlan: (String) -> Unit,
    onOpenWeight: () -> Unit,
    onSyncNow: () -> Unit,
    onNavVisibilityChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var autoUpdateInfo by remember { androidx.compose.runtime.mutableStateOf<com.example.service.update.AppUpdateInfo?>(null) }
    var showAutoUpdateDialog by remember { androidx.compose.runtime.mutableStateOf(false) }

    // Auto-check for updates when Home opens
    LaunchedEffect(Unit) {
        val info = com.example.service.update.InAppUpdateManager.checkForUpdate()
        if (info.hasUpdate) {
            autoUpdateInfo = info
            showAutoUpdateDialog = true
        }
    }

    // Dynamic floating nav response to scroll
    val isScrollingUp by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 ||
                    listState.isScrollInProgress.not() ||
                    listState.firstVisibleItemScrollOffset == 0
        }
    }

    LaunchedEffect(isScrollingUp) {
        onNavVisibilityChanged(isScrollingUp)
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header
        item {
            HeaderSection(uiState = uiState, onSyncNow = onSyncNow)
        }

        // Prominent Start Jump Rope Workout Button
        item {
            StartWorkoutHero(
                activePlan = uiState.activePlan,
                onStartWorkout = onStartWorkout,
                onOpenPlan = onOpenPlan
            )
        }

        // Weekly Progress Ring & Stats Card
        item {
            WeeklyProgressCard(
                weeklyMinutes = uiState.weeklyActiveMinutes,
                goalMinutes = uiState.preferences.weeklyGoalMinutes,
                streakDays = uiState.currentStreakDays,
                totalJumps = uiState.totalLifetimeJumps
            )
        }

        // Today's Session / Active Plan Card
        item {
            TodaySessionCard(
                activePlan = uiState.activePlan,
                onOpenPlan = onOpenPlan,
                onStartWorkout = onStartWorkout
            )
        }

        // Weekly Weight Due Prompt (When Due)
        if (uiState.isWeightDue) {
            item {
                WeeklyWeightPromptCard(onOpenWeight = onOpenWeight)
            }
        }
    }

    if (showAutoUpdateDialog && autoUpdateInfo != null) {
        val info = autoUpdateInfo!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAutoUpdateDialog = false },
            title = { androidx.compose.material3.Text("កំណែថ្មីមានស្រាប់: v${info.latestVersionName}") },
            text = {
                androidx.compose.foundation.layout.Column {
                    androidx.compose.material3.Text("JUMPHUB មានកំណែថ្មីដែលល្អប្រសើរជាងមុន! ចុចអាប់ដេតឥឡូវដើម្បីទាញយក និងដំឡើងដោយស្វ័យប្រវត្តិ។")
                    if (info.releaseNotes.isNotBlank()) {
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.Text(
                            text = info.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        showAutoUpdateDialog = false
                        info.downloadUrl?.let { url ->
                            com.example.service.update.InAppUpdateManager.startDownloadAndInstall(
                                context = context,
                                downloadUrl = url,
                                versionName = info.latestVersionName
                            )
                        }
                    }
                ) {
                    androidx.compose.material3.Text(stringResource(R.string.action_update_now))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showAutoUpdateDialog = false }) {
                    androidx.compose.material3.Text(stringResource(R.string.action_later))
                }
            }
        )
    }
}

@Composable
private fun HeaderSection(
    uiState: HomeUiState,
    onSyncNow: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Sync Status Pill (Clickable)
        val syncReport = uiState.syncReport
        val (icon, label, color) = when (syncReport.state) {
            SyncState.SYNCING -> Triple(Icons.Default.Sync, stringResource(R.string.action_sync_now), MaterialTheme.colorScheme.primary)
            SyncState.SUCCESS -> Triple(Icons.Default.CloudDone, stringResource(R.string.home_sync_up_to_date), MaterialTheme.colorScheme.primary)
            SyncState.OFFLINE_SAVED -> Triple(Icons.Default.CloudOff, stringResource(R.string.home_sync_offline), MaterialTheme.colorScheme.secondary)
            else -> Triple(Icons.Default.CloudDone, stringResource(R.string.home_sync_up_to_date), MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onSyncNow)
                .testTag("home_sync_pill"),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StartWorkoutHero(
    activePlan: UserActivePlan?,
    onStartWorkout: () -> Unit,
    onOpenPlan: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("start_workout_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (activePlan != null) activePlan.name else stringResource(R.string.action_start_workout),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (activePlan != null) "Week ${activePlan.currentWeek} · Day ${activePlan.currentDay} (WHO Guideline Standard)"
                else "សូមជ្រើសរើស Training Plan ជាមុនសិន ដើម្បីហាត់តាមកម្រិតត្រឹមត្រូវតាម WHO!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                modifier = Modifier.padding(horizontal = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    if (activePlan != null) {
                        onStartWorkout()
                    } else {
                        onOpenPlan("select")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("hero_start_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (activePlan != null) stringResource(R.string.action_start) else "ជ្រើសរើស Plan ឥឡូវនេះ",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun WeeklyProgressCard(
    weeklyMinutes: Int,
    goalMinutes: Int,
    streakDays: Int,
    totalJumps: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weekly_progress_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.home_weekly_progress),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.home_streak_days, streakDays),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Progress Indicator
                val progress = if (goalMinutes > 0) (weeklyMinutes.toFloat() / goalMinutes).coerceIn(0f, 1f) else 0f
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(76.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(76.dp),
                        strokeWidth = 8.dp,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        color = MaterialTheme.colorScheme.primary,
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_weekly_goal),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.home_active_minutes, weeklyMinutes, goalMinutes),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${stringResource(R.string.home_total_jumps)}: $totalJumps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TodaySessionCard(
    activePlan: UserActivePlan?,
    onOpenPlan: (String) -> Unit,
    onStartWorkout: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("today_session_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.home_today_session),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = if (activePlan != null) activePlan.name else stringResource(R.string.plan_health_title),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Week ${activePlan?.currentWeek ?: 1} · Day ${activePlan?.currentDay ?: 1}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "8 Rounds · 30s Jump / 60s Rest",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onStartWorkout,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.action_start))
                }
            }
        }
    }
}

@Composable
private fun WeeklyWeightPromptCard(
    onOpenWeight: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weekly_weight_card")
            .clickable(onClick = onOpenWeight),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Scale,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_weight_due_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.home_weight_due_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
