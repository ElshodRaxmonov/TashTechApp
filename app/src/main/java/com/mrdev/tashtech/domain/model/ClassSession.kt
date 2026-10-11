package com.mrdev.tashtech.domain.model

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime

data class ClassSession(
    val origin: SessionOrigin = SessionOrigin.Official,
    val kind: EventKind? = null,
    val eventId: Long? = null,
    val id: String, // "<group>#<index in source file>", stable list key and future reminder link
    val day: DayOfWeek,
    val start: LocalTime,
    val end: LocalTime,
    val subject: String,
    val room: String,
    val teacher: String,
    val lessonType: LessonType? = null
) {
    val duration: Duration get() = Duration.between(start, end)
}

enum class SessionOrigin { Official, Custom }
data class Timetable(val groups: Map<String, List<ClassSession>>)
