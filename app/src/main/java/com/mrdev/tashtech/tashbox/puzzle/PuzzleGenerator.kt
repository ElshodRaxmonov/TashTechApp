package com.mrdev.tashtech.tashbox.puzzle

import kotlin.collections.plus
import kotlin.random.Random

/**
 * Generates puzzles from controlled rule types. Fully deterministic for a given (round, seed, level).
 * Every candidate is checked by [PuzzleSolver]; ambiguous puzzles are thrown away and regenerated.
 */
object PuzzleGenerator {
    const val ROUNDS = 5
    private const val MAX_ATTEMPTS = 6000
    private val LETTERS = listOf("A", "B", "C", "D")

    /** Number of examples in the Fault round (and therefore number of answer buttons). */
    const val FAULT_EXAMPLE_COUNT = 4

    /** Easy gives this many times the Medium answer time (12 s becomes 24 s, the Final 30 s becomes 60 s). */
    const val EASY_TIME_FACTOR = 2.0

    internal data class RoundConfig(
        val roundType: RoundType,
        val gridSize: Int,
        val exampleCount: Int,
        val flashMillis: Long,
        val answerMillis: Long,
        val difficulty: Int
    )

    internal fun config(round: Int, level: Difficulty = Difficulty.MEDIUM): RoundConfig {
        val medium = when (round) {
            1 -> RoundConfig(RoundType.PREDICT, 3, 3, 0, 12_000, 1)
            2 -> RoundConfig(RoundType.ATTRIBUTE, 3, 3, 0, 15_000, 2)
            3 -> RoundConfig(RoundType.SNAPSHOT, 4, 0, 3_000, 12_000, 3)
            4 -> RoundConfig(RoundType.FAULT, 3, FAULT_EXAMPLE_COUNT, 0, 20_000, 4)
            else -> RoundConfig(RoundType.FINAL, 4, 4, 0, 30_000, 5)
        }
        return when (level) {
            Difficulty.MEDIUM -> medium
            Difficulty.EASY -> medium.copy(
                gridSize = 3,
                exampleCount = if (medium.exampleCount > 0) 4 else 0,
                flashMillis = if (medium.flashMillis > 0) 5_000 else 0,
                answerMillis = (medium.answerMillis * EASY_TIME_FACTOR).toLong()
            )
            Difficulty.HARD -> medium.copy(
                gridSize = if (round == 3) 5 else 4,
                answerMillis = (medium.answerMillis * 0.8).toLong()
            )
        }
    }

    fun generate(round: Int, seed: Long, level: Difficulty = Difficulty.MEDIUM): Puzzle {
        require(round in 1..ROUNDS) { "round must be 1..$ROUNDS" }
        val rnd = Random(seed * 31L + round + level.ordinal * 7_919L)
        val cfg = config(round, level)
        repeat(MAX_ATTEMPTS) {
            val candidate = when (cfg.roundType) {
                RoundType.SNAPSHOT -> snapshot(round, seed, cfg, level, rnd)
                RoundType.FAULT -> fault(round, seed, cfg, level, rnd)
                else -> predict(round, seed, cfg, level, rnd)
            }
            if (candidate != null && PuzzleSolver.isValid(candidate)) return candidate
        }
        error("Could not generate a valid puzzle for round $round, seed $seed, level $level")
    }

    // ---- builders -------------------------------------------------------------------------

    private fun predict(round: Int, seed: Long, cfg: RoundConfig, level: Difficulty, rnd: Random): Puzzle? {
        val rule = pickRule(round, level, rnd)
        val inputs = distinctGrids(rnd, cfg.gridSize, cfg.exampleCount + 1) ?: return null
        val examples = inputs.dropLast(1).map { Example(it, rule.apply(it)) }
        if (examples.any { it.input == it.output }) return null
        val query = inputs.last()
        val answer = rule.apply(query)
        if (answer == query) return null

        val space = PuzzleSolver.hypothesisSpace(round, level)
        val distractors = LinkedHashSet<Grid>()
        space.map { it.apply(query) }.filter { it != answer }.distinct().shuffled(rnd).take(3).forEach { distractors.add(it) }
        var guard = 0
        while (distractors.size < 3 && guard++ < 200) {
            val idx = rnd.nextInt(answer.cells.size)
            val newValue = (0..Grid.MAX_COLOR).filter { it != answer.cells[idx] }.random(rnd)
            distractors.add(answer.withCell(idx, newValue))
        }
        if (distractors.size < 3) return null

        val shuffled = (distractors.toList() + answer).shuffled(rnd)
        val options = shuffled.mapIndexed { i, g -> AnswerOption(LETTERS[i], grid = g) }
        return Puzzle(
            id = "$seed-$round", round = round, roundType = cfg.roundType,
            question = QuestionKind.PREDICT_OUTPUT, difficulty = cfg.difficulty,
            examples = examples, queryInput = query,
            answerMillis = cfg.answerMillis, options = options,
            correctIndex = shuffled.indexOf(answer),
            metadata = mapOf(
                "rule" to rule.label,
                "explain" to rule.explain,
                "seed" to seed.toString()
            ),
            level = level
        )
    }

