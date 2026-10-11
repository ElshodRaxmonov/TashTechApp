package com.mrdev.tashtech.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserEventDao {
    @Query("SELECT * FROM user_events ORDER BY epochDay, startMinute")
    fun observeAll(): Flow<List<UserEventEntity>>

    @Insert suspend fun insert(entity: UserEventEntity): Long
    @Update
    suspend fun update(entity: UserEventEntity): Int // rows changed; 0 = no such event

    @Query("DELETE FROM user_events WHERE id = :id")
    suspend fun delete(id: Long)
}