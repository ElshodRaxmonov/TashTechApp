package com.mrdev.tashtech.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.mrdev.tashtech.domain.model.NewsArticle
import com.mrdev.tashtech.domain.model.NewsCategory
import com.mrdev.tashtech.domain.model.NewsSource
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_articles")
data class SavedArticleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val summary: String,
    val imageUrl: String?,
    val articleUrl: String,
    val source: String,
    val category: String,
    val publishedAtMillis: Long,
    val savedAtMillis: Long,
)

@Dao
interface SavedArticleDao {
    @Query("SELECT * FROM saved_articles ORDER BY savedAtMillis DESC")
    fun observeAll(): Flow<List<SavedArticleEntity>>

    @Query("SELECT id FROM saved_articles")
    fun observeIds(): Flow<List<String>>

    @Query("SELECT * FROM saved_articles WHERE id = :id")
    suspend fun get(id: String): SavedArticleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SavedArticleEntity)

    @Query("DELETE FROM saved_articles WHERE id = :id")
    suspend fun delete(id: String)
}

internal fun SavedArticleEntity.toArticle() = NewsArticle(
    id = id, title = title, summary = summary, imageUrl = imageUrl, articleUrl = articleUrl,
    source = NewsSource.entries.firstOrNull { it.name == source } ?: NewsSource.TechCrunch,
    publishedAtMillis = publishedAtMillis,
    category = NewsCategory.entries.firstOrNull { it.name == category } ?: NewsCategory.Tech,
    saved = true,
)

internal fun NewsArticle.toEntity(savedAt: Long) = SavedArticleEntity(
    id, title, summary, imageUrl, articleUrl, source.name, category.name, publishedAtMillis, savedAt,
)