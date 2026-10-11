package com.mrdev.tashtech.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
enum class NewsSource(val title: String, val feedUrl: String) {
    IeeeSpectrum("IEEE Spectrum", "https://spectrum.ieee.org/customfeeds/feed/all-topics/rss"),
    ArsTechnica("Ars Technica", "https://feeds.arstechnica.com/arstechnica/index"),
    TechCrunch("TechCrunch", "https://techcrunch.com/feed/"),
}

/** Order is the display order of the category filter. */
@Serializable
enum class NewsCategory { AI, Robotics, Computing, Hardware, Semiconductors, Energy, Space, Security, Science, Transportation, Software, Startups, Telecom, Tech }

@Serializable
data class NewsArticle(
    val id: String,
    val title: String,
    val summary: String,
    val imageUrl: String? = null,
    val articleUrl: String,
    val source: NewsSource,
    val publishedAtMillis: Long, // 0 = unknown
    val category: NewsCategory,
    @Transient val saved: Boolean = false,
)