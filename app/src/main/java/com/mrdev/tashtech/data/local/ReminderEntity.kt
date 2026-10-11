package com.mrdev.tashtech.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mrdev.tashtech.domain.model.Reminder
import com.mrdev.tashtech.domain.reminder.ReminderRules
import com.mrdev.tashtech.domain.schedule.AppTime
import java.time.Instant
import java.time.LocalDateTime

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val note: String,
    val course: String,
    val dueAtMillis: Long,
    val offsets: String,
    val completed: Boolean,
)

internal fun ReminderEntity.toDomain() = Reminder(
    id = id,
    title = title,
    note = note,
    course = course,
    dueAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(dueAtMillis), AppTime.ZONE),
    offsetsMinutes = ReminderRules.decodeOffsets(offsets),
    completed = completed,
)

internal fun Reminder.toEntity() = ReminderEntity(
    id = id,
    title = title,
    note = note,
    course = course,
    dueAtMillis = dueAtMillis,
    offsets = ReminderRules.encodeOffsets(offsetsMinutes),
    completed = completed,
)