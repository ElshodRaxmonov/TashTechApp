package com.mrdev.tashtech.ui.news

import android.annotation.SuppressLint
import android.text.format.DateUtils
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mrdev.tashtech.R
import com.mrdev.tashtech.domain.model.NewsArticle
import com.mrdev.tashtech.domain.model.NewsCategory
import com.mrdev.tashtech.domain.model.NewsSource
import com.mrdev.tashtech.ui.motion.Motion
import com.mrdev.tashtech.ui.theme.Dimens


val NewsCardHeight = 232.dp

@StringRes
internal fun NewsCategory.labelRes(): Int = when (this) {
    NewsCategory.AI -> R.string.news_cat_ai
    NewsCategory.Robotics -> R.string.news_cat_robotics
    NewsCategory.Computing -> R.string.news_cat_computing
    NewsCategory.Hardware -> R.string.news_cat_hardware
    NewsCategory.Semiconductors -> R.string.news_cat_semiconductors
    NewsCategory.Energy -> R.string.news_cat_energy
    NewsCategory.Space -> R.string.news_cat_space
    NewsCategory.Security -> R.string.news_cat_security
    NewsCategory.Science -> R.string.news_cat_science
    NewsCategory.Transportation -> R.string.news_cat_transportation
    NewsCategory.Software -> R.string.news_cat_software
    NewsCategory.Startups -> R.string.news_cat_startups
    NewsCategory.Telecom -> R.string.news_cat_telecom
    NewsCategory.Tech -> R.string.news_cat_tech
}

@Composable
internal fun NewsCategory.label(): String = stringResource(labelRes())
/** Container + content colors per publisher, so the placeholder and chips stay on-brand. */
@Composable
internal fun NewsSource.colors(): Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color> {
    val c = MaterialTheme.colorScheme
    return when (this) {
        NewsSource.IeeeSpectrum -> c.secondaryContainer to c.onSecondaryContainer
        NewsSource.ArsTechnica -> c.tertiaryContainer to c.onTertiaryContainer
        NewsSource.TechCrunch -> c.primaryContainer to c.onPrimaryContainer
    }
}

@Composable
internal fun publishedText(millis: Long): String {
    if (millis <= 0L) return ""
    LocalConfiguration.current
    val context = LocalContext.current
    val now = System.currentTimeMillis()
    return if (now - millis < 7 * DateUtils.DAY_IN_MILLIS) {
        DateUtils.getRelativeTimeSpanString(
            millis,
            now,
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    } else {
        DateUtils.formatDateTime(
            context,
            millis,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH
        )
    }
}

/** The placeholder sits behind the image, so a missing, slow or broken image never changes the layout. */
@Composable
internal fun NewsImage(imageUrl: String?, source: NewsSource, category: NewsCategory, modifier: Modifier = Modifier) {
    Box(modifier) {
        NewsPlaceholder(source, category, Modifier.fillMaxSize())
        if (imageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(imageUrl).crossfade(true)
                    .build(),
                contentDescription = null, // decorative: the title carries the meaning
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private val NewsSource.slug: String
    get() = when (this) {
        NewsSource.IeeeSpectrum -> "ieee"
        NewsSource.ArsTechnica -> "ars"
        NewsSource.TechCrunch -> "techcrunch"
    }

@SuppressLint("DiscouragedApi") // dynamic lookup is intentional: the artwork is optional
@Composable
private fun placeholderRes(source: NewsSource): Int {
    val context = LocalContext.current
    return remember(source) {
        listOf("news_placeholder_${source.slug}", "news_placeholder")
                .firstNotNullOfOrNull { name ->
                context.resources.getIdentifier(name, "drawable", context.packageName)
                    .takeIf { it != 0 }
            } ?: 0
    }
}
@SuppressLint("DiscouragedApi") // dynamic lookup is intentional: the artwork is supplied by hand and may be incomplete
@Composable
private fun topicImage(source: NewsSource, category: NewsCategory): Int {
    if (source != NewsSource.TechCrunch) return 0 // other publishers keep their own behaviour
    val context = LocalContext.current
    return remember(category) {
        listOf("news_tc_${category.name.lowercase()}", "news_tc_default").firstNotNullOfOrNull { name ->
            context.resources.getIdentifier(name, "drawable", context.packageName).takeIf { it != 0 }
        } ?: 0
    }
}
@Composable
private fun NewsPlaceholder(source: NewsSource, category: NewsCategory,modifier: Modifier = Modifier) {
    val topic = topicImage(source, category)
    if (topic != 0) {
        Image(painterResource(topic), contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
        return
    }
    val (container, content) = source.colors()
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                listOf(
                    container,
                    MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Outlined.Newspaper,
            contentDescription = null,
            tint = content.copy(alpha = 0.55f),
            modifier = Modifier.size(36.dp)
        )


    }
}

@Composable
fun SourceChip(source: NewsSource, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.small,
        color = colors.surface.copy(alpha = 0.92f),
        contentColor = colors.onSurface,
        modifier = modifier,
    ) {
        Text(
            source.title,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(
                horizontal = Dimens.SpaceSm,
                vertical = Dimens.SpaceXs
            ),
        )
    }
}

@Composable
fun BookmarkButton(
    article: NewsArticle,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val label = stringResource(
        if (article.saved) R.string.news_unsave_cd else R.string.news_save_cd,
        article.title
    )
    IconButton(
        onClick = onToggle,
        modifier = modifier.semantics { contentDescription = label },
    ) {
        Surface(
            shape = CircleShape,
            color = colors.surface.copy(alpha = 0.9f),
            contentColor = if (article.saved) colors.primary else colors.onSurface
        ) {
            Crossfade(
                targetState = article.saved,
                animationSpec = tween(Motion.Micro),
                label = "bookmark"
            ) { saved ->
                Icon(
                    if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(20.dp),
                )
            }
        }
    }
}