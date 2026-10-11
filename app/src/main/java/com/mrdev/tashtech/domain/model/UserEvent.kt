package com.mrdev.tashtech.domain.model

import java.time.LocalDate
import java.time.LocalTime

enum class EventKind { Lesson, Event, Study, Other }
const val MAX_EVENT_TITLE = 80
data class UserEvent(
    val id: Long = 0L,
    val title: String,
    val kind: EventKind = EventKind.Event,
    val date: LocalDate,
    val start: LocalTime,
    val end: LocalTime,
    val location: String = "",
    val repeatWeekly: Boolean = false,
) {
    /** One-off on [date], or every week on the same weekday from [date] onwards. */
    fun occursOn(day: LocalDate): Boolean =
        day == date || (repeatWeekly && !day.isBefore(date) && day.dayOfWeek == date.dayOfWeek)


     fun toSession(on: LocalDate) = ClassSession(
        id = "event#$id@$on",
        day = on.dayOfWeek,
        start = start,
        end = end,
        subject = title,
        room = location,
        teacher = "",
        origin = SessionOrigin.Custom,
        kind = kind,
        eventId = id,
    )
}
fun UserEvent.isValid(): Boolean = title.isNotBlank() && title.length <= MAX_EVENT_TITLE && end.isAfter(start)
