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

sealed interface NewsDetailsState {
    data object Loading : NewsDetailsState
    data object NotFound : NewsDetailsState
    data class Found(val article: NewsArticle) : NewsDetailsState
}

class NewsDetailsViewModel(private val repository: NewsRepository, id: String) : ViewModel() {

    private val base = MutableStateFlow<NewsArticle?>(null)
    private val searched = MutableStateFlow(false)

    val state: StateFlow<NewsDetailsState> =
        combine<NewsArticle?, Boolean, Set<String>, NewsDetailsState>(
            base, searched, repository.savedIds,
        ) { article, done, ids ->
            when {
                article != null -> NewsDetailsState.Found(article.copy(saved = article.id in ids))
                done -> NewsDetailsState.NotFound
                else -> NewsDetailsState.Loading
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NewsDetailsState.Loading)

    init {
        viewModelScope.launch {
            base.value = repository.find(id)
            searched.value = true
        }
    }

    fun toggleSaved() {
        val article = (state.value as? NewsDetailsState.Found)?.article ?: return
        viewModelScope.launch { repository.setSaved(article, !article.saved) }
    }
}