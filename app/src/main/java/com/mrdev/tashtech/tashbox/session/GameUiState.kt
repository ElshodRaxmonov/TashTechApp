package com.mrdev.tashtech.tashbox.session

import com.mrdev.tashtech.tashbox.engine.RoundResultData
import com.mrdev.tashtech.tashbox.leaderboard.GameResult
import com.mrdev.tashtech.tashbox.puzzle.Difficulty
import com.mrdev.tashtech.tashbox.puzzle.Puzzle

enum class SessionRole { SOLO, HOST, CLIENT }

enum class UiPhase {
    CONNECTING, CONNECT_FAILED, LOBBY, COUNTDOWN, FLASH, ANSWERING, ROUND_RESULT, FINISHED, HOST_LEFT
}

data class UiPlayer(
    val id: String,
    val name: String,
    val score: Int = 0,
    val isYou: Boolean = false,
    val isHost: Boolean = false,
    val ready: Boolean = false,
    val connected: Boolean = true,
    /** Locked in an answer this round (the answer itself stays hidden until the round ends). */
    val answered: Boolean = false,
    /** Only set after the round result: was this player right? */
    val lastCorrect: Boolean? = null
)

/** Single immutable state object that every screen renders (solo, host and client alike). */
data class GameUiState(
    val role: SessionRole,
    val phase: UiPhase = UiPhase.CONNECTING,
    val round: Int = 0,
    val totalRounds: Int = 5,
    val score: Int = 0,
    val remainingMillis: Long = 0,
    val flashRemainingMillis: Long = 0,
    val countdownSeconds: Int = 0,
    val puzzle: Puzzle? = null,
    val selectedAnswer: Int? = null,
    val players: List<UiPlayer> = emptyList(),
    val myPlayerId: String = "",
    val roundResult: RoundResultData? = null,
    val result: GameResult? = null,
    val roomId: String = "",
    val roomName: String = "",
    val qrPayload: String? = null,
    val canStart: Boolean = false,
    /** Short banner, e.g. "Madina disconnected. Reconnecting...". */
    val notice: String? = null,
    /** Body text for CONNECT_FAILED / HOST_LEFT. */
    val errorMessage: String? = null,
    val level: Difficulty = Difficulty.MEDIUM,
    /** My result per finished round (true = correct), used by the progress bar and results. */
    val roundHistory: List<Boolean> = emptyList()
)
