package com.mrdev.tashtech.tashbox.puzzle

import kotlinx.serialization.Serializable

/**
 * Easy:   3x3 grids, one simple rule per round (even in the Final), 4 examples, twice the time.
 * Medium: the classic TashBox (3x3 / 4x4 grids, Final = two rules).
 * Hard:   4x4 grids, a 5x5 memory round, every kind of rule, 20% less time.
 * Scoring is identical on every level; best scores are kept per level.
 */
@Serializable
enum class Difficulty(val label: String, val tagline: String) {
    EASY("Easy", "Simple rules, more time"),
    MEDIUM("Medium", "The classic TashBox"),
    HARD("Hard", "Bigger grids, less time")
}
