package com.mrdev.tashtech.ui.browser

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.NotificationAdd
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.mrdev.tashtech.R
import com.mrdev.tashtech.ui.theme.Dimens

private const val MotionMs = 220

private class ToolAction(val icon: ImageVector, @StringRes val label: Int, val onClick: () -> Unit)

@Composable
fun BoxScope.StudentToolsFab(onNote: () -> Unit, onRemind: () -> Unit, onShare: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val colors = MaterialTheme.colorScheme
    val scrim by animateColorAsState(
        if (expanded) Color.Black.copy(alpha = 0.25f) else Color.Transparent,
        tween(MotionMs), label = "tools_scrim",
    )
    BackHandler(enabled = expanded) { expanded = false }
    LaunchedEffect(imeVisible) { if (imeVisible) expanded = false }
    if (imeVisible) return // don't sit on top of the page's text fields while typing

    val overlayActive = expanded || scrim.alpha > 0.01f
    val rootModifier = if (overlayActive) {
        Modifier
            .matchParentSize()
            .background(scrim)
            .then(
                if (expanded) Modifier.pointerInput(Unit) {
                    detectTapGestures {
                        expanded = false
                    }
                } else Modifier,
            )
    } else {
        Modifier.align(Alignment.BottomStart) // idle: no full-size Compose layer above the WebView
    }

    val actions = listOf(
        ToolAction(Icons.Outlined.EditNote, R.string.tools_note, onNote),
        ToolAction(Icons.Outlined.NotificationAdd, R.string.tools_remind, onRemind),
        ToolAction(Icons.Outlined.Share, R.string.tools_share, onShare),
    )
    val origin = TransformOrigin(0f, 1f)

    Box(
        rootModifier
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(Dimens.SpaceSm),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
            horizontalAlignment = Alignment.Start,
        ) {
            actions.forEachIndexed { index, action ->
                val stagger = (actions.lastIndex - index) * 40 // nearest to the FAB appears first
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn(tween(MotionMs, stagger)) +
                            slideInVertically(
                                tween(
                                    MotionMs,
                                    stagger,
                                    FastOutSlowInEasing
                                )
                            ) { it / 2 } +
                            scaleIn(
                                tween(MotionMs, stagger),
                                initialScale = 0.8f,
                                transformOrigin = origin
                            ),
                    exit = fadeOut(tween(120)) +
                            slideOutVertically(tween(150)) { it / 2 } +
                            scaleOut(tween(150), targetScale = 0.8f, transformOrigin = origin),
                ) {
                    ToolRow(action) { expanded = false; action.onClick() }
                }
            }
            SmallFloatingActionButton(
                onClick = { expanded = !expanded },
                containerColor = if (expanded) colors.primaryContainer else colors.surfaceContainerHigh,
                contentColor = if (expanded) colors.onPrimaryContainer else colors.onSurfaceVariant,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Crossfade(
                    targetState = expanded,
                    animationSpec = tween(MotionMs),
                    label = "tools_icon"
                ) { open ->
                    Icon(
                        imageVector = if (open) Icons.Outlined.Close else Icons.Outlined.AutoAwesome,
                        contentDescription = stringResource(if (open) R.string.tools_close else R.string.tools_open),
                    )
                }
            }
        }
    }
}

/** Same 40dp circle and 48dp slot as the main button, so every icon sits on one vertical axis. */
@Composable
private fun ToolRow(action: ToolAction, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val label = stringResource(action.label)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)
    ) {
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = colors.surfaceContainerHigh,
            contentColor = colors.onSurface,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
            modifier = Modifier.minimumInteractiveComponentSize(),
        ) { Icon(action.icon, contentDescription = label) }
        // The label is tappable too, but hidden from TalkBack: the button above already announces it.
        Surface(
            onClick = onClick,
            shape = MaterialTheme.shapes.small,
            color = colors.surfaceContainerHighest,
            contentColor = colors.onSurface,
            shadowElevation = 2.dp,
            modifier = Modifier
                .heightIn(min = 40.dp)
                .clearAndSetSemantics {},
        ) {
            Box(
                Modifier.padding(horizontal = Dimens.SpaceMd),
                contentAlignment = Alignment.Center
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
