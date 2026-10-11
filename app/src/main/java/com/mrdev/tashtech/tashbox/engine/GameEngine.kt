package com.mrdev.tashtech.tashbox.engine

import com.mrdev.tashtech.tashbox.leaderboard.GameMode
import com.mrdev.tashtech.tashbox.leaderboard.GameResult
import com.mrdev.tashtech.tashbox.leaderboard.PlayerResult
import com.mrdev.tashtech.tashbox.puzzle.Difficulty
import com.mrdev.tashtech.tashbox.puzzle.Puzzle
import com.mrdev.tashtech.tashbox.puzzle.PuzzleGenerator
import com.mrdev.tashtech.tashbox.scoring.ScoreCalculator
import kotlinx.serialization.Serializable

enum class EnginePhase { IDLE, COUNTDOWN, FLASH, ANSWERING, ROUND_RESULT, FINISHED }

data class EngineConfig(
    val totalRounds: Int = PuzzleGenerator.ROUNDS,
    val countdownMillis: Long = 3_000,
    val resultMillis: Long = 3_000,
    val level: Difficulty = Difficulty.MEDIUM,
    /** Solo: after a wrong answer / timeout the match waits until the student presses Next. */
    val waitForContinue: Boolean = false,
    /** Multiplayer cannot wait for one student, so rounds with a mistake show their review a bit longer. */
    val reviewMillis: Long = 7_000
)

data class EnginePlayer(val id: String, val name: String)

@Serializable
data class PlayerRoundResult(
    val playerId: String,
    val answered: Boolean,
    val chosen: Int?,
    val correct: Boolean,
    val points: Int,
    val speedBonus: Int,
    val totalScore: Int
)

@Serializable
data class RoundResultData(
    val round: Int,
    val correctIndex: Int,
    val results: List<PlayerRoundResult>,
    /** Plain-English "what was the rule" shown after the round. */
    val explanation: String = ""
)

sealed interface EngineEvent {
    data class CountdownStarted(val millis: Long) : EngineEvent
    /** Carries the FULL puzzle (with the answer). Whoever forwards it must call [Puzzle.redacted]. */
    data class RoundStarted(val round: Int, val puzzle: Puzzle) : EngineEvent
    data class AnswerAccepted(val round: Int, val playerId: String) : EngineEvent
    data class RoundEnded(val result: RoundResultData) : EngineEvent
    data class MatchFinished(val result: GameResult) : EngineEvent
}

enum class AnswerStatus {
    ACCEPTED, UNKNOWN_PLAYER, PLAYER_LEFT, WRONG_ROUND, NOT_ANSWERING, ALREADY_ANSWERED, INVALID_OPTION
}

data class SubmitOutcome(val status: AnswerStatus, val events: List<EngineEvent>)

/** What a reconnecting player needs to rejoin the round in progress. */
data class RoundSnapshot(val round: Int, val puzzle: Puzzle, val flashRemaining: Long, val answerRemaining: Long)

/**
 * Authoritative match state machine. Pure Kotlin: no Android, no coroutines, no networking.
 * Time is passed in explicitly (monotonic milliseconds), so tests can drive it deterministically.
 *
 * NOT thread-safe: the owner (SoloSession / HostSession) confines it to a single dispatcher.
 *
 * Timeline: COUNTDOWN -> [FLASH ->] ANSWERING -> ROUND_RESULT -> next round ... -> FINISHED.
 * A round ends early as soon as every connected player has answered.
 */
