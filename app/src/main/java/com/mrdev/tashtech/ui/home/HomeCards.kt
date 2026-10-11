package com.mrdev.tashtech.ui.home

import androidx.annotation.StringRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mrdev.tashtech.R
import com.mrdev.tashtech.domain.model.ClassSession
import com.mrdev.tashtech.domain.model.SessionOrigin
import com.mrdev.tashtech.domain.schedule.SessionStatus
import com.mrdev.tashtech.domain.schedule.TimelineEntry
import com.mrdev.tashtech.ui.components.GlassSurface
import com.mrdev.tashtech.ui.reminders.ReminderItem
import com.mrdev.tashtech.ui.reminders.dueText
import com.mrdev.tashtech.ui.schedule.LessonTypeBadge
import com.mrdev.tashtech.ui.schedule.accent
import com.mrdev.tashtech.ui.schedule.label
import com.mrdev.tashtech.ui.schedule.semesterEndText
import com.mrdev.tashtech.ui.theme.Dimens
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private fun LocalTime.fmt(): String = format(TimeFormat)

@Composable
internal fun formatDuration(totalMinutes: Int): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return when {
        h == 0 -> stringResource(R.string.duration_minutes, m)
        m == 0 -> stringResource(R.string.duration_hours, h)
        else -> stringResource(R.string.duration_hours_minutes, h, m)
    }
}

private fun Duration.totalMinutes(): Int = toMinutes().toInt()

