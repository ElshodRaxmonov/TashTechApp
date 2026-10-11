package com.mrdev.tashtech.tashbox.puzzle

enum class RuleType {
    POSITION_SHIFT, COLOR_TRANSFORM, COUNT_CHANGE, ROTATION, SYMMETRY, NODE_RELATION, COMBINATION
}

enum class Axis { LEFT_RIGHT, UP_DOWN, DIAGONAL }

/** Name of a node value for explanations: 1 = ● circle, 2 = ■ square, 3 = ▲ triangle, 0 = dark. */
internal fun sym(v: Int): String = when (v) { 1 -> "●"; 2 -> "■"; 3 -> "▲"; else -> "dark" }

/**
 * A deterministic transformation Grid -> Grid. These are the hidden "black box" rules.
 * Rules are pure functions, which is what makes puzzle validation (see [PuzzleSolver]) possible.
 */
sealed class Rule {
    abstract val type: RuleType
    abstract val label: String
    /** Plain-English description shown to the player after a round (and in the tutorial). */
    abstract val explain: String
    abstract fun apply(g: Grid): Grid

    /** Moves every node [dx] columns right and [dy] rows down, wrapping around the edges. */
    data class Shift(val dx: Int, val dy: Int) : Rule() {
        override val type: RuleType get() = RuleType.POSITION_SHIFT
        override val label: String get() = "shift($dx,$dy)"
        override val explain: String get() {
            fun steps(n: Int) = if (n == 1) "1 step" else "$n steps"
            val parts = listOfNotNull(if (dx > 0) "${steps(dx)} right" else null, if (dy > 0) "${steps(dy)} down" else null)
            return "Slides every node ${parts.joinToString(" and ")}. Nodes that leave one edge come back on the opposite side."
        }
        override fun apply(g: Grid): Grid =
            Grid.build(g.size) { r, c -> g[(r - dy).mod(g.size), (c - dx).mod(g.size)] }
    }

    /** All lit nodes fall to the bottom of their column. */
    object GravityDown : Rule() {
        override val type: RuleType get() = RuleType.POSITION_SHIFT
        override val label: String get() = "gravity"
        override val explain: String get() = "Every lit node falls straight down as far as it can."
        override fun apply(g: Grid): Grid {
            val n = g.size
            val cols = List(n) { c -> (0 until n).map { r -> g[r, c] }.filter { it != 0 } }
            return Grid.build(n) { r, c ->
                val col = cols[c]
                val offset = n - col.size
                if (r >= offset) col[r - offset] else 0
            }
        }
    }

    data class ColorSwap(val a: Int, val b: Int) : Rule() {
        override val type: RuleType get() = RuleType.COLOR_TRANSFORM
        override val label: String get() = "swap($a,$b)"
        override val explain: String get() = "Swaps ${sym(a)} and ${sym(b)}: every ${sym(a)} becomes ${sym(b)} and every ${sym(b)} becomes ${sym(a)}."
        override fun apply(g: Grid): Grid =
            Grid(g.size, g.cells.map { v -> if (v == a) b else if (v == b) a else v })
    }

    data class ColorCycle(val forward: Boolean) : Rule() {
        override val type: RuleType get() = RuleType.COLOR_TRANSFORM
        override val label: String get() = if (forward) "cycle+" else "cycle-"
        override val explain: String get() =
            if (forward) "Every ● becomes ■, every ■ becomes ▲, and every ▲ becomes ●."
            else "Every ● becomes ▲, every ▲ becomes ■, and every ■ becomes ●."
        override fun apply(g: Grid): Grid = Grid(g.size, g.cells.map { v ->
            when {
                v == 0 -> 0
                forward -> v % 3 + 1
                else -> (v + 1) % 3 + 1
            }
        })
    }

    /** Rotates clockwise by [quarterTurns] x 90 degrees. */
    data class Rotate(val quarterTurns: Int) : Rule() {
        override val type: RuleType get() = RuleType.ROTATION
        override val label: String get() = "rotate($quarterTurns)"
        override val explain: String get() = when (quarterTurns) {
            1 -> "Turns the whole grid a quarter turn clockwise."
            2 -> "Turns the whole grid upside down (a half turn)."
            else -> "Turns the whole grid a quarter turn counter-clockwise."
        }
        override fun apply(g: Grid): Grid {
            var cur = g
            repeat(quarterTurns) {
                val prev = cur
                val s = prev.size
                cur = Grid.build(s) { r, c -> prev[s - 1 - c, r] }
            }
            return cur
        }
    }

