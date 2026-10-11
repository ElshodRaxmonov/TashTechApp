package com.mrdev.tashtech.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders")
    fun observeAll(): Flow<List<ReminderEntity>>
    @Query("SELECT COUNT(*) FROM reminders WHERE completed = 0")
    fun observeActiveCount(): Flow<Int>
    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun get(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE completed = 0")
    suspend fun getActive(): List<ReminderEntity>

    /** id = 0 inserts; an existing id replaces the row. Returns the row id either way. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: ReminderEntity): Long

    @Query("UPDATE reminders SET completed = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM reminders WHERE completed = 1")
    suspend fun deleteCompleted()
}