// ---------------------------------------------------------------- hero
@Composable
internal fun ServiceMiniTile(
    service: ServiceIcon,
    @StringRes title: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val (icon, tile, onTile) = when (service) {
        ServiceIcon.Sis -> Triple(
            Icons.Outlined.School,
            colors.tertiaryContainer,
            colors.onTertiaryContainer
        )

        ServiceIcon.Lms -> Triple(
            Icons.AutoMirrored.Outlined.MenuBook,
            colors.secondaryContainer,
            colors.onSecondaryContainer
        )
    }
    GlassSurface(
        onClick = onClick,
        modifier = modifier.semantics(mergeDescendants = true) { role = Role.Button },
        shape = RoundedCornerShape(20.dp),
        tint = tile,
        contentColor = onTile,
        alpha = 0.8f,
        shadow = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
internal fun HeroCard(hero: HeroState, group: String, semesterEnded: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    when (hero) {
        is HeroState.Current -> ClassHero(
            session = hero.session, icon = Icons.Outlined.PlayCircle,
            label = stringResource(R.string.hero_label_current),
            countdown = stringResource(R.string.hero_left, formatDuration(hero.minutesLeft)),
            progress = hero.progress, group = group, live = true,
            container = colors.primary, content = colors.onPrimary, onClick = onClick,
        )

        is HeroState.Next -> {
            val time = hero.session.start.fmt()
            val countdown = when {
                hero.minutesUntil != null -> stringResource(
                    R.string.hero_starts_in,
                    formatDuration(hero.minutesUntil)
                )

                hero.dayOffset == 1 -> stringResource(R.string.hero_tomorrow_at, time)
                else -> {
                    val locale = LocalConfiguration.current.locales[0]
                    val day = hero.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
                        .replaceFirstChar { it.titlecase(locale) }
                    stringResource(R.string.hero_day_at, day, time)
                }
            }
            ClassHero(
                session = hero.session,
                icon = Icons.Outlined.Schedule,
                label = stringResource(R.string.hero_label_next),
                countdown = countdown,
                progress = null,
                group = group,
                live = false,
                container = colors.primaryContainer,
                content = colors.onPrimaryContainer,
                onClick = onClick,
            )
        }

        HeroState.None -> GlassSurface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            alpha = 0.8f,
        ) {
            Column(
                modifier = Modifier.padding(Dimens.CardPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.weight(1f))
                    HeroGroupChip(group, colors.onSurface)
                }
                if (semesterEnded) {
                    Text(
                        stringResource(R.string.hero_semester_ended_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        stringResource(R.string.hero_semester_ended_body, semesterEndText()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                } else {
                    Text(
                        stringResource(R.string.hero_empty_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        stringResource(R.string.hero_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ClassHero(
    session: ClassSession, icon: ImageVector, label: String, countdown: String,
    progress: Float?, group: String, live: Boolean,
    container: Color, content: Color, onClick: () -> Unit,
) {
    GlassSurface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tint = container,
        contentColor = content,
        alpha = 0.88f,
        shadow = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)
            ) {
                if (live) LiveDot(content) else Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                HeroGroupChip(group, content) // the group now lives inside the card
            }
            Text(
                session.subject,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            session.lessonType?.let { LessonTypeBadge(it, onTinted = content) }
            Text(countdown, style = MaterialTheme.typography.titleSmall)
            InfoLine(
                Icons.Outlined.Schedule,
                "${session.start.fmt()} – ${session.end.fmt()} · ${formatDuration(session.duration.totalMinutes())}"
            )
            if (session.room.isNotBlank()) InfoLine(Icons.Outlined.MeetingRoom, session.room)
            if (session.teacher.isNotBlank()) InfoLine(Icons.Outlined.Person, session.teacher)
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Dimens.SpaceXs),
                    color = content,
                    trackColor = content.copy(alpha = 0.25f),
                )
            }
        }
    }
}

@Composable
private fun HeroGroupChip(group: String, content: Color) {
    val cd = stringResource(R.string.home_group_cd, group)
    Surface(
        shape = MaterialTheme.shapes.small,
        color = content.copy(alpha = 0.16f),
        contentColor = content,
        modifier = Modifier.semantics { contentDescription = cd },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
            modifier = Modifier.padding(horizontal = Dimens.SpaceSm, vertical = Dimens.SpaceXs),
        ) {
            Icon(Icons.Outlined.Groups, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(group, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

/** A gently pulsing "live" dot: only for a class that's in progress. */
@Composable
private fun LiveDot(color: Color) {
    val alpha by rememberInfiniteTransition(label = "live").animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "live_alpha",
    )
    Box(
        Modifier
            .size(10.dp)
            .graphicsLayer { this.alpha = alpha }
            .background(color, CircleShape))
}

@Composable
private fun InfoLine(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

// ------------------------------------------------------------ timeline

@Composable
internal fun EmptyDayCard(title: String, body: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceContainer,
        border = BorderStroke(Dimens.BorderThin, colors.outlineVariant),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
            modifier = Modifier.padding(Dimens.CardPadding),
        ) {
            Icon(Icons.Outlined.EventAvailable, contentDescription = null, tint = colors.secondary)
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun TimelineRow(entry: TimelineEntry, onSessionClick: ((ClassSession) -> Unit)? = null) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 48.dp)
                .padding(top = Dimens.SpaceMd)
        ) {
            Text(entry.start.fmt(), style = MaterialTheme.typography.titleSmall)
            Text(
                entry.end.fmt(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        when (entry) {
            is TimelineEntry.Class -> ClassCard(
                entry = entry,
                onClick = onSessionClick?.takeIf { entry.session.origin == SessionOrigin.Custom }
                    ?.let { cb -> { cb(entry.session) } },
                modifier = Modifier.weight(1f),
            )

            is TimelineEntry.FreePeriod -> FreePeriodCard(entry, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ClassCard(
    entry: TimelineEntry.Class,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {

    val colors = MaterialTheme.colorScheme
    val session = entry.session
    val custom = session.origin == SessionOrigin.Custom
    val container: Color
    val onContainer: Color
    val border: BorderStroke
    val icon: ImageVector
    val statusRes: Int
    when (entry.status) {
        SessionStatus.Current -> {
            container = colors.primaryContainer; onContainer = colors.onPrimaryContainer
            border = BorderStroke(2.dp, colors.primary)
            icon = Icons.Outlined.PlayCircle; statusRes = R.string.status_current
        }

        SessionStatus.Upcoming -> {
            container = if (custom) colors.tertiaryContainer else colors.surfaceContainer
            onContainer = if (custom) colors.onTertiaryContainer else colors.onSurface
            border = BorderStroke(
                Dimens.BorderThin,
                if (custom) colors.tertiary else colors.outlineVariant
            )
            icon = Icons.Outlined.Schedule; statusRes = R.string.status_upcoming
        }

        SessionStatus.Completed -> {
            container = colors.background; onContainer = colors.onSurfaceVariant
            border = BorderStroke(
                Dimens.BorderThin,
                if (custom) colors.tertiary.copy(alpha = 0.5f) else colors.outlineVariant
            )
            icon = Icons.Outlined.CheckCircle; statusRes = R.string.status_completed
        }
    }
    val editLabel = stringResource(R.string.schedule_edit_event)
    val stripe = session.lessonType?.takeIf { !custom }?.accent()
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = onContainer,
        border = border,
        modifier = modifier,
    ) {
        // The clickable sits inside the surface so the ripple is clipped to the rounded shape.
        Column(
            modifier = (if (onClick != null) Modifier.clickable(
                onClickLabel = editLabel,
                role = Role.Button,
                onClick = onClick
            ) else Modifier)
                .fillMaxWidth()
                .then(if (stripe != null) Modifier.drawBehind {
                    drawRect(
                        stripe,
                        size = Size(4.dp.toPx(), size.height)
                    )
                } else Modifier)
                .padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
        ) {
            Row(

                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)
            ) {
                if (onClick != null) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    ) // decorative: the card already announces "Edit your event"
                }
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(
                    stringResource(statusRes),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                session.kind?.takeIf { custom }?.let { kind ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = colors.tertiary,
                        contentColor = colors.onTertiary
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
                            modifier = Modifier.padding(
                                horizontal = Dimens.SpaceSm,
                                vertical = 2.dp
                            ),
                        ) {
                            Icon(
                                Icons.Outlined.Person,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(kind.label(), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                session.lessonType?.takeIf { !custom }?.let { LessonTypeBadge(it) }

            }
            Text(
                session.subject,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            val details =
                listOf(session.room, session.teacher).filter { it.isNotBlank() }.joinToString(" · ")
            if (details.isNotEmpty()) Text(details, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun FreePeriodCard(entry: TimelineEntry.FreePeriod, modifier: Modifier = Modifier) {

    val colors = MaterialTheme.colorScheme
    val active = entry.status == SessionStatus.Current
    val minutes = Duration.between(entry.start, entry.end).totalMinutes()
    val dashColor = if (active) colors.secondary else colors.outlineVariant
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (active) colors.secondaryContainer else Color.Transparent,
        contentColor = if (active) colors.onSecondaryContainer else colors.onSurfaceVariant,
        modifier = modifier.drawBehind {
            val w = 1.5.dp.toPx()
            drawRoundRect(
                color = dashColor, // colors.secondary when active, else colors.outlineVariant
                topLeft = Offset(w / 2, w / 2),
                size = Size(size.width - w, size.height - w),
                cornerRadius = CornerRadius(12.dp.toPx()),
                style = Stroke(
                    width = w,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                ),
            )
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            modifier = Modifier.padding(horizontal = Dimens.CardPadding, vertical = Dimens.SpaceMd),
        ) {
            Icon(
                Icons.Outlined.HourglassEmpty,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                stringResource(R.string.home_free_period, formatDuration(minutes)),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

// ------------------------------------------------------------ services

internal enum class ServiceIcon { Sis, Lms }

@Composable
internal fun ServiceCard(
    icon: ServiceIcon,
    title: Int,
    subtitle: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val (vector, tileColor, onTile) = when (icon) {
        ServiceIcon.Sis -> Triple(
            Icons.Outlined.School,
            colors.tertiaryContainer,
            colors.onTertiaryContainer
        )

        ServiceIcon.Lms -> Triple(
            Icons.AutoMirrored.Outlined.MenuBook,
            colors.secondaryContainer,
            colors.onSecondaryContainer
        )
    }
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceContainer,
        border = BorderStroke(Dimens.BorderThin, colors.outlineVariant),
        modifier = modifier
            .heightIn(min = Dimens.MinTouchTarget)
            .semantics(mergeDescendants = true) {
                role = Role.Button
            },
    ) {
        Column(
            modifier = Modifier.padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = tileColor,
                    contentColor = onTile
                ) {
                    Icon(
                        vector,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(Dimens.SpaceSm)
                            .size(22.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(Dimens.SpaceXs))
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BentoTile(
    onClick: () -> Unit,
    icon: ImageVector,
    @StringRes label: Int,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        shape = MaterialTheme.shapes.large,
        tint = colors.surfaceContainer,
        contentColor = colors.onSurfaceVariant,
        alpha = 0.7f
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = colors.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    stringResource(label),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.secondary
                )
            }
            content()
        }
    }
}

@Composable
internal fun TodayTile(
    summary: DaySummary,
    semesterEnded: Boolean,
    onOpenSchedule: () -> Unit,
    onOpenSis: () -> Unit,
    onOpenLms: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    GlassSurface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        alpha = 0.7f,
        shadow = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .padding(start = 2.dp)
                    .weight(1.3f)
                    .clip(MaterialTheme.shapes.small)
                    .clickable(role = Role.Button, onClick = onOpenSchedule)
                    .semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)
                ) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        null,
                        tint = colors.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        stringResource(R.string.home_today_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.secondary
                    )
                }
                if (summary.total == 0 || summary.first == null || summary.last == null) {
                    Text(
                        stringResource(if (semesterEnded) R.string.hero_semester_ended_title else R.string.home_today_empty_title),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        if (semesterEnded) semesterEndText() else stringResource(R.string.home_today_empty_body),
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        pluralStringResource(
                            R.plurals.schedule_class_count,
                            summary.total,
                            summary.total
                        ), style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        stringResource(
                            R.string.home_today_range,
                            summary.first.fmt(),
                            summary.last.fmt()
                        ),
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                    )
                    Text(
                        if (summary.remaining == 0) stringResource(R.string.home_today_done) else stringResource(
                            R.string.home_today_left,
                            summary.remaining
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (summary.remaining == 0) colors.onSurfaceVariant else colors.secondary,
                    )
                }
            }
            Spacer(Modifier.width(Dimens.SpaceSm))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
            ) {
                ServiceChip(
                    ServiceIcon.Sis,
                    R.string.home_sis_title,
                    onOpenSis,
                    Modifier.weight(1f)
                )
                ServiceChip(
                    ServiceIcon.Lms,
                    R.string.home_lms_title,
                    onOpenLms,
                    Modifier.weight(1f)
                )
            }
        }
    }
}

/** Small and distinct: SIS is the tertiary (amber) pill with a cap, LMS the secondary (teal) pill with a book. */
@Composable
private fun ServiceChip(
    service: ServiceIcon,
    @StringRes label: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val (icon, container, content) = when (service) {
        ServiceIcon.Sis -> Triple(
            Icons.Outlined.School,
            colors.tertiaryContainer,
            colors.onTertiaryContainer
        )

        ServiceIcon.Lms -> Triple(
            Icons.AutoMirrored.Outlined.MenuBook,
            colors.secondaryContainer,
            colors.onSecondaryContainer
        )
    }
    GlassSurface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        alpha = 0.85f,
        tint = container,
        contentColor = content,
        modifier = modifier
            .wrapContentSize(align = Alignment.Center) // <-- Add this to cancel the parent's forced stretching
            .heightIn(min = 40.dp, max = 50.dp)
            .semantics { role = Role.Button },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp), // slightly adjusted for better breathing room
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(stringResource(label), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
internal fun RemindersTile(
    next: ReminderItem?,
    activeCount: Int,
    now: LocalDateTime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    BentoTile(onClick, Icons.Outlined.Notifications, R.string.home_reminders_title, modifier) {
        if (next == null) {
            Text(
                stringResource(R.string.home_reminders_empty),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                stringResource(R.string.home_reminders_add),
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary
            )
        } else {
            Text(
                next.reminder.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            // Overdue = warning icon + the word "Overdue" + error color, never color alone.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)
            ) {
                if (next.isOverdue) Icon(
                    Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = colors.error,
                    modifier = Modifier.size(14.dp)
                )
                val due = dueText(next.reminder.dueAt, now)
                Text(
                    if (next.isOverdue) stringResource(
                        R.string.reminder_overdue_format,
                        due
                    ) else due,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (next.isOverdue) colors.error else colors.onSurfaceVariant,
                )
            }
            if (activeCount > 1) {
                Text(
                    stringResource(R.string.home_reminders_more, activeCount - 1),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

internal val TimelineEntry.listKey: String
    get() = when (this) {
        is TimelineEntry.Class -> session.id
        is TimelineEntry.FreePeriod -> "free-$start"
    }
