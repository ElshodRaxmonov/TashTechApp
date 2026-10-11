package com.mrdev.tashtech.tashbox.session

import com.mrdev.tashtech.tashbox.leaderboard.GameMode
import com.mrdev.tashtech.tashbox.leaderboard.GameResult
import com.mrdev.tashtech.tashbox.leaderboard.PlayerResult
import com.mrdev.tashtech.tashbox.network.ErrorMessage
import com.mrdev.tashtech.tashbox.network.GameFinished
import com.mrdev.tashtech.tashbox.network.JoinAccepted
import com.mrdev.tashtech.tashbox.network.JoinRejected
import com.mrdev.tashtech.tashbox.network.LevelChange
import com.mrdev.tashtech.tashbox.network.NetworkMessage
import com.mrdev.tashtech.tashbox.network.PlayerAnswered
import com.mrdev.tashtech.tashbox.network.PlayerDisconnected
import com.mrdev.tashtech.tashbox.network.PlayerLeft
import com.mrdev.tashtech.tashbox.network.PlayerList
import com.mrdev.tashtech.tashbox.network.PlayerReconnected
import com.mrdev.tashtech.tashbox.network.Rematch
import com.mrdev.tashtech.tashbox.network.RoundResult
import com.mrdev.tashtech.tashbox.network.RoundStart
import com.mrdev.tashtech.tashbox.network.StartGame
import com.mrdev.tashtech.tashbox.room.LobbyPlayer
import com.mrdev.tashtech.tashbox.room.RejectReason
import com.mrdev.tashtech.tashbox.timer.MonotonicClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Reduces protocol messages (+ local clock ticks) into [GameUiState]. Contains no game rules:
 * it only mirrors what the authoritative host says. Timers are local countdowns that start when a
 * message arrives, so no clock synchronisation between phones is needed.
 */
