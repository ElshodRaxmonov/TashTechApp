package com.mrdev.tashtech.ui.news

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrdev.tashtech.R
import com.mrdev.tashtech.ui.AppViewModelProvider
import com.mrdev.tashtech.ui.home.EmptyDayCard
import com.mrdev.tashtech.ui.theme.Dimens

@Composable
fun HomeNewsSection(
    onOpenAll: () -> Unit,
    onOpenArticle: (String) -> Unit,
    viewModel: NewsPreviewViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.news_section_title),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onBackground,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onOpenAll) { Text(stringResource(R.string.home_see_all)) }
        }
        when (val s = state) {
            NewsPreviewState.Loading -> Surface(
                shape = MaterialTheme.shapes.large,
                color = colors.surfaceContainer,
                border = BorderStroke(Dimens.BorderThin, colors.outlineVariant),
                modifier = Modifier.fillMaxWidth().height(NewsCardHeight),
            ) {
                Box(contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) }
            }
            NewsPreviewState.Empty -> EmptyDayCard(
                stringResource(R.string.news_empty_title),
                stringResource(R.string.news_empty_body),
            )
            NewsPreviewState.Error -> Surface(
                shape = MaterialTheme.shapes.large,
                color = colors.surfaceContainer,
                border = BorderStroke(Dimens.BorderThin, colors.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(Dimens.CardPadding), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                    Text(stringResource(R.string.news_error), style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = viewModel::refresh) { Text(stringResource(R.string.common_retry)) }
                }
            }
            is NewsPreviewState.Content -> NewsCardStack(
                articles = s.items,
                onOpen = { onOpenArticle(it.id) },
                onToggleSaved = viewModel::toggleSaved,
            )
        }
    }
}