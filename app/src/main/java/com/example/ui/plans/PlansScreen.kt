package com.example.ui.plans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.TrainingPlan
import com.example.domain.model.UserActivePlan

@Composable
fun PlansScreen(
    plans: List<TrainingPlan>,
    activePlan: UserActivePlan?,
    selectedTrainingDays: Set<Int>,
    trainingStreak: Pair<Int, Int>, // (currentStreak, longestStreak)
    trainingAdherencePct: Int,
    onSelectPlan: (TrainingPlan) -> Unit,
    onToggleTrainingDay: (Int) -> Unit,
    onNavVisibilityChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

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
            .testTag("plans_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 4.dp)) {
                Text(
                    text = stringResource(R.string.plans_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.plans_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Training Adherence & Training Streak Card
        item {
            TrainingAdherenceCard(
                adherencePct = trainingAdherencePct,
                currentStreak = trainingStreak.first,
                longestStreak = trainingStreak.second
            )
        }

        // WHO Physical Activity Guidance Card
        item {
            WhoGuidanceCard()
        }

        // User Training Days Schedule Picker
        item {
            TrainingDaysPickerCard(
                selectedDays = selectedTrainingDays,
                onToggleDay = onToggleTrainingDay
            )
        }

        // Section Title: Available Programs
        item {
            Text(
                text = "Training Programs",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Plans List
        items(plans, key = { it.id }) { plan ->
            val isActive = activePlan?.planId == plan.id
            PlanItemCard(
                plan = plan,
                isActive = isActive,
                onSelectPlan = { onSelectPlan(plan) }
            )
        }
    }
}

@Composable
private fun TrainingAdherenceCard(
    adherencePct: Int,
    currentStreak: Int,
    longestStreak: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Training Schedule Adherence",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$adherencePct%",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Training Streak",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$currentStreak sessions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Longest Streak",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$longestStreak sessions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Rest days do not break your training streak.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun WhoGuidanceCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Designed Around WHO Physical Activity Recommendations",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "WHO physical activity guidelines recommend 150-300 min of moderate or 75-150 min of vigorous activity per week, plus muscle strengthening on 2+ days. Jump rope targets are designed by JUMPHUB to help you achieve these health recommendations progressively.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TrainingDaysPickerCard(
    selectedDays: Set<Int>,
    onToggleDay: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Your Training Days Schedule",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Select 2 to 7 days per week. Reminders will only alert you on selected days.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Days row (Mon - Sun)
            val dayItems = listOf(
                2 to "Mon",
                3 to "Tue",
                4 to "Wed",
                5 to "Thu",
                6 to "Fri",
                7 to "Sat",
                1 to "Sun"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayItems.forEach { (calDay, label) ->
                    val isSelected = selectedDays.contains(calDay)
                    Surface(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable { onToggleDay(calDay) },
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label.take(2),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanItemCard(
    plan: TrainingPlan,
    isActive: Boolean,
    onSelectPlan: () -> Unit
) {
    val (titleRes, descRes, icon, badgeColor) = when (plan.category) {
        "HEALTH" -> Quadruple(R.string.plan_health_title, R.string.plan_health_desc, Icons.Default.Favorite, MaterialTheme.colorScheme.primary)
        "ENDURANCE" -> Quadruple(R.string.plan_endurance_title, R.string.plan_endurance_desc, Icons.Default.DirectionsRun, MaterialTheme.colorScheme.secondary)
        "FAT_LOSS" -> Quadruple(R.string.plan_fat_loss_title, R.string.plan_fat_loss_desc, Icons.Default.LocalFireDepartment, MaterialTheme.colorScheme.tertiary)
        "QUICK" -> Quadruple(R.string.plan_quick_title, R.string.plan_quick_desc, Icons.Default.Speed, MaterialTheme.colorScheme.primary)
        else -> Quadruple(R.string.plan_free_jump_title, R.string.plan_free_jump_desc, Icons.Default.FitnessCenter, MaterialTheme.colorScheme.secondary)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("plan_card_${plan.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isActive) 2.dp else 1.dp,
            color = if (isActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(titleRes),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${plan.totalWeeks} Weeks · ${plan.daysPerWeek} Days/Week",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (isActive) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.plan_active_badge),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(descRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onSelectPlan,
                    enabled = !isActive,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        disabledContentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (isActive) stringResource(R.string.plan_active_badge)
                        else stringResource(R.string.plan_select)
                    )
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
