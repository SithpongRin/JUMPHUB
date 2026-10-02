package com.example

import com.example.service.notification.NotificationHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class NotificationReminderTest {

    @Test
    fun testShouldTriggerReminderOnlyOnPlannedDays() {
        val plannedDays = setOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY)

        // Monday (planned, not completed) -> should trigger
        assertTrue(
            NotificationHelper.shouldTriggerReminder(
                dayOfWeek = Calendar.MONDAY,
                plannedDays = plannedDays,
                workoutCompletedToday = false
            )
        )

        // Tuesday (rest day) -> should NOT trigger
        assertFalse(
            NotificationHelper.shouldTriggerReminder(
                dayOfWeek = Calendar.TUESDAY,
                plannedDays = plannedDays,
                workoutCompletedToday = false
            )
        )

        // Thursday (rest day) -> should NOT trigger
        assertFalse(
            NotificationHelper.shouldTriggerReminder(
                dayOfWeek = Calendar.THURSDAY,
                plannedDays = plannedDays,
                workoutCompletedToday = false
            )
        )
    }

    @Test
    fun testShouldNotTriggerReminderIfWorkoutAlreadyCompletedToday() {
        val plannedDays = setOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY)

        // Monday (planned, but already completed today) -> should NOT trigger
        assertFalse(
            NotificationHelper.shouldTriggerReminder(
                dayOfWeek = Calendar.MONDAY,
                plannedDays = plannedDays,
                workoutCompletedToday = true
            )
        )
    }
}
