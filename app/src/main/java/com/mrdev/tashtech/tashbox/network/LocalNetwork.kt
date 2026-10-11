package com.mrdev.tashtech.tashbox.network

import java.net.Inet4Address
import java.net.NetworkInterface
import kotlin.collections.iterator

object LocalNetwork {
    /**
     * Finds this phone's address on the local Wi-Fi (or on its own hotspot). Returns null when the
     * phone is only on mobile data / offline. Needs no permissions and no internet.
     */
    fun findLocalIpv4(): String? {
        val candidates = ArrayList<Pair<Int, String>>()
        val interfaces = try {
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
        } catch (e: Exception) {
            emptyList()
        }
        for (nif in interfaces) {
            val up = runCatching { nif.isUp && !nif.isLoopback }.getOrDefault(false)
            if (!up) continue
            val name = nif.name.lowercase()
            val priority = when {
                name.startsWith("wlan") || name.startsWith("swlan") || name.startsWith("ap") || name.startsWith("eth") -> 0
                name.startsWith("rmnet") || name.startsWith("ccmni") || name.startsWith("v4-") || name.startsWith("tun") -> 2
                else -> 1
            }
            for (addr in nif.inetAddresses) {
                if (addr is Inet4Address && addr.isSiteLocalAddress) {
                    addr.hostAddress?.let { candidates += priority to it }
                }
            }
        }
        val best = candidates.minByOrNull { it.first } ?: return null
        return if (best.first >= 2) null else best.second // mobile-data-only addresses are not usable
    }
}
