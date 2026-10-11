package com.mrdev.tashtech.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrdev.tashtech.data.preferences.UserPreferencesRepository
import com.mrdev.tashtech.data.preferences.UserProfile
import com.mrdev.tashtech.data.repository.ReminderRepository
import com.mrdev.tashtech.data.repository.TimetableRepository
import com.mrdev.tashtech.data.repository.UserEventRepository
import com.mrdev.tashtech.domain.schedule.minuteTicker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock

class HomeViewModel(
    userPrefs: UserPreferencesRepository,
    private val timetable: TimetableRepository,
    private val reminders: ReminderRepository,
    private val events: UserEventRepository,
    private val clock: Clock,
) : ViewModel() {

    private val retryTrigger = MutableStateFlow(0)

    // WhileSubscribed: the minute ticker only runs while Home is on screen (STARTED),
    // and restarts with a fresh `now` when the user comes back.
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> =
        combine(userPrefs.profile.filterNotNull(), retryTrigger) { profile, _ -> profile }
            .flatMapLatest { profile -> contentFlow(profile) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    fun retry() = retryTrigger.update { it + 1 }

    private fun contentFlow(profile: UserProfile): Flow<HomeUiState> = flow {
        emit(HomeUiState.Loading)
        val sessions = try {
            timetable.getSessions(profile.group)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(HomeUiState.Error)
            return@flow
        }
        emitAll(
            combine(
                minuteTicker(clock),
                reminders.observeAll().catch { emit(emptyList()) },
                events.observeAll().catch { emit(emptyList()) },
            ) { now, reminderList, eventList -> buildHomeContent(profile, sessions, now, reminderList, eventList) },
        )
    }
}