package com.mrdev.tashtech.ui.reminders

import com.mrdev.tashtech.domain.model.Reminder
import com.mrdev.tashtech.domain.reminder.ReminderRules
import java.time.LocalDateTime

data class ReminderItem(val reminder: Reminder, val isOverdue: Boolean)

sealed interface RemindersUiState {
    data object Loading : RemindersUiState
    data object Error : RemindersUiState
    data class Content(
        val active: List<ReminderItem>,
        val completed: List<ReminderItem>,
        val now: LocalDateTime,
    ) : RemindersUiState
}

/** Active reminders, soonest first (overdue ones therefore lead). Shared with Home. */
internal fun activeReminderItems(reminders: List<Reminder>, now: LocalDateTime): List<ReminderItem> =
    reminders.filter { !it.completed }
        .sortedBy { it.dueAt }
        .map { ReminderItem(it, ReminderRules.isOverdue(it, now)) }

internal fun buildRemindersContent(reminders: List<Reminder>, now: LocalDateTime) =
    RemindersUiState.Content(
        active = activeReminderItems(reminders, now),
        completed = reminders.filter { it.completed }
            .sortedByDescending { it.dueAt }
            .map { ReminderItem(it, isOverdue = false) },
        now = now,
    )