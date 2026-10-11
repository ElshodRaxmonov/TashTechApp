package com.mrdev.tashtech.ui.navigation

import kotlinx.serialization.Serializable



@Serializable
data object OnboardingRoute
@Serializable
data object TashBoxRoute
@Serializable data object NewsRoute
@Serializable data class NewsDetailsRoute(val id: String)

@Serializable data object ProfileRoute
@Serializable data object SettingsRoute
@Serializable
data object HomeRoute
@Serializable
data object ScheduleRoute
@Serializable
data object RemindersRoute
@Serializable
data object ServicesRoute
@Serializable data class BrowserRoute(val url: String, val article: Boolean = false)
@Serializable data object ScannerRoute
@Serializable data object NotesRoute

@Serializable data class ReminderEditorRoute(
    val reminderId: Long = 0L,
    val prefillTitle: String = "",
    val prefillNote: String = "",
)