package com.mrdev.tashtech.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrdev.tashtech.data.preferences.UserPreferencesRepository
import com.mrdev.tashtech.data.repository.ReminderRepository
import com.mrdev.tashtech.domain.model.Reminder
import com.mrdev.tashtech.domain.schedule.minuteTicker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDateTime

class RemindersViewModel(
    private val repository: ReminderRepository,
    private val userPrefs: UserPreferencesRepository,
    clock: Clock,
) : ViewModel() {

    private val retryTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<RemindersUiState> = retryTrigger
        .flatMapLatest {
            combine<List<Reminder>, LocalDateTime, RemindersUiState>(
                repository.observeAll(),
                minuteTicker(clock),
            ) { reminders, now -> buildRemindersContent(reminders, now) }
                .catch { emit(RemindersUiState.Error) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemindersUiState.Loading)

    val notificationPromptShown: StateFlow<Boolean> = userPrefs.notificationPromptShown
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun toggleCompleted(id: Long, completed: Boolean) {
        viewModelScope.launch { repository.setCompleted(id, completed) }
    }

    fun clearCompleted() {
        viewModelScope.launch { repository.deleteCompleted() }
    }

    fun markNotificationPromptShown() {
        viewModelScope.launch { userPrefs.markNotificationPromptShown() }
    }

    fun retry() = retryTrigger.update { it + 1 }
}