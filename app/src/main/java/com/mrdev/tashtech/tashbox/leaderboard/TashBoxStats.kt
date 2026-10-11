package com.mrdev.tashtech.tashbox.leaderboard

import android.content.Context
import com.mrdev.tashtech.tashbox.puzzle.Difficulty

/**
 * Tiny local stats store (SharedPreferences). Independent of the app's leaderboard: it powers "Best" per
 * difficulty, remembers the player's name / last difficulty and whether the tutorial was seen.
 */
class TashBoxStats(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("tashbox_stats", Context.MODE_PRIVATE)

    data class Summary(
        val previousBest: Int,
        val bestScore: Int,
        val isNewBest: Boolean,
        val matchesPlayed: Int,
        val wins: Int
    )

    var playerName: String
        get() = prefs.getString(KEY_NAME, "").orEmpty()
        set(value) { prefs.edit().putString(KEY_NAME, value).apply() }

    var tutorialDone: Boolean
        get() = prefs.getBoolean(KEY_TUTORIAL, false)
        set(value) { prefs.edit().putBoolean(KEY_TUTORIAL, value).apply() }

    /** Defaults to EASY so first-time players start gently. */
    var lastLevel: Difficulty
        get() = runCatching { Difficulty.valueOf(prefs.getString(KEY_LEVEL, null) ?: "EASY") }.getOrDefault(Difficulty.EASY)
        set(value) { prefs.edit().putString(KEY_LEVEL, value.name).apply() }

    fun bestSolo(level: Difficulty): Int = prefs.getInt(bestKey(GameMode.SOLO, level), 0)

    fun bestSoloAll(): Map<Difficulty, Int> = Difficulty.values().associateWith { bestSolo(it) }

    fun record(result: GameResult, myPlayerId: String): Summary {
        val mine = result.playerResult(myPlayerId)
        val key = bestKey(result.mode, result.level)
        val previous = prefs.getInt(key, 0)
        val matches = prefs.getInt(KEY_MATCHES, 0)
        val wins = prefs.getInt(KEY_WINS, 0)
        if (mine == null || result.endedEarly) return Summary(previous, previous, false, matches, wins)

        val best = maxOf(previous, mine.score)
        val won = result.mode == GameMode.MULTIPLAYER && result.standings.size > 1 && mine.rank == 1
        prefs.edit()
            .putInt(key, best)
            .putInt(KEY_MATCHES, matches + 1)
            .putInt(KEY_WINS, wins + if (won) 1 else 0)
            .putInt(KEY_CORRECT, prefs.getInt(KEY_CORRECT, 0) + mine.correctAnswers)
            .putInt(KEY_ROUNDS, prefs.getInt(KEY_ROUNDS, 0) + mine.totalRounds)
            .apply()
        return Summary(previous, best, mine.score > previous, matches + 1, wins + if (won) 1 else 0)
    }

    private fun bestKey(mode: GameMode, level: Difficulty) = "best_${mode.name.lowercase()}_${level.name.lowercase()}"

    private companion object {
        const val KEY_NAME = "player_name"
        const val KEY_TUTORIAL = "tutorial_done"
        const val KEY_LEVEL = "last_level"
        const val KEY_MATCHES = "matches"
        const val KEY_WINS = "wins"
        const val KEY_CORRECT = "total_correct"
        const val KEY_ROUNDS = "total_rounds"
    }
}
