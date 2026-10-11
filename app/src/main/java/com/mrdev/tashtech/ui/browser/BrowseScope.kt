package com.mrdev.tashtech.ui.browser

import com.mrdev.tashtech.data.config.OfficialDomain
import java.net.URI
import java.net.URISyntaxException

/** What the in-app browser may load: the official TashTech domain, or (article mode) the article's own site. */
internal class BrowseScope(initialUrl: String, private val articleMode: Boolean) {
    private val articleSite: String? = if (articleMode) OfficialDomain.hostOf(initialUrl)?.let(::siteOf) else null

    /** Automatic (non-tapped) redirects to other https hosts: only for news sites, never for SIS/LMS. */
    val allowsAutoRedirects: Boolean get() = articleMode

    fun allows(url: String): Boolean =
        if (!articleMode) OfficialDomain.isBrowsable(url) else isHttpsSameSite(url)

    private fun isHttpsSameSite(url: String): Boolean {
        val uri = try { URI(url.trim()) } catch (e: URISyntaxException) { return false }
        val host = uri.host?.lowercase() ?: return false
        return uri.scheme.equals("https", true) && uri.userInfo == null && siteOf(host) == articleSite
    }
}

/** "spectrum.ieee.org" → "ieee.org". Good enough for these publishers; no public-suffix list needed. */
internal fun siteOf(host: String): String = host.lowercase().split('.').takeLast(2).joinToString(".")