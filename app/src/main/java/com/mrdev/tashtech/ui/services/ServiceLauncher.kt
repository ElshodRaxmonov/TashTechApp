package com.mrdev.tashtech.ui.services

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import com.mrdev.tashtech.data.config.AcademicService
import com.mrdev.tashtech.ui.theme.BrandPalette

/**
 * Opens the official page in a Custom Tab. The app never sees credentials: sign-in and
 * password saving are handled by the browser. Without a Custom Tabs provider this degrades
 * to the default browser. Returns false only if no browser exists at all.
 */
fun openService(context: Context, service: AcademicService): Boolean = openUrl(context, service.url)


fun openUrl(context: Context, url: String): Boolean {
    if (!url.startsWith("https://") && !url.startsWith("http://")) return false
    val light = CustomTabColorSchemeParams.Builder()
        .setToolbarColor(BrandPalette.Brand40.toArgb())
        .build()
    val dark = CustomTabColorSchemeParams.Builder()
        .setToolbarColor(BrandPalette.Brand30.toArgb())
        .build()
    val intent = CustomTabsIntent.Builder()
        .setShowTitle(true)
        .setColorScheme(
            when (AppCompatDelegate.getDefaultNightMode()) {
                AppCompatDelegate.MODE_NIGHT_YES -> CustomTabsIntent.COLOR_SCHEME_DARK
                AppCompatDelegate.MODE_NIGHT_NO -> CustomTabsIntent.COLOR_SCHEME_LIGHT
                else -> CustomTabsIntent.COLOR_SCHEME_SYSTEM
            },
        )
        .build()
    return try {
        intent.launchUrl(context, url.toUri())
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

/** Hands an http(s) link to whichever app handles it (Telegram, Instagram, a browser...). Falls back to a Custom Tab. */
fun openLinkExternally(context: Context, url: String): Boolean {
    if (!url.startsWith("https://", ignoreCase = true) && !url.startsWith(
            "http://",
            ignoreCase = true
        )
    ) return false
    val intent = Intent(Intent.ACTION_VIEW, url.toUri()).addCategory(Intent.CATEGORY_BROWSABLE)
    return try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        openUrl(context, url)
    }
}
