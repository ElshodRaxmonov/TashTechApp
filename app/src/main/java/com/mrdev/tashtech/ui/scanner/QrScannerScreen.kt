package com.mrdev.tashtech.ui.scanner

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ZoomState
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrdev.tashtech.R
import com.mrdev.tashtech.ui.services.openLinkExternally
import com.mrdev.tashtech.ui.theme.DarkSuccess
import com.mrdev.tashtech.ui.theme.Dimens
import com.mrdev.tashtech.ui.theme.extended
import java.util.concurrent.Executors

@Composable
fun QrScannerScreen(
    onClose: () -> Unit,
    onOpenInApp: (String) -> Unit,
    viewModel: QrScannerViewModel = viewModel(),
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    var hasPermission by remember { mutableStateOf(hasCameraPermission(context)) }
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasPermission = granted
            asked = true
        }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasPermission = hasCameraPermission(context)
    } // back from Settings
    LaunchedEffect(Unit) { if (!hasPermission && !asked) launcher.launch(android.Manifest.permission.CAMERA) } // asked in context: the user just tapped "scan"

    val canAskAgain = !asked || (activity != null &&
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                android.Manifest.permission.CAMERA
            ))

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (hasPermission) Color.Black else MaterialTheme.colorScheme.background,
    ) {
        if (hasPermission) {
            ScannerContent(
                state = state,
                onQr = viewModel::onQrDetected,
                onRescan = viewModel::rescan,
                onOpenInApp = onOpenInApp,
                onClose = onClose,
            )
        } else {
            PermissionRequired(
                canAskAgain = canAskAgain,
                onAllow = { launcher.launch(Manifest.permission.CAMERA) },
                onOpenSettings = {
                    context.startActivity(
                        Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            "package:${context.packageName}".toUri()
                        ),
                    )
                },
                onClose = onClose,
            )
        }
    }
}

private fun hasCameraPermission(context: Context) =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

@Composable
private fun ScannerContent(
    state: ScanState,
    onQr: (String) -> Unit,
    onRescan: () -> Unit,
    onOpenInApp: (String) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scanning = state is ScanState.Scanning

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS) // preview + analysis only: no capture, nothing saved
            imageAnalysisBackpressureStrategy =
                ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST // always the freshest frame
            imageAnalysisResolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        android.util.Size(1280, 720),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                    ),
                )
                .build()
            isPinchToZoomEnabled = true
            isTapToFocusEnabled = true
        }
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val currentOnQr by rememberUpdatedState(onQr)
    val analyzer = remember { QrAnalyzer { currentOnQr(it) } }

    DisposableEffect(lifecycleOwner) {
        cameraController.bindToLifecycle(lifecycleOwner)
        onDispose {
            cameraController.unbind()
            analyzer.close()
            executor.shutdown()
        }
    }
    // Stop analysing after a result; resume on "Scan again".
    LaunchedEffect(scanning) {
        if (scanning) cameraController.setImageAnalysisAnalyzer(executor, analyzer)
        else cameraController.clearImageAnalysisAnalyzer()
    }

    var zoom by remember { mutableStateOf<ZoomState?>(null) }
    DisposableEffect(cameraController, lifecycleOwner) {
        val observer = Observer<ZoomState> { zoom = it }
        cameraController.zoomState.observe(lifecycleOwner, observer)
        onDispose { cameraController.zoomState.removeObserver(observer) }
    }


    var reticle by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val pulse by rememberInfiniteTransition(label = "reticle")
        .animateFloat(
            0.55f,
            1f,
            infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "pulse"
        )
    val frameColor = when (state) {
        ScanState.Scanning -> Color.White.copy(alpha = pulse)
        is ScanState.Result -> DarkSuccess
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    controller = cameraController // enables pinch-to-zoom and tap-to-focus
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Dimmed surround with a clear window and rounded corner brackets.
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawRect(Color.Black.copy(alpha = 0.55f))
            reticle?.let { drawReticle(it, frameColor) }
        }
        // The whole frame is scanned (faster and more forgiving); the reticle only guides aiming.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .sizeIn(maxWidth = 320.dp, maxHeight = 320.dp)
                    .fillMaxWidth(0.7f)
                    .aspectRatio(1f)
                    .onGloballyPositioned { reticle = it.boundsInParent() },
            )
            Spacer(Modifier.height(Dimens.SpaceLg))
            Text(
                text = stringResource(R.string.scanner_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
            )
            Spacer(Modifier.weight(1.4f))
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(Dimens.SpaceSm)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape),
        ) {
            Icon(Icons.Outlined.Close, stringResource(R.string.scanner_close), tint = Color.White)
        }

        if (scanning) {
            ZoomControls(
                zoom = zoom,
                onLinearZoom = { cameraController.setLinearZoom(it.coerceIn(0f, 1f)) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(bottom = Dimens.SpaceXl),
            )
        }

        AnimatedContent(
            targetState = state,
            modifier = Modifier.align(Alignment.BottomCenter),
            transitionSpec = {
                (slideInVertically(tween(250)) { it / 2 } + fadeIn(tween(250))) togetherWith fadeOut(
                    tween(150)
                )
            },
            label = "scan_result",
        ) { s ->
            when (s) {
                ScanState.Scanning -> Spacer(Modifier.height(0.dp))
                is ScanState.Result -> ResultPanel(s.content, onOpenInApp, onRescan)
            }
        }
    }
}

