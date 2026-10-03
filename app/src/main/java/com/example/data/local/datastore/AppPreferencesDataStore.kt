package com.example.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "jumphub_preferences")

data class UserPreferences(
    val uiLanguage: String = "en", // "en" or "km"
    val voiceLanguage: String = "en", // "en" or "km" (independent from uiLanguage)
    val voiceEnabled: Boolean = true,
    val voiceVolume: Float = 1.0f,
    val voiceCueMode: String = "PHASE_ONLY", // "PHASE_ONLY", "EVERY_JUMP", "EVERY_N_JUMPS", "TARGET_MILESTONES", "EVERY_N_MINUTES", "CUSTOM"
    val voiceJumpInterval: Int = 25,
    val voiceTimeIntervalMinutes: Int = 1,
    val voiceTargetMilestonesEnabled: Boolean = true,
    val voicePhaseCuesEnabled: Boolean = true,
    val voiceRoundSummaryEnabled: Boolean = true,
    val voiceRoundSummaryJumps: Boolean = true,
    val voiceRoundSummaryJpm: Boolean = true,
    val voiceRoundSummaryStreak: Boolean = true,
    val announcementInterval: Int = 10,
    val rhythmBeep: Boolean = false,
    val vibration: Boolean = true,
    val phonePosition: String = "pocket", // "pocket" or "hand"
    val sensitivity: String = "medium", // "low", "medium", "high"
    val themeMode: String = "system", // "system", "dark", "light"
    val weeklyGoalMinutes: Int = 150,
    val metValue: Float = 11.5f,
    val streakGapToleranceSec: Float = 2.0f,
    val minActiveTimeStreakSec: Int = 180,
    val onboardingCompleted: Boolean = false,
    val parqAcknowledged: Boolean = false,
    val lastSyncTimestamp: Long = 0L,
    // Profile information
    val userName: String = "Athlete",
    val userAge: Int = 28,
    val userHeightCm: Float = 175f,
    val userWeightKg: Float? = null, // Optional; if null calories marked as unavailable
    // Training Schedule & Adherence
    val trainingDays: String = "2,4,6", // 2=Mon, 3=Tue, 4=Wed, 5=Thu, 6=Fri, 7=Sat, 1=Sun (Calendar constants)
    val trainingReminderHour: Int = 18,
    val trainingReminderMinute: Int = 0,
    val trainingRemindersEnabled: Boolean = true,
    val dayReminderTimes: String = "", // Format: "day:hour:min,day:hour:min" e.g. "2:18:00,4:18:30"
    // Weekly Weight Check-in
    val weeklyWeightCheckinDay: Int = 1, // 1 = Sunday in Calendar constants
    val weeklyWeightCheckinHour: Int = 8,
    val weeklyWeightCheckinMinute: Int = 0
) {
    fun getTrainingDaysSet(): Set<Int> {
        return trainingDays.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
    }

    /**
     * Returns custom reminder time (hour, minute) for a specific day of week if configured,
     * otherwise falls back to default (trainingReminderHour, trainingReminderMinute).
     */
    fun getReminderTimeForDay(dayOfWeek: Int): Pair<Int, Int> {
        if (dayReminderTimes.isNotBlank()) {
            val entry = dayReminderTimes.split(",")
                .map { it.trim().split(":") }
                .firstOrNull { it.size == 3 && it[0].toIntOrNull() == dayOfWeek }
            if (entry != null) {
                val h = entry[1].toIntOrNull() ?: trainingReminderHour
                val m = entry[2].toIntOrNull() ?: trainingReminderMinute
                return Pair(h, m)
            }
        }
        return Pair(trainingReminderHour, trainingReminderMinute)
    }
}

class AppPreferencesDataStore(private val context: Context) {

