package com.mrdev.tashtech.tashbox.session

import com.mrdev.tashtech.tashbox.engine.AnswerStatus
import com.mrdev.tashtech.tashbox.engine.EngineEvent
import com.mrdev.tashtech.tashbox.engine.EnginePhase
import com.mrdev.tashtech.tashbox.engine.EnginePlayer
import com.mrdev.tashtech.tashbox.engine.GameEngine
import com.mrdev.tashtech.tashbox.leaderboard.GameMode
import com.mrdev.tashtech.tashbox.network.AnswerSubmit
import com.mrdev.tashtech.tashbox.network.ErrorMessage
import com.mrdev.tashtech.tashbox.network.HostEvent
import com.mrdev.tashtech.tashbox.network.HostTransport
import com.mrdev.tashtech.tashbox.network.JoinAccepted
import com.mrdev.tashtech.tashbox.network.JoinRejected
import com.mrdev.tashtech.tashbox.network.LevelChange
import com.mrdev.tashtech.tashbox.engine.EngineConfig
import com.mrdev.tashtech.tashbox.puzzle.Difficulty
import com.mrdev.tashtech.tashbox.network.JoinRoom
import com.mrdev.tashtech.tashbox.network.LeaveRoom
import com.mrdev.tashtech.tashbox.network.NetworkMessage
import com.mrdev.tashtech.tashbox.network.PROTOCOL_VERSION
import com.mrdev.tashtech.tashbox.network.PlayerAnswered
import com.mrdev.tashtech.tashbox.network.PlayerDisconnected
import com.mrdev.tashtech.tashbox.network.PlayerLeft
import com.mrdev.tashtech.tashbox.network.PlayerList
import com.mrdev.tashtech.tashbox.network.PlayerReconnected
import com.mrdev.tashtech.tashbox.network.Ready
import com.mrdev.tashtech.tashbox.network.Rematch
import com.mrdev.tashtech.tashbox.network.RoundResult
import com.mrdev.tashtech.tashbox.network.RoundStart
import com.mrdev.tashtech.tashbox.puzzle.PuzzleGenerator
import com.mrdev.tashtech.tashbox.room.JoinResult
import com.mrdev.tashtech.tashbox.room.QrPayload
import com.mrdev.tashtech.tashbox.room.RejectReason
import com.mrdev.tashtech.tashbox.room.RoomManager
import com.mrdev.tashtech.tashbox.timer.MonotonicClock
import com.mrdev.tashtech.tashbox.timer.SystemMonotonicClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread
import kotlin.random.Random

/**
 * The authoritative side of a multiplayer match. Owns the room, the engine and the transport.
 * Everything below runs on ONE single-threaded dispatcher, so no locks are needed.
 *
 * The host's own phone is just another player (id "host") that talks to the engine directly;
 * its UI is fed by the very same messages that are broadcast to the clients.
 */
