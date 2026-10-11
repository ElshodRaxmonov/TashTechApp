package com.mrdev.tashtech.data.repository

import com.mrdev.tashtech.data.local.NoteDao
import com.mrdev.tashtech.data.local.NoteEntity
import com.mrdev.tashtech.domain.model.MAX_NOTE_TEXT
import com.mrdev.tashtech.domain.model.Note
import com.mrdev.tashtech.domain.schedule.AppTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime

class NoteRepository(
    private val dao: NoteDao,
    private val clock: Clock = AppTime.systemClock(),
) {
    suspend fun update(id: Long, text: String) = dao.updateText(id, text.trim().take(MAX_NOTE_TEXT))
    fun observeAll(): Flow<List<Note>> = dao.observeAll().map { list ->
        list.map {
            Note(it.id, it.text, it.sourceUrl, LocalDateTime.ofInstant(Instant.ofEpochMilli(it.createdAtMillis), AppTime.ZONE))
        }
    }

    fun observeCount(): Flow<Int> = dao.observeCount()
    suspend fun add(text: String, sourceUrl: String?) {
        dao.insert(
            NoteEntity(
                text = text.trim().take(MAX_NOTE_TEXT),
                sourceUrl = sourceUrl.orEmpty(),
                createdAtMillis = clock.millis(),
            ),
        )
    }

    suspend fun delete(id: Long) = dao.delete(id)
}