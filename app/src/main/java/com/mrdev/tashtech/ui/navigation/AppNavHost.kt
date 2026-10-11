package com.mrdev.tashtech.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.mrdev.tashtech.data.config.AcademicService
import com.mrdev.tashtech.data.config.OfficialDomain
import com.mrdev.tashtech.tashbox.ui.TashBoxScreen
import com.mrdev.tashtech.ui.browser.BrowserScreen
import com.mrdev.tashtech.ui.home.HomeScreen
import com.mrdev.tashtech.ui.motion.LocalNavAnimatedScope
import com.mrdev.tashtech.ui.motion.LocalSharedTransitionScope
import com.mrdev.tashtech.ui.motion.Motion
import com.mrdev.tashtech.ui.news.NewsDetailsScreen
import com.mrdev.tashtech.ui.news.NewsScreen
import com.mrdev.tashtech.ui.notes.NotesScreen
import com.mrdev.tashtech.ui.onboarding.OnboardingScreen
import com.mrdev.tashtech.ui.profile.ProfileScreen
import com.mrdev.tashtech.ui.reminders.ReminderEditorScreen
import com.mrdev.tashtech.ui.reminders.RemindersScreen
import com.mrdev.tashtech.ui.scanner.QrScannerScreen
import com.mrdev.tashtech.ui.schedule.ScheduleScreen
import com.mrdev.tashtech.ui.services.ServicesScreen
import com.mrdev.tashtech.ui.services.openLinkExternally
import com.mrdev.tashtech.ui.settings.SettingsScreen
import com.mrdev.tashtech.ui.theme.Dimens

