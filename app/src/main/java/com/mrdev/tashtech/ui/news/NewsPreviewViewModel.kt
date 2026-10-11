package com.mrdev.tashtech.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrdev.tashtech.data.repository.NewsRepository
import com.mrdev.tashtech.domain.model.NewsArticle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.collections.isNotEmpty

sealed interface NewsPreviewState {
    data object Loading : NewsPreviewState
    data object Error : NewsPreviewState
    data object Empty : NewsPreviewState
    data class Content(val items: List<NewsArticle>) : NewsPreviewState
}

class NewsPreviewViewModel(private val repository: NewsRepository) : ViewModel() {

    private enum class Load { Loading, Done, Failed }
    private val load = MutableStateFlow(Load.Loading)

    val uiState: StateFlow<NewsPreviewState> = combine(repository.articles, repository.savedIds, load) { feed, ids, l ->
        val items = curate(feed.map { it.copy(saved = it.id in ids) } as List<NewsArticle>)
        when {
            items.isNotEmpty() -> NewsPreviewState.Content(items) // cached content stays visible even if a refresh failed
            l == Load.Loading -> NewsPreviewState.Loading
            l == Load.Failed -> NewsPreviewState.Error
            else -> NewsPreviewState.Empty
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NewsPreviewState.Loading)

    init { refresh() }

    fun refresh() {
        load.value = Load.Loading
        viewModelScope.launch { load.value = if (repository.refresh()) Load.Done else Load.Failed }
    }

    fun toggleSaved(article: NewsArticle) {
        viewModelScope.launch { repository.setSaved(article, !article.saved) }
    }
}