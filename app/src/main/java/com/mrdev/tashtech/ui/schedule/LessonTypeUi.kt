package com.mrdev.tashtech.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mrdev.tashtech.R
import com.mrdev.tashtech.domain.model.LessonType
import com.mrdev.tashtech.ui.theme.Dimens

internal val LessonType.icon: ImageVector
    get() = when (this) {
        LessonType.Lecture -> Icons.Outlined.Slideshow
        LessonType.Practical -> Icons.Outlined.Build
    }

@Composable
internal fun LessonType.label(): String = stringResource(
    when (this) {
        LessonType.Lecture -> R.string.lesson_type_lecture
        LessonType.Practical -> R.string.lesson_type_practical
    },
)

@Composable
internal fun LessonType.accent(): androidx.compose.ui.graphics.Color = when (this){
    LessonType.Lecture -> MaterialTheme.colorScheme.secondary
    LessonType.Practical -> MaterialTheme.colorScheme.tertiary
}

/** [onTinted]: pass the surrounding content color when the badge sits on a colored card. */
@Composable
internal fun LessonTypeBadge(type: LessonType, modifier: Modifier = Modifier, onTinted: androidx.compose.ui.graphics.Color? = null) {
    val colors = MaterialTheme.colorScheme
    val (bg, fg) = when {
        onTinted != null -> onTinted.copy(alpha = 0.16f) to onTinted
        type == LessonType.Lecture -> colors.secondaryContainer to colors.onSecondaryContainer
        else -> colors.tertiaryContainer to colors.onTertiaryContainer
    }
    Surface(shape = MaterialTheme.shapes.small, color = bg, contentColor = fg, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
            modifier = Modifier.padding(horizontal = Dimens.SpaceSm, vertical = 2.dp),
        ) {
            Icon(type.icon, contentDescription = null, modifier = Modifier.size(12.dp))
            Text(type.label(), style = MaterialTheme.typography.labelSmall)
        }
    }
}