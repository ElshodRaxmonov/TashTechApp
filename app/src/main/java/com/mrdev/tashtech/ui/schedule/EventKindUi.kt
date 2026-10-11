package com.mrdev.tashtech.ui.schedule

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.School
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.mrdev.tashtech.R
import com.mrdev.tashtech.domain.model.EventKind

@Composable
internal fun EventKind.label(): String = stringResource(
    when (this) {
        EventKind.Lesson -> R.string.event_kind_lesson
        EventKind.Event -> R.string.event_kind_event
        EventKind.Study -> R.string.event_kind_study
        EventKind.Other -> R.string.event_kind_other
    },
)

internal val EventKind.icon: ImageVector
    get() = when (this) {
        EventKind.Lesson -> Icons.Outlined.School
        EventKind.Event -> Icons.Outlined.Event
        EventKind.Study -> Icons.AutoMirrored.Outlined.MenuBook
        EventKind.Other -> Icons.Outlined.Bookmark
    }