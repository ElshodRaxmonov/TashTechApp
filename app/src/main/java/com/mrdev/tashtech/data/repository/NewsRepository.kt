package com.mrdev.tashtech.data.repository

import com.mrdev.tashtech.data.local.SavedArticleDao
import com.mrdev.tashtech.data.local.toArticle
import com.mrdev.tashtech.data.local.toEntity
import com.mrdev.tashtech.data.news.NewsApi
import com.mrdev.tashtech.domain.model.NewsArticle
import com.mrdev.tashtech.domain.model.NewsSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock

private const val FEED_TTL_MS = 15 * 60 * 1000L
private const val PER_SOURCE_LIMIT = 40
private const val TOTAL_LIMIT = 120

/** Fetch, merge, cache and save. One instance is shared by Home, News and Details. */
class NewsRepository(
    private val dao: SavedArticleDao,
    private val cacheFile: File,
    private val api: NewsApi,
    private val clock: java.time.Clock = java.time.Clock.systemUTC(),
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val refreshLock = Mutex()
    private val diskLock = Mutex()
    private var diskLoaded = false
    @Volatile private var lastFetchMillis = 0L // already exists: make it @Volatile


    private val _articles = MutableStateFlow<List<NewsArticle>>(emptyList())
    val articles: StateFlow<List<NewsArticle>> = _articles.asStateFlow()

    private val _failedSources = MutableStateFlow<Set<NewsSource>>(emptySet())
    val failedSources: StateFlow<Set<NewsSource>> = _failedSources.asStateFlow()

    val saved: Flow<List<NewsArticle>> = dao.observeAll().map { list -> list.map { it.toArticle() } }
    val savedIds: Flow<Set<String>> = dao.observeIds().map { it.toSet() }

    /**
     * Cached articles appear immediately; the network is hit at most once per TTL unless [force].
     * Returns false only when every source failed (cached content, if any, stays visible).
     */
    suspend fun refresh(force: Boolean = false): Boolean = refreshLock.withLock {
        loadDiskCache()
        val fresh = clock.millis() - lastFetchMillis < FEED_TTL_MS
        if (!force && fresh && _articles.value.isNotEmpty()) return@withLock true

        val results = coroutineScope {
            NewsSource.entries.map { s -> async { s to attempt { api.fetch(s) } } }.awaitAll()
        }
        val succeeded = results.filter { it.second != null }
        if (succeeded.isEmpty()) {
            _failedSources.value = NewsSource.entries.toSet()
            return@withLock false
        }
        val failed = results.filter { it.second == null }.map { it.first }.toSet()
        val fetched = succeeded.flatMap { it.second.orEmpty().take(PER_SOURCE_LIMIT) }
        val kept = _articles.value.filter { it.source in failed } // a failing source keeps its last good items
        val merged = (fetched + kept).distinctBy { it.id }
            .sortedByDescending { it.publishedAtMillis }.take(TOTAL_LIMIT)

        _articles.value = merged
        _failedSources.value = failed
        lastFetchMillis = clock.millis()
        writeDiskCache(merged)
        true
    }

    suspend fun find(id: String): NewsArticle? {
        loadDiskCache()
        return _articles.value.firstOrNull { it.id == id } ?: dao.get(id)?.toArticle()
    }

    suspend fun setSaved(article: NewsArticle, saved: Boolean) {
        if (saved) dao.upsert(article.toEntity(clock.millis())) else dao.delete(article.id)
    }
    suspend fun cachedArticles(): List<NewsArticle> {
        loadDiskCache()
        return _articles.value
    }
    private suspend fun <T> attempt(block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private suspend fun loadDiskCache() = diskLock.withLock {
        if (diskLoaded) return@withLock
        diskLoaded = true
        if (_articles.value.isNotEmpty()) return@withLock
        val cached = withContext(io) {
            try {
                if (cacheFile.exists()) json.decodeFromString<List<NewsArticle>>(cacheFile.readText()) else emptyList()
            } catch (e: Exception) { // corrupt cache is not an error: the next refresh rebuilds it
                emptyList()
            }
        }
        if (_articles.value.isEmpty()) _articles.value = cached
        if (cached.isNotEmpty()) lastFetchMillis = cacheFile.lastModified()
    }
    fun cacheAgeMillis(): Long = if (lastFetchMillis == 0L) Long.MAX_VALUE else clock.millis() - lastFetchMillis

    private suspend fun writeDiskCache(list: List<NewsArticle>) = withContext(io) {
        try {
            cacheFile.parentFile?.mkdirs()
            cacheFile.writeText(json.encodeToString(list))
        } catch (e: Exception) { /* caching is best-effort */ }
    }
}