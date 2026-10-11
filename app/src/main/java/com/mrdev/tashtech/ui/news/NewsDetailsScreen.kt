package com.mrdev.tashtech.ui.news

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrdev.tashtech.R
import com.mrdev.tashtech.data.config.OfficialDomain
import com.mrdev.tashtech.domain.schedule.AppTime
import com.mrdev.tashtech.ui.AppViewModelProvider
import com.mrdev.tashtech.ui.components.LoadingScreen
import com.mrdev.tashtech.ui.components.adaptiveScreenPadding
import com.mrdev.tashtech.ui.motion.Motion
import com.mrdev.tashtech.ui.theme.Dimens
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsDetailsScreen(
    onBack: () -> Unit,
    onOpenInApp: (String) -> Unit,
    viewModel: NewsDetailsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val pad = adaptiveScreenPadding()
    val found = state as? NewsDetailsState.Found

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.news_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            stringResource(R.string.common_back)
                        )
                    }
                },
                actions = {
                    found?.article?.let { article ->
                        val label = stringResource(
                            if (article.saved) R.string.news_unsave_cd else R.string.news_save_cd,
                            article.title,
                        )
                        IconButton(
                            onClick = viewModel::toggleSaved,
                            modifier = Modifier.semantics { contentDescription = label }) {
                            Crossfade(
                                article.saved,
                                animationSpec = tween(Motion.Micro),
                                label = "details_bookmark"
                            ) { saved ->
                                Icon(
                                    if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = null,
                                    tint = if (saved) colors.primary else colors.onSurface,
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
            )
        },
    ) { padding ->
        when (state) {
            NewsDetailsState.Loading -> Box(Modifier.padding(padding)) { LoadingScreen() }
            NewsDetailsState.NotFound -> Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(pad),
                Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(
                        Dimens.SpaceMd
                    )
                ) {
                    Text(
                        stringResource(R.string.news_not_found),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    OutlinedButton(onClick = onBack) { Text(stringResource(R.string.common_back)) }
                }
            }

            is NewsDetailsState.Found -> {
                val article = (state as NewsDetailsState.Found).article
                val locale = LocalConfiguration.current.locales[0]
                val full = remember(article.publishedAtMillis, locale) {
                    if (article.publishedAtMillis <= 0L) ""
                    else java.time.Instant.ofEpochMilli(article.publishedAtMillis)
                        .atZone(AppTime.ZONE)
                        .format(
                            DateTimeFormatter.ofLocalizedDateTime(
                                FormatStyle.MEDIUM,
                                FormatStyle.SHORT
                            ).withLocale(locale)
                        )
                }
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = pad)
                        .padding(bottom = Dimens.SpaceXl),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
                ) {
                    NewsImage(
                        article.imageUrl, article.source,
                        article.category,
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(MaterialTheme.shapes.large),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)
                    ) {
                        val (container, content) = article.source.colors()
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = container,
                            contentColor = content
                        ) {
                            Text(
                                article.source.title,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(
                                    horizontal = Dimens.SpaceSm,
                                    vertical = Dimens.SpaceXs
                                )
                            )
                        }
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = colors.surfaceVariant,
                            contentColor = colors.onSurfaceVariant
                        ) {
                            Text(
                                article.category.label(),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(
                                    horizontal = Dimens.SpaceSm,
                                    vertical = Dimens.SpaceXs
                                )
                            )
                        }
                    }
                    Text(
                        article.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.onBackground
                    )
                    if (full.isNotEmpty()) {
                        Text(
                            full,
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onSurfaceVariant
                        )
                    }
                    Text(
                        text = article.summary.ifBlank { stringResource(R.string.news_summary_missing) },
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (article.summary.isBlank()) colors.onSurfaceVariant else colors.onSurface,
                    )
                    Button(
                        onClick = { onOpenInApp(article.articleUrl) },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(Dimens.SpaceSm))
                        Text(
                            stringResource(R.string.news_open_article),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    // Attribution: the summary comes from the publisher's public feed, the article stays on their site.
                    Text(
                        stringResource(
                            R.string.news_attribution,
                            article.source.title,
                            OfficialDomain.hostOf(article.articleUrl) ?: article.source.title
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}