package com.mrdev.tashtech.ui.browser

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.mrdev.tashtech.R
import com.mrdev.tashtech.domain.model.MAX_NOTE_TEXT
import com.mrdev.tashtech.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickNoteSheet(
    onSave: (text: String, includeLink: Boolean) -> Unit,
    onDismiss: () -> Unit,
    @StringRes titleRes: Int = R.string.note_sheet_title,
    initialText: String = "",
    showLinkOption: Boolean = true, // only meaningful while browsing SIS/LMS
) {
    var text by rememberSaveable { mutableStateOf(initialText) }
    var includeLink by rememberSaveable { mutableStateOf(true) }
    val focus = remember { FocusRequester() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = Dimens.ScreenPadding)
                .padding(bottom = Dimens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
        ) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(MAX_NOTE_TEXT) },
                placeholder = { Text(stringResource(R.string.note_hint)) },
                minLines = 3,
                maxLines = 8,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
            )
            if (showLinkOption) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimens.MinTouchTarget)
                        .toggleable(
                            value = includeLink,
                            role = Role.Checkbox,
                            onValueChange = { includeLink = it }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = includeLink, onCheckedChange = null)
                    Spacer(Modifier.width(Dimens.SpaceSm))
                    Text(
                        stringResource(R.string.note_attach_link),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            Button(
                onClick = { onSave(text.trim(), includeLink) },
                enabled = text.isNotBlank(),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
            ) {
                Text(
                    stringResource(R.string.note_save),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}