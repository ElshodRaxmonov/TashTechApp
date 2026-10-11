package com.mrdev.tashtech.domain.reminder

import com.mrdev.tashtech.domain.model.Reminder
import java.time.LocalDateTime

enum class OffsetUnit(val minutes: Int) { Minutes(1), Hours(60), Days(24 * 60) }

/** Pure reminder logic: no Android, fully unit-tested. */
object ReminderRules {
    const val MAX_OFFSETS = 5
    const val MAX_OFFSET_MINUTES = 30 * 24 * 60
    val PRESET_OFFSETS = listOf(0, 60, 24 * 60)

    fun normalizeOffsets(offsets: Collection<Int>): List<Int> =
        offsets.filter { it in 0..MAX_OFFSET_MINUTES }.distinct().sorted().take(MAX_OFFSETS)

    fun encodeOffsets(offsets: List<Int>): String = offsets.joinToString(",")

    fun decodeOffsets(raw: String): List<Int> =
        normalizeOffsets(raw.split(',').mapNotNull { it.trim().toIntOrNull() })

    /** null when the amount is not positive or exceeds [MAX_OFFSET_MINUTES]. */
    fun customOffsetMinutes(amount: Int, unit: OffsetUnit): Int? {
        if (amount <= 0) return null
        val minutes = amount.toLong() * unit.minutes
        return if (minutes <= MAX_OFFSET_MINUTES) minutes.toInt() else null
    }

    /** One alarm to schedule. [slot] is the index in the normalized offset list (stable per reminder). */
    data class Trigger(val slot: Int, val offsetMinutes: Int, val atMillis: Long)

    fun triggers(reminder: Reminder, nowMillis: Long): List<Trigger> {
        if (reminder.completed) return emptyList()
        return normalizeOffsets(reminder.offsetsMinutes).mapIndexedNotNull { slot, offset ->
            val at = reminder.dueAtMillis - offset * 60_000L
            if (at > nowMillis) Trigger(slot, offset, at) else null
        }
    }
    /** The earliest alert still ahead of [nowMillis], or null when nothing will ring. */
    fun nextTrigger(reminder: Reminder, nowMillis: Long): Trigger? =
        triggers(reminder, nowMillis).minByOrNull { it.atMillis }
    fun isOverdue(reminder: Reminder, now: LocalDateTime): Boolean =
        !reminder.completed && reminder.dueAt.isBefore(now)
}