    private object Keys {
        val UI_LANGUAGE = stringPreferencesKey("ui_language")
        val VOICE_LANGUAGE = stringPreferencesKey("voice_language")
        val VOICE_ENABLED = booleanPreferencesKey("voice_enabled")
        val VOICE_VOLUME = floatPreferencesKey("voice_volume")
        val VOICE_CUE_MODE = stringPreferencesKey("voice_cue_mode")
        val VOICE_JUMP_INTERVAL = intPreferencesKey("voice_jump_interval")
        val VOICE_TIME_INTERVAL_MINUTES = intPreferencesKey("voice_time_interval_minutes")
        val VOICE_TARGET_MILESTONES_ENABLED = booleanPreferencesKey("voice_target_milestones_enabled")
        val VOICE_PHASE_CUES_ENABLED = booleanPreferencesKey("voice_phase_cues_enabled")
        val VOICE_ROUND_SUMMARY_ENABLED = booleanPreferencesKey("voice_round_summary_enabled")
        val VOICE_ROUND_SUMMARY_JUMPS = booleanPreferencesKey("voice_round_summary_jumps")
        val VOICE_ROUND_SUMMARY_JPM = booleanPreferencesKey("voice_round_summary_jpm")
        val VOICE_ROUND_SUMMARY_STREAK = booleanPreferencesKey("voice_round_summary_streak")
        val ANNOUNCEMENT_INTERVAL = intPreferencesKey("announcement_interval")
        val RHYTHM_BEEP = booleanPreferencesKey("rhythm_beep")
        val VIBRATION = booleanPreferencesKey("vibration")
        val PHONE_POSITION = stringPreferencesKey("phone_position")
        val SENSITIVITY = stringPreferencesKey("sensitivity")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val WEEKLY_GOAL_MINUTES = intPreferencesKey("weekly_goal_minutes")
        val MET_VALUE = floatPreferencesKey("met_value")
        val STREAK_GAP_TOLERANCE_SEC = floatPreferencesKey("streak_gap_tolerance_sec")
        val MIN_ACTIVE_TIME_STREAK_SEC = intPreferencesKey("min_active_time_streak_sec")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val PARQ_ACKNOWLEDGED = booleanPreferencesKey("parq_acknowledged")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")

        // Profile keys
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_AGE = intPreferencesKey("user_age")
        val USER_HEIGHT_CM = floatPreferencesKey("user_height_cm")
        val USER_WEIGHT_KG = floatPreferencesKey("user_weight_kg")

        // Training schedule keys
        val TRAINING_DAYS = stringPreferencesKey("training_days")
        val TRAINING_REMINDER_HOUR = intPreferencesKey("training_reminder_hour")
        val TRAINING_REMINDER_MINUTE = intPreferencesKey("training_reminder_minute")
        val TRAINING_REMINDERS_ENABLED = booleanPreferencesKey("training_reminders_enabled")
        val DAY_REMINDER_TIMES = stringPreferencesKey("day_reminder_times")

