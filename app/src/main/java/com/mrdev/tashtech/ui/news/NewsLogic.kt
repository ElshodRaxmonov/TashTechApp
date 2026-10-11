package com.mrdev.tashtech.ui.news

import com.mrdev.tashtech.domain.model.NewsArticle
import com.mrdev.tashtech.domain.model.NewsCategory
import com.mrdev.tashtech.domain.model.NewsSource

internal data class NewsFilters(
    val source: NewsSource? = null,
    val category: NewsCategory? = null,
    val savedOnly: Boolean = false,
)

internal fun availableCategories(articles: List<NewsArticle>, source: NewsSource?): List<NewsCategory> =
    articles.filter { source == null || it.source == source }.map { it.category }.distinct().sortedBy { it.ordinal }

/** A short, mixed set for Home: round-robin across sources so one publisher never dominates. */
internal fun curate(articles: List<NewsArticle>, count: Int = 6): List<NewsArticle> {
    val bySource = NewsSource.entries.map { s -> articles.filter { it.source == s }.sortedByDescending { it.publishedAtMillis } }
    val picked = mutableListOf<NewsArticle>()
    var round = 0
    while (picked.size < count && bySource.any { round < it.size }) {
        bySource.forEach { list -> if (picked.size < count && round < list.size) picked += list[round] }
        round++
    }
    return picked.sortedByDescending { it.publishedAtMillis }
}