package com.example.ui.navigation

import android.content.Intent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.AppContainer
import com.example.domain.model.PersonalRecord
import com.example.domain.model.RecordType
import com.example.domain.model.WorkoutSession
import com.example.service.WorkoutForegroundService
import com.example.ui.ProvideAppLocale
import com.example.ui.account.AccountScreen
import com.example.ui.components.DynamicFloatingNavigationBar
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.plans.PlansScreen
import com.example.ui.progress.ProgressScreen
import com.example.ui.records.RecordsScreen
import com.example.ui.sessions.SessionDetailScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.JUMPHUBTheme
import com.example.ui.weight.WeightScreen
import com.example.ui.workout.WorkoutScreen
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun JumphubApp(container: AppContainer) {
    val context = LocalContext.current
    val preferences by container.preferences.userPreferencesFlow.collectAsState(initial = com.example.data.local.datastore.UserPreferences())
    val currentUser by container.authRepository.currentUserFlow.collectAsState(initial = null)
    val syncReport by container.syncEngine.syncReport.collectAsState(initial = com.example.data.sync.SyncReport())

    val isDarkTheme = when (preferences.themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    ProvideAppLocale(languageCode = preferences.uiLanguage) {
        JUMPHUBTheme(darkTheme = isDarkTheme) {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val currentDestination = NavDestination.fromRoute(currentRoute)

            // Dynamic nav bar visibility driven by screen type and scroll state
            var isNavVisibleByScroll by remember { mutableStateOf(true) }
            val isMainTab = currentDestination != null
            val isFloatingNavVisible = isMainTab && isNavVisibleByScroll

            val coroutineScope = rememberCoroutineScope()

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    val startDestination = if (preferences.onboardingCompleted) Screen.Home.route else Screen.Onboarding.route

                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(Screen.Onboarding.route) {
                            OnboardingScreen(
                                onCompleteOnboarding = { age, heightCm, weightKg ->
                                    coroutineScope.launch {
                                        container.preferences.setProfile(age, heightCm, weightKg)
                                        container.preferences.setOnboardingCompleted(true)
                                        container.preferences.setParqAcknowledged(true)
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        composable(Screen.Home.route) {
                            val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.provideFactory(container))
                            val homeUiState by homeViewModel.uiState.collectAsState()

                            HomeScreen(
                                uiState = homeUiState,
                                onStartWorkout = {
                                    val intent = Intent(context, WorkoutForegroundService::class.java).apply {
                                        action = WorkoutForegroundService.ACTION_START
                                        putExtra(WorkoutForegroundService.EXTRA_ROUNDS, 8)
                                        putExtra(WorkoutForegroundService.EXTRA_ROUND_DURATION, 30)
                                        putExtra(WorkoutForegroundService.EXTRA_REST_DURATION, 60)
                                        putExtra(WorkoutForegroundService.EXTRA_TARGET_JUMPS, 500)
                                    }
                                    context.startService(intent)
                                    navController.navigate(Screen.Workout.route)
                                },
                                onOpenPlan = { navController.navigate(Screen.Plans.route) },
                                onOpenWeight = { navController.navigate(Screen.Weight.route) },
                                onSyncNow = { homeViewModel.triggerSync() },
                                onNavVisibilityChanged = { isNavVisibleByScroll = it }
                            )
                        }

                        composable(Screen.Plans.route) {
                            val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.provideFactory(container))
                            val homeUiState by homeViewModel.uiState.collectAsState()
                            val plans by container.planRepository.getAvailablePlans().collectAsState(initial = emptyList())
                            val activePlan by container.planRepository.getActivePlan().collectAsState(initial = null)

                            val selectedDays = preferences.getTrainingDaysSet()
                            val trainingStreak = Pair(homeUiState.currentStreakDays, homeUiState.longestStreakDays)

                            PlansScreen(
                                plans = plans,
                                activePlan = activePlan,
                                selectedTrainingDays = selectedDays,
                                trainingStreak = trainingStreak,
                                trainingAdherencePct = homeUiState.trainingAdherencePct,
                                onSelectPlan = { plan ->
                                    coroutineScope.launch {
                                        container.planRepository.selectPlan(plan.id, plan.category)
                                    }
                                },
                                onToggleTrainingDay = { day ->
                                    coroutineScope.launch {
                                        val currentDays = selectedDays.toMutableSet()
                                        if (currentDays.contains(day)) {
                                            if (currentDays.size > 2) currentDays.remove(day)
                                        } else {
                                            currentDays.add(day)
                                        }
                                        val csv = currentDays.sorted().joinToString(",")
                                        container.preferences.setTrainingSchedule(
                                            csv,
                                            preferences.trainingReminderHour,
                                            preferences.trainingReminderMinute,
                                            preferences.trainingRemindersEnabled
                                        )
                                    }
                                },
                                onNavVisibilityChanged = { isNavVisibleByScroll = it }
                            )
                        }

                        composable(Screen.Progress.route) {
                            val sessions by container.workoutRepository.getAllSessions().collectAsState(initial = emptyList())
                            val dailyStats by container.progressRepository.getDailyStats().collectAsState(initial = emptyList())

                            ProgressScreen(
                                sessions = sessions,
                                dailyStats = dailyStats,
                                onOpenSession = { sessionId ->
                                    navController.navigate("session_detail/$sessionId")
                                },
                                onNavVisibilityChanged = { isNavVisibleByScroll = it }
                            )
                        }

                        composable(Screen.Records.route) {
                            val records by container.progressRepository.getPersonalRecords().collectAsState(initial = emptyList())

                            RecordsScreen(
                                records = records,
                                onNavVisibilityChanged = { isNavVisibleByScroll = it }
                            )
                        }

                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                preferences = preferences,
                                onSetUiLanguage = { lang ->
                                    coroutineScope.launch { container.preferences.setUiLanguage(lang) }
                                },
                                onSetVoiceLanguage = { lang ->
                                    coroutineScope.launch { container.preferences.setVoiceLanguage(lang) }
                                },
                                onSetVoiceCueMode = { mode ->
                                    coroutineScope.launch { container.preferences.setVoiceCueMode(mode) }
                                },
                                onSetVoiceJumpInterval = { interval ->
                                    coroutineScope.launch { container.preferences.setVoiceJumpInterval(interval) }
                                },
                                onSetVoiceTimeInterval = { minutes ->
                                    coroutineScope.launch { container.preferences.setVoiceTimeIntervalMinutes(minutes) }
                                },
                                onSetVoiceTargetMilestones = { enabled ->
                                    coroutineScope.launch { container.preferences.setVoiceTargetMilestonesEnabled(enabled) }
                                },
                                onSetThemeMode = { mode ->
                                    coroutineScope.launch { container.preferences.setThemeMode(mode) }
                                },
                                onSetVoiceEnabled = { enabled ->
                                    coroutineScope.launch { container.preferences.setVoiceEnabled(enabled) }
                                },
                                onSetPhonePosition = { pos ->
                                    coroutineScope.launch { container.preferences.setPhonePosition(pos) }
                                },
                                onSetSensitivity = { sens ->
                                    coroutineScope.launch { container.preferences.setSensitivity(sens) }
                                },
                                onSetProfile = { age, height, weight ->
                                    coroutineScope.launch { container.preferences.setProfile(age, height, weight) }
                                },
                                onOpenAccount = { navController.navigate(Screen.Account.route) },
                                onOpenWeight = { navController.navigate(Screen.Weight.route) },
                                onCheckUpdates = { /* Update checking handled via GitHub release */ },
                                onResetData = {
                                    coroutineScope.launch {
                                        container.database.sessionDao().clearAll()
                                        container.preferences.setOnboardingCompleted(false)
                                        navController.navigate(Screen.Onboarding.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                },
                                onNavVisibilityChanged = { isNavVisibleByScroll = it }
                            )
                        }

                        composable(Screen.Account.route) {
                            AccountScreen(
                                currentUser = currentUser,
                                syncReport = syncReport,
                                onSyncNow = {
                                    coroutineScope.launch { container.syncEngine.syncNow() }
                                },
                                onSignInGuest = {
                                    coroutineScope.launch { container.authRepository.signInAsGuest() }
                                },
                                onSignOut = {
                                    coroutineScope.launch { container.authRepository.signOut() }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.Weight.route) {
                            val metrics by container.progressRepository.getAllMetrics().collectAsState(initial = emptyList())

                            WeightScreen(
                                metrics = metrics,
                                onLogWeight = { w, waist ->
                                    coroutineScope.launch {
                                        container.progressRepository.logWeight(w, waist)
                                        container.preferences.setProfile(preferences.userAge, preferences.userHeightCm, w)
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "session_detail/{sessionId}",
                            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                            val session by container.workoutRepository.getSessionById(sessionId).collectAsState(initial = null)
                            var rounds by remember { mutableStateOf<List<com.example.domain.model.RoundRecord>>(emptyList()) }

                            androidx.compose.runtime.LaunchedEffect(sessionId) {
                                rounds = container.workoutRepository.getRoundsForSession(sessionId)
                            }

                            SessionDetailScreen(
                                session = session,
                                rounds = rounds,
                                onDeleteSession = { uuid ->
                                    coroutineScope.launch {
                                        container.workoutRepository.deleteSession(uuid)
                                        navController.popBackStack()
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.Workout.route) {
                            WorkoutScreen(
                                onFinishWorkout = { state ->
                                    coroutineScope.launch {
                                        val session = WorkoutSession(
                                            uuid = UUID.randomUUID().toString(),
                                            date = System.currentTimeMillis(),
                                            totalJumps = state.totalJumps,
                                            detectedTotal = state.detectedJumps,
                                            correctedTotal = state.correctedJumps,
                                            activeSec = state.activeSeconds,
                                            avgRate = state.currentCadenceJpm,
                                            bestStreak = state.bestStreak,
                                            calories = state.estimatedCalories,
                                            weightSnapshot = preferences.userWeightKg,
                                            metUsed = preferences.metValue
                                        )
                                        container.workoutRepository.saveSession(session, emptyList())

                                        // Update personal records if exceeded
                                        if (state.totalJumps > 0) {
                                            container.progressRepository.saveRecord(
                                                PersonalRecord(
                                                    uuid = UUID.randomUUID().toString(),
                                                    type = RecordType.MOST_SESSION,
                                                    value = state.totalJumps.toFloat(),
                                                    sessionId = session.uuid
                                                )
                                            )
                                        }
                                        if (state.bestStreak > 0) {
                                            container.progressRepository.saveRecord(
                                                PersonalRecord(
                                                    uuid = UUID.randomUUID().toString(),
                                                    type = RecordType.LONGEST_STREAK,
                                                    value = state.bestStreak.toFloat(),
                                                    sessionId = session.uuid
                                                )
                                            )
                                        }
                                        if (state.currentCadenceJpm > 0) {
                                            container.progressRepository.saveRecord(
                                                PersonalRecord(
                                                    uuid = UUID.randomUUID().toString(),
                                                    type = RecordType.PEAK_RATE,
                                                    value = state.currentCadenceJpm,
                                                    sessionId = session.uuid
                                                )
                                            )
                                        }

                                        navController.navigate("session_detail/${session.uuid}") {
                                            popUpTo(Screen.Workout.route) { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }
                    }

                    // Dynamic Floating Navigation Bar
                    DynamicFloatingNavigationBar(
                        currentDestination = currentDestination,
                        isVisible = isFloatingNavVisible,
                        onNavigate = { destination ->
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}
