package com.mrdev.tashtech.ui.services

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrdev.tashtech.data.repository.NoteRepository
import com.mrdev.tashtech.data.repository.ReminderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ToolCounts(val activeReminders: Int = 0, val notes: Int = 0)

class ToolsViewModel(reminders: ReminderRepository, notes: NoteRepository) : ViewModel() {
    val counts: StateFlow<ToolCounts> =
        combine(reminders.observeActiveCount(), notes.observeCount()) { r, n -> ToolCounts(r, n) }
            .catch { emit(ToolCounts()) } // a failed count must never break the screen
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ToolCounts())
}