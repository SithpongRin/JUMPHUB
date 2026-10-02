package com.example.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object Plans : Screen("plans")
    data object PlanDetail : Screen("plan_detail/{planId}") {
        fun createRoute(planId: String) = "plan_detail/$planId"
    }
    data object Workout : Screen("workout")
    data object Summary : Screen("summary/{sessionId}") {
        fun createRoute(sessionId: String) = "summary/$sessionId"
    }
    data object Progress : Screen("progress")
    data object Records : Screen("records")
    data object Weight : Screen("weight")
    data object Settings : Screen("settings")
    data object Account : Screen("account")
}

enum class NavDestination(
    val route: String,
    @StringRes val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    PLANS("plans", R.string.nav_plans, Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter),
    PROGRESS("progress", R.string.nav_progress, Icons.Filled.Timeline, Icons.Outlined.Timeline),
    RECORDS("records", R.string.nav_records, Icons.Filled.Leaderboard, Icons.Outlined.Leaderboard),
    SETTINGS("settings", R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings);

    companion object {
        fun fromRoute(route: String?): NavDestination? {
            return entries.find { it.route == route }
        }
    }
}
