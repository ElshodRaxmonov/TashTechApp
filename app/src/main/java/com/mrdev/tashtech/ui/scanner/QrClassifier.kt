package com.mrdev.tashtech.ui.scanner

import com.mrdev.tashtech.data.config.OfficialDomain
import java.net.URI
import java.net.URISyntaxException

sealed interface QrContent {
    data class Link(
        val url: String,
        val host: String,
        val isOfficial: Boolean,
        val isSecure: Boolean,
    ) : QrContent

    data class Text(val text: String) : QrContent
}

private const val MAX_LINK_LENGTH = 2048

/** Only explicit http(s) URLs with a host become links; everything else is plain text and never launched. */
internal fun classifyQr(raw: String): QrContent {
    val text = raw.trim()
    val uri = try { URI(text) } catch (e: URISyntaxException) { null }
    val scheme = uri?.scheme?.lowercase()
    val host = uri?.host?.lowercase()
    if (uri != null && (scheme == "https" || scheme == "http") && !host.isNullOrEmpty() && text.length <= MAX_LINK_LENGTH) {
        return QrContent.Link(
            url = text,
            host = host,
            isOfficial = OfficialDomain.isOfficial(text),
            isSecure = scheme == "https",
        )
    }
    return QrContent.Text(text)
}