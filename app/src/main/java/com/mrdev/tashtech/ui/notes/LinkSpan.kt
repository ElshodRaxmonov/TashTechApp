package com.mrdev.tashtech.ui.notes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import java.net.URI
import java.net.URISyntaxException

internal data class LinkSpan(val start: Int, val end: Int, val url: String)

private val UrlRegex = Regex("""(?i)\b(?:https?://|www\.)[^\s<>"']+""")
private const val TrailingPunctuation = ".,;:!?)]}\"'"

/** http(s) and www. links only. Trailing sentence punctuation is not part of the link. */
internal fun findLinks(text: String): List<LinkSpan> = buildList {
    for (match in UrlRegex.findAll(text)) {
        var end = match.range.last + 1
        while (end > match.range.first && text[end - 1] in TrailingPunctuation) end--
        val raw = text.substring(match.range.first, end)
        val url = if (raw.startsWith("www.", ignoreCase = true)) "https://$raw" else raw
        val host = try { URI(url).host } catch (e: URISyntaxException) { null }
        if (!host.isNullOrBlank() && host.contains('.')) add(LinkSpan(match.range.first, end, url))
    }
}

internal fun linkifiedText(text: String, linkColor: Color, onOpen: (String) -> Unit): AnnotatedString =
    buildAnnotatedString {
        var cursor = 0
        for (link in findLinks(text)) {
            append(text.substring(cursor, link.start))
            withLink(
                LinkAnnotation.Clickable(
                    tag = link.url,
                    styles = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)),
                    linkInteractionListener = LinkInteractionListener { onOpen(link.url) },
                ),
            ) { append(text.substring(link.start, link.end)) }
            cursor = link.end
        }
        append(text.substring(cursor))
    }