class HostSession(
    parentScope: CoroutineScope,
    private val transport: HostTransport,
    localIp: String,
    port: Int,
    hostName: String,
    private val clock: MonotonicClock = SystemMonotonicClock,
    private val room: RoomManager = RoomManager.create(hostName),
    private var seed: Long = Random.nextLong(),
    private var level: Difficulty = Difficulty.MEDIUM
) : GameSession {

    private val jobAndScope = childScope(parentScope)
    private val job = jobAndScope.first
    private val scope = jobAndScope.second
    private val dispatcher = newEngineDispatcher()
    private val model = SessionUiModel(SessionRole.HOST, clock, RoomManager.HOST_ID)

    override val state: StateFlow<GameUiState> = model.state

    private var engine: GameEngine? = null
    private val connToPlayer = HashMap<Int, String>()
    private val playerToConn = HashMap<String, Int>()
    private val graceJobs = HashMap<String, Job>()
    private val leftPlayers = HashSet<String>()

    val qrText: String =
        QrPayload(PROTOCOL_VERSION, localIp, port, room.roomId, room.token, room.hostDisplayName).encode()

    init {
        model.onMessage(
            JoinAccepted(room.roomId, room.roomName, RoomManager.HOST_ID, room.list(), false, PuzzleGenerator.ROUNDS, level)
        )
        model.patch { it.copy(qrPayload = qrText, canStart = room.canStart) }
        scope.launch(dispatcher) { transport.events.collect { handle(it) } }
    }

    // ---- public controls --------------------------------------------------------------------

    fun startMatch() { scope.launch(dispatcher) { doStart() } }

    /** Host picks the difficulty in the lobby; everybody sees the change immediately. */
    fun setLevel(newLevel: Difficulty) {
        scope.launch(dispatcher) {
            if (engine != null) return@launch
            level = newLevel
            emit(LevelChange(newLevel))
        }
    }

    /** After a finished match: everybody returns to the lobby of the same room. */
    fun rematch() {
        scope.launch(dispatcher) {
            val e = engine ?: return@launch
            if (e.phase != EnginePhase.FINISHED) return@launch
            engine = null
            seed = Random.nextLong()
            leftPlayers.clear()
            room.resetForRematch()
            emit(Rematch)
            refreshLobby()
        }
    }

    override fun submitAnswer(optionIndex: Int) {
        if (!model.selectLocal(optionIndex)) return
        val round = model.state.value.round
        scope.launch(dispatcher) { engineSubmit(RoomManager.HOST_ID, round, optionIndex) }
    }

    override fun close() {
        job.cancel()
        thread(isDaemon = true) { runBlocking { runCatching { transport.close() } } }
    }

    // ---- transport events -------------------------------------------------------------------

    private suspend fun handle(event: HostEvent) {
        when (event) {
            is HostEvent.Connected -> Unit // nothing happens until the client sends a valid JoinRoom
            is HostEvent.Received -> handleMessage(event.connectionId, event.message)
            is HostEvent.Disconnected -> handleDisconnect(event.connectionId)
        }
    }

    private suspend fun handleMessage(conn: Int, m: NetworkMessage) {
        when (m) {
            is JoinRoom -> handleJoin(conn, m)
            is Ready -> connToPlayer[conn]?.let { room.setReady(it, m.ready); refreshLobby() }
            is AnswerSubmit -> {
                val pid = connToPlayer[conn] ?: return
                // Trust the connection, not the message: ignore answers claiming another identity or room.
                if (m.roomId != room.roomId || m.playerId != pid) return
                engineSubmit(pid, m.round, m.answerIndex)
            }
            is LeaveRoom -> connToPlayer[conn]?.let { playerGone(it) }
            else -> Unit // host-to-client message types sent by a client are ignored
        }
    }

    private suspend fun handleJoin(conn: Int, m: JoinRoom) {
        if (connToPlayer.containsKey(conn)) return
        if (m.protocolVersion != PROTOCOL_VERSION) return reject(conn, RejectReason.VERSION_MISMATCH)
        if (m.playerId != null && m.playerId in leftPlayers) return reject(conn, RejectReason.MATCH_IN_PROGRESS)

        when (val r = room.join(m.roomId, m.token, m.playerName, m.playerId)) {
            is JoinResult.Rejected -> reject(conn, r.reason)
            is JoinResult.Joined -> {
                val pid = r.player.id
                playerToConn[pid]?.let { old -> connToPlayer.remove(old); transport.disconnect(old) }
                connToPlayer[conn] = pid
                playerToConn[pid] = conn
                graceJobs.remove(pid)?.cancel()

                transport.send(
                    conn,
                    JoinAccepted(room.roomId, room.roomName, pid, room.list(), r.reconnected, PuzzleGenerator.ROUNDS, level)
                )
                val e = engine
                if (r.reconnected && e != null) {
                    val now = clock.nowMillis()
                    dispatch(e.setConnected(pid, true, now))
                    e.currentRound(now)?.let { cr ->
                        transport.send(conn, RoundStart(cr.round, e.totalRounds, cr.puzzle.redacted(), cr.flashRemaining, cr.answerRemaining))
                        e.answeredPlayerIds().forEach { transport.send(conn, PlayerAnswered(cr.round, it)) }
                    }
                    if (e.phase == EnginePhase.ROUND_RESULT) e.lastRoundResult?.let { transport.send(conn, RoundResult(it)) }
                    pushPlayers()
                    emit(PlayerReconnected(pid))
                } else {
                    refreshLobby()
                }
            }
        }
    }

    private suspend fun reject(conn: Int, reason: RejectReason) {
        transport.send(conn, JoinRejected(reason))
        transport.disconnect(conn)
    }

    private suspend fun handleDisconnect(conn: Int) {
        val pid = connToPlayer.remove(conn) ?: return
        if (playerToConn[pid] != conn) return // a newer connection already replaced this one
        playerToConn.remove(pid)
        val e = engine
        if (e == null) {
            room.remove(pid)
            refreshLobby()
            return
        }
        if (e.phase == EnginePhase.FINISHED) {
            room.setConnected(pid, false)
            pushPlayers()
            return
        }
        // Mid-match: keep their seat for 10 seconds.
        room.setConnected(pid, false)
        dispatch(e.setConnected(pid, false, clock.nowMillis()))
        pushPlayers()
        emit(PlayerDisconnected(pid, RECONNECT_GRACE_MS))
        graceJobs[pid] = scope.launch(dispatcher) {
            delay(RECONNECT_GRACE_MS)
            playerGone(pid)
        }
    }

    private suspend fun playerGone(pid: String) {
        graceJobs.remove(pid)?.cancel()
        playerToConn.remove(pid)?.let { connToPlayer.remove(it); transport.disconnect(it) }
        val e = engine
        if (e == null) {
            room.remove(pid)
            refreshLobby()
            return
        }
        leftPlayers += pid
        room.setConnected(pid, false)
        dispatch(e.markLeft(pid, clock.nowMillis()))
        pushPlayers()
        emit(PlayerLeft(pid))
    }

    // ---- match control ----------------------------------------------------------------------

    private suspend fun doStart() {
        if (engine != null || !room.canStart) return
        room.lock()
        val players = room.list().filter { it.connected }.map { EnginePlayer(it.id, it.name) }
        val e = GameEngine(players, seed, GameMode.MULTIPLAYER, EngineConfig(level = level))
        engine = e
        model.patch { it.copy(canStart = false) }
        dispatch(e.start(clock.nowMillis()))
        scope.launch(dispatcher) {
            while (isActive && e.phase != EnginePhase.FINISHED) {
                dispatch(e.tick(clock.nowMillis()))
                model.tick()
                delay(TICK_MS)
            }
        }
    }

    private suspend fun engineSubmit(playerId: String, round: Int, option: Int) {
        val e = engine ?: return
        val out = e.submitAnswer(playerId, round, option, clock.nowMillis())
        dispatch(out.events)
        if (out.status != AnswerStatus.ACCEPTED && playerId != RoomManager.HOST_ID) {
            playerToConn[playerId]?.let { transport.send(it, ErrorMessage("answer_rejected", out.status.name)) }
        }
    }

    private suspend fun dispatch(events: List<EngineEvent>) {
        val total = engine?.totalRounds ?: PuzzleGenerator.ROUNDS
        events.forEach { emit(EngineMessages.toMessage(it, total)) }
    }

    /** Updates the host's own UI and broadcasts the same message to every client. */
    private suspend fun emit(message: NetworkMessage) {
        model.onMessage(message)
        transport.broadcast(message)
    }

    private suspend fun pushPlayers() = emit(PlayerList(room.list()))

    private suspend fun refreshLobby() {
        pushPlayers()
        model.patch { it.copy(canStart = room.canStart) }
    }
}
