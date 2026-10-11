package com.mrdev.tashtech.ui.schedule

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrdev.tashtech.R
import com.mrdev.tashtech.domain.model.UserEvent
import com.mrdev.tashtech.ui.AppViewModelProvider
import com.mrdev.tashtech.ui.components.DatePickerHost
import com.mrdev.tashtech.ui.components.ErrorScreen
import com.mrdev.tashtech.ui.components.GroupRow
import com.mrdev.tashtech.ui.components.GroupSearchField
import com.mrdev.tashtech.ui.components.LoadingScreen
import com.mrdev.tashtech.ui.components.LottieEmptyState
import com.mrdev.tashtech.ui.components.ScreenFrame
import com.mrdev.tashtech.ui.components.adaptiveScreenPadding
import com.mrdev.tashtech.ui.home.GroupChip
import com.mrdev.tashtech.ui.home.TimelineRow
import com.mrdev.tashtech.ui.home.listKey
import com.mrdev.tashtech.ui.motion.Motion
import com.mrdev.tashtech.ui.motion.Settle
import com.mrdev.tashtech.ui.motion.rememberPageEntrance
import com.mrdev.tashtech.ui.navigation.LocalBottomBarPadding
import com.mrdev.tashtech.ui.onboarding.filterGroups
import com.mrdev.tashtech.ui.theme.Dimens
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private sealed interface EditorTarget {
    data object New : EditorTarget
    data class Edit(val event: UserEvent) : EditorTarget
}

@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel = viewModel(factory = AppViewModelProvider.Factory),

    ) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    DisposableEffect(viewModel) {
        onDispose {
            if (activity?.isChangingConfigurations != true) viewModel.resetViewedGroup()
        }
    }

    ScreenFrame {
        when (val s = state) {
            ScheduleUiState.Loading -> LoadingScreen()
            ScheduleUiState.Error -> ErrorScreen(R.string.schedule_error, viewModel::retry)
            is ScheduleUiState.Content -> ScheduleContent(
                state = s,
                onModeChange = viewModel::onModeChange,
                onDateSelected = viewModel::onDateSelected,
                onGroupSelected = viewModel::onViewGroup,
                onResetGroup = viewModel::resetViewedGroup,
                onSaveEvent = viewModel::saveEvent,
                onDeleteEvent = viewModel::deleteEvent,
                onShiftWeek = viewModel::shiftWeek,
                onJumpToday = viewModel::jumpToToday,
            )
        }
    }
}

