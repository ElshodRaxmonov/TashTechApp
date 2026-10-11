package com.mrdev.tashtech.ui.navigation

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.mrdev.tashtech.MainActivity

sealed interface LaunchTarget {
    data object Scanner : LaunchTarget
    data object Game : LaunchTarget
    data object News : LaunchTarget
    data class Article(val id: String) : LaunchTarget
    data class Browser(val url: String) : LaunchTarget // resolved from our own cache, never from caller input
}

object AppLinks {
    const val ACTION_OPEN_SCANNER = "com.mrdev.tashtech.action.OPEN_SCANNER"
    const val ACTION_OPEN_GAME = "com.mrdev.tashtech.action.OPEN_GAME"
    const val ACTION_OPEN_NEWS = "com.mrdev.tashtech.action.OPEN_NEWS"
    const val ACTION_OPEN_ARTICLE = "com.mrdev.tashtech.action.OPEN_ARTICLE"
    const val EXTRA_ARTICLE_ID = "article_id"
    private val ArticleId = Regex("[0-9a-f]{16}")

    /** An unknown action, or a malformed id, is simply ignored. */
    fun parse(intent: Intent?): LaunchTarget? = when (intent?.action) {
        ACTION_OPEN_SCANNER -> LaunchTarget.Scanner
        ACTION_OPEN_GAME -> LaunchTarget.Game
        ACTION_OPEN_NEWS -> LaunchTarget.News
        ACTION_OPEN_ARTICLE -> intent.getStringExtra(EXTRA_ARTICLE_ID)?.takeIf { ArticleId.matches(it) }?.let(LaunchTarget::Article)
        else -> null
    }

    /** Explicit intent to our own activity. Reuses a running instance (delivered via onNewIntent). */
    fun intentFor(context: Context, action: String, articleId: String? = null): Intent =
        Intent(context, MainActivity::class.java).setAction(action).apply {
            if (articleId != null) {
                putExtra(EXTRA_ARTICLE_ID, articleId)
                data = "tashtech://news/$articleId".toUri() // gives each article its own PendingIntent
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
}