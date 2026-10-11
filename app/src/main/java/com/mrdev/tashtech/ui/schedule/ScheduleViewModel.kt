package com.mrdev.tashtech.ui.schedule

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrdev.tashtech.data.preferences.UserPreferencesRepository
import com.mrdev.tashtech.data.repository.TimetableRepository
import com.mrdev.tashtech.data.repository.UserEventRepository
import com.mrdev.tashtech.domain.model.UserEvent
import com.mrdev.tashtech.domain.schedule.minuteTicker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

class ScheduleViewModel(
    userPrefs: UserPreferencesRepository,
    private val timetable: TimetableRepository,
    private val userEvents: UserEventRepository,
    private val clock: Clock,
) : ViewModel() {

    private val retryTrigger = MutableStateFlow(0)
    private val mode = MutableStateFlow(ScheduleMode.Today)
    private val selectedDate = MutableStateFlow(LocalDate.now(clock))

    /** null = the student's own group. Temporary: the screen resets it when the tab is left. */
    private val viewedGroup = MutableStateFlow<String?>(null)

    // Skip the full-screen spinner when merely switching groups.
    private var hasContent = false

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ScheduleUiState> =
        combine(userPrefs.profile.filterNotNull(), viewedGroup, retryTrigger) { profile, viewed, _ ->
            profile.group to (viewed ?: profile.group)
        }
            .flatMapLatest { (own, shown) ->
                flow {
                    if (!hasContent) emit(ScheduleUiState.Loading)
                    val sessions: List<com.mrdev.tashtech.domain.model.ClassSession>
                    val groups: List<String>
                    try {
                        sessions = timetable.getSessions(shown)
                        groups = timetable.getGroups()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        hasContent = false
                        emit(ScheduleUiState.Error)
                        return@flow
                    }
                    emitAll(
                        combine(
                            minuteTicker(clock), mode, selectedDate,
                            userEvents.observeAll().catch { emit(emptyList()) },
                        ) { now, m, d, events ->
                            try {
                                buildScheduleContent(shown, own, groups, sessions, now, m, d, events).also { hasContent = true }
                            } catch (e: Exception) {
                                Log.e("TashTechSchedule", "Couldn't build the schedule", e)
                                hasContent = false
                                ScheduleUiState.Error
                            }
                        }
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScheduleUiState.Loading)
    fun saveEvent(event: UserEvent) { viewModelScope.launch { userEvents.save(event) } }
    fun deleteEvent(id: Long) { viewModelScope.launch { userEvents.delete(id) } }
    fun onModeChange(value: ScheduleMode) = mode.update { value }
    fun shiftWeek(weeks: Long) = selectedDate.update {
        it.plusWeeks(weeks).coerceIn(Semester.START, maxOf(Semester.END, LocalDate.now(clock)))
    }

    fun jumpToToday() { selectedDate.value = LocalDate.now(clock) }
    fun onDateSelected(date: LocalDate) = selectedDate.update { date }

    fun onViewGroup(group: String) = viewedGroup.update { group }

    fun resetViewedGroup() = viewedGroup.update { null }

    fun retry() = retryTrigger.update { it + 1 }
}