package com.mrdev.tashtech.tashbox.scoring

import kotlin.math.roundToInt

data class RoundScore(val base: Int, val speedBonus: Int) {
    val total: Int get() = base + speedBonus
}

/**
 * correct   = 100 x round multiplier  (1.0, 1.0, 1.0, 1.25, 2.0)
 * speed     = up to +25, shrinking linearly over the answer window (correct answers only)
 * wrong / no answer = 0, never negative
 */
object ScoreCalculator {
    const val BASE_POINTS = 100
    const val MAX_SPEED_BONUS = 25
    private val MULTIPLIERS = doubleArrayOf(1.0, 1.0, 1.0, 1.25, 2.0)

    fun multiplier(round: Int): Double = MULTIPLIERS[(round - 1).coerceIn(0, MULTIPLIERS.lastIndex)]

    fun score(round: Int, correct: Boolean, elapsedMillis: Long, windowMillis: Long): RoundScore {
        if (!correct) return RoundScore(0, 0)
        val base = (BASE_POINTS * multiplier(round)).roundToInt()
        val fraction = if (windowMillis <= 0) 0.0
        else 1.0 - elapsedMillis.coerceIn(0L, windowMillis).toDouble() / windowMillis
        return RoundScore(base, (MAX_SPEED_BONUS * fraction).roundToInt())
    }

    /** No answer before the deadline. */
    fun timeout(): RoundScore = RoundScore(0, 0)
}
