package com.mrdev.tashtech.domain.model

import com.mrdev.tashtech.domain.schedule.AppTime
import java.time.LocalDateTime

/** [dueAt] is wall-clock time in Asia/Tashkent; alarms are stored as absolute instants. */
data class Reminder(
    val id: Long = 0L,
    val title: String,
    val note: String = "",
    val course: String = "",
    val dueAt: LocalDateTime,
    /** Minutes before [dueAt] at which to alert; 0 = at the due time. */
    val offsetsMinutes: List<Int> = listOf(0),
    val completed: Boolean = false,
) {
    val dueAtMillis: Long
        get() = dueAt.atZone(AppTime.ZONE).toInstant().toEpochMilli()
}