private fun DrawScope.drawReticle(rect: androidx.compose.ui.geometry.Rect, color: Color) {
    val radius = 24.dp.toPx()
    drawRoundRect(
        color = Color.Black,
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = CornerRadius(radius),
        blendMode = androidx.compose.ui.graphics.BlendMode.Clear,
    )
    val len = rect.width * 0.16f
    val shadowStroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun corner(cx: Float, cy: Float, sx: Float, sy: Float) {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx, cy + sy * len)
            lineTo(cx, cy + sy * radius)
            quadraticTo(cx, cy, cx + sx * radius, cy)
            lineTo(cx + sx * len, cy)
        }
        drawPath(path, Color.Black.copy(alpha = 0.5f), style = shadowStroke)
        drawPath(path, color, style = stroke)
    }
    corner(rect.left, rect.top, 1f, 1f)
    corner(rect.right, rect.top, -1f, 1f)
    corner(rect.left, rect.bottom, 1f, -1f)
    corner(rect.right, rect.bottom, -1f, -1f)
}

/** Linear zoom feels even across the whole range, whatever the camera's maximum ratio is. */
@Composable
private fun ZoomControls(
    zoom: ZoomState?,
    onLinearZoom: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val linear = zoom?.linearZoom ?: 0f
    val ratio = zoom?.zoomRatio ?: 1f
    val zoomLabel = stringResource(R.string.scanner_zoom)
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.Black.copy(alpha = 0.55f),
        contentColor = Color.White,
        modifier = modifier
            .padding(horizontal = Dimens.ScreenPadding)
            .widthIn(max = 420.dp)
            .fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = Dimens.SpaceXs)
        ) {
            IconButton(
                onClick = { onLinearZoom(linear - 0.1f) },
                enabled = zoom != null && linear > 0f
            ) {
                Icon(Icons.Outlined.Remove, stringResource(R.string.scanner_zoom_out))
            }
            Slider(
                value = linear,
                onValueChange = onLinearZoom,
                enabled = zoom != null,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                    disabledThumbColor = Color.White.copy(alpha = 0.4f),
                    disabledActiveTrackColor = Color.White.copy(alpha = 0.4f),
                    disabledInactiveTrackColor = Color.White.copy(alpha = 0.15f),
                ),
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = zoomLabel },
            )
            IconButton(
                onClick = { onLinearZoom(linear + 0.1f) },
                enabled = zoom != null && linear < 1f
            ) {
                Icon(Icons.Outlined.Add, stringResource(R.string.scanner_zoom_in))
            }
            Text(
                text = String.format(LocalLocale.current.platformLocale, "%.1f×", ratio),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(min = 44.dp)
                    .padding(end = Dimens.SpaceSm),
            )
        }
    }
}

