package com.example.service.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationHelper {
    const val CHANNEL_WORKOUT_ACTIVE = "jumphub_workout_active"
    const val CHANNEL_REMINDERS = "jumphub_workout_reminders"
    const val CHANNEL_WEIGHT = "jumphub_weekly_weight"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            val activeWorkoutChannel = NotificationChannel(
                CHANNEL_WORKOUT_ACTIVE,
                "Active Workout",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live jump-rope workout metrics"
                setShowBadge(false)
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Workout Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminds you of planned jump-rope sessions"
            }

            val weightChannel = NotificationChannel(
                CHANNEL_WEIGHT,
                "Weekly Weight",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Weekly body weight logging reminder"
            }

            notificationManager?.createNotificationChannels(
                listOf(activeWorkoutChannel, reminderChannel, weightChannel)
            )
        }
    }

    fun buildWorkoutNotification(
        context: Context,
        jumps: Int,
        activeTimeFormatted: String,
        phase: String
    ): android.app.Notification {
        createNotificationChannels(context)

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        return androidx.core.app.NotificationCompat.Builder(context, CHANNEL_WORKOUT_ACTIVE)
            .setContentTitle("JUMPHUB Active Workout · $phase")
            .setContentText("$jumps Jumps | Time: $activeTimeFormatted")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    /**
     * Determines whether a workout reminder should trigger for the given day.
     * Rules:
     * - Only trigger on planned training days (never on rest days).
     * - Do NOT trigger if a planned workout has already been completed today.
     */
    fun shouldTriggerReminder(
        dayOfWeek: Int,
        plannedDays: Set<Int>,
        workoutCompletedToday: Boolean
    ): Boolean {
        if (!plannedDays.contains(dayOfWeek)) return false
        if (workoutCompletedToday) return false
        return true
    }

    fun buildTrainingReminderNotification(
        context: Context,
        planTitle: String = "Jump-Rope Session"
    ): android.app.Notification {
        createNotificationChannels(context)

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            1,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        return androidx.core.app.NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setContentTitle("Time to Jump! ⚡")
            .setContentText("Your scheduled $planTitle is waiting for you today.")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_REMINDER)
            .build()
    }
}
