package com.mrdev.tashtech.tashbox.network

import com.mrdev.tashtech.tashbox.engine.RoundResultData
import com.mrdev.tashtech.tashbox.leaderboard.GameResult
import com.mrdev.tashtech.tashbox.puzzle.Difficulty
import com.mrdev.tashtech.tashbox.puzzle.Puzzle
import com.mrdev.tashtech.tashbox.room.LobbyPlayer
import com.mrdev.tashtech.tashbox.room.RejectReason
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val PROTOCOL_VERSION = 1

/**
 * Versioned wire protocol. One JSON object per line (newline-delimited JSON).
 * Clients only ever send: join, ready, answer(roomId, playerId, round, answerIndex), leave, heartbeat.
 * They NEVER send scores or timing; the host decides correctness, time and points.
 */
@Serializable
sealed interface NetworkMessage

// ---- client -> host ---------------------------------------------------------------------------

@Serializable @SerialName("join")
data class JoinRoom(
    val protocolVersion: Int = PROTOCOL_VERSION,
    val roomId: String,
    val token: String,
    val playerName: String,
    /** Set when re-joining after a dropped connection. */
    val playerId: String? = null
) : NetworkMessage

@Serializable @SerialName("ready")
data class Ready(val ready: Boolean) : NetworkMessage

@Serializable @SerialName("answer")
data class AnswerSubmit(val roomId: String, val playerId: String, val round: Int, val answerIndex: Int) : NetworkMessage

@Serializable @SerialName("leave")
object LeaveRoom : NetworkMessage

@Serializable @SerialName("hb")
object Heartbeat : NetworkMessage

// ---- host -> clients --------------------------------------------------------------------------

@Serializable @SerialName("joined")
data class JoinAccepted(
    val roomId: String,
    val roomName: String,
    val playerId: String,
    val players: List<LobbyPlayer>,
    val reconnected: Boolean,
    val totalRounds: Int,
    val level: Difficulty = Difficulty.MEDIUM
) : NetworkMessage

@Serializable @SerialName("rejected")
data class JoinRejected(val reason: RejectReason) : NetworkMessage

/** The host picked another difficulty in the lobby. */
@Serializable @SerialName("level")
data class LevelChange(val level: Difficulty) : NetworkMessage

@Serializable @SerialName("players")
data class PlayerList(val players: List<LobbyPlayer>) : NetworkMessage

@Serializable @SerialName("start")
data class StartGame(val countdownMillis: Long, val totalRounds: Int) : NetworkMessage

/** [flashMillis]/[answerMillis] are REMAINING durations, so a reconnecting client lands on the right time. */
@Serializable @SerialName("round")
data class RoundStart(
    val round: Int,
    val totalRounds: Int,
    val puzzle: Puzzle,
    val flashMillis: Long,
    val answerMillis: Long
) : NetworkMessage

/** Someone locked in an answer (not which one, and not whether it is right). */
@Serializable @SerialName("answered")
data class PlayerAnswered(val round: Int, val playerId: String) : NetworkMessage

@Serializable @SerialName("result")
data class RoundResult(val result: RoundResultData) : NetworkMessage

@Serializable @SerialName("finished")
data class GameFinished(val result: GameResult) : NetworkMessage

@Serializable @SerialName("rematch")
object Rematch : NetworkMessage

@Serializable @SerialName("p_disc")
data class PlayerDisconnected(val playerId: String, val graceMillis: Long) : NetworkMessage

@Serializable @SerialName("p_back")
data class PlayerReconnected(val playerId: String) : NetworkMessage

@Serializable @SerialName("p_left")
data class PlayerLeft(val playerId: String) : NetworkMessage

@Serializable @SerialName("error")
data class ErrorMessage(val code: String, val message: String) : NetworkMessage

object Protocol {
    const val MAX_LINE_CHARS = 128 * 1024

    val json: Json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(message: NetworkMessage): String =
        json.encodeToString(NetworkMessage.serializer(), message)

    /** Returns null for malformed / unknown / invalid messages instead of throwing. */
    fun decode(line: String): NetworkMessage? =
        try {
            json.decodeFromString(NetworkMessage.serializer(), line)
        } catch (e: Exception) {
            null
        }
}
