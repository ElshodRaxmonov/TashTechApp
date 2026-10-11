package com.mrdev.tashtech.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val text: String,
    val sourceUrl: String,
    val createdAtMillis: Long,
)

@Dao
interface NoteDao {
    @Query("SELECT COUNT(*) FROM notes")
    fun observeCount(): Flow<Int>
    @Query("SELECT * FROM notes ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<NoteEntity>>
    @Query("UPDATE notes SET text = :text WHERE id = :id")
    suspend fun updateText(id: Long, text: String)
    @Insert
    suspend fun insert(entity: NoteEntity): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)
}