package com.mrdev.tashtech.tashbox.leaderboard

import com.mrdev.tashtech.tashbox.puzzle.Difficulty
import kotlinx.serialization.Serializable

@Serializable
enum class GameMode { SOLO, MULTIPLAYER }

@Serializable
data class PlayerResult(
    val playerId: String,
    val name: String,
    val score: Int,
    val correctAnswers: Int,
    val totalRounds: Int,
    /** 1 = winner. Players with identical score and correct answers share a rank. */
    val rank: Int,
    val leftEarly: Boolean = false
) {
    val accuracy: Float get() = if (totalRounds == 0) 0f else correctAnswers.toFloat() / totalRounds
}

/**
 * Clean, framework-free result of a finished match. This is what you hand to the app's
 * university-wide leaderboard (see INTEGRATION.md) - TashBox itself never touches that leaderboard.
 */
@Serializable
data class GameResult(
    val matchId: String,
    val mode: GameMode,
    val seed: Long,
    val totalRounds: Int,
    val standings: List<PlayerResult>,
    /** True when the match was cut short (host left / everybody left). */
    val endedEarly: Boolean = false,
    val finishedAtEpochMillis: Long = 0L,
    val level: Difficulty = Difficulty.MEDIUM
) {
    fun playerResult(playerId: String): PlayerResult? = standings.firstOrNull { it.playerId == playerId }
    val winner: PlayerResult? get() = standings.firstOrNull()
}
