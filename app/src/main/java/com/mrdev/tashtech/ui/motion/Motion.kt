package com.mrdev.tashtech.ui.motion

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

object Motion {
    const val Micro = 140        // press feedback
    const val Card = 260         // card appearance
    const val Page = 320         // page and screen transitions
    const val StaggerStep = 40   // gap between groups
    const val MaxStaggerSteps = 4 // groups past this appear together, so nobody waits for content

    val EnterEasing: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f) // ease-out: things arriving
    val MoveEasing: Easing = FastOutSlowInEasing                         // ease-in-out: spatial movement
}

/** False until the splash has finished; entrance animations hold until it is true. */
val LocalAppReady = compositionLocalOf { true }

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalNavAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Tracks whether this page's first-visit entrance is still running. */
@Stable
class PageEntrance(firstVisit: Boolean) {
    var active by mutableStateOf(firstVisit)
        internal set
}
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedProfile(
    key: String,
    text: Boolean = false
): Modifier {
    val scope = LocalSharedTransitionScope.current ?: return this
    val visibility = LocalNavAnimatedScope.current ?: return this

    val bounds = remember {
        BoundsTransform { _, _ ->
            spring(
                dampingRatio = 0.85f,
                stiffness = 300f
            )
        }
    }

    return this.then(
        with(scope) {
            val contentState = rememberSharedContentState(key)

            if (text) {
                Modifier.sharedBounds(
                    sharedContentState = contentState,
                    animatedVisibilityScope = visibility,
                    boundsTransform = bounds
                )
            } else {
                Modifier.sharedElement(
                    sharedContentState = contentState,
                    animatedVisibilityScope = visibility,
                    boundsTransform = bounds
                )
            }
        }
    )
}


/** Saved per back-stack entry, so coming back to a page doesn't replay the entrance. */
@Composable
fun rememberPageEntrance(): PageEntrance {
    val seen = rememberSaveable { mutableStateOf(false) }
    val entrance = remember { PageEntrance(firstVisit = !seen.value) }
    val ready = LocalAppReady.current
    LaunchedEffect(ready) {
        if (ready) {
            seen.value = true
            if (entrance.active) {
                delay(Motion.Page + Motion.StaggerStep * Motion.MaxStaggerSteps + Motion.Card + 100L)
                // Items scrolled into view after this point appear without animating.
                entrance.active = false
            }
        }
    }
    return entrance
}

/**
 * Cards settle into place: alpha 0→1, 12dp up, 0.98→1 scale. [step] sets hierarchy:
 * 0 header, 1 primary card, 2+ secondary. Groups beyond [Motion.MaxStaggerSteps] share a step.
 */
@Composable
fun Modifier.settleIn(entrance: PageEntrance, step: Int, distance: Dp = 12.dp): Modifier {
    val ready = LocalAppReady.current
    val progress = remember { Animatable(if (entrance.active) 0f else 1f) }
    LaunchedEffect(ready) {
        if (ready && progress.value < 1f) {
            progress.animateTo(
                1f,
                tween(
                    Motion.Card,
                    delayMillis = Motion.StaggerStep * step.coerceIn(0, Motion.MaxStaggerSteps),
                    easing = Motion.EnterEasing,
                ),
            )
        }
    }
    val offsetPx = with(LocalDensity.current) { distance.toPx() }
    return graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * offsetPx
        val s = 0.98f + 0.02f * p
        scaleX = s
        scaleY = s
    }
}

/** Wraps an existing composable so it can settle in without changing its own signature. */
@Composable
fun Settle(
    entrance: PageEntrance,
    step: Int,
    modifier: Modifier = Modifier,
    distance: Dp = 12.dp,
    content: @Composable () -> Unit,
) {
    Box(modifier.settleIn(entrance, step, distance)) { content() }
}

/** About 0.98× while pressed, nothing else: no bounce, no spring. */
@Composable
fun Modifier.pressScale(source: InteractionSource, pressedScale: Float = 0.98f): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) pressedScale else 1f,
        tween(Motion.Micro, easing = Motion.MoveEasing),
        label = "press",
    )
    return graphicsLayer { scaleX = scale; scaleY = scale }
}