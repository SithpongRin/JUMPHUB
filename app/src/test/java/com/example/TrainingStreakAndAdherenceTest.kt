package com.example

import com.example.domain.workout.TrainingAdherenceCalculator
import com.example.domain.workout.TrainingStreakCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class TrainingStreakAndAdherenceTest {

    @Test
    fun testAdherenceCalculation() {
        assertEquals(100, TrainingAdherenceCalculator.calculateAdherence(plannedDaysCount = 4, completedSessionsCount = 4))
        assertEquals(75, TrainingAdherenceCalculator.calculateAdherence(plannedDaysCount = 4, completedSessionsCount = 3))
        assertEquals(50, TrainingAdherenceCalculator.calculateAdherence(plannedDaysCount = 4, completedSessionsCount = 2))
        assertEquals(0, TrainingAdherenceCalculator.calculateAdherence(plannedDaysCount = 4, completedSessionsCount = 0))
    }

    @Test
    fun testRestDaysDoNotBreakTrainingStreak() {
        // Scheduled training days: Monday (2), Wednesday (4), Friday (6)
        val scheduledDays = setOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY)

        // Generate timestamps for Mon, Wed, Fri
        val cal = Calendar.getInstance()
        val workoutDates = mutableListOf<Long>()

        // Simulate last 3 scheduled training days completed
        for (i in 0..14) {
            val checkCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
            }
            if (scheduledDays.contains(checkCal.get(Calendar.DAY_OF_WEEK))) {
                workoutDates.add(checkCal.timeInMillis)
            }
        }

        val (currentStreak, longestStreak) = TrainingStreakCalculator.calculateTrainingStreak(
            scheduledDays = scheduledDays,
            workoutDatesMs = workoutDates
        )

        // Streak should be contiguous across rest days
        assert(currentStreak >= 3) { "Current training streak should persist across rest days" }
        assert(longestStreak >= currentStreak)
    }

    @Test
    fun testDetailedAdherenceCalculation() {
        val scheduledDays = setOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY)
        val now = System.currentTimeMillis()
        val workoutDates = listOf(
            now - 1 * 24 * 60 * 60 * 1000L,
            now - 3 * 24 * 60 * 60 * 1000L
        )

        val adherence = TrainingAdherenceCalculator.calculateDetailedAdherence(
            scheduledDays = scheduledDays,
            workoutDatesMs = workoutDates
        )

        assertEquals(3, adherence.planned)
        assertEquals(2, adherence.completed)
        assertEquals(1, adherence.missed)
        assertEquals(0, adherence.skipped)
        assertEquals(66, adherence.completionPercentage)
        assert(adherence.currentStreak >= 0)
        assert(adherence.longestStreak >= adherence.currentStreak)
    }

    @Test
    fun testWeeklyPlanProgressCalculator() {
        val progressZero = com.example.domain.workout.WeeklyPlanProgressCalculator.calculateProgress(150, 0)
        assertEquals(0f, progressZero, 0.001f)

        val progressHalf = com.example.domain.workout.WeeklyPlanProgressCalculator.calculateProgress(150, 75)
        assertEquals(0.5f, progressHalf, 0.001f)

        val progressFull = com.example.domain.workout.WeeklyPlanProgressCalculator.calculateProgress(150, 150)
        assertEquals(1.0f, progressFull, 0.001f)

        val progressCap = com.example.domain.workout.WeeklyPlanProgressCalculator.calculateProgress(150, 200)
        assertEquals(1.0f, progressCap, 0.001f)
    }
}
