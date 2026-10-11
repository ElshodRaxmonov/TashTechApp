package com.mrdev.tashtech.widget

import android.content.Context
import com.mrdev.tashtech.domain.schedule.AppTime
import com.mrdev.tashtech.ui.home.HeroState
import com.mrdev.tashtech.ui.home.HomeUiState
import com.mrdev.tashtech.ui.home.buildHomeContent
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import kotlin.coroutines.cancellation.CancellationException

internal sealed interface HubData {
    data object SignedOut : HubData
    data class Ready(val content: HomeUiState.Content) : HubData
}

internal suspend fun loadHubData(context: Context): HubData {
    val c = container(context)
    val profile = c.userPreferences.profile.first() ?: return HubData.SignedOut
    suspend fun <T> safe(fallback: T, block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        fallback
    }

    val sessions = safe(emptyList()) { c.timetable.getSessions(profile.group) }
    val events = safe(emptyList()) { c.userEvents.observeAll().first() }
    val reminders = safe(emptyList()) { c.reminders.observeAll().first() }
    val now = LocalDateTime.now(AppTime.systemClock())
     return HubData.Ready(buildHomeContent(profile, sessions, now, reminders, events))
}

/** The next moment the widget's content can change: a class starting or ending, a reminder due, or midnight. */
internal fun nextWidgetRefresh(content: HomeUiState.Content): LocalDateTime? {
    val now = content.now
    val candidates = buildList {
        when (val hero = content.hero) {
            is HeroState.Current -> add(LocalDateTime.of(now.toLocalDate(), hero.session.end))
            is HeroState.Next -> if (hero.dayOffset == 0) add(
                LocalDateTime.of(
                    hero.date,
                    hero.session.start
                )
            )

            HeroState.None -> Unit
        }
        content.nextReminder?.reminder?.dueAt?.let(::add)
        add(now.toLocalDate().plusDays(1).atStartOfDay())
    }
    return candidates.filter { it.isAfter(now.plusSeconds(30)) }.minOrNull()
}