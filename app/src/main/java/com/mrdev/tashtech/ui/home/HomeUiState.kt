package com.mrdev.tashtech.ui.home

import com.mrdev.tashtech.data.preferences.UserProfile
import com.mrdev.tashtech.domain.model.ClassSession
import com.mrdev.tashtech.domain.model.Reminder
import com.mrdev.tashtech.domain.model.Sex
import com.mrdev.tashtech.domain.model.UserEvent
import com.mrdev.tashtech.domain.schedule.ScheduleCalculator
import com.mrdev.tashtech.ui.reminders.ReminderItem
import com.mrdev.tashtech.ui.reminders.activeReminderItems
import com.mrdev.tashtech.ui.schedule.Semester
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

enum class DayPart { Morning, Afternoon, Evening }

sealed interface HeroState {
    data class Current(
        val session: ClassSession,
        val minutesLeft: Int,
        val progress: Float,
    ) : HeroState

    /** [minutesUntil] is only set when the class is later today. */
    data class Next(
        val session: ClassSession,
        val date: LocalDate,
        val dayOffset: Int,
        val minutesUntil: Int?,
    ) : HeroState

    data object None : HeroState
}
data class DaySummary(
    val total: Int,
    val remaining: Int,
    val first: LocalTime?,
    val last: LocalTime?,
)
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object Error : HomeUiState
    data class Content(
        val name: String,
        val group: String,
        val dayPart: DayPart,
        val hero: HeroState,
        val now: LocalDateTime,
        val summary: DaySummary,
        val nextReminder: ReminderItem?,
        val activeReminderCount: Int,
        val semesterEnded: Boolean,
        val sex: Sex
    ) : HomeUiState {
    }
}

/** Pure: everything Home shows is derived from (profile, sessions, now). */

internal fun buildHomeContent(
    profile: UserProfile,
    sessions: List<ClassSession>,
    now: LocalDateTime,
    reminders: List<Reminder> = emptyList(),
    events: List<UserEvent> = emptyList(),
): HomeUiState.Content {
    val time = now.toLocalTime()
    val sessionsFor: (LocalDate) -> List<ClassSession> =
        { d -> ScheduleCalculator.sessionsForDate(sessions, events, d) }
    val current = ScheduleCalculator.currentClass(sessionsFor, now)

    val hero: HeroState = if (current != null) {
        val total = current.duration.seconds.toFloat()
        val elapsed = Duration.between(current.start, time).seconds.toFloat()
        HeroState.Current(
            session = current,
            minutesLeft = ceilMinutes(Duration.between(time, current.end)),
            progress = (elapsed / total).coerceIn(0f, 1f),
        )
    } else {
        ScheduleCalculator.nextClass(sessionsFor, now)?.let { next ->
            val offset = ChronoUnit.DAYS.between(now.toLocalDate(), next.date).toInt()
            HeroState.Next(
                session = next.session,
                date = next.date,
                dayOffset = offset,
                minutesUntil = if (offset == 0) ceilMinutes(Duration.between(time, next.session.start)) else null,
            )
        } ?: HeroState.None
    }

    val today = sessionsFor(now.toLocalDate())
    val active = activeReminderItems(reminders, now)
    return HomeUiState.Content(
        sex = profile.sex,
        name = profile.name,
        group = profile.group,
        dayPart = dayPartOf(now.hour),
        hero = hero,
        summary = DaySummary(
            total = today.size,
            remaining = today.count { it.end > time },
            first = today.minOfOrNull { it.start },
            last = today.maxOfOrNull { it.end },
        ),
        now = now,
        nextReminder = active.firstOrNull(),
        activeReminderCount = active.size,
        semesterEnded = now.toLocalDate().isAfter(Semester.END)
    )
}
internal fun dayPartOf(hour: Int): DayPart = when (hour) {
    in 5..11 -> DayPart.Morning
    in 12..17 -> DayPart.Afternoon
    else -> DayPart.Evening
}

/** Round up so "59 s left" reads "1 min", never "0 min". */
private fun ceilMinutes(d: Duration): Int = ((d.seconds + 59) / 60).toInt().coerceAtLeast(1)