internal class SessionUiModel(
    role: SessionRole,
    private val clock: MonotonicClock,
    myPlayerId: String = ""
) {
    private val _state = MutableStateFlow(GameUiState(role = role, myPlayerId = myPlayerId))
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    @Volatile private var countdownEndsAt = 0L
    @Volatile private var flashEndsAt = 0L
    @Volatile private var answerEndsAt = 0L
    @Volatile private var noticeUntil = Long.MAX_VALUE

    fun patch(block: (GameUiState) -> GameUiState) { _state.update(block) }

    fun onMessage(m: NetworkMessage) {
        val now = clock.nowMillis()
        _state.update { reduce(it, m, now) }
    }

    /** Locks the local selection. Returns false if an answer is not currently allowed. */
    fun selectLocal(index: Int): Boolean {
        var accepted = false
        _state.update { s ->
            accepted = s.phase == UiPhase.ANSWERING && s.selectedAnswer == null &&
                s.puzzle != null && index in s.puzzle.options.indices
            if (accepted) s.copy(selectedAnswer = index) else s
        }
        return accepted
    }

    fun tick() {
        val now = clock.nowMillis()
        _state.update { s ->
            var next = when (s.phase) {
                UiPhase.COUNTDOWN -> s.copy(countdownSeconds = ceilSeconds(countdownEndsAt - now))
                UiPhase.FLASH ->
                    if (now >= flashEndsAt) s.copy(phase = UiPhase.ANSWERING, flashRemainingMillis = 0, remainingMillis = quantize(answerEndsAt - now))
                    else s.copy(flashRemainingMillis = quantize(flashEndsAt - now))
                UiPhase.ANSWERING -> s.copy(remainingMillis = quantize(answerEndsAt - now))
                else -> s
            }
            if (next.notice != null && now > noticeUntil) next = next.copy(notice = null)
            next
        }
    }

    fun fail(message: String) {
        _state.update { it.copy(phase = UiPhase.CONNECT_FAILED, errorMessage = message) }
    }

    /** Host vanished: show the standings reached so far (if a match was running). */
    fun hostLeft() {
        _state.update { s ->
            if (s.phase == UiPhase.FINISHED) s
            else s.copy(
                phase = UiPhase.HOST_LEFT,
                result = if (s.round > 0) partialResult(s) else null,
                errorMessage = "The host left the game.",
                notice = null
            )
        }
    }

    // ---- reducer -------------------------------------------------------------------------

    private fun reduce(s: GameUiState, m: NetworkMessage, now: Long): GameUiState = when (m) {
        is JoinAccepted -> {
            val withId = s.copy(myPlayerId = m.playerId)
            withId.copy(
                roomId = m.roomId, roomName = m.roomName, totalRounds = m.totalRounds, level = m.level,
                players = mergeList(withId, m.players),
                phase = if (m.reconnected && s.phase != UiPhase.CONNECTING) s.phase else UiPhase.LOBBY,
                notice = null, errorMessage = null
            )
        }
        is JoinRejected -> s.copy(phase = UiPhase.CONNECT_FAILED, errorMessage = rejectText(m.reason))
        is PlayerList -> s.copy(players = mergeList(s, m.players))
        is LevelChange -> s.copy(level = m.level)
        is StartGame -> {
            countdownEndsAt = now + m.countdownMillis
            s.copy(
                phase = UiPhase.COUNTDOWN, totalRounds = m.totalRounds, countdownSeconds = ceilSeconds(m.countdownMillis),
                round = 0, score = 0, puzzle = null, selectedAnswer = null, roundResult = null, result = null, roundHistory = emptyList(),
                players = s.players.map { it.copy(score = 0, answered = false, lastCorrect = null) }
            )
        }
        is RoundStart -> {
            flashEndsAt = now + m.flashMillis
            answerEndsAt = flashEndsAt + m.answerMillis
            s.copy(
                phase = if (m.flashMillis > 0) UiPhase.FLASH else UiPhase.ANSWERING,
                round = m.round, totalRounds = m.totalRounds, puzzle = m.puzzle,
                selectedAnswer = if (m.round == s.round) s.selectedAnswer else null,
                roundResult = null,
                remainingMillis = m.answerMillis, flashRemainingMillis = m.flashMillis,
                players = s.players.map { it.copy(answered = false, lastCorrect = null) },
                notice = if (s.notice?.contains("Reconnecting") == true) null else s.notice
            )
        }
        is PlayerAnswered -> s.copy(players = s.players.map { if (it.id == m.playerId) it.copy(answered = true) else it })
        is RoundResult -> {
            val byId = m.result.results.associateBy { it.playerId }
            val players = s.players.map { p ->
                byId[p.id]?.let { r -> p.copy(score = r.totalScore, answered = r.answered, lastCorrect = if (r.answered) r.correct else null) } ?: p
            }
            s.copy(
                phase = UiPhase.ROUND_RESULT, roundResult = m.result, players = players,
                score = byId[s.myPlayerId]?.totalScore ?: s.score,
                roundHistory = s.roundHistory.take(m.result.round - 1) + (byId[s.myPlayerId]?.correct == true)
            )
        }
        is GameFinished -> s.copy(
            phase = UiPhase.FINISHED, result = m.result, notice = null,
            score = m.result.playerResult(s.myPlayerId)?.score ?: s.score
        )
        is Rematch -> s.copy(
            phase = UiPhase.LOBBY, round = 0, score = 0, puzzle = null, selectedAnswer = null,
            roundResult = null, result = null, notice = null, roundHistory = emptyList(),
            players = s.players.map { it.copy(score = 0, answered = false, lastCorrect = null) }
        )
        is PlayerDisconnected -> {
            noticeUntil = Long.MAX_VALUE
            s.copy(
                players = s.players.map { if (it.id == m.playerId) it.copy(connected = false) else it },
                notice = "${nameOf(s, m.playerId)} disconnected.\nReconnecting..."
            )
        }
        is PlayerReconnected -> {
            noticeUntil = now + NOTICE_MS
            s.copy(
                players = s.players.map { if (it.id == m.playerId) it.copy(connected = true) else it },
                notice = "${nameOf(s, m.playerId)} reconnected."
            )
        }
        is PlayerLeft -> {
            noticeUntil = now + NOTICE_MS
            s.copy(
                players = s.players.map { if (it.id == m.playerId) it.copy(connected = false) else it },
                notice = "${nameOf(s, m.playerId)} left the game."
            )
        }
        is ErrorMessage -> s // diagnostic only, never shown to players
        else -> s
    }

    private fun mergeList(s: GameUiState, list: List<LobbyPlayer>): List<UiPlayer> =
        list.map { lp ->
            val old = s.players.firstOrNull { it.id == lp.id }
            UiPlayer(
                id = lp.id, name = lp.name, score = old?.score ?: 0, isYou = lp.id == s.myPlayerId,
                isHost = lp.isHost, ready = lp.ready, connected = lp.connected,
                answered = old?.answered ?: false, lastCorrect = old?.lastCorrect
            )
        }

    private fun nameOf(s: GameUiState, id: String) = s.players.firstOrNull { it.id == id }?.name ?: "A player"

    private fun partialResult(s: GameUiState): GameResult {
        val sorted = s.players.sortedWith(compareByDescending<UiPlayer> { it.score }.thenBy { it.name })
        val standings = ArrayList<PlayerResult>()
        sorted.forEachIndexed { i, p ->
            val prev = sorted.getOrNull(i - 1)
            val rank = if (prev != null && prev.score == p.score) standings[i - 1].rank else i + 1
            standings += PlayerResult(p.id, p.name, p.score, 0, s.totalRounds, rank, !p.connected)
        }
        return GameResult("partial", GameMode.MULTIPLAYER, 0L, s.totalRounds, standings, endedEarly = true)
    }

    private fun rejectText(r: RejectReason) = when (r) {
        RejectReason.ROOM_FULL -> "This room is full (6 players)."
        RejectReason.MATCH_IN_PROGRESS -> "This game has already started."
        RejectReason.WRONG_ROOM -> "This room code is no longer valid.\nAsk the host to show a fresh QR code."
        RejectReason.VERSION_MISMATCH -> "Your TashBox version doesn't match the host's.\nUpdate the app and try again."
    }

    private fun ceilSeconds(ms: Long): Int = ((ms.coerceAtLeast(0) + 999) / 1000).toInt()
    private fun quantize(ms: Long): Long = ((ms.coerceAtLeast(0) + 99) / 100) * 100

    companion object {
        private const val NOTICE_MS = 4_000L
    }
}
