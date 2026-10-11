package com.mrdev.tashtech.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mrdev.tashtech.domain.model.EventKind
import com.mrdev.tashtech.domain.model.UserEvent
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "user_events")
data class UserEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val kind: String,
    val epochDay: Long,
    val startMinute: Int,
    val endMinute: Int,
    val location: String,
    val repeatWeekly: Boolean,
)

internal fun UserEventEntity.toDomain() = UserEvent(
    id = id,
    title = title,
    kind = EventKind.entries.firstOrNull { it.name == kind } ?: EventKind.Other,
    date = LocalDate.ofEpochDay(epochDay),
    start = LocalTime.ofSecondOfDay(startMinute * 60L),
    end = LocalTime.ofSecondOfDay(endMinute * 60L),
    location = location,
    repeatWeekly = repeatWeekly,
)

internal fun UserEvent.toEntity() = UserEventEntity(
    id = id,
    title = title,
    kind = kind.name,
    epochDay = date.toEpochDay(),
    startMinute = start.hour * 60 + start.minute,
    endMinute = end.hour * 60 + end.minute,
    location = location,
    repeatWeekly = repeatWeekly,
)