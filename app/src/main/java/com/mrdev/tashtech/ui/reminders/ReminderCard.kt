package com.mrdev.tashtech.ui.reminders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mrdev.tashtech.R
import com.mrdev.tashtech.ui.motion.pressScale
import com.mrdev.tashtech.ui.theme.Dimens
import java.time.LocalDateTime

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReminderCard(
    item: ReminderItem,
    now: LocalDateTime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToggle: ((Boolean) -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val reminder = item.reminder
    val done = reminder.completed
    val source = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = source,
        shape = MaterialTheme.shapes.medium,
        color = if (done) colors.background else colors.surfaceContainer,
        contentColor = if (done) colors.onSurfaceVariant else colors.onSurface,
        border = if (item.isOverdue) BorderStroke(2.dp, colors.error)
        else BorderStroke(Dimens.BorderThin, colors.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .pressScale(source),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(
                start = if (onToggle == null) Dimens.CardPadding else Dimens.SpaceXs,
                end = Dimens.CardPadding,
                top = Dimens.SpaceXs,
                bottom = Dimens.SpaceXs,
            ),
        ) {
            if (onToggle != null) {
                val cd = stringResource(
                    if (done) R.string.reminder_toggle_undo_cd else R.string.reminder_toggle_done_cd,
                    reminder.title,
                )
                Checkbox(
                    checked = done,
                    onCheckedChange = onToggle,
                    modifier = Modifier.semantics { contentDescription = cd },
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = Dimens.SpaceSm)
                    .semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
            ) {
                Text(
                    reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (reminder.note.isNotBlank()) {
                    Text(
                        reminder.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
                ) {
                    DueBadge(reminder.dueAt, now, item.isOverdue)
                    if (reminder.course.isNotBlank()) CourseTag(reminder.course)
                }
            }
        }
    }
}

/** Overdue = error colors + warning icon + the word "Overdue", never color alone. */
@Composable
private fun DueBadge(due: LocalDateTime, now: LocalDateTime, overdue: Boolean) {
    val colors = MaterialTheme.colorScheme
    val label = dueText(due, now)
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (overdue) colors.errorContainer else colors.surfaceVariant,
        contentColor = if (overdue) colors.onErrorContainer else colors.onSurfaceVariant,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
            modifier = Modifier.padding(horizontal = Dimens.SpaceSm, vertical = Dimens.SpaceXs),
        ) {
            Icon(
                if (overdue) Icons.Outlined.Warning else Icons.Outlined.Schedule,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                if (overdue) stringResource(R.string.reminder_overdue_format, label) else label,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun CourseTag(course: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            course,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = Dimens.SpaceSm, vertical = Dimens.SpaceXs),
        )
    }
}