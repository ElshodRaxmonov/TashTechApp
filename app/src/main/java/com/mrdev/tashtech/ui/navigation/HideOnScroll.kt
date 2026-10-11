package com.mrdev.tashtech.ui.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.dp

/** Space a top-level screen must leave at its bottom so content isn't hidden by the floating bar. */
val LocalBottomBarPadding = compositionLocalOf { 0.dp }

@Stable
class HideOnScrollState(private val thresholdPx: Float) {
    var visible by mutableStateOf(true)
        private set
    private var accumulated = 0f

    fun show() {
        visible = true
        accumulated = 0f
    }

    /** Observes (never consumes) scroll deltas from any scrollable child. */
    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val dy = available.y
            if (dy == 0f) return Offset.Zero
            if (accumulated * dy < 0f) accumulated = 0f // direction changed: start counting again
            accumulated += dy
            when {
                accumulated < -thresholdPx -> visible = false // finger swipes up: hide
                accumulated > thresholdPx -> visible = true // finger swipes down: show
            }
            return Offset.Zero
        }
    }
}