package com.mrdev.tashtech.tashbox.puzzle

import kotlinx.serialization.Serializable

enum class RoundType { PREDICT, ATTRIBUTE, SNAPSHOT, FAULT, FINAL }

enum class QuestionKind {
    /** Pick the grid the black box would output for [Puzzle.queryInput]. */
    PREDICT_OUTPUT,

    /** Memory: [Puzzle.flashGrid] flashes, then [Puzzle.shownGrid] appears. Which node changed? */
    FIND_CHANGED_NODE,

    /** One of the examples does not follow the rule. Which one? */
    FIND_FAULTY_EXAMPLE
}

@Serializable
data class Example(val input: Grid, val output: Grid)

/**
 * One answer choice. Exactly one of [grid] / [node] is set for PREDICT and SNAPSHOT puzzles;
 * for FAULT puzzles both are null and option i simply means "example i + 1".
 */
@Serializable
data class AnswerOption(val label: String, val grid: Grid? = null, val node: Int? = null)

@Serializable
data class Puzzle(
    val id: String,
    val round: Int,
    val roundType: RoundType,
    val question: QuestionKind,
    val difficulty: Int,
    val examples: List<Example> = emptyList(),
    val queryInput: Grid? = null,
    val flashGrid: Grid? = null,
    val shownGrid: Grid? = null,
    val flashMillis: Long = 0L,
    val answerMillis: Long,
    val options: List<AnswerOption>,
    /** Index into [options]. Host-only: clients receive [redacted] puzzles with -1. */
    val correctIndex: Int,
    val metadata: Map<String, String> = emptyMap(),
    val level: Difficulty = Difficulty.MEDIUM
) {
    /** The version that is sent over the network: no answer, no rule description. */
    fun redacted(): Puzzle = copy(correctIndex = -1, metadata = emptyMap())
}
