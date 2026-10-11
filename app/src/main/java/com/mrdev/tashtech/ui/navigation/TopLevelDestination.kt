package com.mrdev.tashtech.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.mrdev.tashtech.R

@Immutable
class TopLevelDestination(
    val route: Any,
    @StringRes val label: Int,
    val icon: ImageVector,
    val matches: (NavDestination) -> Boolean,
)

val TopLevelDestinations = listOf(
    TopLevelDestination(HomeRoute, R.string.nav_home, Icons.Outlined.Home) { it.hasRoute<HomeRoute>() },
    TopLevelDestination(ScheduleRoute, R.string.nav_schedule, Icons.Outlined.CalendarMonth) { it.hasRoute<ScheduleRoute>() },
    TopLevelDestination(ServicesRoute, R.string.nav_tools, Icons.Outlined.GridView) { it.hasRoute<ServicesRoute>() },
)

fun NavDestination?.isIn(top: TopLevelDestination): Boolean =
    this?.hierarchy?.any(top.matches) == true