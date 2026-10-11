package com.mrdev.tashtech.tashbox.room

import kotlinx.serialization.Serializable
import java.security.SecureRandom

@Serializable
data class LobbyPlayer(
    val id: String,
    val name: String,
    val ready: Boolean = false,
    val connected: Boolean = true,
    val isHost: Boolean = false
)

@Serializable
enum class RejectReason { VERSION_MISMATCH, WRONG_ROOM, ROOM_FULL, MATCH_IN_PROGRESS }

sealed interface JoinResult {
    data class Joined(val player: LobbyPlayer, val reconnected: Boolean) : JoinResult
    data class Rejected(val reason: RejectReason) : JoinResult
}

/**
 * Pure bookkeeping for one local room: who is in it, names, capacity, join token.
 * No networking here; HostSession feeds it join/leave events.
 */
class RoomManager(
    val roomId: String,
    val token: String,
    hostName: String,
    private val maxPlayers: Int = MAX_PLAYERS,
    private val minPlayers: Int = MIN_PLAYERS,
    private val newId: () -> String = { randomHex(4) }
) {
    private val players = LinkedHashMap<String, LobbyPlayer>()

    var locked: Boolean = false
        private set

    init {
        players[HOST_ID] = LobbyPlayer(HOST_ID, cleanName(hostName), ready = true, connected = true, isHost = true)
    }

    val hostDisplayName: String get() = players.getValue(HOST_ID).name
    val roomName: String get() = "$hostDisplayName's Room"
    val canStart: Boolean get() = !locked && connectedCount() >= minPlayers

    fun list(): List<LobbyPlayer> = players.values.toList()
    fun connectedCount(): Int = players.values.count { it.connected }
    fun lock() { locked = true }

    fun join(roomId: String, token: String, name: String, existingId: String?): JoinResult {
        if (roomId != this.roomId || token != this.token) return JoinResult.Rejected(RejectReason.WRONG_ROOM)
        val existing = existingId?.let { players[it] }
        if (existing != null && !existing.isHost) {
            val back = existing.copy(connected = true, ready = true)
            players[back.id] = back
            return JoinResult.Joined(back, reconnected = true)
        }
        if (locked) return JoinResult.Rejected(RejectReason.MATCH_IN_PROGRESS)
        if (players.size >= maxPlayers) return JoinResult.Rejected(RejectReason.ROOM_FULL)
        val player = LobbyPlayer(newId(), uniqueName(cleanName(name)), ready = false, connected = true)
        players[player.id] = player
        return JoinResult.Joined(player, reconnected = false)
    }

    fun setReady(id: String, ready: Boolean) { players[id]?.let { players[id] = it.copy(ready = ready) } }
    fun setConnected(id: String, connected: Boolean) { players[id]?.let { players[id] = it.copy(connected = connected) } }
    fun remove(id: String) { if (id != HOST_ID) players.remove(id) }

    /** After a finished match: unlock the room and drop players who are gone. */
    fun resetForRematch() {
        locked = false
        players.values.filter { !it.connected && !it.isHost }.forEach { players.remove(it.id) }
    }

    private fun uniqueName(base: String): String {
        val taken = players.values.map { it.name.lowercase() }.toSet()
        if (base.lowercase() !in taken) return base
        var n = 2
        while ("$base $n".lowercase() in taken) n++
        return "$base $n"
    }

    companion object {
        const val MAX_PLAYERS = 6
        const val MIN_PLAYERS = 2
        const val HOST_ID = "host"

        fun create(hostName: String, rnd: SecureRandom = SecureRandom()): RoomManager =
            RoomManager(
                roomId = "%04d".format(rnd.nextInt(10_000)),
                token = randomHex(3, rnd),
                hostName = hostName
            )

        fun cleanName(raw: String): String {
            val n = raw.trim().replace(Regex("\\s+"), " ").take(20)
            return n.ifBlank { "Player" }
        }

        fun randomHex(bytes: Int, rnd: SecureRandom = SecureRandom()): String {
            val b = ByteArray(bytes).also { rnd.nextBytes(it) }
            return b.joinToString("") { "%02x".format(it) }
        }
    }
}
