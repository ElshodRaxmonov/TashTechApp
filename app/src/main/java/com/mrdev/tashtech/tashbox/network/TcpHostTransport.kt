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
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Plain java.net TCP server running on the host phone. Newline-delimited JSON.
 * Binds to all interfaces (Wi-Fi or the phone's own hotspot) on a free port; the QR code tells
 * clients which one. Heartbeats every 2 s; a client that is silent for 7 s counts as disconnected.
 */
class TcpHostTransport private constructor(
    private val server: ServerSocket,
    private val scope: CoroutineScope
) : HostTransport {

    private class Conn(val id: Int, val socket: Socket) {
        val out = Channel<String>(Channel.UNLIMITED)
    }

    private val conns = ConcurrentHashMap<Int, Conn>()
    private val nextId = AtomicInteger(1)
    private val channel = Channel<HostEvent>(Channel.UNLIMITED)
    @Volatile private var closed = false

    override val events: Flow<HostEvent> = channel.receiveAsFlow()
    val port: Int get() = server.localPort

    init {
        scope.launch(Dispatchers.IO) { acceptLoop() }
        scope.launch(Dispatchers.IO) {
            while (isActive && !closed) {
                delay(HEARTBEAT_MS)
                broadcast(Heartbeat)
            }
        }
    }

    private fun acceptLoop() {
        while (!closed) {
            val socket = try {
                server.accept()
            } catch (e: IOException) {
                break
            }
            try {
                socket.tcpNoDelay = true
                socket.soTimeout = READ_TIMEOUT_MS
            } catch (e: IOException) {
                runCatching { socket.close() }
                continue
            }
            val conn = Conn(nextId.getAndIncrement(), socket)
            conns[conn.id] = conn
            channel.trySend(HostEvent.Connected(conn.id))
            scope.launch(Dispatchers.IO) { readLoop(conn) }
            scope.launch(Dispatchers.IO) { writeLoop(conn) }
        }
    }

    private fun readLoop(conn: Conn) {
        try {
            val reader = conn.socket.getInputStream().bufferedReader(Charsets.UTF_8)
            while (true) {
                val line = LineReader.readLine(reader) ?: break
                val message = Protocol.decode(line) ?: continue
                if (message is Heartbeat) continue
                channel.trySend(HostEvent.Received(conn.id, message))
            }
        } catch (e: IOException) {
            // includes SocketTimeoutException: silent for too long
        } finally {
            drop(conn)
        }
    }

    private suspend fun writeLoop(conn: Conn) {
        try {
            val writer = conn.socket.getOutputStream().bufferedWriter(Charsets.UTF_8)
            for (line in conn.out) {
                writer.write(line)
                writer.write("\n")
                writer.flush()
            }
        } catch (e: IOException) {
            // fall through to drop
        } finally {
            drop(conn)
        }
    }

    private fun drop(conn: Conn) {
        if (conns.remove(conn.id) != null) {
            conn.out.close()
            runCatching { conn.socket.close() }
            channel.trySend(HostEvent.Disconnected(conn.id))
        }
    }

    override suspend fun send(connectionId: Int, message: NetworkMessage) {
        conns[connectionId]?.out?.trySend(Protocol.encode(message))
    }

    override suspend fun broadcast(message: NetworkMessage) {
        val line = Protocol.encode(message)
        conns.values.forEach { it.out.trySend(line) }
    }

    override suspend fun disconnect(connectionId: Int) {
        // Closing the queue lets the writer flush everything first, then drop() closes the socket.
        conns[connectionId]?.out?.close()
    }

    override suspend fun close() {
        closed = true
        runCatching { server.close() }
        conns.values.toList().forEach { drop(it) }
        channel.close()
    }

    companion object {
        const val HEARTBEAT_MS = 2_000L
        const val READ_TIMEOUT_MS = 7_000

        suspend fun open(scope: CoroutineScope, port: Int = 0): TcpHostTransport =
            withContext(Dispatchers.IO) { TcpHostTransport(ServerSocket(port, 16), scope) }
    }
}
