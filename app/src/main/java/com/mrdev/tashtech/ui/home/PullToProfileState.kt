package com.mrdev.tashtech.ui.home


import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Pull-down-at-the-top gesture. [progress] is 0..1 at the threshold (it can overshoot slightly).
 * Observes the list's scroll without disturbing normal scrolling: it only reacts to drag left over
 * after the list has hit its top edge.
 */
@Stable
class PullToProfileState(
    private val thresholdPx: Float,
    private val scope: CoroutineScope,
    private val onTrigger: () -> Unit,
) {
    var pullPx by mutableFloatStateOf(0f)
        private set
    val progress: Float get() = pullPx / thresholdPx
    private var settling: Job? = null

    val connection = object : NestedScrollConnection {
        // Dragging back up while the avatar is enlarged shrinks it before the list scrolls.
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput || pullPx <= 0f || available.y >= 0f) return Offset.Zero
            val consumed = max(available.y, -pullPx)
            pullPx += consumed
            return Offset(0f, consumed)
        }

        // The list is at its top and the finger keeps pulling down: build up (with resistance).
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
            settling?.cancel()
            pullPx = (pullPx + available.y * 0.55f).coerceAtMost(thresholdPx * 1.25f)
            return Offset(0f, available.y)
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (pullPx <= 0f) return Velocity.Zero
            // On trigger the avatar stays where it is: the shared transition takes it from there.
            if (pullPx >= thresholdPx) onTrigger() else settle()
            return available
        }
    }

    private fun settle() {
        settling = scope.launch {
            animate(pullPx, 0f, animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)) { value, _ ->
                pullPx = value
            }
        }
    }
}