private val GameEntry: ((NavHostController) -> Unit)? = null
@Composable
fun AppNavHost(
    startDestination: Any,
    launchTarget: LaunchTarget? = null,
    onLaunchTargetConsumed: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
) {
    val context = LocalContext.current


    val destination = navController.currentBackStackEntryAsState().value?.destination
    val showBottomBar = TopLevelDestinations.any { destination.isIn(it) }

    val density = LocalDensity.current
    val barState =
        remember(density) { HideOnScrollState(thresholdPx = with(density) { 24.dp.toPx() }) }
    LaunchedEffect(destination) { barState.show() } // every navigation brings the bar back

    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val reservedForBar =
        if (showBottomBar) FloatingBarHeight + FloatingBarMargin * 2 + navInset else 0.dp

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CompositionLocalProvider(LocalBottomBarPadding provides reservedForBar) {
            SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
                CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(barState.connection),

                        enterTransition = {
                            val dir = tabDirection()
                            if (dir != null) {
                                slideInHorizontally(
                                    tween(
                                        Motion.Page,
                                        easing = Motion.MoveEasing
                                    )
                                ) { (it * 0.10f * dir).toInt() } +
                                        fadeIn(tween(Motion.Page, easing = Motion.EnterEasing)) +
                                        scaleIn(
                                            tween(Motion.Page, easing = Motion.MoveEasing),
                                            initialScale = 0.97f
                                        )
                            } else {
                                slideInVertically(
                                    tween(
                                        Motion.Page,
                                        easing = Motion.MoveEasing
                                    )
                                ) { it / 40 } +
                                        fadeIn(tween(Motion.Page, easing = Motion.EnterEasing)) +
                                        scaleIn(
                                            tween(Motion.Page, easing = Motion.MoveEasing),
                                            initialScale = 0.98f
                                        )
                            }
                        },
                        exitTransition = {
                            val dir = tabDirection()
                            if (dir != null) {
                                slideOutHorizontally(
                                    tween(
                                        Motion.Page,
                                        easing = Motion.MoveEasing
                                    )
                                ) { (-it * 0.06f * dir).toInt() } +
                                        fadeOut(tween(Motion.Card, easing = Motion.MoveEasing)) +
                                        scaleOut(
                                            tween(Motion.Page, easing = Motion.MoveEasing),
                                            targetScale = 0.97f
                                        )
                            } else {
                                fadeOut(tween(Motion.Card, easing = Motion.MoveEasing)) +
                                        scaleOut(
                                            tween(Motion.Page, easing = Motion.MoveEasing),
                                            targetScale = 0.98f
                                        )
                            }
                        },
                        popEnterTransition = {
                            fadeIn(tween(Motion.Page, easing = Motion.EnterEasing)) +
                                    scaleIn(
                                        tween(Motion.Page, easing = Motion.MoveEasing),
                                        initialScale = 0.98f
                                    )
                        },
                        popExitTransition = {
                            slideOutVertically(
                                tween(
                                    Motion.Page,
                                    easing = Motion.MoveEasing
                                )
                            ) { it / 40 } +
                                    fadeOut(tween(Motion.Card, easing = Motion.MoveEasing)) +
                                    scaleOut(
                                        tween(Motion.Page, easing = Motion.MoveEasing),
                                        targetScale = 0.98f
                                    )
                        },
                    ) {
                        composable<NotesRoute> {
                            val context = LocalContext.current
                            NotesScreen(
                                onBack = { navController.popBackStack() },
                                onOpenLink = { url ->
                                    if (OfficialDomain.isOfficial(url)) navController.navigate(
                                        BrowserRoute(
                                            url
                                        )
                                    )
                                    else openLinkExternally(context, url)
                                },
                            )
                        }
                        composable<TashBoxRoute> {
                            TashBoxScreen(
                                onExit = { navController.popBackStack() },
                            )
                        }
                        composable<OnboardingRoute> {
                            OnboardingScreen(
                                onFinished = {
                                    navController.navigate(HomeRoute) {
                                        popUpTo<OnboardingRoute> { inclusive = true }
                                    }
                                },
                            )
                        }

                        composable<ServicesRoute> {
                            ServicesScreen(
                                onOpen = { navController.navigate(BrowserRoute(it.url)) },
                                onOpenNotes = { navController.navigate(NotesRoute) },
                                onOpenReminders = { navController.navigate(RemindersRoute) },
                                onOpenNews = { navController.navigate(NewsRoute) },
                                onOpenSettings = { navController.navigate(SettingsRoute) },
                                onOpenGame = { navController.navigate(TashBoxRoute) },
                            )
                        }
                        composable<NewsRoute> {
                            NewsScreen(
                                onBack = { navController.popBackStack() },
                                onOpenArticle = { navController.navigate(NewsDetailsRoute(it)) },
                            )
                        }
                        composable<NewsDetailsRoute> {
                            NewsDetailsScreen(
                                onBack = { navController.popBackStack() },
                                onOpenInApp = { url ->
                                    navController.navigate(
                                        BrowserRoute(
                                            url,
                                            article = true
                                        )
                                    )
                                },
                            )
                        }
                        composable<SettingsRoute> {
                            SettingsScreen(
                                onBack = { navController.popBackStack() },
                                onOpenService = { navController.navigate(BrowserRoute(it.url)) },
                            )
                        }
                        composable<HomeRoute> {
                            CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                                HomeScreen(
                                    onOpenSchedule = { navController.navigateTopLevel(ScheduleRoute) },
                                    onOpenReminders = { navController.navigate(RemindersRoute) },
                                    onAddReminder = { navController.navigate(ReminderEditorRoute()) },
                                    onOpenSis = {
                                        navController.navigate(
                                            BrowserRoute(
                                                AcademicService.Sis.url
                                            )
                                        )
                                    },
                                    onOpenLms = {
                                        navController.navigate(
                                            BrowserRoute(
                                                AcademicService.Lms.url
                                            )
                                        )
                                    },
                                    onOpenNews = { navController.navigate(NewsRoute) },
                                    onOpenArticle = { navController.navigate(NewsDetailsRoute(it)) },
                                    onOpenProfile = { navController.navigate(ProfileRoute) },
                                )
                            }
                        }
                        composable<ProfileRoute>(
                            // Fade only: the avatar and name fly by themselves, the page just appears around them.
                            enterTransition = {
                                fadeIn(
                                    tween(
                                        Motion.Page,
                                        easing = Motion.EnterEasing
                                    )
                                )
                            },
                            exitTransition = { fadeOut(tween(Motion.Card)) },
                            popEnterTransition = {
                                fadeIn(
                                    tween(
                                        Motion.Page,
                                        easing = Motion.EnterEasing
                                    )
                                )
                            },
                            popExitTransition = { fadeOut(tween(Motion.Card)) },
                        ) {
                            CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                                ProfileScreen(onBack = { navController.popBackStack() })
                            }
                        }
                        composable<ScheduleRoute> { ScheduleScreen() }
                        composable<BrowserRoute> { entry ->
                            val route = entry.toRoute<BrowserRoute>()
                            BrowserScreen(
                                url = route.url,
                                articleMode = route.article,
                                onBack = { navController.popBackStack() },
                                onOpenNotes = { navController.navigate(NotesRoute) },
                                onCreateReminder = { title, note ->
                                    navController.navigate(
                                        ReminderEditorRoute(
                                            prefillTitle = title,
                                            prefillNote = note
                                        )
                                    )
                                },
                            )
                        }
                        composable<ScannerRoute> {
                            QrScannerScreen(
                                onClose = { navController.popBackStack() },
                                onOpenInApp = { url ->
                                    navController.navigate(BrowserRoute(url)) {
                                        popUpTo<ScannerRoute> {
                                            inclusive = true
                                        }
                                    }
                                },
                            )
                        }

                        composable<RemindersRoute> {
                            RemindersScreen(
                                onBack = { navController.popBackStack() },
                                onAdd = { navController.navigate(ReminderEditorRoute()) },
                                onEdit = { navController.navigate(ReminderEditorRoute(it)) },
                            )
                        }
                        composable<ReminderEditorRoute> {
                            ReminderEditorScreen(onClose = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
        LaunchedEffect(launchTarget) {
            val target = launchTarget ?: return@LaunchedEffect
            if (startDestination == HomeRoute) { // before onboarding there is nothing to open
                when (target) {
                    LaunchTarget.Scanner -> navController.navigate(ScannerRoute)
                    LaunchTarget.News -> navController.navigate(NewsRoute)
                    is LaunchTarget.Browser -> navController.navigate(BrowserRoute(target.url, article = true))
                    LaunchTarget.Game -> GameEntry?.invoke(navController) ?: navController.navigateTopLevel(ServicesRoute)
                    is LaunchTarget.Article -> navController.navigate(NewsRoute) // resolved to Browser earlier; this is only a fallback
                }
            }
            onLaunchTargetConsumed()
        }

        val imeVisible = WindowInsets.ime.getBottom(density) > 0
        AnimatedVisibility(
            visible = showBottomBar && barState.visible && !imeVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(250)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(250)) { it } + fadeOut(tween(150)),
        ) {
            AnimatedBottomBar(
                current = destination,
                onSelect = navController::navigateTopLevel,
                onScanQr = { navController.navigate(ScannerRoute) },
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = Dimens.SpaceLg, vertical = FloatingBarMargin),
            )
        }
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabDirection(): Int? {
    val from = TopLevelDestinations.indexOfFirst { initialState.destination.isIn(it) }
    val to = TopLevelDestinations.indexOfFirst { targetState.destination.isIn(it) }
    return if (from >= 0 && to >= 0 && from != to) (if (to > from) 1 else -1) else null
}

@Composable
private fun AppBottomBar(current: NavDestination?, onSelect: (Any) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        TopLevelDestinations.forEach { top ->
            NavigationBarItem(
                selected = current.isIn(top),
                onClick = { onSelect(top.route) },
                icon = { Icon(top.icon, contentDescription = null) }, // the label names it
                label = { Text(stringResource(top.label)) },
                alwaysShowLabel = true,
            )
        }
    }
}

private fun NavHostController.navigateTopLevel(route: Any) = navigate(route) {
    popUpTo<HomeRoute> { saveState = true }
    launchSingleTop = true
    restoreState = true
}