        // Weekly weight check-in keys
        val WEEKLY_WEIGHT_CHECKIN_DAY = intPreferencesKey("weekly_weight_checkin_day")
        val WEEKLY_WEIGHT_CHECKIN_HOUR = intPreferencesKey("weekly_weight_checkin_hour")
        val WEEKLY_WEIGHT_CHECKIN_MINUTE = intPreferencesKey("weekly_weight_checkin_minute")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        val weight = preferences[Keys.USER_WEIGHT_KG]
        UserPreferences(
            uiLanguage = preferences[Keys.UI_LANGUAGE] ?: "en",
            voiceLanguage = preferences[Keys.VOICE_LANGUAGE] ?: "en",
            voiceEnabled = preferences[Keys.VOICE_ENABLED] ?: true,
            voiceVolume = preferences[Keys.VOICE_VOLUME] ?: 1.0f,
            voiceCueMode = preferences[Keys.VOICE_CUE_MODE] ?: "PHASE_ONLY",
            voiceJumpInterval = preferences[Keys.VOICE_JUMP_INTERVAL] ?: 25,
            voiceTimeIntervalMinutes = preferences[Keys.VOICE_TIME_INTERVAL_MINUTES] ?: 1,
            voiceTargetMilestonesEnabled = preferences[Keys.VOICE_TARGET_MILESTONES_ENABLED] ?: true,
            voicePhaseCuesEnabled = preferences[Keys.VOICE_PHASE_CUES_ENABLED] ?: true,
            voiceRoundSummaryEnabled = preferences[Keys.VOICE_ROUND_SUMMARY_ENABLED] ?: true,
            voiceRoundSummaryJumps = preferences[Keys.VOICE_ROUND_SUMMARY_JUMPS] ?: true,
            voiceRoundSummaryJpm = preferences[Keys.VOICE_ROUND_SUMMARY_JPM] ?: true,
            voiceRoundSummaryStreak = preferences[Keys.VOICE_ROUND_SUMMARY_STREAK] ?: true,
            announcementInterval = preferences[Keys.ANNOUNCEMENT_INTERVAL] ?: 10,
            rhythmBeep = preferences[Keys.RHYTHM_BEEP] ?: false,
            vibration = preferences[Keys.VIBRATION] ?: true,
            phonePosition = preferences[Keys.PHONE_POSITION] ?: "pocket",
            sensitivity = preferences[Keys.SENSITIVITY] ?: "medium",
            themeMode = preferences[Keys.THEME_MODE] ?: "system",
            weeklyGoalMinutes = preferences[Keys.WEEKLY_GOAL_MINUTES] ?: 150,
            metValue = preferences[Keys.MET_VALUE] ?: 11.5f,
            streakGapToleranceSec = preferences[Keys.STREAK_GAP_TOLERANCE_SEC] ?: 2.0f,
            minActiveTimeStreakSec = preferences[Keys.MIN_ACTIVE_TIME_STREAK_SEC] ?: 180,
            onboardingCompleted = preferences[Keys.ONBOARDING_COMPLETED] ?: true,
            parqAcknowledged = preferences[Keys.PARQ_ACKNOWLEDGED] ?: true,
            lastSyncTimestamp = preferences[Keys.LAST_SYNC_TIMESTAMP] ?: 0L,
            userName = preferences[Keys.USER_NAME] ?: "Athlete",
            userAge = preferences[Keys.USER_AGE] ?: 28,
            userHeightCm = preferences[Keys.USER_HEIGHT_CM] ?: 175f,
            userWeightKg = if (weight != null && weight > 0f) weight else null,
            trainingDays = preferences[Keys.TRAINING_DAYS] ?: "2,4,6",
            trainingReminderHour = preferences[Keys.TRAINING_REMINDER_HOUR] ?: 18,
            trainingReminderMinute = preferences[Keys.TRAINING_REMINDER_MINUTE] ?: 0,
            trainingRemindersEnabled = preferences[Keys.TRAINING_REMINDERS_ENABLED] ?: true,
            dayReminderTimes = preferences[Keys.DAY_REMINDER_TIMES] ?: "",
            weeklyWeightCheckinDay = preferences[Keys.WEEKLY_WEIGHT_CHECKIN_DAY] ?: 1,
            weeklyWeightCheckinHour = preferences[Keys.WEEKLY_WEIGHT_CHECKIN_HOUR] ?: 8,
            weeklyWeightCheckinMinute = preferences[Keys.WEEKLY_WEIGHT_CHECKIN_MINUTE] ?: 0
        )
    }.flowOn(Dispatchers.IO)

    suspend fun setUiLanguage(lang: String) {
        context.dataStore.edit { it[Keys.UI_LANGUAGE] = lang }
    }

