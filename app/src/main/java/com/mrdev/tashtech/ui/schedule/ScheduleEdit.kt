package com.mrdev.tashtech.ui.schedule

import com.mrdev.tashtech.domain.model.ClassSession
import com.mrdev.tashtech.domain.model.SessionOrigin
import com.mrdev.tashtech.domain.model.UserEvent

/** Only the student's own events resolve to something editable. Official timetable entries never do. */
internal fun resolveEditTarget(events: List<UserEvent>, session: ClassSession): UserEvent? {
    if (session.origin != SessionOrigin.Custom) return null
    val id = session.eventId ?: return null
    return events.firstOrNull { it.id == id }
}