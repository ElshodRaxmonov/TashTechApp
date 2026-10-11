package com.mrdev.tashtech.ui.browser

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrdev.tashtech.R
import com.mrdev.tashtech.data.config.AcademicService
import com.mrdev.tashtech.data.config.OfficialDomain
import com.mrdev.tashtech.ui.AppViewModelProvider.Factory
import com.mrdev.tashtech.ui.services.openUrl
import com.mrdev.tashtech.ui.theme.Dimens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Stable
private class BrowserState(initialUrl: String) {
    var url by mutableStateOf(initialUrl)
    var title by mutableStateOf("")
    var progress by mutableFloatStateOf(0f)
    var loading by mutableStateOf(true)
    var canGoBack by mutableStateOf(false)
    var failed by mutableStateOf(false)
    var detail by mutableStateOf<String?>(null)   // short technical reason, shown on the error screen
    var blocked by mutableStateOf<String?>(null)  // off-site address the page tried to redirect to
    var httpStatus by mutableIntStateOf(0)
}

@Composable
fun BrowserScreen(
    url: String,
    articleMode: Boolean,
    onBack: () -> Unit,
    onOpenNotes: () -> Unit,
    onCreateReminder: (title: String, note: String) -> Unit,
    viewModel: BrowserViewModel = viewModel(factory = Factory),
) {
    val scope =
        rememberCoroutineScope()                                         // keep: used by scope.launch { … }
    val browseScope = remember(url, articleMode) { BrowseScope(url, articleMode) } // new name
    val context = LocalContext.current
    val resources = LocalResources.current
    val state = remember(url) { BrowserState(url) }
    val savedWebState =
        rememberSaveable { Bundle() }   // kept fresh in onPageFinished, survives rotation
    val snackbar = remember { SnackbarHostState() }
    var showNoteSheet by rememberSaveable { mutableStateOf(false) }

    val noteSaved = stringResource(R.string.note_saved)
    val noteFailed = stringResource(R.string.note_save_failed)
    val noteView = stringResource(R.string.note_saved_view)
    val downloadMsg = stringResource(R.string.browser_download_unsupported)
    val downloadAction = stringResource(R.string.browser_download_action)
    val shareTitle = stringResource(R.string.tools_share_chooser)

    LaunchedEffect(Unit) {
        viewModel.noteResults.collect { ok ->
            val result = snackbar.showSnackbar(
                message = if (ok) noteSaved else noteFailed,
                actionLabel = if (ok) noteView else null,
            )
            if (ok && result == SnackbarResult.ActionPerformed) onOpenNotes()
        }
    }

    // A callback left unanswered freezes the page's file input, so every path below answers it.
    class ChooserHolder {
        var callback: ValueCallback<Array<Uri>>? = null
    }

    val chooser = remember { ChooserHolder() }
    val chooserLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            chooser.callback?.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(
                    result.resultCode,
                    result.data
                )
            )
            chooser.callback = null
        }
    var generation by remember { mutableIntStateOf(0) }

    val webView = remember(url, generation) {
        createWebView(
            context = context,
            state = state,
            savedState = savedWebState,
            browseScope = browseScope, // new
            onExternalLink = { link ->
                if (openExternal(context, link)) {
                    scope.launch {
                        snackbar.showSnackbar(
                            resources.getString(
                                R.string.browser_opened_outside,
                                link.toUri().host.orEmpty()
                            ),
                        )
                    }
                }
            },

            onDownload = { link ->
                scope.launch {
                    if (snackbar.showSnackbar(downloadMsg, downloadAction) == SnackbarResult.ActionPerformed) {
                        openUrl(context, link)
                    }
                }},
            onRenderGone = { generation++ },
            onFileChooser = { callback, params ->
                chooser.callback?.onReceiveValue(null) // answer any pending request first
                chooser.callback = callback
                try {
                    chooserLauncher.launch(params.createIntent())
                    true
                } catch (e: ActivityNotFoundException) {
                    chooser.callback = null
                    callback.onReceiveValue(null)
                    false
                }
            },

            )
    }

    DisposableEffect(webView) {
        onDispose {
            webView.saveState(savedWebState)
            (webView.parent as? ViewGroup)?.removeView(webView) // never destroy() while attached
            webView.stopLoading()
            webView.destroy()
            chooser.callback?.onReceiveValue(null)
            chooser.callback = null
        }
    }
    LaunchedEffect(webView) {
        when {
            !savedWebState.isEmpty && webView.restoreState(savedWebState) != null -> Unit
            browseScope.allows(url) -> webView.loadUrl(url)                       // was OfficialDomain.isOfficial(url)
            else -> {
                state.detail = "Not an allowed address"; state.failed = true
            }
        }
    }

