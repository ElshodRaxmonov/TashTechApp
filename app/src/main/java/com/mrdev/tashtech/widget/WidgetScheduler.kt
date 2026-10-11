package com.mrdev.tashtech.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.mrdev.tashtech.domain.schedule.AppTime
import java.time.LocalDateTime

internal object WidgetScheduler {
    private const val REQUEST_HUB = 7101

    fun scheduleHub(context: Context, at: LocalDateTime?) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val ids = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, HubWidgetReceiver::class.java))
        val pending = pendingIntent(context, ids)
        alarms.cancel(pending)
        if (at == null || ids.isEmpty()) return
        // RTC (not RTC_WAKEUP): refresh when the phone is next awake; saves battery, needs no permission.
        alarms.setAndAllowWhileIdle(AlarmManager.RTC, at.atZone(AppTime.ZONE).toInstant().toEpochMilli(), pending)
    }

    fun cancelHub(context: Context) = scheduleHub(context, null)

    private fun pendingIntent(context: Context, ids: IntArray): PendingIntent {
        val intent = Intent(context, HubWidgetReceiver::class.java)
            .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        return PendingIntent.getBroadcast(context, REQUEST_HUB, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}