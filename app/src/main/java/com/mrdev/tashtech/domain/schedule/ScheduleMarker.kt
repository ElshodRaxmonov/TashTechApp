package com.mrdev.tashtech.domain.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mrdev.tashtech.R
import com.mrdev.tashtech.ui.theme.Dimens

enum class ScheduleMarker { Exam, Assignment, Reminder, Announcement }
private class MarkerStyle(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    @androidx.annotation.StringRes val label: Int,
    val container: androidx.compose.ui.graphics.Color,
    val content: androidx.compose.ui.graphics.Color,
)

@Composable
internal fun MarkerBadge(marker: ScheduleMarker, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme

    val style = when (marker) {
        ScheduleMarker.Exam -> MarkerStyle(Icons.Outlined.Warning, R.string.marker_exam, c.errorContainer, c.onErrorContainer)
        ScheduleMarker.Assignment -> MarkerStyle(Icons.AutoMirrored.Outlined.Assignment, R.string.marker_assignment, c.tertiaryContainer, c.onTertiaryContainer)
        ScheduleMarker.Reminder -> MarkerStyle(Icons.Outlined.Notifications, R.string.marker_reminder, c.secondaryContainer, c.onSecondaryContainer)
        ScheduleMarker.Announcement -> MarkerStyle(Icons.Outlined.Campaign, R.string.marker_announcement, c.surfaceVariant, c.onSurfaceVariant)
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = style.container as androidx.compose.ui.graphics.Color,
        contentColor = style.content as androidx.compose.ui.graphics.Color,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
            modifier = Modifier.padding(horizontal = Dimens.SpaceSm, vertical = Dimens.SpaceXs),
        ) {
            Icon(style.icon as androidx.compose.ui.graphics.vector.ImageVector, null, Modifier.size(16.dp))
            Text(stringResource(style.label as Int), style = MaterialTheme.typography.labelMedium)
        }
    }
}