@Composable
private fun ResultPanel(content: QrContent, onOpenInApp: (String) -> Unit, onRescan: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val link = content as? QrContent.Link
    val raw = when (content) {
        is QrContent.Link -> content.url
        is QrContent.Text -> content.text
    }
    var copied by remember(raw) { mutableStateOf(false) }
    val chooserTitle = stringResource(R.string.scanner_share_chooser)

    Surface(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = colors.surface,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)
            ) {
                Icon(
                    imageVector = if (link != null) Icons.Outlined.Link else Icons.Outlined.TextSnippet,
                    contentDescription = null,
                    tint = MaterialTheme.extended.success,
                    modifier = Modifier.size(32.dp),
                )
                Column {
                    Text(
                        stringResource(if (link != null) R.string.scanner_result_link else R.string.scanner_result_text),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    if (link != null) {
                        Text(
                            link.host,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.secondary
                        )
                    }
                }
            }
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = colors.surfaceContainer,
                border = BorderStroke(Dimens.BorderThin, colors.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    raw,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(Dimens.SpaceMd),
                )
            }
            if (link != null) {
                val insecure = !link.isSecure
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                    Icon(
                        if (insecure) Icons.Outlined.Warning else Icons.Outlined.Info,
                        contentDescription = null,
                        tint = if (insecure) colors.error else colors.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        stringResource(
                            when {
                                link.isOfficial -> R.string.scanner_official_note
                                insecure -> R.string.scanner_insecure_warning
                                else -> R.string.scanner_external_note
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = {
                        if (link.isOfficial) onOpenInApp(link.url) else openLinkExternally(
                            context,
                            link.url
                        )
                    },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                ) {
                    Text(
                        stringResource(
                            when {
                                link.isOfficial -> R.string.scanner_open_tashtech
                                insecure -> R.string.scanner_open_anyway
                                else -> R.string.scanner_open_link
                            },
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                val action = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                val copy = {
                    context.getSystemService(ClipboardManager::class.java)
                        .setPrimaryClip(ClipData.newPlainText("QR", raw))
                    copied = true
                }
                val copyLabel =
                    stringResource(if (copied) R.string.scanner_copied else R.string.scanner_copy)
                if (link != null) {
                    OutlinedButton(
                        onClick = copy,
                        shape = MaterialTheme.shapes.medium,
                        modifier = action
                    ) { Text(copyLabel) }
                } else {
                    Button(
                        onClick = copy,
                        shape = MaterialTheme.shapes.medium,
                        modifier = action
                    ) { Text(copyLabel) }
                }
                OutlinedButton(
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(Intent.EXTRA_TEXT, raw)
                        context.startActivity(Intent.createChooser(send, chooserTitle))
                    },
                    shape = MaterialTheme.shapes.medium,
                    modifier = action,
                ) { Text(stringResource(R.string.scanner_share)) }
            }
            TextButton(onClick = onRescan, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.scanner_scan_again))
            }
        }
    }
}

@Composable
private fun PermissionRequired(
    canAskAgain: Boolean,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.CameraAlt,
            contentDescription = null,
            tint = colors.secondary,
            modifier = Modifier.size(48.dp)
        )
        Text(
            stringResource(R.string.scanner_permission_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(
                if (canAskAgain) R.string.scanner_permission_body else R.string.scanner_permission_denied_body,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = if (canAskAgain) onAllow else onOpenSettings,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(stringResource(if (canAskAgain) R.string.scanner_permission_allow else R.string.scanner_permission_settings))
        }
        TextButton(onClick = onClose) { Text(stringResource(R.string.scanner_close)) }
    }
}