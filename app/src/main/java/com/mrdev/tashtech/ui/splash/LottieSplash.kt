package com.mrdev.tashtech.ui.splash

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.delay

private const val SPLASH_ASSET = "tashtech_splash.json"
private const val LOAD_TIMEOUT_MS = 1_500L
private const val MAX_SPLASH_MS = 5_000L

/** Plays the supplied Lottie once. A missing, corrupt or slow file never blocks the app. */
@Composable
fun LottieSplash(ready: Boolean, onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val finish by rememberUpdatedState(onFinished)
    val isReady by rememberUpdatedState(ready)
    val result = rememberLottieComposition(LottieCompositionSpec.Asset(SPLASH_ASSET))
    val composition = result.value
    val currentComposition by rememberUpdatedState(composition)

    var playing by remember { mutableStateOf(true) }
    var shownOnce by remember { mutableStateOf(false) }
    val anim = animateLottieCompositionAsState(composition, isPlaying = playing, restartOnPlay = true, iterations = 1)

    LaunchedEffect(Unit) { // "remove animations" accessibility setting
        val scale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        if (scale == 0f) shownOnce = true
    }
    LaunchedEffect(result.isFailure) { if (result.isFailure) shownOnce = true } // asset missing: wait only for readiness
    LaunchedEffect(Unit) { delay(LOAD_TIMEOUT_MS); if (currentComposition == null) shownOnce = true }
    LaunchedEffect(anim.isAtEnd, composition) {
        if (composition != null && anim.isAtEnd) {
            shownOnce = true
            if (!isReady) { // keep the brand animation alive while startup work finishes
                playing = false
                withFrameNanos { }
                playing = true
            }
        }
    }
    LaunchedEffect(shownOnce, ready) { if (shownOnce && ready) finish() } // real readiness, not a timer
    LaunchedEffect(Unit) { delay(MAX_SPLASH_MS); finish() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {},
        contentAlignment = Alignment.Center,
    ) {
        LottieAnimation(
            composition = composition,
            progress = { anim.progress },
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            maintainOriginalImageBounds = true,
        )
    }
}