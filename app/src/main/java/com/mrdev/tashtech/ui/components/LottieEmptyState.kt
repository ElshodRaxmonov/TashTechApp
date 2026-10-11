package com.mrdev.tashtech.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.mrdev.tashtech.ui.theme.Dimens


@Composable
fun LottieEmptyState(
    asset: String,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    fallbackIcon: ImageVector = Icons.Outlined.Info,
    action: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val result = rememberLottieComposition(LottieCompositionSpec.Asset(asset))
    val animationsOn = remember {
        android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
    val progress by animateLottieCompositionAsState(
        result.value, iterations = LottieConstants.IterateForever, isPlaying = animationsOn,
    )
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = Dimens.SpaceLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        when {
            result.value != null -> LottieAnimation(
                composition = result.value,
                progress = { progress },
                modifier = Modifier.fillMaxWidth(0.6f).widthIn(max = 240.dp).heightIn(max = 220.dp),
            )
            result.isLoading -> Spacer(Modifier.height(120.dp)) // reserve space so the layout doesn't jump
            else -> Icon(fallbackIcon, contentDescription = null, tint = colors.secondary, modifier = Modifier.size(48.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        action?.invoke()
    }
}

