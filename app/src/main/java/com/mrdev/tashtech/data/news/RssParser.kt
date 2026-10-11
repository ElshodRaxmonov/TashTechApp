package com.mrdev.tashtech.data.news

import com.mrdev.tashtech.domain.model.NewsArticle
import com.mrdev.tashtech.domain.model.NewsSource
import com.mrdev.tashtech.domain.news.NewsCategories
import org.xmlpull.v1.XmlPullParser
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private class RawItem {
    var title = ""; var link = ""; var description = ""; var content = ""
    var pubDate = ""; var creator = ""; var image: String? = null
    val categories = mutableListOf<String>()
}

internal object RssParser {

    /** Namespace processing must be off: elements are matched by their prefixed names (media:content…). */
    fun parse(parser: XmlPullParser, source: NewsSource, nowMillis: Long): List<NewsArticle> {
        val articles = mutableListOf<NewsArticle>()
        var item: RawItem? = null
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                if (parser.name == "item") item = RawItem() else item?.let { readField(parser, parser.name, it) }
            } else if (event == XmlPullParser.END_TAG && parser.name == "item") {
                item?.let { toArticle(it, source, nowMillis) }?.let(articles::add)
                item = null
            }
            event = parser.next()
        }
        return articles
    }

    private fun readField(p: XmlPullParser, name: String, item: RawItem) {
        when (name) {
            "title" -> item.title = readText(p)
            "link" -> item.link = readText(p)
            "description" -> item.description = readText(p)
            "content:encoded" -> item.content = readText(p)
            "pubDate", "dc:date" -> readText(p).let { if (item.pubDate.isEmpty()) item.pubDate = it }
            "category" -> readText(p).trim().takeIf { it.isNotEmpty() }?.let(item.categories::add)
            "dc:creator", "author" -> item.creator = readText(p)
            "media:content" -> {
                val type = p.getAttributeValue(null, "type").orEmpty()
                val medium = p.getAttributeValue(null, "medium").orEmpty()
                val url = p.getAttributeValue(null, "url")
                if (item.image == null && url != null && !type.startsWith("video") && medium != "video") item.image = url
            }
            "media:thumbnail" -> {
                val url = p.getAttributeValue(null, "url")
                if (url != null) { if (item.image == null) item.image = url }
                else readText(p).trim().takeIf { it.isNotEmpty() && item.image == null }?.let { item.image = it } // IEEE puts the URL in the element text
            }
            "enclosure" -> {
                val type = p.getAttributeValue(null, "type").orEmpty()
                val url = p.getAttributeValue(null, "url")
                if (item.image == null && url != null && type.startsWith("image")) item.image = url
            }
        }
    }

    /** Collects text until the element closes, tolerating nested tags. */
    private fun readText(p: XmlPullParser): String {
        val sb = StringBuilder()
        var depth = 1
        while (depth > 0) {
            when (p.next()) {
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> sb.append(p.text)
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> return sb.toString()
            }
        }
        return sb.toString()
    }

    private fun toArticle(raw: RawItem, source: NewsSource, nowMillis: Long): NewsArticle? {
        val url = raw.link.trim()
        val title = stripHtml(raw.title)
        if (title.isBlank() || !(url.startsWith("https://") || url.startsWith("http://"))) return null
        if (isPromotional(raw.categories, raw.creator)) return null
        val image = normalizeImageUrl(raw.image ?: firstImageIn(raw.content) ?: firstImageIn(raw.description))
        return NewsArticle(
            id = articleId(url),
            title = title,
            summary = cleanSummary(raw.description.ifBlank { raw.content }),
            imageUrl = image,
            articleUrl = url,
            source = source,
            publishedAtMillis = parseDate(raw.pubDate, nowMillis),
            category = NewsCategories.classify(raw.categories, title),
        )
    }

    /** TechCrunch mixes event ticket promos into the news feed. */
    private fun isPromotional(categories: List<String>, creator: String): Boolean =
        creator.trim().equals("TechCrunch Events", ignoreCase = true) ||
                categories.any { it.equals("TechCrunch Disrupt", true) || it.equals("Sponsored", true) }

    internal fun parseDate(raw: String, nowMillis: Long): Long {
        val s = raw.trim()
        if (s.isEmpty()) return 0L
        val millis = try {
            ZonedDateTime.parse(s, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
        } catch (e: DateTimeParseException) {
            try { OffsetDateTime.parse(s).toInstant().toEpochMilli() } catch (e2: DateTimeParseException) { return 0L }
        }
        return millis.coerceAtMost(nowMillis) // a clock-skewed future date shouldn't pin an article to the top
    }
}