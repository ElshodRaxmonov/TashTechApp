package com.mrdev.tashtech.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrdev.tashtech.data.repository.NoteRepository
import com.mrdev.tashtech.domain.model.Note
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(private val repository: NoteRepository) : ViewModel() {
    /** null while loading. */
    val notes: StateFlow<List<Note>?> = repository.observeAll()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }
    /** id == null creates a new note; otherwise edits that note's text (its source link is kept). */
    fun save(id: Long?, text: String) {
        viewModelScope.launch {
            if (id == null) repository.add(text, sourceUrl = null) else repository.update(id, text)
        }
    }
}