// A load that never starts shouldn't look like a blank page.
    LaunchedEffect(state.loading, state.url) {
        if (state.loading) {
            delay(25_000)
            if (state.loading && state.progress < 0.1f) {
                state.detail = "Timed out"
                state.failed = true
            }
        }
    }

// An HTTP error page may be the server rejecting WebView; offer Chrome without hiding the page.
    val httpMessage = stringResource(R.string.browser_http_error, state.httpStatus)
    val openLabel = stringResource(R.string.browser_open_external)
    LaunchedEffect(state.httpStatus) {
        if (state.httpStatus != 0 &&
            snackbar.showSnackbar(httpMessage, openLabel) == SnackbarResult.ActionPerformed
        ) openUrl(context, state.url)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { webView.onPause() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { webView.onResume() }
    BackHandler(enabled = state.canGoBack) { webView.goBack() }

    val host = OfficialDomain.hostOf(state.url).orEmpty()
    val serviceLabel =
        AcademicService.entries.firstOrNull { it.host == host }?.name?.uppercase() ?: host

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            BrowserTopBar(
                title = state.title.ifBlank { serviceLabel },
                host = host,
                onBack = onBack,
                onReload = { webView.reload() },
                onOpenExternal = { openUrl(context, state.url) },
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(
                        start = Dimens.SpaceMd,
                        end = Dimens.SpaceMd,
                        bottom = Dimens.SpaceMd
                    )
                    .clip(MaterialTheme.shapes.large)
                    .border(
                        BorderStroke(
                            Dimens.BorderThin,
                            MaterialTheme.colorScheme.outlineVariant
                        ), MaterialTheme.shapes.large
                    ),
            ) {
                AndroidView(
                    factory = { webView },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        view.layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                )
                if (state.loading) {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                    )
                }
                if (state.failed) {
                    BrowserError(
                        detail = state.detail,
                        blockedHost = state.blocked?.toUri()?.host,
                        onRetry = {
                            state.failed = false; state.detail = null; state.blocked = null
                            if (webView.url == null) webView.loadUrl(url) else webView.reload()
                        },
                        onOpenBrowser = { openUrl(context, state.blocked ?: state.url) },
                    )
                }
                StudentToolsFab(
                    onNote = { showNoteSheet = true },
                    onRemind = {
                        onCreateReminder(
                            state.title.ifBlank { serviceLabel },
                            state.url
                        )
                    },
                    onShare = { shareUrl(context, state.url, shareTitle) },
                )
            }
        }
    }

    if (showNoteSheet) {
        QuickNoteSheet(
            onSave = { text, includeLink ->
                viewModel.saveNote(text, if (includeLink) state.url else null)
                showNoteSheet = false
            },
            onDismiss = { showNoteSheet = false },
        )
    }
}

