package com.example.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import kotlinx.coroutines.launch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.datastore.UserPreferences

@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    onSetUiLanguage: (String) -> Unit,
    onSetVoiceLanguage: (String) -> Unit,
    onSetVoiceCueMode: (String) -> Unit,
    onSetVoiceJumpInterval: (Int) -> Unit,
    onSetVoiceTimeInterval: (Int) -> Unit = {},
    onSetVoiceTargetMilestones: (Boolean) -> Unit,
    onSetVoiceRoundSummaryEnabled: (Boolean) -> Unit = {},
    onSetVoiceRoundSummaryJumps: (Boolean) -> Unit = {},
    onSetVoiceRoundSummaryJpm: (Boolean) -> Unit = {},
    onSetVoiceRoundSummaryStreak: (Boolean) -> Unit = {},
    onSetThemeMode: (String) -> Unit,
    onSetVoiceEnabled: (Boolean) -> Unit,
    onSetPhonePosition: (String) -> Unit,
    onSetSensitivity: (String) -> Unit,
    onSetProfile: (name: String, age: Int, heightCm: Float, weightKg: Float?) -> Unit,
    onOpenAccount: () -> Unit,
    onOpenWeight: () -> Unit,
    onCheckUpdates: () -> Unit,
    onResetData: () -> Unit,
    onNavVisibilityChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    var showResetDialog by remember { mutableStateOf(false) }
    var isCheckingUpdates by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<com.example.service.update.AppUpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf<String?>(null) }

    var nameInput by remember(preferences.userName) { mutableStateOf(preferences.userName) }
    var ageInput by remember(preferences.userAge) { mutableStateOf("${preferences.userAge}") }
    var heightInput by remember(preferences.userHeightCm) { mutableStateOf("${preferences.userHeightCm.toInt()}") }
    var weightInput by remember(preferences.userWeightKg) { mutableStateOf(preferences.userWeightKg?.let { String.format("%.1f", it) } ?: "") }
    var profileSavedSuccess by remember { mutableStateOf(false) }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 6.dp)) {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Account & Sync Card
        item {
            SettingsCard(title = stringResource(R.string.settings_section_account), icon = Icons.Default.AccountCircle) {
                SettingsActionRow(
                    label = stringResource(R.string.account_title),
                    value = if (preferences.lastSyncTimestamp > 0) stringResource(R.string.home_sync_up_to_date) else stringResource(R.string.settings_never_synced),
                    onClick = onOpenAccount
                )
            }
        }

        // Voice Cue System Card
        item {
            SettingsCard(title = stringResource(R.string.settings_section_audio), icon = Icons.Default.VolumeUp) {
                // Voice enabled switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_voice_enabled),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = preferences.voiceEnabled,
                        onCheckedChange = onSetVoiceEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Voice Language Toggle
                SettingsChoiceRow(
                    label = stringResource(R.string.settings_voice_language),
                    currentValue = if (preferences.voiceLanguage == "km") "Khmer" else "English",
                    options = listOf("en" to "English", "km" to "Khmer"),
                    onSelect = onSetVoiceLanguage
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Voice Cue Mode
                Text(
                    text = "Voice Cue Mode",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                val cueModes = listOf(
                    "PHASE_ONLY" to "Phase Only",
                    "EVERY_JUMP" to "Every Jump",
                    "EVERY_N_JUMPS" to "Every N Jumps",
                    "TARGET_MILESTONES" to "Milestones",
                    "EVERY_N_MINUTES" to "Every N Min",
                    "CUSTOM" to "Custom"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    cueModes.take(3).forEach { (modeKey, modeTitle) ->
                        val isSelected = preferences.voiceCueMode == modeKey
                        OutlinedButton(
                            onClick = { onSetVoiceCueMode(modeKey) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                else androidx.compose.ui.graphics.Color.Transparent
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        ) {
                            Text(
                                text = modeTitle,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    cueModes.drop(3).forEach { (modeKey, modeTitle) ->
                        val isSelected = preferences.voiceCueMode == modeKey
                        OutlinedButton(
                            onClick = { onSetVoiceCueMode(modeKey) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                else androidx.compose.ui.graphics.Color.Transparent
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        ) {
                            Text(
                                text = modeTitle,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (preferences.voiceCueMode == "EVERY_N_JUMPS" || preferences.voiceCueMode == "CUSTOM") {
                    Spacer(modifier = Modifier.height(14.dp))
                    SettingsChoiceRow(
                        label = "Jump Interval",
                        currentValue = "${preferences.voiceJumpInterval} jumps",
                        options = listOf("10" to "10", "25" to "25", "50" to "50", "100" to "100"),
                        onSelect = { onSetVoiceJumpInterval(it.toIntOrNull() ?: 25) }
                    )
                }

                if (preferences.voiceCueMode == "EVERY_N_MINUTES" || preferences.voiceCueMode == "CUSTOM") {
                    Spacer(modifier = Modifier.height(14.dp))
                    SettingsChoiceRow(
                        label = "Time Interval",
                        currentValue = "${preferences.voiceTimeIntervalMinutes} min",
                        options = listOf("1" to "1 min", "2" to "2 min", "5" to "5 min"),
                        onSelect = { onSetVoiceTimeInterval(it.toIntOrNull() ?: 1) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Target Milestones Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Target Milestones (25%, 50%, 75%, 100%)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Announces progress towards target jumps",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = preferences.voiceTargetMilestonesEnabled,
                        onCheckedChange = onSetVoiceTargetMilestones,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Round Summary Voice Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_round_summary_voice),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.settings_round_summary_voice_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = preferences.voiceRoundSummaryEnabled,
                        onCheckedChange = onSetVoiceRoundSummaryEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                if (preferences.voiceRoundSummaryEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Speak jumps toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.settings_speak_jumps),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Switch(
                                checked = preferences.voiceRoundSummaryJumps,
                                onCheckedChange = onSetVoiceRoundSummaryJumps,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }

                        // Speak JPM toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.settings_speak_jpm),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Switch(
                                checked = preferences.voiceRoundSummaryJpm,
                                onCheckedChange = onSetVoiceRoundSummaryJpm,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }

                        // Speak streak toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.settings_speak_streak),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Switch(
                                checked = preferences.voiceRoundSummaryStreak,
                                onCheckedChange = onSetVoiceRoundSummaryStreak,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
            }
        }

        // Basic Profile Information Card
        item {
            SettingsCard(title = "Athlete Profile & Calories", icon = Icons.Default.Person) {
                Text(
                    text = "Used to personalize your experience and estimate calorie expenditure (MET x Weight x Active Hours). Weight is optional; if omitted, calories are marked unavailable.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it
                        profileSavedSuccess = false
                    },
                    label = { Text("Athlete Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = ageInput,
                        onValueChange = {
                            ageInput = it
                            profileSavedSuccess = false
                        },
                        label = { Text("Age") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = {
                            heightInput = it
                            profileSavedSuccess = false
                        },
                        label = { Text("Height (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = {
                            weightInput = it
                            profileSavedSuccess = false
                        },
                        label = { Text("Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (profileSavedSuccess) {
                        Text(
                            text = "Profile updated successfully",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Button(
                        onClick = {
                            val name = nameInput.trim().ifBlank { "Athlete" }
                            val age = ageInput.toIntOrNull() ?: 28
                            val height = heightInput.toFloatOrNull() ?: 175f
                            val weight = weightInput.toFloatOrNull()
                            onSetProfile(name, age, height, weight)
                            profileSavedSuccess = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Save Profile")
                    }
                }
            }
        }

        // Weekly Weight Tracking Shortcut Card
        item {
            SettingsCard(title = "Weekly Weight Check-in", icon = Icons.Default.Scale) {
                SettingsActionRow(
                    label = "Weight Tracking & Trend Chart",
                    value = preferences.userWeightKg?.let { "${String.format("%.1f", it)} kg" } ?: "Not logged",
                    onClick = onOpenWeight
                )
            }
        }

        // Appearance & Language (Khmer & English independent UI and Voice)
        item {
            SettingsCard(title = stringResource(R.string.settings_section_appearance), icon = Icons.Default.Palette) {
                // UI Language Toggle
                SettingsChoiceRow(
                    label = stringResource(R.string.settings_ui_language),
                    currentValue = "English",
                    options = listOf("en" to "English"),
                    onSelect = onSetUiLanguage
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Theme Mode Choice
                SettingsChoiceRow(
                    label = stringResource(R.string.settings_theme),
                    currentValue = when (preferences.themeMode) {
                        "dark" -> stringResource(R.string.settings_theme_dark)
                        "light" -> stringResource(R.string.settings_theme_light)
                        else -> stringResource(R.string.settings_theme_system)
                    },
                    options = listOf("system" to "System", "dark" to "Dark", "light" to "Light"),
                    onSelect = onSetThemeMode
                )
            }
        }

        // Sensor & Detection
        item {
            SettingsCard(title = stringResource(R.string.settings_section_detection), icon = Icons.Default.Sensors) {
                SettingsChoiceRow(
                    label = stringResource(R.string.settings_phone_position),
                    currentValue = if (preferences.phonePosition == "hand") stringResource(R.string.settings_position_hand) else stringResource(R.string.settings_position_pocket),
                    options = listOf("pocket" to "Pocket/Waist", "hand" to "Hand"),
                    onSelect = onSetPhonePosition
                )

                Spacer(modifier = Modifier.height(14.dp))

                SettingsChoiceRow(
                    label = stringResource(R.string.settings_sensitivity),
                    currentValue = preferences.sensitivity.replaceFirstChar { it.uppercase() },
                    options = listOf("low" to "Low", "medium" to "Medium", "high" to "High"),
                    onSelect = onSetSensitivity
                )
            }
        }

        // About & Updates
        item {
            SettingsCard(title = stringResource(R.string.settings_section_about), icon = Icons.Default.SystemUpdate) {
                SettingsActionRow(
                    label = stringResource(R.string.action_check_updates),
                    value = if (isCheckingUpdates) "Checking..." else "v${com.example.BuildConfig.VERSION_NAME}",
                    onClick = {
                        if (!isCheckingUpdates) {
                            isCheckingUpdates = true
                            coroutineScope.launch {
                                val info = com.example.service.update.InAppUpdateManager.checkForUpdate()
                                isCheckingUpdates = false
                                updateInfo = info
                                if (info.hasUpdate) {
                                    showUpdateDialog = true
                                } else {
                                    android.widget.Toast.makeText(context, "You are using the latest version (v${com.example.BuildConfig.VERSION_NAME})", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
            }
        }

        // Reset Section (Separated & Destructive)
        item {
            SettingsCard(title = stringResource(R.string.settings_section_reset), icon = Icons.Default.DeleteForever) {
                Button(
                    onClick = { showResetDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("reset_data_button")
                ) {
                    Text(
                        text = stringResource(R.string.action_reset_data),
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }
    }

    if (showUpdateDialog && updateInfo != null) {
        val info = updateInfo!!
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("New Version Available: v${info.latestVersionName}") },
            text = {
                Column {
                    Text("A new version of JUMPHUB is available. Tap update to download and install now without losing your workout data.")
                    if (info.releaseNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = info.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUpdateDialog = false
                        info.downloadUrl?.let { url ->
                            com.example.service.update.InAppUpdateManager.startDownloadAndInstall(
                                context = context,
                                downloadUrl = url,
                                versionName = info.latestVersionName
                            )
                        }
                    }
                ) {
                    Text(stringResource(R.string.action_update_now))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text(stringResource(R.string.action_later))
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.action_reset_data)) },
            text = { Text("Are you sure you want to reset all local workout data and preferences? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onResetData()
                    }
                ) {
                    Text(stringResource(R.string.action_reset_data), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
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
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun SettingsActionRow(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SettingsChoiceRow(
    label: String,
    currentValue: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (key, display) ->
                val isSelected = display.equals(currentValue, ignoreCase = true) ||
                        (key == "km" && currentValue.contains("Khmer", ignoreCase = true)) ||
                        (key == "en" && currentValue.contains("English", ignoreCase = true)) ||
                        currentValue.startsWith(key)
                OutlinedButton(
                    onClick = { onSelect(key) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        else androidx.compose.ui.graphics.Color.Transparent
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                ) {
                    Text(
                        text = display,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
