package com.example.domain.workout

import java.util.Calendar

object WorkoutMetricsCalculator {

    /**
     * Calculates Jumps Per Minute using only active jumping seconds.
     * Never divides by total elapsed time. Returns 0 if activeSeconds == 0.
     */
    fun calculateJpm(totalJumps: Int, activeSeconds: Int): Float {
        if (activeSeconds <= 0 || totalJumps <= 0) return 0f
        return (totalJumps.toFloat() / activeSeconds) * 60f
    }

    /**
     * Estimates burned calories: MET * weightKg * activeHours.
     * Returns null if weightKg is null or <= 0 (weight unavailable).
     */
    fun calculateEstimatedCalories(activeSeconds: Int, weightKg: Float?, met: Float = 11.5f): Float? {
        if (weightKg == null || weightKg <= 0f || activeSeconds <= 0) return null
        val activeHours = activeSeconds / 3600f
        return met * weightKg * activeHours
    }

    /**
     * Calculates the peak rate (jumps in a rolling window) from jump timestamps.
     * windowSec is usually 30 or 60.
     */
    fun calculatePeakWindowRate(jumpTimestampsMs: List<Long>, windowSec: Int): Int {
        if (jumpTimestampsMs.isEmpty() || windowSec <= 0) return 0
        val windowMs = windowSec * 1000L
        var maxInWindow = 0
        var left = 0

        for (right in jumpTimestampsMs.indices) {
            while (jumpTimestampsMs[right] - jumpTimestampsMs[left] > windowMs) {
                left++
            }
            val count = right - left + 1
            if (count > maxInWindow) {
                maxInWindow = count
            }
        }
        return maxInWindow
    }
}

object TrainingStreakCalculator {

    /**
     * Calculates the current and longest training streak based on scheduled training days.
     * Scheduled days: Set of Calendar Day integers (e.g. Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY).
     * Rest days do NOT break the training streak.
     *
     * @param scheduledDays set of Calendar.DAY_OF_WEEK integers (1 = Sunday ... 7 = Saturday)
     * @param workoutDatesMs sorted list of timestamps when user completed sessions
     * @return Pair of (currentTrainingStreak, longestTrainingStreak)
     */
    fun calculateTrainingStreak(
        scheduledDays: Set<Int>,
        workoutDatesMs: List<Long>
    ): Pair<Int, Int> {
        if (scheduledDays.isEmpty() || workoutDatesMs.isEmpty()) return Pair(0, 0)

        // Convert workout timestamps to set of unique "year-dayOfYear" days
        val workoutCalendarDays = workoutDatesMs.map { ts ->
            val cal = Calendar.getInstance().apply { timeInMillis = ts }
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
        }.toSet()

        // Inspect past 60 days
        val today = Calendar.getInstance()
        var currentStreak = 0
        var longestStreak = 0
        var activeStreakCounter = 0
        var isCurrentStreakActive = true

        val checkCal = Calendar.getInstance().apply {
            timeInMillis = today.timeInMillis
        }

        // Iterate backward up to 60 days
        for (i in 0..60) {
            val dayOfWeek = checkCal.get(Calendar.DAY_OF_WEEK)
            val dayKey = "${checkCal.get(Calendar.YEAR)}-${checkCal.get(Calendar.DAY_OF_YEAR)}"

            if (scheduledDays.contains(dayOfWeek)) {
                val completed = workoutCalendarDays.contains(dayKey)
                if (completed) {
                    activeStreakCounter++
                    if (isCurrentStreakActive) {
                        currentStreak++
                    }
                    if (activeStreakCounter > longestStreak) {
                        longestStreak = activeStreakCounter
                    }
                } else {
                    // Missed a planned workout day
                    // If it's today and not over yet, don't break current streak yet
                    val isToday = i == 0
                    if (!isToday) {
                        isCurrentStreakActive = false
                        activeStreakCounter = 0
                    }
                }
            }
            // Rest days are skipped without breaking streak!
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        }

        return Pair(currentStreak, maxOf(currentStreak, longestStreak))
    }
}

data class AdherenceDetails(
    val planned: Int,
    val completed: Int,
    val missed: Int,
    val skipped: Int,
    val completionPercentage: Int,
    val currentStreak: Int,
    val longestStreak: Int
)

object TrainingAdherenceCalculator {

    /**
     * Calculates weekly training adherence percentage.
     * @param plannedDaysCount number of planned training days in the week
     * @param completedSessionsCount number of completed sessions in that week
     */
    fun calculateAdherence(plannedDaysCount: Int, completedSessionsCount: Int): Int {
        if (plannedDaysCount <= 0) return 100
        val percentage = (completedSessionsCount.toFloat() / plannedDaysCount) * 100f
        return percentage.coerceIn(0f, 100f).toInt()
    }

    /**
     * Computes comprehensive adherence statistics including planned, completed, missed, skipped,
     * adherence percentage, current streak, and longest streak.
     */
    fun calculateDetailedAdherence(
        scheduledDays: Set<Int>,
        workoutDatesMs: List<Long>,
        skippedDatesMs: List<Long> = emptyList()
    ): AdherenceDetails {
        val (currentStreak, longestStreak) = TrainingStreakCalculator.calculateTrainingStreak(
            scheduledDays = scheduledDays,
            workoutDatesMs = workoutDatesMs
        )

        val plannedCount = scheduledDays.size
        // Calculate sessions completed in the current calendar week (last 7 days window)
        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - 7 * 24 * 60 * 60 * 1000L
        val weekSessions = workoutDatesMs.filter { it >= sevenDaysAgo }
        val completedCount = weekSessions.size
        val skippedCount = skippedDatesMs.filter { it >= sevenDaysAgo }.size
        val missedCount = (plannedCount - completedCount - skippedCount).coerceAtLeast(0)
        val completionPct = calculateAdherence(plannedCount, completedCount)

        return AdherenceDetails(
            planned = plannedCount,
            completed = completedCount,
            missed = missedCount,
            skipped = skippedCount,
            completionPercentage = completionPct,
            currentStreak = currentStreak,
            longestStreak = longestStreak
        )
    }
}

object WeeklyPlanProgressCalculator {
    /**
     * Calculates weekly plan progression given target weekly volume and completed volume.
     */
    fun calculateProgress(targetMinutes: Int, completedMinutes: Int): Float {
        if (targetMinutes <= 0) return 1.0f
        return (completedMinutes.toFloat() / targetMinutes).coerceIn(0f, 1f)
    }
}