@Composable
private fun ScheduleContent(
    state: ScheduleUiState.Content,
    onModeChange: (ScheduleMode) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onGroupSelected: (String) -> Unit,
    onResetGroup: () -> Unit,
    onSaveEvent: (UserEvent) -> Unit,
    onDeleteEvent: (Long) -> Unit,
    onShiftWeek: (Long) -> Unit,
    onJumpToday: () -> Unit
) {
    val entrance = rememberPageEntrance()
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var editor by remember { mutableStateOf<EditorTarget?>(null) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val pad = adaptiveScreenPadding()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        contentPadding = PaddingValues(
            start = pad,
            end = pad,
            top = Dimens.SpaceLg,
            bottom = Dimens.SpaceLg + LocalBottomBarPadding.current,
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        item(key = "header") {
            Settle(entrance, 0, distance = 8.dp) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)
                ) {
                    Text(
                        text = stringResource(R.string.schedule_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    if (state.isOwnGroup) {
                        IconButton(onClick = { editor = EditorTarget.New }) {
                            Icon(Icons.Outlined.Add, stringResource(R.string.schedule_add_event))
                        }
                    }
                    GroupChip(
                        group = state.group,
                        highlighted = !state.isOwnGroup,
                        onClick = { showPicker = true })
                }
            }
        }
        item(key = "semester") {
            Settle(entrance, 0) {
                Text(
                    stringResource(R.string.schedule_until, semesterEndText()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!state.isOwnGroup) {
            item(key = "viewing") {
                ViewingOtherGroupBanner(
                    state.group,
                    state.ownGroup,
                    onResetGroup
                )
            }
        }
        item(key = "mode") { Settle(entrance, 0) { ModeSelector(state.mode, onModeChange) } }
        if (state.mode == ScheduleMode.Week) {
            item(key = "week_nav") {
                WeekNavigator(
                    state = state, locale = locale,
                    onPrev = { onShiftWeek(-1) }, onNext = { onShiftWeek(1) },
                    onToday = onJumpToday, onPick = { showDatePicker = true },
                )
            }
            item(key = "week") { WeekStrip(state, locale, onDateSelected) }
        }
        item(key = "heading") { Settle(entrance, 0) { DayHeading(state, locale) } }
        if (state.timeline.isEmpty()) {
            item(key = "empty") {
                when {
                    state.afterSemester -> LottieEmptyState(
                        "no_class.json",
                        stringResource(R.string.schedule_semester_over_title),
                        stringResource(R.string.schedule_semester_over_body, semesterEndText()),
                    )

                    state.isViewingToday -> LottieEmptyState(
                        "no_class.json",
                        stringResource(R.string.home_today_empty_title),
                        stringResource(R.string.home_today_empty_body),
                    )

                    else -> LottieEmptyState(
                        "no_class.json",
                        stringResource(R.string.schedule_empty_day_title),
                        stringResource(R.string.schedule_empty_day_body),
                    )
                }

            }
        } else {
            itemsIndexed(
                state.timeline,
                key = { index, entry -> "$index-${entry.listKey}" }) { index, entry ->
                Settle(
                    entrance = entrance,
                    step = index + 2, // groups beyond step 4 share the last step
                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(Motion.Card, easing = Motion.EnterEasing),
                        placementSpec = tween(Motion.Card, easing = Motion.MoveEasing),
                        fadeOutSpec = tween(Motion.Micro),
                    ),
                ) {
                    TimelineRow(
                        entry,
                        onSessionClick = { session ->
                            resolveEditTarget(state.events, session)?.let {
                                editor = EditorTarget.Edit(it)
                            }
                        },
                    )
                }
            }
        }
    }
    if (showDatePicker) {
        DatePickerHost(
            initial = state.selectedDate,
            minDate = state.minDate,
            maxDate = state.maxDate,
            onPicked = onDateSelected,
            onDismiss = { showDatePicker = false },
        )
    }
    editor?.let { target ->
        val editing = (target as? EditorTarget.Edit)?.event
        EventEditorSheet(
            initial = editing,
            defaultDate = state.selectedDate,
            onSave = { onSaveEvent(it); editor = null },
            onDelete = if (editing != null) ({ id -> onDeleteEvent(id); editor = null }) else null,
            onDismiss = { editor = null },
        )
    }
    if (showPicker) {
        GroupPickerSheet(
            groups = state.allGroups,
            shown = state.group,
            ownGroup = state.ownGroup,
            onSelect = { onGroupSelected(it); showPicker = false },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun WeekNavigator(
    state: ScheduleUiState.Content,
    locale: Locale,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onPick: () -> Unit,
) {
    val range = remember(state.weekStart, state.weekEnd, locale) {
        val end = state.weekEnd.format(DateTimeFormatter.ofPattern("d MMM", locale))
        if (state.weekStart.month == state.weekEnd.month) "${state.weekStart.dayOfMonth}–$end"
        else "${state.weekStart.format(DateTimeFormatter.ofPattern("d MMM", locale))} – $end"
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev, enabled = state.canGoPrev) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                stringResource(R.string.schedule_prev_week)
            )
        }
        TextButton(onClick = onPick, modifier = Modifier.weight(1f)) {
            Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(Dimens.SpaceSm))
            Text(range, style = MaterialTheme.typography.titleSmall, maxLines = 1)
        }
        if (!state.isCurrentWeek) {
            TextButton(onClick = onToday) { Text(stringResource(R.string.schedule_today_action)) }
        }
        IconButton(onClick = onNext, enabled = state.canGoNext) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                stringResource(R.string.schedule_next_week)
            )
        }
    }
}