class GameEngine(
    players: List<EnginePlayer>,
    val matchSeed: Long,
    val mode: GameMode,
    private val config: EngineConfig = EngineConfig(),
    private val matchId: String = "m$matchSeed"
) {
    private class PlayerState(val id: String, val name: String) {
        var score = 0
        var correct = 0
        var connected = true
        var left = false
    }

    private class Answer(val option: Int, val atMs: Long)

    private val states = LinkedHashMap<String, PlayerState>().apply {
        players.forEach { put(it.id, PlayerState(it.id, it.name)) }
    }
    private val answers = HashMap<String, Answer>()

    var phase: EnginePhase = EnginePhase.IDLE
        private set
    var round: Int = 0
        private set
    var puzzle: Puzzle? = null
        private set
    var lastRoundResult: RoundResultData? = null
        private set

    private var phaseEndsAt = 0L
    private var answerStartedAt = 0L
    private var answerEndsAt = 0L
    private var lastAnswerAt = 0L

    val totalRounds: Int get() = config.totalRounds
    val level: Difficulty get() = config.level

    // ---- lifecycle --------------------------------------------------------------------------

    fun start(now: Long): List<EngineEvent> {
        check(phase == EnginePhase.IDLE) { "Already started" }
        phase = EnginePhase.COUNTDOWN
        phaseEndsAt = now + config.countdownMillis
        return listOf(EngineEvent.CountdownStarted(config.countdownMillis))
    }

    /** Advances the state machine up to [now]. Safe to call as often as you like. */
    fun tick(now: Long): List<EngineEvent> {
        val out = ArrayList<EngineEvent>()
        var progressed = true
        while (progressed) {
            progressed = false
            if (phase != EnginePhase.IDLE && phase != EnginePhase.FINISHED && states.values.all { it.left }) {
                finish(out, endedEarly = true)
                break
            }
            when (phase) {
                EnginePhase.COUNTDOWN -> if (now >= phaseEndsAt) {
                    beginRound(1, phaseEndsAt, out); progressed = true
                }
                EnginePhase.FLASH -> if (now >= phaseEndsAt) {
                    enterAnswering(phaseEndsAt); progressed = true
                }
                EnginePhase.ANSWERING -> if (now >= answerEndsAt || allAnswered()) {
                    endRound(out); progressed = true
                }
                EnginePhase.ROUND_RESULT -> if (now >= phaseEndsAt) {
                    if (round >= config.totalRounds) finish(out, endedEarly = false)
                    else beginRound(round + 1, phaseEndsAt, out)
                    progressed = true
                }
                else -> Unit
            }
        }
        return out
    }

    fun submitAnswer(playerId: String, roundNumber: Int, option: Int, now: Long): SubmitOutcome {
        val events = ArrayList<EngineEvent>(tick(now))
        val st = states[playerId]
        val status = when {
            st == null -> AnswerStatus.UNKNOWN_PLAYER
            st.left -> AnswerStatus.PLAYER_LEFT
            roundNumber != round -> AnswerStatus.WRONG_ROUND
            phase != EnginePhase.ANSWERING -> AnswerStatus.NOT_ANSWERING
            answers.containsKey(playerId) -> AnswerStatus.ALREADY_ANSWERED
            option !in (puzzle?.options?.indices ?: IntRange.EMPTY) -> AnswerStatus.INVALID_OPTION
            else -> AnswerStatus.ACCEPTED
        }
        if (status == AnswerStatus.ACCEPTED) {
            answers[playerId] = Answer(option, now)
            lastAnswerAt = now
            events += EngineEvent.AnswerAccepted(round, playerId)
            events += tick(now)
        }
        return SubmitOutcome(status, events)
    }

    // ---- players ----------------------------------------------------------------------------

    fun setConnected(playerId: String, connected: Boolean, now: Long): List<EngineEvent> {
        states[playerId]?.let { if (!it.left) it.connected = connected }
        return tick(now)
    }

    fun markLeft(playerId: String, now: Long): List<EngineEvent> {
        states[playerId]?.let { it.left = true; it.connected = false }
        return tick(now)
    }

    /** The student pressed Next on the round-result screen: move on now. No-op in any other phase. */
    fun continueNext(now: Long): List<EngineEvent> {
        if (phase == EnginePhase.ROUND_RESULT) phaseEndsAt = minOf(phaseEndsAt, now)
        return tick(now)
    }

    /** Ends the match immediately (e.g. the host closed the room). */
    fun abort(): GameResult {
        phase = EnginePhase.FINISHED
        return buildResult(endedEarly = true)
    }

    // ---- queries ----------------------------------------------------------------------------

    fun hasAnswered(playerId: String): Boolean = answers.containsKey(playerId)
    fun answeredPlayerIds(): List<String> = answers.keys.toList()
    fun scoreOf(playerId: String): Int = states[playerId]?.score ?: 0

    fun currentRound(now: Long): RoundSnapshot? {
        val p = puzzle ?: return null
        return when (phase) {
            EnginePhase.FLASH -> RoundSnapshot(round, p, (phaseEndsAt - now).coerceAtLeast(0), p.answerMillis)
            EnginePhase.ANSWERING -> RoundSnapshot(round, p, 0, (answerEndsAt - now).coerceAtLeast(0))
            else -> null
        }
    }

    fun result(): GameResult = buildResult(endedEarly = false)

    // ---- internals --------------------------------------------------------------------------

    private fun allAnswered(): Boolean {
        val blocking = states.values.filter { it.connected && !it.left }
        return blocking.isNotEmpty() && blocking.all { answers.containsKey(it.id) }
    }

    private fun beginRound(n: Int, startAt: Long, out: MutableList<EngineEvent>) {
        round = n
        val p = PuzzleGenerator.generate(n, matchSeed * 1_000_003L + n, config.level)
        puzzle = p
        answers.clear()
        lastAnswerAt = startAt
        out += EngineEvent.RoundStarted(n, p)
        if (p.flashMillis > 0) {
            phase = EnginePhase.FLASH
            phaseEndsAt = startAt + p.flashMillis
        } else {
            enterAnswering(startAt)
        }
    }

    private fun enterAnswering(startAt: Long) {
        val p = puzzle!!
        phase = EnginePhase.ANSWERING
        answerStartedAt = startAt
        answerEndsAt = startAt + p.answerMillis
    }

    private fun endRound(out: MutableList<EngineEvent>) {
        val p = puzzle!!
        val endTime = if (allAnswered()) minOf(lastAnswerAt, answerEndsAt) else answerEndsAt
        val results = states.values.map { st ->
            val a = answers[st.id]
            if (a == null) {
                PlayerRoundResult(st.id, false, null, false, 0, 0, st.score)
            } else {
                val correct = a.option == p.correctIndex
                val rs = if (correct) {
                    ScoreCalculator.score(round, true, (a.atMs - answerStartedAt).coerceAtLeast(0), p.answerMillis)
                } else ScoreCalculator.timeout()
                st.score += rs.total
                if (correct) st.correct++
                PlayerRoundResult(st.id, true, a.option, correct, rs.total, rs.speedBonus, st.score)
            }
        }
        val data = RoundResultData(round, p.correctIndex, results, p.metadata["explain"].orEmpty())
        lastRoundResult = data
        phase = EnginePhase.ROUND_RESULT
        val anyMiss = results.any { !it.correct }
        phaseEndsAt = when {
            anyMiss && config.waitForContinue -> Long.MAX_VALUE // waits for continueNext()
            anyMiss -> endTime + config.reviewMillis
            else -> endTime + config.resultMillis
        }
        out += EngineEvent.RoundEnded(data)
    }

    private fun finish(out: MutableList<EngineEvent>, endedEarly: Boolean) {
        phase = EnginePhase.FINISHED
        out += EngineEvent.MatchFinished(buildResult(endedEarly))
    }

    private fun buildResult(endedEarly: Boolean): GameResult {
        val sorted = states.values.sortedWith(
            compareByDescending<PlayerState> { it.score }.thenByDescending { it.correct }.thenBy { it.name }
        )
        val standings = ArrayList<PlayerResult>()
        sorted.forEachIndexed { i, st ->
            val prev = sorted.getOrNull(i - 1)
            val rank = if (prev != null && prev.score == st.score && prev.correct == st.correct) standings[i - 1].rank else i + 1
            standings += PlayerResult(st.id, st.name, st.score, st.correct, config.totalRounds, rank, st.left)
        }
        return GameResult(matchId, mode, matchSeed, config.totalRounds, standings, endedEarly, level = config.level)
    }
}
