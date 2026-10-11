package com.mrdev.tashtech.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrdev.tashtech.R
import com.mrdev.tashtech.data.config.AcademicService
import com.mrdev.tashtech.ui.AppViewModelProvider
import com.mrdev.tashtech.ui.components.GlassSurface
import com.mrdev.tashtech.ui.components.ScreenFrame
import com.mrdev.tashtech.ui.components.adaptiveScreenPadding
import com.mrdev.tashtech.ui.reminders.openExactAlarmSettings
import com.mrdev.tashtech.ui.reminders.openNotificationSettings
import com.mrdev.tashtech.ui.reminders.rememberAlertsStatus
import com.mrdev.tashtech.ui.theme.Dimens
import com.mrdev.tashtech.ui.theme.ThemeMode

private enum class AppLanguage(val tag: String, @StringRes val label: Int) {
    System("", R.string.settings_language_system),
    English("en", R.string.settings_language_english),
    Uzbek("uz", R.string.settings_language_uzbek),
}

private fun currentLanguage(): AppLanguage {
    val locales = AppCompatDelegate.getApplicationLocales()
    val tag = if (locales.isEmpty) "" else locales[0]?.language.orEmpty()
    return AppLanguage.entries.firstOrNull { it.tag == tag } ?: AppLanguage.System
}

private fun applyLanguage(language: AppLanguage) {
    AppCompatDelegate.setApplicationLocales(
        if (language.tag.isEmpty()) LocaleListCompat.getEmptyLocaleList()
        else LocaleListCompat.forLanguageTags(language.tag),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenService: (AcademicService) -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    var changeId by rememberSaveable { mutableStateOf(false) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val alerts by rememberAlertsStatus()
    val context = LocalContext.current
    val pad = adaptiveScreenPadding()
    var language by remember { mutableStateOf(currentLanguage()) }
    var showPrivacy by rememberSaveable { mutableStateOf(false) }
    var showCredits by rememberSaveable { mutableStateOf(false) }
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(
                context.packageName,
                0
            ).versionName
        }.getOrNull().orEmpty()
    }

    ScreenFrame {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                stringResource(R.string.common_back)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(
                    start = pad,
                    end = pad,
                    top = Dimens.SpaceSm,
                    bottom = Dimens.SpaceXl
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
            ) {

                item {
                    Section(R.string.settings_appearance) {
                        RowLabel(R.string.settings_theme)
                        Segmented(
                            options = ThemeMode.entries,
                            selected = state.themeMode,
                            label = {
                                stringResource(
                                    when (it) {
                                        ThemeMode.System -> R.string.settings_theme_system
                                        ThemeMode.Light -> R.string.settings_theme_light
                                        ThemeMode.Dark -> R.string.settings_theme_dark
                                    },
                                )
                            },
                            onSelect = viewModel::setTheme,
                        )
                        Spacer(Modifier.height(Dimens.SpaceMd))
                        RowLabel(R.string.settings_language)
                        Segmented(
                            options = AppLanguage.entries,
                            selected = language,
                            label = { stringResource(it.label) },
                            onSelect = {
                                language = it; applyLanguage(it)
                            }, // recreates the activity in the new language
                        )
                    }
                }
                item {
                    Section(R.string.settings_notifications) {
                        SwitchRow(
                            title = R.string.settings_reminders_toggle,
                            subtitle = R.string.settings_reminders_toggle_desc,
                            checked = state.remindersEnabled,
                            onChange = viewModel::setRemindersEnabled,
                        )
                        SettingsRow(
                            Icons.Outlined.NotificationsActive, R.string.settings_test_alert,
                            subtitle = stringResource(R.string.settings_test_alert_desc),
                            onClick = {
                                viewModel.scheduleTestAlert()
                                Toast.makeText(
                                    context,
                                    R.string.settings_test_alert_scheduled,
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                        )
                        SettingsRow(
                            Icons.Outlined.BatterySaver, R.string.settings_battery,
                            subtitle = stringResource(R.string.settings_battery_desc),
                            onClick = {
                                try {
                                    context.startActivity(Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                } catch (e: ActivityNotFoundException) { /* some devices don't have this screen */
                                }
                            },
                        )
                        if (!alerts.notificationsAllowed) {
                            SettingsRow(
                                Icons.Outlined.NotificationsOff,
                                R.string.alerts_notifications_title,
                                subtitle = stringResource(R.string.alerts_notifications_action),
                                onClick = { openNotificationSettings(context) },
                            )
                        }
                        if (!alerts.exactAlarmsAllowed) {
                            SettingsRow(
                                Icons.Outlined.Schedule, R.string.alerts_exact_title,
                                subtitle = stringResource(R.string.alerts_exact_action),
                                onClick = { openExactAlarmSettings(context) },
                            )
                        }
                    }
                }

                item {
                    Section(R.string.settings_about) {
                        SettingsRow(
                            Icons.Outlined.Info,
                            R.string.settings_version,
                            subtitle = version
                        )
                        SettingsRow(
                            Icons.Outlined.Lock,
                            R.string.settings_privacy,
                            onClick = { showPrivacy = true })
                        Spacer(Modifier.height(4.dp))
                        DeveloperCard()
                    }
                }

            }
        }
    }

    if (showPrivacy) InfoDialog(
        R.string.settings_privacy,
        R.string.settings_privacy_body
    ) { showPrivacy = false }
    if (showCredits) InfoDialog(
        R.string.settings_credits,
        R.string.settings_credits_body
    ) { showCredits = false }
}


@Composable
private fun Section(@StringRes title: Int, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(start = Dimens.SpaceXs),
        )
        GlassSurface(modifier = Modifier.fillMaxWidth(), alpha = 0.7f) {
            Column(
                modifier = Modifier.padding(
                    horizontal = Dimens.CardPadding,
                    vertical = Dimens.SpaceXs
                ), content = content
            )
        }
    }
}

@Composable
private fun RowLabel(@StringRes title: Int) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = Dimens.SpaceSm, bottom = Dimens.SpaceXs),
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    @StringRes title: Int,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.MinTouchTarget)
            .then(
                if (onClick != null) Modifier.clickable(
                    role = Role.Button,
                    onClick = onClick
                ) else Modifier
            )
            .padding(vertical = Dimens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        Icon(icon, contentDescription = null, tint = colors.secondary)
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
        if (onClick != null) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SwitchRow(
    @StringRes title: Int,
    @StringRes subtitle: Int,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.MinTouchTarget)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = Dimens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun <T> Segmented(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Dimens.SpaceXs)
    ) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                label = { Text(label(option), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}


@Composable
private fun InfoDialog(@StringRes title: Int, @StringRes body: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            Text(
                stringResource(body),
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_ok)) } },
    )
}