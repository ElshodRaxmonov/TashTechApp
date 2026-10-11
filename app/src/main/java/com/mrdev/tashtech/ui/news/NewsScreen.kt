package com.mrdev.tashtech.ui.news

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrdev.tashtech.R
import com.mrdev.tashtech.domain.model.NewsSource
import com.mrdev.tashtech.ui.AppViewModelProvider
import com.mrdev.tashtech.ui.components.LoadingScreen
import com.mrdev.tashtech.ui.components.adaptiveScreenPadding
import com.mrdev.tashtech.ui.home.EmptyDayCard
import com.mrdev.tashtech.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    onBack: () -> Unit,
    onOpenArticle: (String) -> Unit,
    viewModel: NewsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pad = adaptiveScreenPadding()
    val colors = MaterialTheme.colorScheme

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.news_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = pad),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                item {
                    FilterChip(
                        selected = state.filters.savedOnly,
                        onClick = viewModel::toggleSavedOnly,
                        label = { Text(stringResource(R.string.news_filter_saved)) },
                        leadingIcon = { Icon(Icons.Outlined.BookmarkBorder, null, Modifier.size(18.dp)) },
                    )
                }
                item {
                    FilterChip(
                        selected = state.filters.source == null,
                        onClick = { viewModel.setSource(null) },
                        label = { Text(stringResource(R.string.news_filter_all)) },
                    )
                }
                items(NewsSource.entries) { source ->
                    FilterChip(
                        selected = state.filters.source == source,
                        onClick = { viewModel.setSource(source) },
                        label = { Text(source.title) },
                    )
                }
            }
            if (state.categories.size > 1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = pad),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
                ) {
                    item {
                        FilterChip(
                            selected = state.filters.category == null,
                            onClick = { viewModel.setCategory(null) },
                            label = { Text(stringResource(R.string.news_filter_all)) },
                        )
                    }
                    items(state.categories) { category ->
                        FilterChip(
                            selected = state.filters.category == category,
                            onClick = { viewModel.setCategory(category) },
                            label = { Text(category.label()) },
                        )
                    }
                }
            }

            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                when {
                    state.items.isNotEmpty() -> LazyColumn(
                        contentPadding = PaddingValues(start = pad, end = pad, top = Dimens.SpaceSm, bottom = Dimens.SpaceXl),
                        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
                    ) {
                        if (state.staleSources.isNotEmpty()) {
                            item(key = "stale") {
                                Text(
                                    stringResource(R.string.news_partial, state.staleSources.joinToString { it.title }),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                        }
                        items(state.items, key = { it.id }) { article ->
                            NewsListItem(
                                article = article,
                                onClick = { onOpenArticle(article.id) },
                                onToggleSaved = { viewModel.toggleSaved(article) },
                            )
                        }
                    }
                    state.loading -> LoadingScreen()
                    else -> Column( // scrollable, so pull-to-refresh also works on empty and error states
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(pad),
                        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
                    ) {
                        when {
                            state.failed -> {
                                Text(stringResource(R.string.news_error), style = MaterialTheme.typography.titleMedium)
                                OutlinedButton(onClick = viewModel::refresh) { Text(stringResource(R.string.common_retry)) }
                            }
                            state.filters.savedOnly -> EmptyDayCard(
                                stringResource(R.string.news_saved_empty_title),
                                stringResource(R.string.news_saved_empty_body),
                            )
                            else -> EmptyDayCard(
                                stringResource(R.string.news_empty_title),
                                stringResource(R.string.news_empty_body),
                            )
                        }
                    }
                }
            }
        }
    }
}