    suspend fun setVoiceLanguage(lang: String) {
        context.dataStore.edit { it[Keys.VOICE_LANGUAGE] = lang }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setVoiceEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VOICE_ENABLED] = enabled }
    }

    suspend fun setVoiceVolume(volume: Float) {
        context.dataStore.edit { it[Keys.VOICE_VOLUME] = volume }
    }

    suspend fun setVoiceCueMode(mode: String) {
        context.dataStore.edit { it[Keys.VOICE_CUE_MODE] = mode }
    }

    suspend fun setVoiceJumpInterval(interval: Int) {
        context.dataStore.edit { it[Keys.VOICE_JUMP_INTERVAL] = interval }
    }

    suspend fun setVoiceTimeIntervalMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.VOICE_TIME_INTERVAL_MINUTES] = minutes }
    }

    suspend fun setVoiceTargetMilestonesEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VOICE_TARGET_MILESTONES_ENABLED] = enabled }
    }

    suspend fun setVoicePhaseCuesEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VOICE_PHASE_CUES_ENABLED] = enabled }
    }

    suspend fun setVoiceRoundSummaryEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VOICE_ROUND_SUMMARY_ENABLED] = enabled }
    }

    suspend fun setVoiceRoundSummaryJumps(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VOICE_ROUND_SUMMARY_JUMPS] = enabled }
    }

    suspend fun setVoiceRoundSummaryJpm(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VOICE_ROUND_SUMMARY_JPM] = enabled }
    }

    suspend fun setVoiceRoundSummaryStreak(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VOICE_ROUND_SUMMARY_STREAK] = enabled }
    }

    suspend fun setPhonePosition(pos: String) {
        context.dataStore.edit { it[Keys.PHONE_POSITION] = pos }
    }

    suspend fun setSensitivity(sens: String) {
        context.dataStore.edit { it[Keys.SENSITIVITY] = sens }
    }

    suspend fun setWeeklyGoalMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.WEEKLY_GOAL_MINUTES] = minutes }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setParqAcknowledged(acknowledged: Boolean) {
        context.dataStore.edit { it[Keys.PARQ_ACKNOWLEDGED] = acknowledged }
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { it[Keys.USER_NAME] = name.trim().ifBlank { "Athlete" } }
    }

    suspend fun setProfile(name: String, age: Int, heightCm: Float, weightKg: Float?) {
        context.dataStore.edit {
            it[Keys.USER_NAME] = name.trim().ifBlank { "Athlete" }
            it[Keys.USER_AGE] = age
            it[Keys.USER_HEIGHT_CM] = heightCm
            if (weightKg != null && weightKg > 0f) {
                it[Keys.USER_WEIGHT_KG] = weightKg
            } else {
                it.remove(Keys.USER_WEIGHT_KG)
            }
        }
    }

    suspend fun setProfile(age: Int, heightCm: Float, weightKg: Float?) {
        context.dataStore.edit {
            it[Keys.USER_AGE] = age
            it[Keys.USER_HEIGHT_CM] = heightCm
            if (weightKg != null && weightKg > 0f) {
                it[Keys.USER_WEIGHT_KG] = weightKg
            } else {
                it.remove(Keys.USER_WEIGHT_KG)
            }
        }
    }

    suspend fun setTrainingSchedule(daysCsv: String, hour: Int, minute: Int, enabled: Boolean, dayTimesCsv: String = "") {
        context.dataStore.edit {
            it[Keys.TRAINING_DAYS] = daysCsv
            it[Keys.TRAINING_REMINDER_HOUR] = hour
            it[Keys.TRAINING_REMINDER_MINUTE] = minute
            it[Keys.TRAINING_REMINDERS_ENABLED] = enabled
            it[Keys.DAY_REMINDER_TIMES] = dayTimesCsv
        }
    }

    suspend fun setDayReminderTimes(dayTimesCsv: String) {
        context.dataStore.edit {
            it[Keys.DAY_REMINDER_TIMES] = dayTimesCsv
        }
    }

    suspend fun setWeeklyWeightCheckin(day: Int, hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.WEEKLY_WEIGHT_CHECKIN_DAY] = day
            it[Keys.WEEKLY_WEIGHT_CHECKIN_HOUR] = hour
            it[Keys.WEEKLY_WEIGHT_CHECKIN_MINUTE] = minute
        }
    }

    suspend fun setLastSyncTimestamp(timestamp: Long) {
        context.dataStore.edit { it[Keys.LAST_SYNC_TIMESTAMP] = timestamp }
    }
}
