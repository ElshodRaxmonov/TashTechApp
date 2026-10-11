package com.mrdev.tashtech.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrdev.tashtech.data.preferences.UserPreferencesRepository
import com.mrdev.tashtech.data.repository.ReminderRepository
import com.mrdev.tashtech.domain.model.Reminder
import com.mrdev.tashtech.domain.reminder.ReminderRules
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

const val MAX_TITLE_LENGTH = 80
const val MAX_NOTE_LENGTH = 300
const val MAX_COURSE_LENGTH = 20

data class SaveResult(val needsNotificationPrompt: Boolean)

data class ReminderEditorUiState(
    val isNew: Boolean,
    val isLoading: Boolean,
    val date: LocalDate,
    val time: LocalTime,
    val title: String = "",
    val note: String = "",
    val course: String = "",
    val offsets: List<Int> = listOf(0),
    val completed: Boolean = false,
    val pastError: Boolean = false,
    val saving: Boolean = false,
    val saveFailed: Boolean = false,
    val saveResult: SaveResult? = null,
    val deleted: Boolean = false,
    val notFound: Boolean = false,
) {
    val canSave: Boolean get() = title.isNotBlank() && !saving && !isLoading
    val canAddOffset: Boolean get() = offsets.size < ReminderRules.MAX_OFFSETS
}

class ReminderEditorViewModel(
    private val repository: ReminderRepository,
    private val userPrefs: UserPreferencesRepository,
    private val clock: Clock,
    private val reminderId: Long,
    private val prefillTitle: String ,
    private val prefillNote: String// 0 = new
) : ViewModel() {

    private val _state = MutableStateFlow(initialState())
    val uiState: StateFlow<ReminderEditorUiState> = _state.asStateFlow()

    init {
        if (reminderId != 0L) load()
    }

    private fun initialState(): ReminderEditorUiState {

        // Default: the next full hour.
        val suggested = LocalDateTime.now(clock).plusHours(1).truncatedTo(ChronoUnit.HOURS)
        return ReminderEditorUiState(
            isNew = reminderId == 0L,
            isLoading = reminderId != 0L,
            date = suggested.toLocalDate(),
            time = suggested.toLocalTime(),
            title = prefillTitle.take(MAX_TITLE_LENGTH),
            note = prefillNote.take(MAX_NOTE_LENGTH),
        )
    }

    private fun load() {
        viewModelScope.launch {
            val reminder = try {
                repository.get(reminderId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            _state.update {
                if (reminder == null) it.copy(isLoading = false, notFound = true)
                else it.copy(
                    isLoading = false,
                    title = reminder.title,
                    note = reminder.note,
                    course = reminder.course,
                    date = reminder.dueAt.toLocalDate(),
                    time = reminder.dueAt.toLocalTime(),
                    offsets = reminder.offsetsMinutes,
                    completed = reminder.completed,
                )
            }
        }
    }

    fun onTitleChange(v: String) = _state.update { it.copy(title = v.take(MAX_TITLE_LENGTH)) }
    fun onNoteChange(v: String) = _state.update { it.copy(note = v.take(MAX_NOTE_LENGTH)) }
    fun onCourseChange(v: String) = _state.update { it.copy(course = v.take(MAX_COURSE_LENGTH)) }
    fun onDateChange(v: LocalDate) = _state.update { it.copy(date = v, pastError = false) }
    fun onTimeChange(v: LocalTime) = _state.update { it.copy(time = v, pastError = false) }

    fun toggleOffset(minutes: Int) = _state.update {
        val next = when {
            minutes in it.offsets -> it.offsets - minutes
            it.canAddOffset -> it.offsets + minutes
            else -> it.offsets
        }
        it.copy(offsets = ReminderRules.normalizeOffsets(next))
    }

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        val due = LocalDateTime.of(s.date, s.time)
        if (s.isNew && !due.isAfter(LocalDateTime.now(clock))) {
            _state.update { it.copy(pastError = true) }
            return
        }
        _state.update { it.copy(saving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                repository.save(
                    Reminder(
                        id = reminderId,
                        title = s.title.trim(),
                        note = s.note.trim(),
                        course = s.course.trim(),
                        dueAt = due,
                        offsetsMinutes = s.offsets,
                        completed = s.completed,
                    ),
                )
                val prompted = userPrefs.notificationPromptShown.first()
                _state.update { it.copy(saving = false, saveResult = SaveResult(!prompted)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, saveFailed = true) }
            }
        }
    }

    fun delete() {
        if (reminderId == 0L) return
        viewModelScope.launch {
            try {
                repository.delete(reminderId)
                _state.update { it.copy(deleted = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(saveFailed = true) }
            }
        }
    }

    fun markNotificationPromptShown() {
        viewModelScope.launch { userPrefs.markNotificationPromptShown() }
    }
}