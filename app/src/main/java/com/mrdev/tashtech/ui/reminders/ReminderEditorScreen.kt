package com.mrdev.tashtech.ui.reminders

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrdev.tashtech.R
import com.mrdev.tashtech.domain.model.Reminder
import com.mrdev.tashtech.domain.reminder.OffsetUnit
import com.mrdev.tashtech.domain.reminder.ReminderRules
import com.mrdev.tashtech.domain.schedule.AppTime
import com.mrdev.tashtech.ui.AppViewModelProvider
import com.mrdev.tashtech.ui.theme.Dimens
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderEditorScreen(
    onClose: () -> Unit,
    viewModel: ReminderEditorViewModel = viewModel(factory = AppViewModelProvider.Factory),

) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val alerts by rememberAlertsStatus()
    val requestPermission = rememberNotificationPermissionRequest { onClose() }
    val locale = LocalConfiguration.current.locales[0]

    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    var showCustom by rememberSaveable { mutableStateOf(false) }
    var showDelete by rememberSaveable { mutableStateOf(false) }

    // After a successful save: ask for notification permission once, in context, else just close.
    LaunchedEffect(state.saveResult) {
        val result = state.saveResult ?: return@LaunchedEffect
        val mustAsk = result.needsNotificationPrompt &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !alerts.notificationsAllowed
        if (mustAsk) {
            viewModel.markNotificationPromptShown()
            requestPermission()
        } else {
            onClose()
        }
    }
    LaunchedEffect(state.deleted, state.notFound) {
        if (state.deleted || state.notFound) onClose()
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isNew) R.string.editor_title_new else R.string.editor_title_edit,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { showDelete = true }) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.editor_delete),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceMd),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
                ) {
                    if (state.saveFailed) {
                        Text(
                            stringResource(R.string.editor_save_error),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Button(
                        onClick = viewModel::save,
                        enabled = state.canSave,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    ) {
                        Text(
                            stringResource(R.string.editor_save),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceSm),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text(stringResource(R.string.editor_title_label)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text(stringResource(R.string.editor_note_label)) },
                placeholder = { Text(stringResource(R.string.editor_note_placeholder)) },
                minLines = 2,
                maxLines = 4,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.course,
                onValueChange = viewModel::onCourseChange,
                label = { Text(stringResource(R.string.editor_course_label)) },
                placeholder = { Text(stringResource(R.string.editor_course_placeholder)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
                PickerField(
                    label = stringResource(R.string.editor_date_label),
                    value = remember(state.date, locale) {
                        state.date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", locale))
                            .replaceFirstChar { it.titlecase(locale) }
                    },
                    icon = Icons.Outlined.CalendarMonth,
                    onClick = { showDate = true },
                    modifier = Modifier.weight(1.4f),
                )
                PickerField(
                    label = stringResource(R.string.editor_time_label),
                    value = state.time.format(DateTimeFormatter.ofPattern("HH:mm")),
                    icon = Icons.Outlined.Schedule,
                    onClick = { showTime = true },
                    modifier = Modifier.weight(1f),
                )
            }
            if (state.pastError) {
                Text(
                    stringResource(R.string.editor_past_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                Text(stringResource(R.string.editor_remind_title), style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
                ) {
                    val custom = state.offsets.filter { it !in ReminderRules.PRESET_OFFSETS }
                    (ReminderRules.PRESET_OFFSETS + custom).forEach { minutes ->
                        val selected = minutes in state.offsets
                        val label = offsetText(minutes)
                        val isCustom = minutes !in ReminderRules.PRESET_OFFSETS
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.toggleOffset(minutes) },
                            label = { Text(label) },
                            leadingIcon = if (selected && !isCustom) {
                                { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null,
                            trailingIcon = if (isCustom) {
                                {
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = stringResource(R.string.editor_remove_offset_cd, label),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            } else null,
                        )
                    }
                    if (state.canAddOffset) {
                        AssistChip(
                            onClick = { showCustom = true },
                            label = { Text(stringResource(R.string.editor_custom)) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                        )
                    }
                }
                if (state.offsets.isEmpty()) {
                    Text(
                        stringResource(R.string.editor_remind_none),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val nextAlert = remember(state.date, state.time, state.offsets) {
                    ReminderRules.nextTrigger(
                        Reminder(
                            title = "",
                            dueAt = LocalDateTime.of(state.date, state.time),
                            offsetsMinutes = state.offsets
                        ),
                        System.currentTimeMillis(),
                    )
                }
                if (state.offsets.isEmpty()) {
                    Text(stringResource(R.string.editor_remind_none), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (nextAlert == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                        Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.editor_alert_none_ahead), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    val at = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(nextAlert.atMillis), AppTime.ZONE)
                    Text(
                        stringResource(R.string.editor_next_alert, dueText(at, LocalDateTime.now(AppTime.systemClock()))),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !alerts.notificationsAllowed) {
                        Text(stringResource(R.string.editor_permission_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (!alerts.exactAlarmsAllowed && state.offsets.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.editor_exact_hint), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = { openExactAlarmSettings(context) }) { Text(stringResource(R.string.alerts_exact_action)) }
                    }
                }
            }
        }
    }

    if (showDate) {
        val todayUtc = remember { LocalDate.now(AppTime.ZONE).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                // New reminders can't be set in the past; existing ones may keep their date.
                override fun isSelectableDate(utcTimeMillis: Long) = !state.isNew || utcTimeMillis >= todayUtc
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        viewModel.onDateChange(java.time.Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDate = false
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDate = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        ) { DatePicker(state = pickerState) }
    }

    if (showTime) {
        val timeState = rememberTimePickerState(state.time.hour, state.time.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onTimeChange(LocalTime.of(timeState.hour, timeState.minute))
                    showTime = false
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTime = false }) { Text(stringResource(R.string.common_cancel)) }
            },
            text = { TimePicker(state = timeState) },
        )
    }

    if (showCustom) {
        CustomOffsetDialog(
            onAdd = { viewModel.toggleOffset(it); showCustom = false },
            onDismiss = { showCustom = false },
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.editor_delete_confirm_title)) },
            text = { Text(stringResource(R.string.editor_delete_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = { showDelete = false; viewModel.delete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

/** Read-only field styled like the text fields; the whole surface is the tap target. */
@Composable
internal fun PickerField(
    label: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = Color.Transparent,
        border = BorderStroke(Dimens.BorderThin, colors.outline),
        modifier = modifier.heightIn(min = 56.dp).semantics(mergeDescendants = true) { role = Role.Button },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            modifier = Modifier.padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceSm),
        ) {
            Icon(icon, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
            }
        }
    }
}

@Composable
private fun CustomOffsetDialog(onAdd: (Int) -> Unit, onDismiss: () -> Unit) {
    var amount by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf(OffsetUnit.Hours) }
    val minutes = amount.toIntOrNull()?.let { ReminderRules.customOffsetMinutes(it, unit) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter(Char::isDigit).take(4) },
                    label = { Text(stringResource(R.string.custom_amount)) },
                    supportingText = { Text(stringResource(R.string.custom_support)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                    OffsetUnit.entries.forEach { u ->
                        FilterChip(
                            selected = u == unit,
                            onClick = { unit = u },
                            label = {
                                Text(
                                    stringResource(
                                        when (u) {
                                            OffsetUnit.Minutes -> R.string.unit_minutes
                                            OffsetUnit.Hours -> R.string.unit_hours
                                            OffsetUnit.Days -> R.string.unit_days
                                        },
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { minutes?.let(onAdd) }, enabled = minutes != null) {
                Text(stringResource(R.string.custom_add))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}