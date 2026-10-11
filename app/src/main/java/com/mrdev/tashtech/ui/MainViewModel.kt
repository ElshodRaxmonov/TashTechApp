package com.mrdev.tashtech.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.mrdev.tashtech.TashTechApp
import com.mrdev.tashtech.data.preferences.UserPreferencesRepository
import com.mrdev.tashtech.data.repository.NewsRepository
import com.mrdev.tashtech.data.repository.TimetableRepository
import com.mrdev.tashtech.domain.schedule.AppTime
import com.mrdev.tashtech.ui.browser.BrowserViewModel
import com.mrdev.tashtech.ui.home.HomeViewModel
import com.mrdev.tashtech.ui.navigation.AppLinks
import com.mrdev.tashtech.ui.navigation.HomeRoute
import com.mrdev.tashtech.ui.navigation.LaunchTarget
import com.mrdev.tashtech.ui.navigation.NewsDetailsRoute
import com.mrdev.tashtech.ui.navigation.OnboardingRoute
import com.mrdev.tashtech.ui.navigation.ReminderEditorRoute
import com.mrdev.tashtech.ui.news.NewsDetailsViewModel
import com.mrdev.tashtech.ui.news.NewsPreviewViewModel
import com.mrdev.tashtech.ui.news.NewsViewModel
import com.mrdev.tashtech.ui.notes.NotesViewModel
import com.mrdev.tashtech.ui.onboarding.OnboardingViewModel
import com.mrdev.tashtech.ui.profile.ProfileViewModel
import com.mrdev.tashtech.ui.reminders.ReminderEditorViewModel
import com.mrdev.tashtech.ui.reminders.RemindersViewModel
import com.mrdev.tashtech.ui.schedule.ScheduleViewModel
import com.mrdev.tashtech.ui.services.ToolsViewModel
import com.mrdev.tashtech.ui.settings.SettingsViewModel
import com.mrdev.tashtech.ui.theme.ThemeMode
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException

data class Bootstrap(val start: Any, val theme: ThemeMode)

private const val STARTUP_WAIT_MS = 3_500L

class MainViewModel(
    userPrefs: UserPreferencesRepository,
    private val timetable: TimetableRepository,
    private val news: NewsRepository,
) : ViewModel() {

    private val _bootstrap = MutableStateFlow<Bootstrap?>(null)
    val bootstrap: StateFlow<Bootstrap?> = _bootstrap.asStateFlow()

    /** True once Home's first data is in memory (or the wait cap hit). Never waits for images. */
    private val _contentReady = MutableStateFlow(false)
    val contentReady: StateFlow<Boolean> = _contentReady.asStateFlow()
    private val _launchTarget = MutableStateFlow<LaunchTarget?>(null)
    val launchTarget: StateFlow<LaunchTarget?> = _launchTarget.asStateFlow()

    fun handleIntent(intent: Intent?) {
        when (val target = AppLinks.parse(intent) ?: return) {
            is LaunchTarget.Article -> viewModelScope.launch {
                val url = news.find(target.id)?.articleUrl?.takeIf { it.startsWith("https://") }
                _launchTarget.value = if (url != null) LaunchTarget.Browser(url) else LaunchTarget.News
            }
            else -> _launchTarget.value = target
        }
    }

    fun consumeLaunchTarget() { _launchTarget.value = null }
    init {
        viewModelScope.launch {
            val profile = userPrefs.profile.first()
            val theme = userPrefs.themeMode.first()
            _bootstrap.value = Bootstrap(if (profile == null) OnboardingRoute else HomeRoute, theme)
            if (profile == null) { // onboarding needs no data
                _contentReady.value = true
                return@launch
            }
            // Warm the exact caches Home reads. async keeps the work alive if the timeout fires,
            // so Home still gets the data a moment later instead of restarting it.
            val sessions = async { attempt { timetable.getSessions(profile.group) } }
            val feed = async { attempt { news.refresh() } }
            withTimeoutOrNull(STARTUP_WAIT_MS) { sessions.await(); feed.await() }
            _contentReady.value = true
        }
    }

    private suspend fun <T> attempt(block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null // a failed news or timetable load must never block the app
    }
}

object AppViewModelProvider {

    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            HomeViewModel(app().container.userPreferences, app().container.timetable, app().container.reminders, app().container.userEvents, AppTime.systemClock())

        }
        initializer { ToolsViewModel(app().container.reminders, app().container.notes) }
        initializer { OnboardingViewModel(app().container.studentSignIn) }
        initializer { SettingsViewModel(app().container.userPreferences, app().container.reminders) }
        initializer { BrowserViewModel(app().container.notes) }
        initializer { NotesViewModel(app().container.notes) }
        initializer {
            RemindersViewModel(
                app().container.reminders,
                app().container.userPreferences,
                AppTime.systemClock()
            )
        }
        initializer { ProfileViewModel(app().container.profile, app().container.studentSignIn)}
        initializer { NewsPreviewViewModel(app().container.news) }
        initializer { NewsViewModel(app().container.news) }
        initializer {
            val route = createSavedStateHandle().toRoute<NewsDetailsRoute>()
            NewsDetailsViewModel(app().container.news, route.id)
        }
        initializer {
            // The editor's route arguments arrive through the nav back stack entry's SavedStateHandle.
            val route = createSavedStateHandle().toRoute<ReminderEditorRoute>()
            ReminderEditorViewModel(
                app().container.reminders,
                app().container.userPreferences,
                AppTime.systemClock(),
                route.reminderId,
                route.prefillTitle,
                route.prefillNote,
            )

        }

        initializer {
            ScheduleViewModel(app().container.userPreferences, app().container.timetable, app().container.userEvents, AppTime.systemClock())
        }
        initializer { MainViewModel(app().container.userPreferences, app().container.timetable, app().container.news) }
    }

    private fun CreationExtras.app(): TashTechApp =
        this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TashTechApp
}
