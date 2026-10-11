package com.mrdev.tashtech.tashbox.session

import com.mrdev.tashtech.tashbox.network.AnswerSubmit
import com.mrdev.tashtech.tashbox.network.GameFinished
import com.mrdev.tashtech.tashbox.network.JoinAccepted
import com.mrdev.tashtech.tashbox.network.JoinRejected
import com.mrdev.tashtech.tashbox.network.JoinRoom
import com.mrdev.tashtech.tashbox.network.LeaveRoom
import com.mrdev.tashtech.tashbox.network.PROTOCOL_VERSION
import com.mrdev.tashtech.tashbox.network.Ready
import com.mrdev.tashtech.tashbox.network.TcpClientTransport
import com.mrdev.tashtech.tashbox.network.Transport
import com.mrdev.tashtech.tashbox.room.QrPayload
import com.mrdev.tashtech.tashbox.timer.MonotonicClock
import com.mrdev.tashtech.tashbox.timer.SystemMonotonicClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException
import kotlin.concurrent.thread

/**
 * Joined player. Sends only join / ready / answer; everything it shows comes from the host.
 * If the connection drops it keeps retrying for 10 seconds (same player id), then gives up
 * and shows the standings reached so far.
 */
class ClientSession(
    parentScope: CoroutineScope,
    private val payload: QrPayload,
    private val playerName: String,
    private val clock: MonotonicClock = SystemMonotonicClock
) : GameSession {

    private val jobAndScope = childScope(parentScope)
    private val job = jobAndScope.first
    private val scope = jobAndScope.second
    private val model = SessionUiModel(SessionRole.CLIENT, clock)

    override val state: StateFlow<GameUiState> = model.state

    @Volatile private var transport: Transport? = null
    @Volatile private var closed = false

    init {
        scope.launch { while (isActive) { model.tick(); delay(100) } }
        scope.launch { run() }
    }

    private suspend fun connectOrNull(): TcpClientTransport? =
        try {
            TcpClientTransport.connect(scope, payload.host, payload.port)
        } catch (e: IOException) {
            null
        }

    private suspend fun run() {
        var joined = false
        var finished = false
        var rejected = false
        var lostAt = 0L
        while (!closed) {
            val t = connectOrNull()
            if (t == null) {
                if (!joined) return model.fail(WRONG_WIFI_MESSAGE)
                if (clock.nowMillis() - lostAt > RECONNECT_GRACE_MS) return model.hostLeft()
                delay(1000)
                continue
            }
            transport = t
            val knownId = model.state.value.myPlayerId.ifEmpty { null }
            t.send(JoinRoom(PROTOCOL_VERSION, payload.roomId, payload.token, playerName, knownId))
            t.messages.collect { m ->
                when (m) {
                    is JoinAccepted -> { joined = true; model.onMessage(m); t.send(Ready(true)) }
                    is JoinRejected -> { rejected = true; model.onMessage(m) }
                    is GameFinished -> { finished = true; model.onMessage(m) }
                    else -> model.onMessage(m)
                }
            }
            // The connection ended.
            if (closed || rejected) return
            if (finished) return
            if (!joined) return model.fail(WRONG_WIFI_MESSAGE)
            lostAt = clock.nowMillis()
            model.patch { it.copy(notice = "Connection lost.\nReconnecting...") }
            delay(1000)
        }
    }

    override fun submitAnswer(optionIndex: Int) {
        if (!model.selectLocal(optionIndex)) return
        val s = model.state.value
        scope.launch { transport?.send(AnswerSubmit(payload.roomId, s.myPlayerId, s.round, optionIndex)) }
    }

    override fun close() {
        closed = true
        val t = transport
        job.cancel()
        if (t != null) thread(isDaemon = true) { runBlocking { runCatching { t.send(LeaveRoom); t.close() } } }
    }
}
