package com.mrdev.tashtech.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mrdev.tashtech.ui.motion.pressScale
import com.mrdev.tashtech.ui.theme.Dimens

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.large,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    alpha: Float = 0.72f,
    shadow: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val rim = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = if (dark) 0.22f else 0.5f),
            Color.White.copy(alpha = if (dark) 0.04f else 0.15f)
        ),
    )
    val sheen = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = if (dark) 0.07f else 0.35f), Color.Transparent),
    )
    val inner: @Composable () -> Unit = { Box(Modifier.background(sheen)) { content() } }
    val fill = tint.copy(alpha = alpha)
    val source = remember { MutableInteractionSource() }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            interactionSource = source,
            modifier = modifier.pressScale(source),
            shape = shape, color = fill, contentColor = contentColor,
            border = BorderStroke(1.dp, rim), shadowElevation = shadow, content = inner,
        )
    } else {
        Surface(
            modifier = modifier, shape = shape, color = fill, contentColor = contentColor,
            border = BorderStroke(1.dp, rim), shadowElevation = shadow, content = inner
        )
    }
}

/** Soft brand glows behind a screen: the "something" the glass sits on. */
@Composable
fun Modifier.ambientBackdrop(): Modifier {
    val c = MaterialTheme.colorScheme
    val strength = if (c.background.luminance() < 0.5f) 0.22f else 0.16f
    val a = c.primary
    val b = c.secondary
    return drawBehind {
        val r = size.maxDimension * 0.7f
        drawRect(
            Brush.radialGradient(
                listOf(a.copy(alpha = strength), Color.Transparent),
                Offset(size.width * 0.95f, size.height * 0.03f),
                r
            )
        )
        drawRect(
            Brush.radialGradient(
                listOf(b.copy(alpha = strength * 0.8f), Color.Transparent),
                Offset(size.width * 0.02f, size.height * 0.45f),
                r * 0.8f
            )
        )
    }
}

/** Backdrop plus a centred max-width column, so tablets and foldables don't stretch phone layouts. */
@Composable
fun ScreenFrame(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier
        .fillMaxSize()
        .ambientBackdrop(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier
            .widthIn(max = Dimens.MaxContentWidth)
            .fillMaxSize(), content = content)
    }
}

/** 14dp on narrow phones (<360dp), 20dp on typical phones, 28dp on large screens. */
@Composable
fun adaptiveScreenPadding(): Dp {
    val width = LocalConfiguration.current.screenWidthDp
    return when {
        width < 360 -> 14.dp
        width < 600 -> Dimens.ScreenPadding
        else -> 28.dp
    }
}