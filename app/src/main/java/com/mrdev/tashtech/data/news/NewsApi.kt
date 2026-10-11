package com.mrdev.tashtech.data.news

import android.util.Xml
import com.mrdev.tashtech.domain.model.NewsArticle
import com.mrdev.tashtech.domain.model.NewsSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Clock

class NewsApi(
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun fetch(source: NewsSource): List<NewsArticle> = withContext(io) {
        val connection = (URL(source.feedUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 10_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "TashTechStudentCompanion/1.0 (Android; RSS reader)")
            setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml")
        }
        try {
            if (connection.responseCode !in 200..299) throw IOException("HTTP ${connection.responseCode}")
            connection.inputStream.use { stream ->
                val parser = Xml.newPullParser().apply {
                    setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                    setInput(stream, null)
                }
                RssParser.parse(parser, source, clock.millis())
            }
        } finally {
            connection.disconnect()
        }
    }
}