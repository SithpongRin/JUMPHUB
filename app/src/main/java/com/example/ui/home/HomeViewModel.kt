package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppContainer
import com.example.data.local.datastore.UserPreferences
import com.example.data.sync.SyncReport
import com.example.domain.model.TrainingPlan
import com.example.domain.model.UserAccount
import com.example.domain.model.UserActivePlan
import com.example.domain.model.WorkoutSession
import com.example.domain.workout.TrainingAdherenceCalculator
import com.example.domain.workout.TrainingStreakCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val currentUser: UserAccount? = null,
    val preferences: UserPreferences = UserPreferences(),
    val latestSession: WorkoutSession? = null,
    val activePlan: UserActivePlan? = null,
    val availablePlans: List<TrainingPlan> = emptyList(),
    val totalLifetimeJumps: Int = 0,
    val totalLifetimeActiveSec: Int = 0,
    val totalSessionCount: Int = 0,
    val weeklyActiveMinutes: Int = 0,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val trainingAdherencePct: Int = 100,
    val isWeightDue: Boolean = true,
    val syncReport: SyncReport = SyncReport()
)

private data class WorkoutStatsSummary(
    val allSessions: List<WorkoutSession>,
    val latestSession: WorkoutSession?,
    val totalJumps: Int,
    val totalActiveSec: Int,
    val sessionCount: Int
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val workoutStatsFlow = combine(
        container.workoutRepository.getAllSessions(),
        container.workoutRepository.getLatestSession(),
        container.workoutRepository.getTotalLifetimeJumps(),
        container.workoutRepository.getTotalLifetimeActiveSec(),
        container.workoutRepository.getTotalSessionCount()
    ) { allSessions, latestSession, lifetimeJumps, lifetimeSec, sessionCount ->
        WorkoutStatsSummary(
            allSessions = allSessions,
            latestSession = latestSession,
            totalJumps = lifetimeJumps ?: 0,
            totalActiveSec = lifetimeSec ?: 0,
            sessionCount = sessionCount
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        container.authRepository.currentUserFlow,
        container.preferences.userPreferencesFlow,
        container.planRepository.getActivePlan(),
        workoutStatsFlow,
        container.syncEngine.syncReport
    ) { user, prefs, activePlan, stats, syncReport ->
        val activeMins = stats.totalActiveSec / 60
        val sessionDates = stats.allSessions.map { it.date }
        val trainingDays = prefs.getTrainingDaysSet()
        val (currentTrainingStreak, longestTrainingStreak) = TrainingStreakCalculator.calculateTrainingStreak(
            scheduledDays = trainingDays,
            workoutDatesMs = sessionDates
        )

        val plannedCount = trainingDays.size
        val adherence = TrainingAdherenceCalculator.calculateAdherence(
            plannedDaysCount = plannedCount,
            completedSessionsCount = stats.allSessions.take(7).size
        )

        // Check if weekly weight is due: due if user has not logged weight in the last 7 days
        val isWeightDue = prefs.userWeightKg == null || (stats.latestSession != null && System.currentTimeMillis() - stats.latestSession.date > 7 * 24 * 60 * 60 * 1000L)

        HomeUiState(
            currentUser = user,
            preferences = prefs,
            latestSession = stats.latestSession,
            activePlan = activePlan,
            totalLifetimeJumps = stats.totalJumps,
            totalLifetimeActiveSec = stats.totalActiveSec,
            totalSessionCount = stats.sessionCount,
            weeklyActiveMinutes = activeMins,
            currentStreakDays = currentTrainingStreak,
            longestStreakDays = longestTrainingStreak,
            trainingAdherencePct = adherence,
            isWeightDue = isWeightDue,
            syncReport = syncReport
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun triggerSync() {
        viewModelScope.launch {
            container.syncEngine.syncNow()
        }
    }

    companion object {
        fun provideFactory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(container) as T
                }
            }
    }
}
