package com.mrdev.tashtech.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.mrdev.tashtech.R
import com.mrdev.tashtech.domain.model.EventKind
import com.mrdev.tashtech.domain.model.UserEvent
import com.mrdev.tashtech.ui.components.DatePickerHost
import com.mrdev.tashtech.ui.components.TimePickerHost
import com.mrdev.tashtech.ui.reminders.MAX_COURSE_LENGTH
import com.mrdev.tashtech.ui.reminders.MAX_TITLE_LENGTH
import com.mrdev.tashtech.ui.reminders.PickerField
import com.mrdev.tashtech.ui.theme.Dimens
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EventEditorSheet(
    initial: UserEvent?,
    defaultDate: LocalDate,
    onSave: (UserEvent) -> Unit,
    onDelete: ((Long) -> Unit)?,
    onDismiss: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    var title by rememberSaveable { mutableStateOf(initial?.title.orEmpty()) }
    var kind by rememberSaveable { mutableStateOf(initial?.kind ?: EventKind.Event) }
    var date by rememberSaveable { mutableStateOf(initial?.date ?: defaultDate) }
    var start by rememberSaveable { mutableStateOf(initial?.start ?: LocalTime.of(14, 0)) }
    var end by rememberSaveable { mutableStateOf(initial?.end ?: LocalTime.of(15, 0)) }
    var location by rememberSaveable { mutableStateOf(initial?.location.orEmpty()) }
    var repeat by rememberSaveable { mutableStateOf(initial?.repeatWeekly ?: false) }
    var showDate by rememberSaveable { mutableStateOf(false) }
    var pickStart by rememberSaveable { mutableStateOf(false) }
    var pickEnd by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val endValid = end.isAfter(start)
    val timeFormat = remember { DateTimeFormatter.ofPattern("HH:mm") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenPadding)
                .padding(bottom = Dimens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
        ) {
            Text(
                stringResource(if (initial == null) R.string.event_sheet_new else R.string.event_sheet_edit),
                style = MaterialTheme.typography.headlineSmall,
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(MAX_TITLE_LENGTH) },
                label = { Text(stringResource(R.string.event_title_label)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
            ) {
                EventKind.entries.forEach { k ->
                    FilterChip(
                        selected = k == kind,
                        onClick = { kind = k },
                        label = { Text(k.label()) },
                        leadingIcon = {
                            Icon(
                                k.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                    )
                }
            }
            PickerField(
                label = stringResource(R.string.editor_date_label),
                value = remember(date, locale) {
                    date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", locale))
                        .replaceFirstChar { it.titlecase(locale) }
                },
                icon = Icons.Outlined.CalendarMonth,
                onClick = { showDate = true },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
                PickerField(
                    label = stringResource(if (repeat) R.string.event_starts_on else R.string.editor_date_label),
                    start.format(timeFormat),
                    Icons.Outlined.Schedule,
                    { pickStart = true },
                    Modifier.weight(1f)
                )
                PickerField(
                    stringResource(R.string.event_end_label),
                    end.format(timeFormat),
                    Icons.Outlined.Schedule,
                    { pickEnd = true },
                    Modifier.weight(1f)
                )
            }
            if (!endValid) {
                Text(
                    stringResource(R.string.event_end_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            OutlinedTextField(
                value = location,
                onValueChange = { location = it.take(MAX_COURSE_LENGTH + 20) },
                label = { Text(stringResource(R.string.event_location_label)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.MinTouchTarget)
                    .toggleable(
                        value = repeat,
                        role = Role.Switch,
                        onValueChange = { repeat = it }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.event_repeat_weekly),
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(checked = repeat, onCheckedChange = null)
                if (initial?.repeatWeekly == true) {
                    Text(
                        stringResource(R.string.event_series_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Button(
                onClick = {
                    onSave(
                        UserEvent(
                            initial?.id ?: 0L,
                            title.trim(),
                            kind,
                            date,
                            start,
                            end,
                            location.trim(),
                            repeat
                        )
                    )
                },
                enabled = title.isNotBlank() && endValid,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
            ) {
                Text(
                    stringResource(R.string.editor_save),
                    style = MaterialTheme.typography.labelLarge
                )
            }
            if (initial != null && onDelete != null) {
                TextButton(
                    onClick = { confirmDelete = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.common_delete)) }
            }
        }
    }

    if (showDate) DatePickerHost(date, { date = it }, { showDate = false })
    if (pickStart) TimePickerHost(start, { start = it }, { pickStart = false })
    if (pickEnd) TimePickerHost(end, { end = it }, { pickEnd = false })
    if (confirmDelete && initial != null && onDelete != null) {
        AlertDialog(

            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.event_delete_title)) },
            text = { Text(stringResource(if (initial.repeatWeekly) R.string.event_delete_series_body else R.string.editor_delete_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = { confirmDelete = false; onDelete(initial.id) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(
                        stringResource(
                            R.string.common_cancel
                        )
                    )
                }
            },
        )
    }
}