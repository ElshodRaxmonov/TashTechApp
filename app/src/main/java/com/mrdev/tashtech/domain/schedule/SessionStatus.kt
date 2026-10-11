package com.mrdev.tashtech.domain.schedule

import com.mrdev.tashtech.domain.model.ClassSession
import com.mrdev.tashtech.domain.model.UserEvent
import com.mrdev.tashtech.ui.schedule.Semester
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

enum class SessionStatus { Completed, Current, Upcoming }

sealed interface TimelineEntry {
    val start: LocalTime
    val end: LocalTime
    val status: SessionStatus

    data class Class(
        val session: ClassSession,
        override val status: SessionStatus,
    ) : TimelineEntry {
        override val start: LocalTime get() = session.start
        override val end: LocalTime get() = session.end
    }

    data class FreePeriod(
        override val start: LocalTime,
        override val end: LocalTime,
        override val status: SessionStatus,
    ) : TimelineEntry
}

data class UpcomingClass(val session: ClassSession, val date: LocalDate)

/**
 * Pure functions, no Android and no clock access: callers pass `now`
 * (e.g. `LocalDateTime.now(AppTime.systemClock())`), which keeps everything trivially testable.
 */
object ScheduleCalculator {
    fun sessionsForDate(official: List<ClassSession>, events: List<UserEvent>, date: LocalDate): List<ClassSession> {
        val base = if (Semester.isActive(date)) sessionsOn(official, date.dayOfWeek) else emptyList()
        return (base + events.filter { it.occursOn(date) }.map { it.toSession(date) })
            .sortedWith(compareBy({ it.start }, { it.end }))
    }
    fun currentClass(sessionsFor: (LocalDate) -> List<ClassSession>, now: LocalDateTime): ClassSession? {
        val t = now.toLocalTime()
        return sessionsFor(now.toLocalDate()).firstOrNull { t >= it.start && t < it.end }
    }

    fun nextClass(sessionsFor: (LocalDate) -> List<ClassSession>, now: LocalDateTime): UpcomingClass? {
        val today = now.toLocalDate()
        val t = now.toLocalTime()
        for (offset in 0..7) {
            val date = today.plusDays(offset.toLong())
            val next = sessionsFor(date).firstOrNull { offset > 0 || it.start > t }
            if (next != null) return UpcomingClass(next, date)
        }
        return null
    }

    /** Gaps shorter than this between classes are breaks, not free periods. */
    val MinFreePeriod: Duration = Duration.ofMinutes(30)

    fun sessionsOn(sessions: List<ClassSession>, day: DayOfWeek): List<ClassSession> =
        sessions.filter { it.day == day }.sortedWith(compareBy({ it.start }, { it.end }))

    /** Status of a session on [date], relative to [now]. Start inclusive, end exclusive. */
    fun statusOf(session: ClassSession, date: LocalDate, now: LocalDateTime): SessionStatus =
        statusOf(session.start, session.end, date, now)

    private fun statusOf(
        start: LocalTime,
        end: LocalTime,
        date: LocalDate,
        now: LocalDateTime,
    ): SessionStatus {
        val today = now.toLocalDate()
        return when {
            date.isBefore(today) -> SessionStatus.Completed
            date.isAfter(today) -> SessionStatus.Upcoming
            else -> {
                val t = now.toLocalTime()
                when {
                    t < start -> SessionStatus.Upcoming
                    t < end -> SessionStatus.Current
                    else -> SessionStatus.Completed
                }
            }
        }
    }

    /** Chronological classes for [date] with free periods between them (never before the first or after the last). */
    fun dayTimeline(
        sessions: List<ClassSession>,
        date: LocalDate,
        now: LocalDateTime,
        minFreePeriod: Duration = MinFreePeriod,
    ): List<TimelineEntry> {
        val entries = mutableListOf<TimelineEntry>()
        var latestEnd: LocalTime? = null
        for (session in sessionsOn(sessions, date.dayOfWeek)) {
            val previousEnd = latestEnd
            if (previousEnd != null && Duration.between(previousEnd, session.start) >= minFreePeriod) {
                entries += TimelineEntry.FreePeriod(
                    start = previousEnd,
                    end = session.start,
                    status = statusOf(previousEnd, session.start, date, now),
                )
            }
            entries += TimelineEntry.Class(session, statusOf(session, date, now))
            // Measured from the latest end so overlapping classes don't create fake gaps.
            latestEnd = if (previousEnd == null || session.end.isAfter(previousEnd)) session.end else previousEnd
        }
        return entries
    }

    fun currentClass(sessions: List<ClassSession>, now: LocalDateTime): ClassSession? {
        val t = now.toLocalTime()
        return sessionsOn(sessions, now.dayOfWeek).firstOrNull { t >= it.start && t < it.end }
    }

    /**
     * First class that starts strictly after [now]: today, otherwise the next day that has one,
     * searching a full week ahead. A class starting exactly now is "current", not "next".
     */
    fun nextClass(sessions: List<ClassSession>, now: LocalDateTime): UpcomingClass? {
        val today = now.toLocalDate()
        val t = now.toLocalTime()
        for (offset in 0..7) {
            val date = today.plusDays(offset.toLong())
            val next = sessionsOn(sessions, date.dayOfWeek).firstOrNull { offset > 0 || it.start > t }
            if (next != null) return UpcomingClass(next, date)
        }
        return null
    }

    /** The 7 dates of the week containing [today], starting on [AppTime.WEEK_START]. */
    fun weekDates(today: LocalDate): List<LocalDate> {
        val first = today.with(TemporalAdjusters.previousOrSame(AppTime.WEEK_START))
        return (0L..6L).map { first.plusDays(it) }
    }
}