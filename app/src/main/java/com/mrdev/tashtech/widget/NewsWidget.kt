package com.mrdev.tashtech.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.Text
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.mrdev.tashtech.R
import com.mrdev.tashtech.domain.model.NewsArticle
import com.mrdev.tashtech.ui.navigation.AppLinks
import com.mrdev.tashtech.ui.news.labelRes
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException

private const val THUMB_PX = 144
private const val STALE_AFTER_MS = 12 * 60 * 60 * 1000L

class NewsWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(DpSize(180.dp, 110.dp), DpSize(250.dp, 110.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = container(context).news
        repo.cachedArticles() // instant: memory or disk. The first frame never waits for the network.
        coroutineScope {
            launch {
                // TTL + the repository's lock make this a no-op when the cache is fresh, even across several widgets.
                try {
                    repo.refresh()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) { /* offline: keep showing the cache */
                }
            }
            provideContent {
                val articles by repo.articles.collectAsState()
                val article = articles.firstOrNull() // the feed is already sorted newest first
                val thumb by produceState<Bitmap?>(null, article?.imageUrl) {
                    value = article?.imageUrl?.let { loadThumbnail(context, it) }
                }
                NewsContent(context, article, thumb, repo.cacheAgeMillis() > STALE_AFTER_MS)
            }
        }
    }
}

class NewsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NewsWidget()
}

/** Refresh button: rate-limited so repeated taps can't hammer the publishers. */
class RefreshNewsAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastRun >= MIN_INTERVAL_MS) {
            lastRun = now
            try {
                container(context).news.refresh(force = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
            }
        }
        NewsWidget().updateAll(context)
    }

    private companion object {
        const val MIN_INTERVAL_MS = 60_000L

        @Volatile
        var lastRun = 0L
    }
}

@Composable
private fun NewsContent(context: Context, article: NewsArticle?, thumb: Bitmap?, stale: Boolean) {
    val wide = LocalSize.current.width >= 240.dp
    val section = context.getString(R.string.widget_news_section)
    val description = if (article == null) section else context.getString(R.string.widget_news_cd, article.title)
    // The whole surface goes to the News page. Nested controls (refresh) take precedence over it.
    val openNews = actionStartActivity(AppLinks.intentFor(context, AppLinks.ACTION_OPEN_NEWS))

    Row(
        modifier = GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.widget_bg)).padding(12.dp)
            .clickable(openNews).semantics { contentDescription = description },
    ) {
        Column(GlanceModifier.defaultWeight().fillMaxHeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = GlanceModifier.size(24.dp).background(ImageProvider(R.drawable.widget_bg_chip)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        ImageProvider(R.drawable.ic_widget_news), contentDescription = null,
                        colorFilter = androidx.glance.ColorFilter.tint(WidgetColors.onAccent), modifier = GlanceModifier.size(14.dp),
                    )
                }
                Spacer(GlanceModifier.width(8.dp))
                Text(section, style = WidgetText.section, maxLines = 1)
            }
            Spacer(GlanceModifier.height(8.dp))
            if (article == null) {
                Text(context.getString(R.string.widget_news_empty), style = WidgetText.detail, maxLines = 3)
            } else {
                Text(article.title, style = WidgetText.title, maxLines = 3)
                Spacer(GlanceModifier.defaultWeight())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        context.getString(article.category.labelRes()).uppercase(),
                        style = WidgetText.chip,
                        modifier = GlanceModifier.background(ImageProvider(R.drawable.widget_bg_chip)).padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1,
                    )
                    if (stale) {
                        Spacer(GlanceModifier.width(6.dp))
                        Text(context.getString(R.string.widget_offline_copy), style = WidgetText.detail, maxLines = 1)
                    }
                }
            }
        }
        if (wide && article != null) {
            Spacer(GlanceModifier.width(10.dp))
            Column(GlanceModifier.fillMaxHeight(), horizontalAlignment = Alignment.End) {
                if (thumb != null) Image(ImageProvider(thumb), contentDescription = null, modifier = GlanceModifier.size(60.dp))
                Spacer(GlanceModifier.defaultWeight())
                Image(
                    ImageProvider(R.drawable.ic_widget_refresh),
                    contentDescription = context.getString(R.string.widget_refresh_cd),
                    colorFilter = androidx.glance.ColorFilter.tint(WidgetColors.accent),
                    modifier = GlanceModifier.size(32.dp).padding(6.dp).clickable(actionRunCallback<RefreshNewsAction>()),
                )
            }
        }
    }
}

/** Fetched through the app's existing Coil loader (disk-cached), bounded, and rounded here because Glance can't clip images. */
private suspend fun loadThumbnail(context: Context, url: String): Bitmap? =
    withTimeoutOrNull(4_000) {
        val result = SingletonImageLoader.get(context).execute(
            ImageRequest.Builder(context).data(url).size(THUMB_PX, THUMB_PX).allowHardware(false)
                .build(),
        )
        (result as? SuccessResult)?.image?.toBitmap()?.roundedSquare(THUMB_PX, 28f)
    }

private fun Bitmap.roundedSquare(sizePx: Int, radius: Float): Bitmap {
    val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = coil3.Canvas(out)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val side = minOf(width, height)
    val src = Rect((width - side) / 2, (height - side) / 2, (width + side) / 2, (height + side) / 2)
    val dst = Rect(0, 0, sizePx, sizePx)
    canvas.drawRoundRect(RectF(dst), radius, radius, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(this, src, dst, paint)
    return out
}