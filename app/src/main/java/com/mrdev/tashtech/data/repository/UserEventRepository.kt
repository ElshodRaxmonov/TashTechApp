package com.mrdev.tashtech.data.repository

import com.mrdev.tashtech.data.local.UserEventDao
import com.mrdev.tashtech.data.local.toDomain
import com.mrdev.tashtech.data.local.toEntity
import com.mrdev.tashtech.domain.model.UserEvent
import com.mrdev.tashtech.domain.model.isValid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class EventSaveResult { Created, Updated, NotFound, Invalid }

class UserEventRepository(private val dao: UserEventDao) {
    fun observeAll(): Flow<List<UserEvent>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    /** id == 0 creates; otherwise updates that exact event. Never creates a duplicate, never revives a deleted one. */
    suspend fun save(event: UserEvent): EventSaveResult {
        if (!event.isValid()) return EventSaveResult.Invalid
        return when {
            event.id == 0L -> { dao.insert(event.toEntity()); EventSaveResult.Created }
            dao.update(event.toEntity()) > 0 -> EventSaveResult.Updated
            else -> EventSaveResult.NotFound
        }
    }

    suspend fun delete(id: Long) = dao.delete(id)
}