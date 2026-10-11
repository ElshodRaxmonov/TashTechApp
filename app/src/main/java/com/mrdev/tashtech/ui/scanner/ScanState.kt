package com.mrdev.tashtech.ui.scanner

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed interface ScanState {
    data object Scanning : ScanState
    data class Result(val content: QrContent) : ScanState
}

class QrScannerViewModel : ViewModel() {
    private val _state = MutableStateFlow<ScanState>(ScanState.Scanning)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    /** The first detection wins; later frames are ignored until [rescan]. */
    fun onQrDetected(raw: String) = _state.update {
        if (it is ScanState.Scanning) ScanState.Result(classifyQr(raw)) else it
    }

    fun rescan() = _state.update { ScanState.Scanning }
}