    private fun snapshot(round: Int, seed: Long, cfg: RoundConfig, level: Difficulty, rnd: Random): Puzzle? {
        val before = randomGrid(rnd, cfg.gridSize)
        val changed = rnd.nextInt(before.cells.size)
        val oldValue = before.cells[changed]
        val newValue = (0..Grid.MAX_COLOR).filter { it != oldValue }.random(rnd)
        val after = before.withCell(changed, newValue)
        val others = before.cells.indices.filter { it != changed }.shuffled(rnd).take(3)
        val nodes = (others + changed).shuffled(rnd)
        val options = nodes.mapIndexed { i, n -> AnswerOption(LETTERS[i], node = n) }
        val row = changed / cfg.gridSize + 1
        val col = changed % cfg.gridSize + 1
        return Puzzle(
            id = "$seed-$round", round = round, roundType = cfg.roundType,
            question = QuestionKind.FIND_CHANGED_NODE, difficulty = cfg.difficulty,
            flashGrid = before, shownGrid = after,
            flashMillis = cfg.flashMillis, answerMillis = cfg.answerMillis,
            options = options, correctIndex = nodes.indexOf(changed),
            metadata = mapOf(
                "changedNode" to changed.toString(), "seed" to seed.toString(),
                "explain" to "The node in row $row, column $col changed from ${sym(oldValue)} to ${
                    sym(
                        newValue
                    )
                }."
            ),
            level = level
        )
    }

    private fun fault(round: Int, seed: Long, cfg: RoundConfig, level: Difficulty, rnd: Random): Puzzle? {
        val pool = if (level == Difficulty.EASY) RuleLibrary.beginner else RuleLibrary.singles
        val rule = pool.random(rnd)
        val inputs = distinctGrids(rnd, cfg.gridSize, cfg.exampleCount) ?: return null
        val good = inputs.map { rule.apply(it) }
        if (inputs.indices.any { good[it] == inputs[it] }) return null
        val faulty = rnd.nextInt(inputs.size)
        val wrongOutputs = RuleLibrary.singles
            .map { it.apply(inputs[faulty]) }
            .filter { it != good[faulty] && it != inputs[faulty] }
            .distinct()
        if (wrongOutputs.isEmpty()) return null
        val outputs = good.toMutableList().also { it[faulty] = wrongOutputs.random(rnd) }
        val examples = inputs.indices.map { Example(inputs[it], outputs[it]) }
        val options = inputs.indices.map { AnswerOption((it + 1).toString()) }
        return Puzzle(
            id = "$seed-$round", round = round, roundType = cfg.roundType,
            question = QuestionKind.FIND_FAULTY_EXAMPLE, difficulty = cfg.difficulty,
            examples = examples, answerMillis = cfg.answerMillis,
            options = options, correctIndex = faulty,
            metadata = mapOf(
                "rule" to rule.label, "seed" to seed.toString(),
                "explain" to "Example ${faulty + 1} was the odd one out. The real rule: ${rule.explain}"
            ),
            level = level
        )
    }

    // ---- helpers --------------------------------------------------------------------------

    private fun pickRule(round: Int, level: Difficulty, rnd: Random): Rule = when {
        level == Difficulty.EASY -> when (round) {
            1 -> RuleLibrary.beginner.random(rnd)
            2 -> RuleLibrary.attributeEasy.random(rnd)
            else -> RuleLibrary.attribute.random(rnd) // Easy Final: still ONE rule, just a livelier one
        }
        round == 1 -> if (level == Difficulty.HARD) RuleLibrary.singles.random(rnd) else RuleLibrary.easy.random(rnd)
        round == 2 -> RuleLibrary.attribute.random(rnd)
        else -> {
            val a = RuleLibrary.singles.random(rnd)
            val b = RuleLibrary.singles.filter { it.type != a.type }.random(rnd)
            Rule.Then(a, b)
        }
    }

    private fun randomGrid(rnd: Random, size: Int): Grid {
        while (true) {
            val g = Grid.build(size) { _, _ ->
                if (rnd.nextFloat() < 0.55f) rnd.nextInt(1, Grid.MAX_COLOR + 1) else 0
            }
            if (g.litCount in 3..(size * size - 3)) return g
        }
    }

    private fun distinctGrids(rnd: Random, size: Int, count: Int): List<Grid>? {
        val out = LinkedHashSet<Grid>()
        var guard = 0
        while (out.size < count && guard++ < count * 50) out.add(randomGrid(rnd, size))
        return if (out.size == count) out.toList() else null
    }
}