@Composable
private fun ModeSelector(mode: ScheduleMode, onModeChange: (ScheduleMode) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        ScheduleMode.entries.forEachIndexed { index, m ->
            SegmentedButton(
                selected = m == mode,
                onClick = { onModeChange(m) },
                shape = SegmentedButtonDefaults.itemShape(index, ScheduleMode.entries.size),
                label = {
                    Text(
                        stringResource(
                            when (m) {
                                ScheduleMode.Today -> R.string.schedule_mode_today
                                ScheduleMode.Week -> R.string.schedule_mode_week
                            },
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun DayHeading(state: ScheduleUiState.Content, locale: Locale) {
    val dateText = remember(state.selectedDate, locale) {
        state.selectedDate
            .format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale))
            .replaceFirstChar { it.titlecase(locale) }
    }
    Column {
        Text(
            text = if (state.isViewingToday) stringResource(
                R.string.schedule_today_heading,
                dateText
            ) else dateText,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (state.classCount > 0) {
            Text(
                text = pluralStringResource(
                    R.plurals.schedule_class_count,
                    state.classCount,
                    state.classCount
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeekStrip(
    state: ScheduleUiState.Content,
    locale: Locale,
    onDateSelected: (LocalDate) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
    ) {
        state.days.forEach { day ->
            DayChipItem(
                day = day,
                selected = day.date == state.selectedDate,
                locale = locale,
                onClick = { onDateSelected(day.date) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Selected = filled + dot; today = outlined; dot = has classes.
 * The full date, "today" and class count are all in the content description.
 */
@Composable
private fun DayChipItem(
    day: DayChip,
    selected: Boolean,
    locale: Locale,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val fullDate = remember(day.date, locale) {
        day.date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale))
            .replaceFirstChar { it.titlecase(locale) }
    }
    val classes = if (day.classCount == 0) {
        stringResource(R.string.schedule_no_classes)
    } else {
        pluralStringResource(R.plurals.schedule_class_count, day.classCount, day.classCount)
    }
    val cd = if (day.isToday) {
        stringResource(R.string.schedule_day_cd_today, fullDate, classes)
    } else {
        stringResource(R.string.schedule_day_cd, fullDate, classes)
    }
    val container = if (selected) colors.primary else colors.surfaceContainer
    val content = if (selected) colors.onPrimary else colors.onSurface
    val border = when {
        selected -> BorderStroke(0.dp, container)
        day.isToday -> BorderStroke(2.dp, colors.primary)
        else -> BorderStroke(Dimens.BorderThin, colors.outlineVariant)
    }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = content,
        border = border,
        modifier = modifier
            .heightIn(min = Dimens.MinTouchTarget)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .clearAndSetSemantics { contentDescription = cd },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = Dimens.SpaceSm),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXxs),
        ) {
            Text(
                text = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(day.date.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium)
            Surface(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape),
                color = if (day.classCount > 0) content else content.copy(alpha = 0f),
            ) {}
        }
    }

}

@Composable
internal fun semesterEndText(): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { Semester.END.format(DateTimeFormatter.ofPattern("d MMMM", locale)) }
}

@Composable
private fun ViewingOtherGroupBanner(group: String, ownGroup: String, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = colors.tertiaryContainer,
        contentColor = colors.onTertiaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = Dimens.CardPadding, end = Dimens.SpaceXs),
        ) {
            Icon(
                Icons.Outlined.Visibility,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = stringResource(R.string.schedule_viewing_banner, group),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Dimens.SpaceSm, vertical = Dimens.SpaceSm),
            )
            TextButton(
                onClick = onBack,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.onTertiaryContainer),
            ) { Text(stringResource(R.string.schedule_back_to_mine, ownGroup)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupPickerSheet(
    groups: List<String>,
    shown: String,
    ownGroup: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val visible = remember(groups, query) { filterGroups(groups, query) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.85f)
                .padding(horizontal = Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
        ) {
            Text(
                stringResource(R.string.schedule_picker_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                stringResource(R.string.schedule_picker_subtitle, ownGroup),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GroupSearchField(query, { query = it }, Modifier.fillMaxWidth())
            if (visible.isEmpty()) {
                Text(
                    stringResource(R.string.onboarding_no_results, query.trim()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.SpaceLg),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
                    contentPadding = PaddingValues(bottom = Dimens.SpaceLg),
                ) {
                    items(visible, key = { it }) { group ->
                        GroupRow(
                            group = group,
                            selected = group == shown,
                            onClick = { onSelect(group) },
                            label = if (group == ownGroup) stringResource(R.string.schedule_picker_own) else null,
                        )
                    }
                }
            }
        }
    }
}