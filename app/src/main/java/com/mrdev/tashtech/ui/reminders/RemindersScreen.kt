package com.mrdev.tashtech.ui.reminders

import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrdev.tashtech.R
import com.mrdev.tashtech.ui.AppViewModelProvider
import com.mrdev.tashtech.ui.components.ErrorScreen
import com.mrdev.tashtech.ui.components.LoadingScreen
import com.mrdev.tashtech.ui.components.LottieEmptyState
import com.mrdev.tashtech.ui.home.EmptyDayCard
import com.mrdev.tashtech.ui.navigation.LocalBottomBarPadding
import com.mrdev.tashtech.ui.theme.Dimens

@Composable
fun RemindersScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: RemindersViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val promptShown by viewModel.notificationPromptShown.collectAsStateWithLifecycle()
    val alerts by rememberAlertsStatus()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val requestNotifications = rememberNotificationPermissionRequest { /* status refreshes on resume */ }

    fun turnOnNotifications() {
        val canAskAgain = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            (!promptShown || (activity != null &&
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, android.Manifest.permission.POST_NOTIFICATIONS,
                )))
        if (canAskAgain) {
            viewModel.markNotificationPromptShown()
            requestNotifications()
        } else {
            openNotificationSettings(context)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val s = state) {
            RemindersUiState.Loading -> LoadingScreen()
            RemindersUiState.Error -> ErrorScreen(R.string.reminders_error, viewModel::retry)
            is RemindersUiState.Content -> RemindersContent(
                state = s,
                alerts = alerts,
                onAdd = onAdd,
                onEdit = onEdit,
                onToggle = viewModel::toggleCompleted,
                onClearCompleted = viewModel::clearCompleted,
                onTurnOnNotifications = ::turnOnNotifications,
                onAllowExactAlarms = { openExactAlarmSettings(context) },
                onBack = onBack
            )
        }
    }
}

@Composable
private fun RemindersContent(
    onBack: () -> Unit,
    state: RemindersUiState.Content,
    alerts: AlertsStatus,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onToggle: (Long, Boolean) -> Unit,
    onClearCompleted: () -> Unit,
    onTurnOnNotifications: () -> Unit,
    onAllowExactAlarms: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val hasActive = state.active.isNotEmpty()
    val isEmpty = !hasActive && state.completed.isEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
            contentPadding = PaddingValues(
                start = Dimens.ScreenPadding, end = Dimens.ScreenPadding,
                top = Dimens.SpaceLg, bottom = 96.dp, // clears the FAB
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
        ) {
            item(key = "header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.common_back))
                    }
                    Column {
                        Text(stringResource(R.string.reminders_title), style = MaterialTheme.typography.headlineMedium, color = colors.onBackground)
                        Text(stringResource(R.string.reminders_subtitle), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                }
            }
            if (hasActive && !alerts.notificationsAllowed) {
                item(key = "banner_notifications") {
                    AlertBanner(
                        R.string.alerts_notifications_title, R.string.alerts_notifications_body,
                        R.string.alerts_notifications_action, onTurnOnNotifications,
                    )
                }
            }
            if (hasActive && !alerts.exactAlarmsAllowed) {
                item(key = "banner_exact") {
                    AlertBanner(
                        R.string.alerts_exact_title, R.string.alerts_exact_body,
                        R.string.alerts_exact_action, onAllowExactAlarms,
                    )
                }
            }

            if (isEmpty) {
                item(key = "empty") { RemindersEmpty(onAdd) }
            } else {
                item(key = "upcoming_title") { SectionLabel(R.string.reminders_section_upcoming) }
                if (!hasActive) {
                    item(key = "all_done") {
                        EmptyDayCard(
                            stringResource(R.string.reminders_all_done),
                            stringResource(R.string.reminders_all_done_body),
                        )
                    }
                } else {
                    items(state.active, key = { "a-${it.reminder.id}" }) { item ->
                        ReminderCard(
                            item = item, now = state.now,
                            onClick = { onEdit(item.reminder.id) },
                            onToggle = { onToggle(item.reminder.id, it) },
                        )
                    }
                }
                if (state.completed.isNotEmpty()) {
                    item(key = "completed_title") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) { SectionLabel(R.string.reminders_section_completed) }
                            TextButton(onClick = onClearCompleted) {
                                Text(stringResource(R.string.reminders_clear_completed))
                            }
                        }
                    }
                    items(state.completed, key = { "c-${it.reminder.id}" }) { item ->
                        ReminderCard(
                            item = item, now = state.now,
                            onClick = { onEdit(item.reminder.id) },
                            onToggle = { onToggle(item.reminder.id, it) },
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onAdd,
            icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.reminders_add)) },
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = LocalBottomBarPadding.current)
                .padding(Dimens.ScreenPadding),
        )
    }
}

@Composable
private fun SectionLabel(@StringRes res: Int) {
    Text(
        stringResource(res),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun AlertBanner(
    @StringRes title: Int,
    @StringRes body: Int,
    @StringRes action: Int,
    onAction: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = colors.tertiaryContainer,
        contentColor = colors.onTertiaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                Icon(Icons.Outlined.NotificationsOff, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            }
            Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
            TextButton(
                onClick = onAction,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.onTertiaryContainer),
            ) { Text(stringResource(action)) }
        }
    }
}
@Composable
private fun RemindersEmpty(onAdd: () -> Unit) {
    LottieEmptyState(
        asset = "no_reminders.json",
        title = stringResource(R.string.reminders_empty_title),
        body = stringResource(R.string.reminders_empty_body),
        fallbackIcon = Icons.Outlined.NotificationsNone,
        action = { Button(onClick = onAdd, shape = MaterialTheme.shapes.medium) { Text(stringResource(R.string.reminders_add)) } },
    )
}