package com.mrdev.tashtech.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrdev.tashtech.data.repository.NewsRepository
import com.mrdev.tashtech.domain.model.NewsArticle
import com.mrdev.tashtech.domain.model.NewsCategory
import com.mrdev.tashtech.domain.model.NewsSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class NewsUiState(
    val filters: NewsFilters = NewsFilters(),
    val items: List<NewsArticle> = emptyList(),
    val categories: List<NewsCategory> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val failed: Boolean = false,
    val staleSources: Set<NewsSource> = emptySet(),
)

class NewsViewModel(private val repository: NewsRepository) : ViewModel() {

    private data class Status(
        val loading: Boolean = true,
        val refreshing: Boolean = false,
        val failed: Boolean = false
    )

    private val filters = MutableStateFlow(NewsFilters())
    private val status = MutableStateFlow(Status())

    internal val uiState: StateFlow<NewsUiState> = combine(
        repository.articles, repository.saved, filters, status, repository.failedSources,
    ) { feed, saved, f, st, failedSources ->
        val savedIds = saved.mapTo(HashSet()) { it.id }
        // Saved-only reads the database snapshot, so saved items outlive any refresh.
        val base = if (f.savedOnly) saved else feed.map { it.copy(saved = it.id in savedIds) }
        val bySource = base.filter { f.source == null || it.source == f.source }
        val categories = availableCategories(base, f.source)
        val category = f.category?.takeIf { it in categories }
        val items = bySource.filter { category == null || it.category == category }
        NewsUiState(
            filters = f.copy(category = category),
            items = items,
            categories = categories,
            loading = st.loading && feed.isEmpty(),
            refreshing = st.refreshing,
            failed = st.failed && feed.isEmpty(),
            staleSources = if (feed.isNotEmpty()) failedSources else emptySet(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NewsUiState())

    init {
        load(force = false)
    }

    fun refresh() = load(force = true)

    private fun load(force: Boolean) {
        status.update { it.copy(refreshing = !it.loading, failed = false) }
        viewModelScope.launch {
            val ok = repository.refresh(force)
            status.value = Status(loading = false, refreshing = false, failed = !ok)
        }
    }

    fun setSource(source: NewsSource?) = filters.update { it.copy(source = source) }
    fun setCategory(category: NewsCategory?) = filters.update { it.copy(category = category) }
    fun toggleSavedOnly() = filters.update { it.copy(savedOnly = !it.savedOnly) }

    fun toggleSaved(article: NewsArticle) {
        viewModelScope.launch { repository.setSaved(article, !article.saved) }
    }
}