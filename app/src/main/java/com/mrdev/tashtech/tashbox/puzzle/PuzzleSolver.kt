package com.mrdev.tashtech.tashbox.puzzle

/**
 * Deterministic validator. It never looks at the rule the generator used; it re-derives the answer
 * from the visible data only, by checking every rule in the hypothesis space.
 *
 *  - PREDICT: every rule consistent with the examples must give the SAME prediction, and exactly
 *    one option may equal it.
 *  - SNAPSHOT: exactly one node differs, and exactly one option points at it.
 *  - FAULT: exactly one example is the odd one out under every almost-fitting rule, and no rule
 *    explains all examples.
 */
object PuzzleSolver {
    const val OPTION_COUNT = 4

    /** Easy puzzles only ever use ONE rule (even in the Final), so only single rules are candidates there. */
    fun hypothesisSpace(round: Int, level: Difficulty = Difficulty.MEDIUM): List<Rule> =
        if (round >= 5 && level != Difficulty.EASY) RuleLibrary.all else RuleLibrary.singles

    fun consistentRules(space: List<Rule>, examples: List<Example>): List<Rule> =
        space.filter { rule -> examples.all { rule.apply(it.input) == it.output } }

    /** The unique correct option index, or null if the puzzle is ambiguous / malformed. */
    fun solve(p: Puzzle): Int? {
        if (p.options.size != OPTION_COUNT) return null
        return when (p.question) {
            QuestionKind.PREDICT_OUTPUT -> solvePredict(p)
            QuestionKind.FIND_CHANGED_NODE -> solveSnapshot(p)
            QuestionKind.FIND_FAULTY_EXAMPLE -> solveFault(p)
        }
    }

    fun isValid(p: Puzzle): Boolean = solve(p).let { it != null && it == p.correctIndex }

    private fun solvePredict(p: Puzzle): Int? {
        val query = p.queryInput ?: return null
        if (p.examples.size < 3) return null
        val consistent = consistentRules(hypothesisSpace(p.round, p.level), p.examples)
        if (consistent.isEmpty()) return null
        val predicted = consistent.map { it.apply(query) }.toSet().singleOrNull() ?: return null
        val grids = p.options.map { it.grid ?: return null }
        if (grids.toSet().size != grids.size) return null
        return grids.indexOf(predicted).takeIf { it >= 0 }
    }

    private fun solveSnapshot(p: Puzzle): Int? {
        val before = p.flashGrid ?: return null
        val after = p.shownGrid ?: return null
        if (before.size != after.size) return null
        val changed = before.diffIndices(after).singleOrNull() ?: return null
        val nodes = p.options.map { it.node ?: return null }
        if (nodes.toSet().size != nodes.size) return null
        if (nodes.any { it !in before.cells.indices }) return null
        return nodes.indexOf(changed).takeIf { it >= 0 }
    }

    private fun solveFault(p: Puzzle): Int? {
        if (p.examples.size != p.options.size) return null
        val suspects = HashSet<Int>()
        for (rule in hypothesisSpace(p.round, p.level)) {
            val misfits = p.examples.indices.filter { rule.apply(p.examples[it].input) != p.examples[it].output }
            if (misfits.isEmpty()) return null // some rule explains every example: nothing is "faulty"
            if (misfits.size == 1) suspects += misfits[0]
        }
        return suspects.singleOrNull()
    }
}
