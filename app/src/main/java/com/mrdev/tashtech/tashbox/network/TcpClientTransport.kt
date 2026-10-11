package com.mrdev.tashtech.tashbox.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

/** Client end of the TCP link. The [messages] flow completes when the connection is lost. */
class TcpClientTransport private constructor(
    private val socket: Socket,
    scope: CoroutineScope
) : Transport {

    private val out = Channel<String>(Channel.UNLIMITED)
    private val inbox = Channel<NetworkMessage>(Channel.UNLIMITED)
    @Volatile private var closed = false

    override val messages: Flow<NetworkMessage> = inbox.receiveAsFlow()

    init {
        scope.launch(Dispatchers.IO) { readLoop() }
        scope.launch(Dispatchers.IO) { writeLoop() }
        scope.launch(Dispatchers.IO) {
            while (isActive && !closed) {
                delay(TcpHostTransport.HEARTBEAT_MS)
                out.trySend(Protocol.encode(Heartbeat))
            }
        }
    }

    private fun readLoop() {
        try {
            val reader = socket.getInputStream().bufferedReader(Charsets.UTF_8)
            while (true) {
                val line = LineReader.readLine(reader) ?: break
                val message = Protocol.decode(line) ?: continue
                if (message is Heartbeat) continue
                inbox.trySend(message)
            }
        } catch (e: IOException) {
            // connection lost or host silent for too long
        } finally {
            shutdown()
            inbox.close()
        }
    }

    private suspend fun writeLoop() {
        try {
            val writer = socket.getOutputStream().bufferedWriter(Charsets.UTF_8)
            for (line in out) {
                writer.write(line)
                writer.write("\n")
                writer.flush()
            }
        } catch (e: IOException) {
            // reader will notice too
        } finally {
            shutdown()
        }
    }

    private fun shutdown() {
        closed = true
        out.close()
        runCatching { socket.close() }
    }

    override suspend fun send(message: NetworkMessage) {
        if (!closed) out.trySend(Protocol.encode(message))
    }

    /** Flushes anything queued (e.g. a LeaveRoom), then closes the socket. */
    override suspend fun close() {
        closed = true
        out.close()
    }

    companion object {
        const val CONNECT_TIMEOUT_MS = 4_000

        /** Throws IOException if the host cannot be reached (wrong Wi-Fi, client isolation, host gone). */
        suspend fun connect(
            scope: CoroutineScope,
            host: String,
            port: Int,
            timeoutMs: Int = CONNECT_TIMEOUT_MS
        ): TcpClientTransport = withContext(Dispatchers.IO) {
            val socket = Socket()
            try {
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                socket.tcpNoDelay = true
                socket.soTimeout = TcpHostTransport.READ_TIMEOUT_MS
            } catch (e: IOException) {
                runCatching { socket.close() }
                throw e
            }
            TcpClientTransport(socket, scope)
        }
    }
}
