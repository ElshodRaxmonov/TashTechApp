package com.mrdev.tashtech.tashbox.network

import kotlinx.coroutines.flow.Flow

/** One link between a client and the host (client side view). The engine never sees sockets. */
interface Transport {
    /** Incoming messages. The flow completes when the connection is gone. */
    val messages: Flow<NetworkMessage>
    suspend fun send(message: NetworkMessage)
    suspend fun close()
}

sealed interface HostEvent {
    data class Connected(val connectionId: Int) : HostEvent
    data class Received(val connectionId: Int, val message: NetworkMessage) : HostEvent
    data class Disconnected(val connectionId: Int) : HostEvent
}

/** Host side: many clients behind one object. */
interface HostTransport {
    val events: Flow<HostEvent>
    suspend fun send(connectionId: Int, message: NetworkMessage)
    suspend fun broadcast(message: NetworkMessage)
    /** Flushes queued messages, then closes the connection. */
    suspend fun disconnect(connectionId: Int)
    suspend fun close()
}
