package com.mrdev.tashtech.ui.news

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mrdev.tashtech.domain.model.NewsArticle
import com.mrdev.tashtech.ui.motion.pressScale
import com.mrdev.tashtech.ui.theme.Dimens

@Composable
fun NewsCard(
    article: NewsArticle,
    onClick: () -> Unit,
    onToggleSaved: () -> Unit,
    modifier: Modifier = Modifier,
    loadImage: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Box(modifier.fillMaxWidth().height(NewsCardHeight)) {
        Surface(
            onClick = onClick,
            interactionSource = interaction,
            shape = MaterialTheme.shapes.large,
            color = colors.surfaceContainerHigh,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxSize().pressScale(interaction),
        ) {
            Box(Modifier.fillMaxSize()) {
                NewsImage(article.imageUrl.takeIf { loadImage }, article.source, article.category, Modifier.fillMaxSize())
                // top quarter: dark gradient scrim so source tags are readable on any bright image header
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.3f)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent),
                            ),
                        ),
                )
                // bottom half: transparent to near-black, so the text is readable on any image
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.5f)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.92f)),
                            ),
                        ),
                )
                SourceChip(article.source, Modifier.align(Alignment.TopStart).padding(Dimens.SpaceMd))
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.2f), contentColor = Color.White) {
                            Text(
                                article.category.label(),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            )
                        }
                        val time = publishedText(article.publishedAtMillis)
                        if (time.isNotEmpty()) {
                            Text(time, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                    Text(
                        article.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        BookmarkButton(article, onToggleSaved, Modifier.align(Alignment.TopEnd).padding(Dimens.SpaceXs))
    }
}
@Composable
fun NewsListItem(
    article: NewsArticle,
    onClick: () -> Unit,
    onToggleSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Box(modifier) {
        Surface(
            onClick = onClick,
            interactionSource = interaction,
            shape = MaterialTheme.shapes.large,
            color = colors.surfaceContainer,
            border = BorderStroke(Dimens.BorderThin, colors.outlineVariant),
            modifier = Modifier.fillMaxWidth().pressScale(interaction),
        ) {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f).heightIn(min = 92.dp), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
                    Text(
                        "${article.source.title} · ${article.category.label()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(article.title, style = MaterialTheme.typography.titleSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Text(
                        publishedText(article.publishedAtMillis),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
                NewsImage(
                    article.imageUrl, article.source,
                    article.category,
                    Modifier.size(84.dp).clip(MaterialTheme.shapes.medium),
                )
            }
        }
        // Outside the clickable surface so it stays a separate TalkBack target. Sits over the thumbnail corner.
        BookmarkButton(article, onToggleSaved, Modifier.align(Alignment.BottomEnd).padding(Dimens.SpaceXs))
    }
}