    data class Mirror(val axis: Axis) : Rule() {
        override val type: RuleType get() = RuleType.SYMMETRY
        override val label: String get() = "mirror($axis)"
        override val explain: String get() = when (axis) {
            Axis.LEFT_RIGHT -> "Flips the grid left to right, like a mirror."
            Axis.UP_DOWN -> "Flips the grid top to bottom: the top row swaps with the bottom row."
            Axis.DIAGONAL -> "Flips the grid along its diagonal: rows become columns."
        }
        override fun apply(g: Grid): Grid {
            val s = g.size
            return when (axis) {
                Axis.LEFT_RIGHT -> Grid.build(s) { r, c -> g[r, s - 1 - c] }
                Axis.UP_DOWN -> Grid.build(s) { r, c -> g[s - 1 - r, c] }
                Axis.DIAGONAL -> Grid.build(s) { r, c -> g[c, r] }
            }
        }
    }

    /** Lit nodes turn off, off nodes light up (color 1). Changes how many nodes are lit. */
    object Invert : Rule() {
        override val type: RuleType get() = RuleType.COUNT_CHANGE
        override val label: String get() = "invert"
        override val explain: String get() = "Lit nodes go dark, and dark nodes light up as ●."
        override fun apply(g: Grid): Grid = Grid(g.size, g.cells.map { if (it == 0) 1 else 0 })
    }

    /** Every lit node passes its color to the empty node on its right. */
    object SpreadRight : Rule() {
        override val type: RuleType get() = RuleType.NODE_RELATION
        override val label: String get() = "spread>"
        override val explain: String get() = "Every lit node paints the empty node on its right with its own shape."
        override fun apply(g: Grid): Grid =
            Grid.build(g.size) { r, c -> if (g[r, c] == 0 && c > 0) g[r, c - 1] else g[r, c] }
    }

    /** Every lit node passes its color to the empty node below it. */
    object SpreadDown : Rule() {
        override val type: RuleType get() = RuleType.NODE_RELATION
        override val label: String get() = "spreadv"
        override val explain: String get() = "Every lit node paints the empty node below it with its own shape."
        override fun apply(g: Grid): Grid =
            Grid.build(g.size) { r, c -> if (g[r, c] == 0 && r > 0) g[r - 1, c] else g[r, c] }
    }

    /** Two rules applied one after the other (used by the Final round). */
    data class Then(val first: Rule, val second: Rule) : Rule() {
        override val type: RuleType get() = RuleType.COMBINATION
        override val label: String get() = "${first.label} then ${second.label}"
        override val explain: String get() = "Two steps. First: ${first.explain}  Then: ${second.explain}"
        override fun apply(g: Grid): Grid = second.apply(first.apply(g))
    }
}

object RuleLibrary {
    val shifts: List<Rule> = buildList {
        for (dy in 0..2) for (dx in 0..2) if (dx != 0 || dy != 0) add(Rule.Shift(dx, dy))
    }
    val colorSwaps: List<Rule> = listOf(Rule.ColorSwap(1, 2), Rule.ColorSwap(1, 3), Rule.ColorSwap(2, 3))
    val colorCycles: List<Rule> = listOf(Rule.ColorCycle(true), Rule.ColorCycle(false))
    val rotations: List<Rule> = (1..3).map { Rule.Rotate(it) }
    val mirrors: List<Rule> = Axis.values().map { Rule.Mirror(it) }
    val others: List<Rule> = listOf(Rule.Invert, Rule.SpreadRight, Rule.SpreadDown, Rule.GravityDown)

    /** Every single rule. This is the hypothesis space the solver checks against. */
    val singles: List<Rule> = shifts + colorSwaps + colorCycles + rotations + mirrors + others

    /** The simplest rules: used on Easy and in the tutorial. */
    val beginner: List<Rule> = listOf(
        Rule.Shift(1, 0), Rule.Shift(0, 1), Rule.Mirror(Axis.LEFT_RIGHT), Rule.Mirror(Axis.UP_DOWN),
        Rule.Rotate(1), Rule.Rotate(2), Rule.ColorSwap(1, 2), Rule.ColorSwap(2, 3)
    )
    val attributeEasy: List<Rule> = colorSwaps + colorCycles + listOf(Rule.Invert)

    /** Rule pools used to *pick* the hidden rule per round. */
    val easy: List<Rule> = shifts + rotations + mirrors + colorSwaps
    val attribute: List<Rule> = colorSwaps + colorCycles + listOf(Rule.Invert, Rule.SpreadRight, Rule.SpreadDown, Rule.GravityDown)

    /** Every ordered pair of two different single rules. */
    val pairs: List<Rule> by lazy {
        singles.flatMap { a -> singles.filter { b -> b != a }.map { b -> Rule.Then(a, b) } }
    }

    /** Hypothesis space for the Final round: single rules and pairs. */
    val all: List<Rule> by lazy { singles + pairs }
}
