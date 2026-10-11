package com.mrdev.tashtech.data.news


import java.security.MessageDigest

internal fun articleId(url: String): String =
    MessageDigest.getInstance("SHA-1").digest(url.trim().toByteArray())
        .joinToString("") { "%02x".format(it) }.take(16)

private val EntityRegex = Regex("&(#x?[0-9a-fA-F]+|[a-zA-Z]+);")
private val NamedEntities = mapOf(
    "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ",
    "hellip" to "…", "mdash" to "—", "ndash" to "–", "lsquo" to "‘", "rsquo" to "’", "ldquo" to "“", "rdquo" to "”",
)

private fun decodeEntities(text: String): String = EntityRegex.replace(text) { m ->
    val body = m.groupValues[1]
    when {
        body.startsWith("#x", true) -> body.drop(2).toIntOrNull(16)
        body.startsWith("#") -> body.drop(1).toIntOrNull()
        else -> null
    }?.takeIf { Character.isValidCodePoint(it) }?.let { String(Character.toChars(it)) }
        ?: NamedEntities[body] ?: m.value
}

/** Feed summaries are HTML fragments: keep only readable text. */
internal fun stripHtml(html: String): String {
    var s = html.replace(Regex("(?is)<(script|style)[^>]*>.*?</\\1>"), " ")
    s = s.replace(Regex("(?i)<br\\s*/?>|</p>|</div>|</li>"), " ")
    s = s.replace(Regex("<[^>]*>"), "")
    return decodeEntities(s).replace(Regex("\\s+"), " ").trim()
}

internal fun truncateAtWord(text: String, max: Int): String {
    if (text.length <= max) return text
    val cut = text.lastIndexOf(' ', max)
    return text.substring(0, if (cut > max * 0.6) cut else max).trimEnd(' ', ',', ';', ':', '.', '-') + "…"
}

/** Drops WordPress "The post … appeared first on …" boilerplate, then shortens. */
internal fun cleanSummary(html: String, max: Int = 280): String =
    truncateAtWord(stripHtml(html).replace(Regex("\\s*The post .* appeared first on .*$"), ""), max)

private val ImgRegex = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
internal fun firstImageIn(html: String): String? = ImgRegex.find(html)?.groupValues?.get(1)

/** https only: plain-http or non-URL values are discarded so a missing image never breaks a card. */
internal fun normalizeImageUrl(raw: String?): String? {
    val url = raw?.trim().orEmpty()
    return when {
        url.startsWith("https://") -> url
        url.startsWith("//") -> "https:$url"
        url.startsWith("http://") -> "https://" + url.removePrefix("http://")
        else -> null
    }
}