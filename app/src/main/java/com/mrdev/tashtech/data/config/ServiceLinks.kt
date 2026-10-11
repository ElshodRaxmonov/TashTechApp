package com.mrdev.tashtech.data.config

import java.net.URI
import java.net.URISyntaxException

/** Single source of truth for official TashTech service URLs. */
enum class AcademicService(val url: String) {
    Sis("https://sis.tashkenttech-edu.uz/login"),
    Lms("https://lms.tashkenttech-edu.uz/login/index.php");

    val host: String get() = URI(url).host
}
object OfficialDomain {
    const val DOMAIN = "tashkenttech-edu.uz"
    private const val MAX_URL_LENGTH = 2048

    fun hostOf(url: String): String? = parse(url)?.host?.lowercase()

    /** https only, official domain or a subdomain, no userinfo, no custom port. */
    fun isOfficial(url: String): Boolean {
        val uri = parse(url) ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        if (uri.userInfo != null) return false
        if (uri.port != -1 && uri.port != 443) return false
        val host = uri.host?.lowercase() ?: return false
        return host == DOMAIN || host.endsWith(".$DOMAIN")
    }
    private val BROWSER_EXTRA_HOSTS: Set<String> = emptySet()

    fun isBrowsable(url: String): Boolean {
        if (isOfficial(url)) return true
        val uri = parse(url) ?: return false
        return uri.scheme.equals("https", ignoreCase = true) && uri.userInfo == null &&
                uri.host?.lowercase() in BROWSER_EXTRA_HOSTS
    }

    /** http on an official host becomes https. Never the other direction, and no custom ports. */
    fun upgradeToHttps(url: String): String? {
        val text = url.trim()
        val uri = parse(text) ?: return null
        if (!uri.scheme.equals("http", ignoreCase = true) || uri.userInfo != null || uri.port != -1) return null
        val host = uri.host?.lowercase() ?: return null
        if (host != DOMAIN && !host.endsWith(".$DOMAIN")) return null
        return "https" + text.substring(4)
    }
    private fun parse(url: String): URI? =
        if (url.length > MAX_URL_LENGTH) null
        else try { URI(url.trim()) } catch (e: URISyntaxException) { null }
}