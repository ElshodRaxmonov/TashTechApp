package com.mrdev.tashtech.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrdev.tashtech.R
import com.mrdev.tashtech.ui.AppViewModelProvider
import com.mrdev.tashtech.ui.components.ErrorScreen
import com.mrdev.tashtech.ui.components.LoadingScreen
import com.mrdev.tashtech.ui.components.ProfileAvatar
import com.mrdev.tashtech.ui.components.ScreenFrame
import com.mrdev.tashtech.ui.components.adaptiveScreenPadding
import com.mrdev.tashtech.ui.motion.PageEntrance
import com.mrdev.tashtech.ui.motion.Settle
import com.mrdev.tashtech.ui.motion.rememberPageEntrance
import com.mrdev.tashtech.ui.motion.sharedProfile
import com.mrdev.tashtech.ui.navigation.LocalBottomBarPadding
import com.mrdev.tashtech.ui.news.HomeNewsSection
import com.mrdev.tashtech.ui.theme.Dimens
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    onOpenSchedule: () -> Unit,
    onOpenSis: () -> Unit,
    onOpenLms: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onOpenReminders: () -> Unit,
    onOpenProfile: () -> Unit,
    onAddReminder: () -> Unit,
    onOpenNews: () -> Unit,
    onOpenArticle: (String) -> Unit
) {
    val entrance = rememberPageEntrance()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ScreenFrame {
        when (val s = state) {
            HomeUiState.Loading -> LoadingScreen()
            HomeUiState.Error -> ErrorScreen(R.string.home_error, viewModel::retry)
            is HomeUiState.Content -> HomeContent(
                s,
                entrance,
                onOpenSchedule,
                onOpenReminders,
                onAddReminder,
                onOpenSis,
                onOpenLms,
                onOpenNews,
                onOpenArticle,
                onOpenProfile,
            )
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Content,
    entrance: PageEntrance,
    onOpenSchedule: () -> Unit,
    onOpenReminders: () -> Unit,
    onAddReminder: () -> Unit,
    onOpenSis: () -> Unit,
    onOpenLms: () -> Unit,
    onOpenNews: () -> Unit,
    onOpenArticle: (String) -> Unit,
    onOpenProfile: () -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val currentOpen by rememberUpdatedState(onOpenProfile)
    val pull = remember(density) {
        PullToProfileState(
            with(density) { 90.dp.toPx() },
            scope
        ) { currentOpen() }
    }

    val pad = adaptiveScreenPadding()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .nestedScroll(pull.connection),
        contentPadding = PaddingValues(
            start = pad, end = pad,
            top = Dimens.SpaceMd, bottom = Dimens.SpaceLg + LocalBottomBarPadding.current,
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        item(key = "header") {
            Settle(entrance, 0, distance = 8.dp) {
                HomeHeader(state, pull, onOpenProfile)
            }
        }
        item(key = "hero") {
            Settle(entrance, 1) {
                HeroCard(
                    state.hero,
                    state.group,
                    state.semesterEnded,
                    onOpenSchedule
                )
            }
        }

        item(key = "bento") {
            Settle(entrance, 2) {
                TodayTile(
                    summary = state.summary,
                    semesterEnded = state.semesterEnded,
                    onOpenSchedule = onOpenSchedule,
                    onOpenSis = onOpenSis,
                    onOpenLms = onOpenLms,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item(key = "news") {
            Settle(entrance, 3) {
                HomeNewsSection(
                    onOpenAll = onOpenNews,
                    onOpenArticle = onOpenArticle
                )
            }
        }

    }
}

@Composable
private fun HomeHeader(
    state: HomeUiState.Content,
    pull: PullToProfileState,
    onOpenProfile: () -> Unit
) {

    val colors = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    val armed by remember(pull) { derivedStateOf { pull.progress >= 1f } }
    val showHint by remember(pull) { derivedStateOf { pull.progress > 0.12f } }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(armed) { if (armed) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }

    val greeting = stringResource(
        when (state.dayPart) {
            DayPart.Morning -> R.string.home_hello_morning
            DayPart.Afternoon -> R.string.home_hello_afternoon
            DayPart.Evening -> R.string.home_hello_evening
        },
    )
    val name = state.name.trim()
    val nameStyle = when {
        name.length <= 14 -> MaterialTheme.typography.headlineLarge
        name.length <= 22 -> MaterialTheme.typography.headlineMedium
        else -> MaterialTheme.typography.headlineSmall
    }
    val date = remember(state.now.toLocalDate(), locale) {
        state.now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale))
            .replaceFirstChar { it.titlecase(locale) }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ProfileAvatar(
            sex = state.sex,
            modifier = Modifier
                .sharedProfile("profile-avatar") // shared element first, size after
                .layout { measurable, constraints ->
                    val side = (56.dp + 48.dp * pull.progress.coerceIn(0f, 1.15f)).roundToPx()
                    val placeable = measurable.measure(Constraints.fixed(side, side))
                    layout(side, side) { placeable.place(0, 0) }
                },
            onClick = onOpenProfile,
            onClickLabel = stringResource(R.string.profile_open_cd),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(greeting, style = MaterialTheme.typography.labelLarge, color = colors.secondary)
            Text(
                name,
                style = nameStyle,
                color = colors.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.sharedProfile("profile-name", text = true),
            )
            Text(date, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            AnimatedVisibility(visible = showHint) {
                Text(
                    stringResource(if (armed) R.string.profile_release_hint else R.string.profile_pull_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.secondary,
                )
            }
        }
    }
}

@Composable
internal fun GroupChip(
    group: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val container = if (highlighted) colors.tertiaryContainer else colors.secondaryContainer
    val content = if (highlighted) colors.onTertiaryContainer else colors.onSecondaryContainer
    val cd = stringResource(R.string.home_group_cd, group)
    val clickLabel = stringResource(R.string.schedule_group_switch_label)
    Surface(
        shape = MaterialTheme.shapes.small,
        color = container,
        contentColor = content,
        modifier = if (onClick == null) modifier.semantics {
            contentDescription = cd
        } else modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            modifier = (if (onClick != null) {
                Modifier
                    .heightIn(min = Dimens.MinTouchTarget)
                    .clickable(onClickLabel = clickLabel, role = Role.Button, onClick = onClick)
            } else Modifier)
                .padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceSm),
        ) {
            Icon(Icons.Outlined.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(group, style = MaterialTheme.typography.labelLarge)
            if (onClick != null) {
                Icon(
                    Icons.Outlined.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}