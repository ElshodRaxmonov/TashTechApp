package com.mrdev.tashtech.tashbox.network

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** In-memory transport pair for unit tests: no sockets, fully deterministic. */
class FakeHostTransport : HostTransport {
    private val channel = Channel<HostEvent>(Channel.UNLIMITED)
    private val clients = ConcurrentHashMap<Int, FakeClientTransport>()
    private val nextId = AtomicInteger(1)

    override val events: Flow<HostEvent> = channel.receiveAsFlow()

    fun connectClient(): FakeClientTransport {
        val id = nextId.getAndIncrement()
        val client = FakeClientTransport(id, this)
        clients[id] = client
        channel.trySend(HostEvent.Connected(id))
        return client
    }

    internal fun fromClient(id: Int, message: NetworkMessage) { channel.trySend(HostEvent.Received(id, message)) }

    internal fun clientClosed(id: Int) {
        if (clients.remove(id) != null) channel.trySend(HostEvent.Disconnected(id))
    }

    /** Simulates a Wi-Fi drop: the host sees a disconnect, the client's message flow completes. */
    fun dropClient(id: Int) {
        clients.remove(id)?.let {
            it.closeInbox()
            channel.trySend(HostEvent.Disconnected(id))
        }
    }

    override suspend fun send(connectionId: Int, message: NetworkMessage) { clients[connectionId]?.deliver(message) }
    override suspend fun broadcast(message: NetworkMessage) { clients.values.forEach { it.deliver(message) } }
    override suspend fun disconnect(connectionId: Int) { dropClient(connectionId) }

    override suspend fun close() {
        clients.keys.toList().forEach { dropClient(it) }
        channel.close()
    }
}

class FakeClientTransport internal constructor(
    val connectionId: Int,
    private val host: FakeHostTransport
) : Transport {
    private val inbox = Channel<NetworkMessage>(Channel.UNLIMITED)
    override val messages: Flow<NetworkMessage> = inbox.receiveAsFlow()

    override suspend fun send(message: NetworkMessage) { host.fromClient(connectionId, message) }
    override suspend fun close() { host.clientClosed(connectionId); inbox.close() }

    internal fun deliver(message: NetworkMessage) { inbox.trySend(message) }
    internal fun closeInbox() { inbox.close() }
}
