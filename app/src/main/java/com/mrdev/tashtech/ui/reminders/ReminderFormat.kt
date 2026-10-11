package com.mrdev.tashtech.ui.reminders

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.mrdev.tashtech.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** "Today · 17:00", "Tomorrow · 17:00", or "Mon, 12 Oct · 17:00". Also used by notifications. */
fun dueLabel(context: Context, due: LocalDateTime, now: LocalDateTime): String {
    val time = due.format(TimeFormat)
    return when (ChronoUnit.DAYS.between(now.toLocalDate(), due.toLocalDate())) {
        0L -> context.getString(R.string.reminder_due_today, time)
        1L -> context.getString(R.string.reminder_due_tomorrow, time)
        else -> {
            val locale = context.resources.configuration.locales[0]
            val date = due.format(DateTimeFormatter.ofPattern("EEE, d MMM", locale))
                .replaceFirstChar { it.titlecase(locale) }
            context.getString(R.string.reminder_due_date, date, time)
        }
    }
}

/** "At due time", "1 hour before", "30 minutes before", "2 days before". */
fun offsetLabel(context: Context, minutes: Int): String = with(context.resources) {
    when {
        minutes == 0 -> getString(R.string.offset_at_time)
        minutes % (24 * 60) == 0 ->
            (minutes / (24 * 60)).let { getQuantityString(R.plurals.offset_days_before, it, it) }
        minutes % 60 == 0 ->
            (minutes / 60).let { getQuantityString(R.plurals.offset_hours_before, it, it) }
        else -> getQuantityString(R.plurals.offset_minutes_before, minutes, minutes)
    }
}

@Composable
internal fun dueText(due: LocalDateTime, now: LocalDateTime): String {
    LocalConfiguration.current // recompose on locale change
    return dueLabel(LocalContext.current, due, now)
}

@Composable
internal fun offsetText(minutes: Int): String {
    LocalConfiguration.current
    return offsetLabel(LocalContext.current, minutes)
}