package com.mrdev.tashtech.data.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import com.mrdev.tashtech.domain.model.Reminder
import com.mrdev.tashtech.domain.reminder.ReminderRules
import com.mrdev.tashtech.domain.schedule.AppTime
import java.time.Clock

object ReminderIntents {
    const val ACTION_FIRE = "com.mrdev.tashtech.action.REMINDER_FIRE"
    const val ACTION_MARK_DONE = "com.mrdev.tashtech.action.REMINDER_MARK_DONE"
    const val EXTRA_REMINDER_ID = "reminder_id"
    const val TEST_ID = -1L
}

interface ReminderScheduler {
    /** Replaces any existing alarms for this reminder (none are kept when it is completed). */
    fun schedule(reminder: Reminder)
    fun cancel(reminderId: Long)
    fun scheduleTest(delayMillis: Long)
}

class AlarmReminderScheduler(
    context: Context,
    private val clock: Clock = AppTime.systemClock(),
) : ReminderScheduler {

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    override fun schedule(reminder: Reminder) {
        cancel(reminder.id)
        ReminderRules.triggers(reminder, clock.millis()).forEach { trigger ->
            setAlarm(trigger.atMillis, pendingIntent(reminder.id, trigger.slot, create = true)!!)
        }
    }

    // Cancelling every possible slot means we never need to know the previous offsets.
    override fun cancel(reminderId: Long) {
        repeat(ReminderRules.MAX_OFFSETS) { slot ->
            pendingIntent(reminderId, slot, create = false)?.let { alarmManager.cancel(it) }
        }
    }

    override fun scheduleTest(delayMillis: Long) {
        val triggerAt = clock.millis() + delayMillis
        val intent = Intent(appContext, ReminderAlarmReceiver::class.java).apply {
            action = ReminderIntents.ACTION_FIRE
            data = "tashtech://reminder/${ReminderIntents.TEST_ID}/0".toUri()
            putExtra(ReminderIntents.EXTRA_REMINDER_ID, ReminderIntents.TEST_ID)
        }
        val pi = PendingIntent.getBroadcast(
            appContext,
            ReminderIntents.TEST_ID.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        setAlarm(triggerAt, pi)
    }

    private fun setAlarm(atMillis: Long, pi: PendingIntent) {
        try {
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
            }
        } catch (e: SecurityException) {
            // Permission revoked between the check and the call: degrade to inexact.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        }
    }

    private fun canScheduleExact() =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun pendingIntent(id: Long, slot: Int, create: Boolean): PendingIntent? {
        val intent = Intent(appContext, ReminderAlarmReceiver::class.java).apply {
            action = ReminderIntents.ACTION_FIRE
            data = "tashtech://reminder/$id/$slot".toUri() // makes each (id, slot) a distinct PendingIntent
            putExtra(ReminderIntents.EXTRA_REMINDER_ID, id)
        }
        val flags = PendingIntent.FLAG_IMMUTABLE or
            if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE
        val requestCode = (id * ReminderRules.MAX_OFFSETS + slot).toInt()
        return PendingIntent.getBroadcast(appContext, requestCode, intent, flags)
    }
}