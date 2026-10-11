package com.mrdev.tashtech.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import com.mrdev.tashtech.R
import com.mrdev.tashtech.ui.components.GlassSurface
import com.mrdev.tashtech.ui.theme.Dimens

private const val DurationMs = 250
private const val SelectedWeight = 2.2f

val FloatingBarHeight = 64.dp
val FloatingBarMargin = 12.dp

private val BarGap = 10.dp

@Composable
fun AnimatedBottomBar(
    current: NavDestination?,
    onSelect: (Any) -> Unit,
    onScanQr: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BarGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassSurface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(32.dp),
                tint = colors.surfaceContainer,
                alpha = 0.78f,
                shadow = 10.dp,
            ) {
                Row(
                    modifier = Modifier
                        .height(FloatingBarHeight)
                        .padding(horizontal = Dimens.SpaceSm)
                        .selectableGroup(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TopLevelDestinations.forEach { top ->
                        BarItem(
                            top,
                            selected = current.isIn(top),
                            onClick = { onSelect(top.route) })
                    }
                }
            }
            QrActionButton(onClick = onScanQr)
        }
    }
}

/** An action, not a destination: it never takes the selected state. */
@Composable
private fun QrActionButton(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, tween(120), label = "qr_press")
    val label = stringResource(R.string.nav_scan_qr)
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = CircleShape,
        color = colors.primary,
        contentColor = colors.onPrimary,
        shadowElevation = 16.dp,
        modifier = Modifier
            .size(FloatingBarHeight)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .semantics { role = Role.Button; contentDescription = label },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                Icons.Outlined.QrCodeScanner,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun RowScope.BarItem(top: TopLevelDestination, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val label = stringResource(top.label)
    val weight by animateFloatAsState(
        if (selected) SelectedWeight else 1f,
        tween(DurationMs, easing = FastOutSlowInEasing), label = "bar_weight",
    )
    val pill by animateColorAsState(
        if (selected) colors.primaryContainer else Color.Transparent,
        tween(DurationMs), label = "bar_pill",
    )
    val content by animateColorAsState(
        if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
        tween(DurationMs), label = "bar_content",
    )

    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            )
            // Unselected items show no text, so the description names the destination for TalkBack.
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {

            Row(
                modifier = Modifier
                    .height(40.dp)
                    .background(pill, CircleShape)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    top.icon,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(24.dp)
                )
                AnimatedVisibility(
                    visible = selected,
                    enter = fadeIn(tween(200, delayMillis = 50)) +
                            expandHorizontally(tween(DurationMs, easing = FastOutSlowInEasing)),
                    exit = fadeOut(tween(120)) + shrinkHorizontally(tween(DurationMs)),
                ) {
                    Text(
                        text = label,
                        color = content,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clearAndSetSemantics {},
                    )
                }
            }

    }
}