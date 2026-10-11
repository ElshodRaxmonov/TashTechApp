package com.mrdev.tashtech.data.reminders

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.mrdev.tashtech.MainActivity
import com.mrdev.tashtech.R
import com.mrdev.tashtech.domain.model.Reminder
import com.mrdev.tashtech.ui.reminders.dueLabel
import com.mrdev.tashtech.ui.theme.BrandPalette
import java.time.LocalDateTime

object ReminderNotifications {
    const val CHANNEL_ID = "reminders"

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_reminders_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = context.getString(R.string.channel_reminders_desc) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** The receiver checks the user's reminder preference first; this checks system permission. */
    @SuppressLint("MissingPermission") // guarded by areNotificationsEnabled()
    fun show(context: Context, reminder: Reminder, now: LocalDateTime) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val notificationId = reminder.id.toInt()
        val open = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val done = PendingIntent.getBroadcast(
            context,
            notificationId,
            Intent(context, ReminderAlarmReceiver::class.java).apply {
                action = ReminderIntents.ACTION_MARK_DONE
                data = "tashtech://reminder/${reminder.id}/done".toUri()
                putExtra(ReminderIntents.EXTRA_REMINDER_ID, reminder.id)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val details = listOf(dueLabel(context, reminder.dueAt, now), reminder.course)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        val longText = listOf(details, reminder.note).filter { it.isNotBlank() }.joinToString("\n")

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(BrandPalette.Brand40.toArgb())
            .setContentTitle(reminder.title)
            .setContentText(details)
            .setStyle(NotificationCompat.BigTextStyle().bigText(longText))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, context.getString(R.string.reminder_action_done), done)
            .build()
        manager.notify(notificationId, notification)
    }
}