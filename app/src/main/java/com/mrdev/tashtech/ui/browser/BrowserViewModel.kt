package com.mrdev.tashtech.ui.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrdev.tashtech.data.repository.NoteRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class BrowserViewModel(private val notes: NoteRepository) : ViewModel() {
    private val results = Channel<Boolean>(Channel.BUFFERED)
    val noteResults: Flow<Boolean> = results.receiveAsFlow()

    fun saveNote(text: String, sourceUrl: String?) {
        viewModelScope.launch {
            val ok = try {
                notes.add(text, sourceUrl); true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            results.send(ok)
        }
    }
}