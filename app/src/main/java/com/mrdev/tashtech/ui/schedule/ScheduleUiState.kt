package com.mrdev.tashtech.ui.schedule

import com.mrdev.tashtech.domain.model.ClassSession
import com.mrdev.tashtech.domain.model.UserEvent
import com.mrdev.tashtech.domain.schedule.ScheduleCalculator
import com.mrdev.tashtech.domain.schedule.TimelineEntry
import java.time.LocalDate
import java.time.LocalDateTime

enum class ScheduleMode { Today, Week }

data class DayChip(val date: LocalDate, val classCount: Int, val isToday: Boolean)

sealed interface ScheduleUiState {
    data object Loading : ScheduleUiState
    data object Error : ScheduleUiState
    data class Content(


        val events: List<UserEvent>,
        val group: String,          // group being displayed
        val ownGroup: String,       // the student's saved group
        val allGroups: List<String>,
        val mode: ScheduleMode,
        val today: LocalDate,
        val selectedDate: LocalDate,
        val days: List<DayChip>,
        val timeline: List<TimelineEntry>,
        val classCount: Int,
        val afterSemester: Boolean= selectedDate.isAfter(Semester.END),
        val weekStart: LocalDate,
        val weekEnd: LocalDate,
        val canGoPrev: Boolean,
        val canGoNext: Boolean,
        val isCurrentWeek: Boolean,
        val minDate: LocalDate,
        val maxDate: LocalDate,
    ) : ScheduleUiState {

        val isViewingToday: Boolean get() = selectedDate == today
        val isOwnGroup: Boolean get() = group == ownGroup
    }

}

/** Pure: Today mode always shows today; Week mode shows [selectedDate] if it is in this week. */
internal fun buildScheduleContent(
    group: String,
    ownGroup: String,
    allGroups: List<String>,
    sessions: List<ClassSession>,
    now: LocalDateTime,
    mode: ScheduleMode,
    selectedDate: LocalDate,
    events: List<UserEvent> = emptyList()
): ScheduleUiState.Content {
    val today = now.toLocalDate()
    val minDate = minOf(Semester.START, today)
    val maxDate = maxOf(Semester.END, today)
    val date = if (mode == ScheduleMode.Week) selectedDate.coerceIn(minDate, maxDate) else today
    val week = ScheduleCalculator.weekDates(date)
    val visibleEvents = if (group == ownGroup) events else emptyList()
    fun sessionsFor(d: LocalDate) = ScheduleCalculator.sessionsForDate(sessions, visibleEvents, d)
    val timeline = ScheduleCalculator.dayTimeline(sessionsFor(date), date, now)
     return ScheduleUiState.Content(
         group = group, ownGroup = ownGroup, allGroups = allGroups,
         mode = mode, today = today, selectedDate = date,
         days = week.map { d -> DayChip(d, sessionsFor(d).size, d == today) },
         timeline = timeline,
         classCount = timeline.count { it is TimelineEntry.Class },
         events = visibleEvents,
         weekStart = week.first(),
         weekEnd = week.last(),
         canGoPrev = week.first().isAfter(ScheduleCalculator.weekDates(minDate).first()),
         canGoNext = week.first().isBefore(ScheduleCalculator.weekDates(maxDate).first()),
         isCurrentWeek = today in week,
         minDate = minDate,
         maxDate = maxDate,
     )
}