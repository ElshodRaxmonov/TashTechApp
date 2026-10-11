package com.mrdev.tashtech.tashbox.session

import kotlinx.coroutines.flow.StateFlow

/** What the UI talks to. Solo, Host and Client sessions all look identical from the outside. */
interface GameSession {
    val state: StateFlow<GameUiState>
    fun submitAnswer(optionIndex: Int)

    /** Solo only: leave the round-result screen. Multiplayer rounds advance on the host's timer. */
    fun continueToNext() {}
    fun close()
}

internal const val TICK_MS = 50L
internal const val RECONNECT_GRACE_MS = 10_000L

internal const val WRONG_WIFI_MESSAGE =
    "Make sure all players are connected\nto the same Wi-Fi network.\n\n" +
        "If the network blocks local devices,\ntry using the host's mobile hotspot."
