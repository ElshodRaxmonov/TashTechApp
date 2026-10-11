package com.mrdev.tashtech.tashbox.room

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * The only thing the QR code carries: where the host is and which room to join.
 * Nothing sensitive: a LAN address, a port, a 4-digit room number and a short join token.
 *
 * Format: tashbox://join?v=1&h=192.168.1.24&p=51234&r=4821&t=ab12cd&n=Elshod
 */
data class QrPayload(
    val version: Int,
    val host: String,
    val port: Int,
    val roomId: String,
    val token: String,
    val hostName: String
) {
    fun encode(): String =
        "$SCHEME?v=$version&h=${enc(host)}&p=$port&r=${enc(roomId)}&t=${enc(token)}&n=${enc(hostName)}"

    companion object {
        const val SCHEME = "tashbox://join"

        private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

        /** Returns null for anything that is not a valid TashBox room code pointing at a local address. */
        fun parse(text: String): QrPayload? {
            if (!text.startsWith("$SCHEME?")) return null
            return try {
                val q = text.substringAfter('?').split('&').filter { it.contains('=') }.associate {
                    val (k, v) = it.split('=', limit = 2)
                    k to URLDecoder.decode(v, "UTF-8")
                }
                val p = QrPayload(
                    version = q.getValue("v").toInt(),
                    host = q.getValue("h"),
                    port = q.getValue("p").toInt(),
                    roomId = q.getValue("r"),
                    token = q.getValue("t"),
                    hostName = q["n"].orEmpty()
                )
                if (isPrivateIpv4(p.host) && p.port in 1..65535 && p.roomId.isNotBlank() && p.token.isNotBlank()) p else null
            } catch (e: Exception) {
                null
            }
        }

        /** TashBox only ever connects to local-network addresses. */
        fun isPrivateIpv4(host: String): Boolean {
            val parts = host.split('.')
            if (parts.size != 4) return false
            val n = parts.map { it.toIntOrNull() ?: return false }
            if (n.any { it !in 0..255 }) return false
            return n[0] == 10 || (n[0] == 172 && n[1] in 16..31) || (n[0] == 192 && n[1] == 168) || (n[0] == 169 && n[1] == 254)
        }
    }
}
