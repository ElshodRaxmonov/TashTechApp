package com.mrdev.tashtech.ui.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrdev.tashtech.R
import com.mrdev.tashtech.data.config.OfficialDomain
import com.mrdev.tashtech.domain.model.Note
import com.mrdev.tashtech.ui.AppViewModelProvider
import com.mrdev.tashtech.ui.browser.QuickNoteSheet
import com.mrdev.tashtech.ui.components.LoadingScreen
import com.mrdev.tashtech.ui.components.LottieEmptyState
import com.mrdev.tashtech.ui.theme.Dimens
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    onBack: () -> Unit,
    onOpenLink: (String) -> Unit,
    viewModel: NotesViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]
    var pendingDelete by remember { mutableStateOf<Note?>(null) }
    var editing by remember { mutableStateOf<Note?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notes_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            stringResource(R.string.common_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.notes_new)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        when (val list = notes) {
            null -> LoadingScreen()
            else -> if (list.isEmpty()) {
                LottieEmptyState(
                    asset = "no_notes.json",
                    title = stringResource(R.string.notes_empty_title),
                    body = stringResource(R.string.notes_empty_body),
                    fallbackIcon = Icons.Outlined.EditNote,
                    modifier = Modifier
                        .padding(padding)
                        .padding(Dimens.ScreenPadding),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(
                        start = Dimens.ScreenPadding, end = Dimens.ScreenPadding,
                        top = Dimens.ScreenPadding, bottom = 96.dp, // clears the FAB
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
                ) {
                    items(list, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            locale = locale,
                            onEdit = { editing = note },
                            onDelete = { pendingDelete = note },
                            onOpenLink = onOpenLink,
                        )
                    }
                }
            }
        }
    }

    if (creating) {
        QuickNoteSheet(
            titleRes = R.string.note_new_title,
            showLinkOption = false,
            onSave = { text, _ -> viewModel.save(null, text); creating = false },
            onDismiss = { creating = false },
        )
    }
    editing?.let { note ->
        QuickNoteSheet(
            titleRes = R.string.note_edit_title,
            initialText = note.text,
            showLinkOption = false,
            onSave = { text, _ -> viewModel.save(note.id, text); editing = null },
            onDismiss = { editing = null },
        )
    }
    pendingDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.notes_delete_title)) },
            text = { Text(stringResource(R.string.editor_delete_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.delete(note.id); pendingDelete = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingDelete = null
                }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

@Composable
private fun NoteCard(
    note: Note,
    locale: Locale,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenLink: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val date = remember(note.createdAt, locale) {
        note.createdAt.format(DateTimeFormatter.ofPattern("d MMM, HH:mm", locale))
    }
    val editLabel = stringResource(R.string.notes_edit_label)
    Surface(
        onClick = onEdit,
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceContainer,
        border = BorderStroke(Dimens.BorderThin, colors.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { onClick(label = editLabel) { onEdit(); true } },
    ) {
        Column(
            modifier = Modifier.padding(
                start = Dimens.CardPadding,
                top = Dimens.CardPadding,
                end = Dimens.SpaceXs,
                bottom = Dimens.SpaceXs
            ),
        ) {
            // Links inside the text open on tap; tapping anywhere else on the card edits the note.
            Text(
                text = linkifiedText(note.text, colors.primary, onOpenLink),
                style = MaterialTheme.typography.bodyLarge,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        date,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant
                    )
                    if (note.sourceUrl.isNotBlank()) {
                        TextButton(
                            onClick = { onOpenLink(note.sourceUrl) },
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Link,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(Dimens.SpaceXs))
                            Text(
                                OfficialDomain.hostOf(note.sourceUrl).orEmpty(),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                            )
                        }
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        stringResource(R.string.notes_delete_cd),
                        tint = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}