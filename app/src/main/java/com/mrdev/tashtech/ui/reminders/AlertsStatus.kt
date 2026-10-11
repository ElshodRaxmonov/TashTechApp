package com.mrdev.tashtech.ui.reminders

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

data class AlertsStatus(val notificationsAllowed: Boolean, val exactAlarmsAllowed: Boolean)

fun readAlertsStatus(context: Context): AlertsStatus {
    val alarms = context.getSystemService(AlarmManager::class.java)
    return AlertsStatus(
        notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled(),
        exactAlarmsAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms(),
    )
}

/** Re-read on every resume, so returning from system settings updates the banners. */
@Composable
fun rememberAlertsStatus(): State<AlertsStatus> {
    val context = LocalContext.current
    val status = remember { mutableStateOf(readAlertsStatus(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { status.value = readAlertsStatus(context) }
    return status
}

fun openNotificationSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
    )
}

fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri()),
        )
    }
}

/** Returns a function that shows the system dialog (API 33+) or reports `true` immediately. */
@Composable
fun rememberNotificationPermissionRequest(onResult: (granted: Boolean) -> Unit): () -> Unit {
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        currentOnResult(it)
    }
    return remember(launcher) {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                currentOnResult(true)
            }
        }
    }
}