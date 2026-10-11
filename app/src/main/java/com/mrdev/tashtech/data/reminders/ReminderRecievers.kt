package com.mrdev.tashtech.data.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.mrdev.tashtech.R
import com.mrdev.tashtech.TashTechApp
import com.mrdev.tashtech.domain.model.Reminder
import com.mrdev.tashtech.domain.schedule.AppTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime

private const val TAG = "ReminderReceivers"

/** Runs [block] off the main thread and keeps the broadcast alive until it finishes. */
private fun BroadcastReceiver.async(block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try {
            block()
        } catch (e: Exception) {
            Log.w(TAG, "Reminder work failed", e)
        } finally {
            pending.finish()
        }
    }
}

/** Fires a reminder notification, or handles the notification's "Mark done" action. */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val id = intent.getLongExtra(ReminderIntents.EXTRA_REMINDER_ID, Long.MIN_VALUE)
        if (id == Long.MIN_VALUE) return
        val app = context.applicationContext as TashTechApp
        async {
            when (action) {
                ReminderIntents.ACTION_FIRE -> {
                    if (id == ReminderIntents.TEST_ID) {
                        val now = LocalDateTime.now(AppTime.systemClock())
                        ReminderNotifications.show(
                            app,
                            Reminder(
                                id = id,
                                title = app.getString(R.string.test_alert_title),
                                note = app.getString(R.string.test_alert_body),
                                dueAt = now,
                            ),
                            now,
                        )
                        return@async
                    }
                    val reminder = app.container.reminders.get(id) ?: return@async
                    if (reminder.completed) return@async
                    if (!app.container.userPreferences.remindersEnabled.first()) return@async
                    ReminderNotifications.show(app, reminder, LocalDateTime.now(AppTime.systemClock()))
                }
                ReminderIntents.ACTION_MARK_DONE -> {
                    app.container.reminders.setCompleted(id, true)
                    NotificationManagerCompat.from(app).cancel(id.toInt())
                }
            }
        }
    }
}

/** Boot, app update, clock change or exact-alarm permission change: re-arm every alarm. */
class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == null) return
        val app = context.applicationContext as TashTechApp
        async { app.container.reminders.rescheduleAll() }
    }
}