private fun openExternal(context: Context, link: String): Boolean {
    val uri = link.toUri()
    return when (uri.scheme?.lowercase()) {
        "mailto", "tel" -> try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri)); true
        } catch (e: ActivityNotFoundException) {
            false
        }

        else -> openUrl(context, link)
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createWebView(
    context: Context,
    state: BrowserState,
    savedState: Bundle,
    browseScope: BrowseScope, // new
    onExternalLink: (String) -> Unit,
    onDownload: (String) -> Unit,
    onRenderGone: () -> Unit,
    onFileChooser: (ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams) -> Boolean,
): WebView = WebView(context).apply {
    val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    if (debuggable) WebView.setWebContentsDebuggingEnabled(true)

    settings.apply {
        setSupportMultipleWindows(true)
        javaScriptCanOpenWindowsAutomatically = true
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false
        useWideViewPort = true
        loadWithOverviewMode = true

        // Critical settings to allow local asset viewer to make requests to remote Moodle endpoints
        @Suppress("DEPRECATION")
        allowUniversalAccessFromFileURLs = true
        @Suppress("DEPRECATION")
        allowFileAccessFromFileURLs = true

        allowContentAccess = true
        allowFileAccess = true

        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        val defaultUA = userAgentString
        userAgentString = defaultUA
            .replace("Version/4.0 ", "")
            .replace("; wv", "")

        if (Build.VERSION.SDK_INT in Build.VERSION_CODES.Q..Build.VERSION_CODES.S_V2) {
            @Suppress("DEPRECATION")
            forceDark = WebSettings.FORCE_DARK_OFF
        }
    }
    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

    val cookieManager = CookieManager.getInstance()
    cookieManager.setAcceptCookie(true)
    cookieManager.setAcceptThirdPartyCookies(this, true)
    fun getInlinePdfHtml(pdfUrl: String): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, minimum-scale=1.0, maximum-scale=5.0, user-scalable=yes">
            <style>
                html, body {
                    margin: 0;
                    padding: 0;
                    background-color: #525659;
                    width: 100%;
                    min-height: 100vh;
                    overflow-x: auto;
                    -webkit-overflow-scrolling: touch;
                }
                #pdf-container {
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    width: 100%;
                    padding: 8px 0;
                    box-sizing: border-box;
                }
                canvas {
                    display: block;
                    margin: 6px auto;
                    box-shadow: 0 4px 10px rgba(0,0,0,0.35);
                    max-width: 96%;
                    height: auto !important;
                    background-color: #ffffff;
                }
                #loading {
                    color: #ffffff;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                    text-align: center;
                    margin-top: 30px;
                    font-size: 16px;
                    font-weight: 500;
                }
            </style>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.min.js"></script>
        </head>
        <body>
            <div id="loading">Loading PDF...</div>
            <div id="pdf-container"></div>

            <script>
                pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js';

                const loadingTask = pdfjsLib.getDocument({
                    url: '$pdfUrl',
                    withCredentials: true
                });

                loadingTask.promise.then(function(pdf) {
                    document.getElementById('loading').style.display = 'none';
                    const container = document.getElementById('pdf-container');
                    
                    // High-DPI scaling factor for crisp text rendering on mobile screens
                    const dpr = window.devicePixelRatio || 1;

                    for (let pageNum = 1; pageNum <= pdf.numPages; pageNum++) {
                        pdf.getPage(pageNum).then(function(page) {
                            // Calculate viewport scale based on device width so text fills the screen nicely
                            const unscaledViewport = page.getViewport({ scale: 1.0 });
                            const screenWidth = window.innerWidth || document.documentElement.clientWidth;
                            const targetScale = ((screenWidth - 16) / unscaledViewport.width) * dpr;
                            
                            const viewport = page.getViewport({ scale: Math.max(targetScale, 1.5 * dpr) });

                            const canvas = document.createElement('canvas');
                            const context = canvas.getContext('2d');

                            canvas.height = viewport.height;
                            canvas.width = viewport.width;

                            // Display size scaled back down to CSS pixels so text renders ultra sharp
                            canvas.style.width = (viewport.width / dpr) + "px";

                            container.appendChild(canvas);

                            const renderContext = {
                                canvasContext: context,
                                viewport: viewport
                            };
                            page.render(renderContext);
                        });
                    }
                }).catch(function(error) {
                    document.getElementById('loading').innerText = 'Failed to load PDF: ' + error.message;
                });
            </script>
        </body>
        </html>
    """.trimIndent()
    }
    webViewClient = object : WebViewClient() {
        private fun isPdfUrl(url: String): Boolean {
            val lowercase = url.lowercase()
            return lowercase.endsWith(".pdf") ||
                    (lowercase.contains("/pluginfile.php/") && lowercase.contains("forcedownload=0")) ||
                    (lowercase.contains("/mod/resource/view.php") && lowercase.contains("redirect=1"))
        }


        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest
        ): Boolean {
            val uri = request.url
            val target = uri.toString()

            // FIX 1: Use getPdfViewerUrl(target) instead of mozilla.github.io
            if (isPdfUrl(target)) {
                // Load inline HTML viewer using loadDataWithBaseURL so cookies match the target domain origin
                view.loadDataWithBaseURL(
                    target,
                    getInlinePdfHtml(target),
                    "text/html",
                    "UTF-8",
                    null
                )
                return true
            }
            if (!request.isForMainFrame) return false
            if (browseScope.allows(target)) return false
            OfficialDomain.upgradeToHttps(target)?.let { view.loadUrl(it); return true }
            when (uri.scheme?.lowercase()) {
                "mailto", "tel" -> onExternalLink(target)
                "http", "https" ->
                    when {
                        request.hasGesture() && !request.isRedirect -> onExternalLink(target) // the user tapped a link to another site
                        browseScope.allowsAutoRedirects && uri.scheme.equals(
                            "https",
                            true
                        ) -> return false // news sites redirect through consent/CDN hosts
                        else -> {
                            state.blocked = target
                            state.detail = "Redirect to ${uri.host}"
                            state.failed = true
                            state.loading = false
                        }
                    }

                else -> Unit
            }
            return true
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            state.loading = true
            state.failed = false
            state.detail = null
            state.blocked = null
            state.httpStatus = 0
            url?.let { state.url = it }
        }

        override fun onPageFinished(view: WebView, url: String?) {
            state.loading = false
            state.canGoBack = view.canGoBack()
            url?.let { state.url = it }
            view.saveState(savedState)
        }

        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
            state.canGoBack = view.canGoBack()
            url?.let { state.url = it }
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            if (debuggable) Log.w(
                "TashTechWeb",
                "error ${error.errorCode} ${error.description} ${request.url}"
            )
            if (request.isForMainFrame) {
                state.detail = "${error.description} (${error.errorCode})"
                state.failed = true
                state.loading = false
            }
        }

        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest,
            response: WebResourceResponse
        ) {
            if (debuggable) Log.w("TashTechWeb", "http ${response.statusCode} ${request.url}")
            if (request.isForMainFrame && response.statusCode >= 400) state.httpStatus =
                response.statusCode
        }

        override fun onReceivedSslError(
            view: WebView,
            handler: SslErrorHandler,
            error: SslError
        ) {
            handler.cancel()
            if (debuggable) Log.w("TashTechWeb", "ssl ${error.primaryError} ${error.url}")
            if (Uri.parse(error.url).host == Uri.parse(state.url).host) {
                state.detail = "Certificate problem: " + when (error.primaryError) {
                    SslError.SSL_UNTRUSTED -> "not trusted (the server may send an incomplete certificate chain)"
                    SslError.SSL_EXPIRED -> "expired"
                    SslError.SSL_IDMISMATCH -> "name mismatch"
                    SslError.SSL_NOTYETVALID, SslError.SSL_DATE_INVALID -> "invalid date"
                    else -> "invalid"
                }
                state.failed = true
                state.loading = false
            }
        }

        override fun onRenderProcessGone(
            view: WebView,
            detail: RenderProcessGoneDetail
        ): Boolean {
            onRenderGone()
            return true
        }
    }

    webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            state.progress = newProgress / 100f
        }

        override fun onShowFileChooser(
            webView: WebView,
            callback: ValueCallback<Array<Uri>>,
            params: FileChooserParams,
        ): Boolean = onFileChooser(callback, params)

        override fun onReceivedTitle(view: WebView, title: String?) {
            state.title = title.orEmpty()
        }

        override fun onConsoleMessage(message: ConsoleMessage): Boolean {
            if (debuggable) Log.w(
                "TashTechWeb",
                "${message.messageLevel()}: ${message.message()} (${message.sourceId()}:${message.lineNumber()})"
            )
            return false
        }
    }

    setDownloadListener { downloadUrl, _, contentDisposition, mimeType, _ ->
        val isPdf = mimeType.equals("application/pdf", ignoreCase = true) ||
                downloadUrl.lowercase().contains(".pdf") ||
                contentDisposition.lowercase().contains(".pdf")

        if (isPdf) {
            post {
                loadDataWithBaseURL(
                    downloadUrl,
                    getInlinePdfHtml(downloadUrl),
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        } else {
            onDownload(downloadUrl)
        }
    }
}

private fun shareUrl(context: Context, url: String, chooserTitle: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url)
    context.startActivity(Intent.createChooser(send, chooserTitle))
}

@Composable
private fun BrowserTopBar(
    title: String,
    host: String,
    onBack: () -> Unit,
    onReload: () -> Unit,
    onOpenExternal: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = Dimens.SpaceXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.common_back))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)
            ) {
                // Showing the real host helps students spot lookalike pages.
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = colors.secondary,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    host,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.secondary,
                    maxLines = 1
                )
            }
        }
        IconButton(onClick = onReload) {
            Icon(
                Icons.Outlined.Refresh,
                stringResource(R.string.browser_reload)
            )
        }
        IconButton(onClick = onOpenExternal) {
            Icon(
                Icons.AutoMirrored.Outlined.OpenInNew,
                stringResource(R.string.browser_open_external)
            )
        }
    }
}

@Composable
private fun BrowserError(
    detail: String?,
    blockedHost: String?,
    onRetry: () -> Unit,
    onOpenBrowser: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        Column(
            modifier = Modifier.padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(
                Dimens.SpaceMd,
                Alignment.CenterVertically
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.browser_error_title),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = if (blockedHost != null) stringResource(
                    R.string.browser_error_redirect_body,
                    blockedHost
                )
                else stringResource(R.string.browser_error_body),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (detail != null) {
                Text(
                    text = stringResource(R.string.browser_error_detail, detail),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Button(onClick = onOpenBrowser, shape = MaterialTheme.shapes.medium) {
                Text(stringResource(R.string.browser_open_external))
            }
            OutlinedButton(onClick = onRetry, shape = MaterialTheme.shapes.medium) {
                Text(stringResource(R.string.common_retry))
            }
        }
    }
}