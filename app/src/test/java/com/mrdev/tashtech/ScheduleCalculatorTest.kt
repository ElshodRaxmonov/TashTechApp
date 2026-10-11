package com.mrdev.tashtech

import com.mrdev.tashtech.domain.model.ClassSession
import com.mrdev.tashtech.domain.schedule.ScheduleCalculator
import com.mrdev.tashtech.domain.schedule.SessionStatus
import com.mrdev.tashtech.domain.schedule.TimelineEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ScheduleCalculatorTest {

    // 2026-10-05 is a Monday.
    private val monday = LocalDate.of(2026, 10, 5)
    private val tuesday = monday.plusDays(1)

    private fun s(id: String, day: DayOfWeek, start: String, end: String) =
        ClassSession(
            id = id,
            day = day,
            start = LocalTime.parse(start),
            end = LocalTime.parse(end),
            subject = id,
            room = "R",
            teacher = "T",
        )

    private fun at(date: LocalDate, time: String) = LocalDateTime.of(date, LocalTime.parse(time))

    private val a = s("A", DayOfWeek.MONDAY, "09:30", "10:50")
    private val b = s("B", DayOfWeek.MONDAY, "11:00", "12:20")
    private val c = s("C", DayOfWeek.MONDAY, "14:00", "15:20")

    @Test fun statusBoundaries() {
        fun status(time: String) = ScheduleCalculator.statusOf(a, monday, at(monday, time))
        assertEquals(SessionStatus.Upcoming, status("09:29"))
        assertEquals(SessionStatus.Current, status("09:30"))
        assertEquals(SessionStatus.Current, status("10:49"))
        assertEquals(SessionStatus.Completed, status("10:50"))
    }

    @Test fun pastDaysAreCompletedFutureDaysUpcoming() {
        assertEquals(SessionStatus.Completed, ScheduleCalculator.statusOf(a, monday, at(tuesday, "08:00")))
        assertEquals(SessionStatus.Upcoming, ScheduleCalculator.statusOf(a, tuesday, at(monday, "23:00")))
    }

    @Test fun sessionsOnFiltersAndSorts() {
        val tue = s("T", DayOfWeek.TUESDAY, "08:00", "09:00")
        val result = ScheduleCalculator.sessionsOn(listOf(c, tue, a), DayOfWeek.MONDAY)
        assertEquals(listOf("A", "C"), result.map { it.id })
    }

    @Test fun timelineInsertsOnlyRealFreePeriods() {
        val timeline = ScheduleCalculator.dayTimeline(listOf(c, a, b), monday, at(monday, "12:30"))
        val summary = timeline.map {
            when (it) {
                is TimelineEntry.Class -> "${it.session.id}:${it.status}"
                is TimelineEntry.FreePeriod -> "free ${it.start}-${it.end}:${it.status}"
            }
        }
        // A→B gap is 10 min (a break), B→C gap is 100 min (free period, in progress).
        assertEquals(
            listOf("A:Completed", "B:Completed", "free 12:20-14:00:Current", "C:Upcoming"),
            summary,
        )
    }

    @Test fun freePeriodMeasuredFromLatestEndWhenOverlapping() {
        val long = s("L", DayOfWeek.MONDAY, "09:00", "12:00")
        val inner = s("I", DayOfWeek.MONDAY, "09:30", "10:00")
        val late = s("Z", DayOfWeek.MONDAY, "12:40", "13:00")
        val free = ScheduleCalculator.dayTimeline(listOf(long, inner, late), monday, at(monday, "08:00"))
            .filterIsInstance<TimelineEntry.FreePeriod>()
        assertEquals(1, free.size)
        assertEquals(LocalTime.of(12, 0), free.single().start)
        assertEquals(LocalTime.of(12, 40), free.single().end)
    }

    @Test fun emptyDayHasEmptyTimeline() {
        assertEquals(emptyList<TimelineEntry>(), ScheduleCalculator.dayTimeline(listOf(a), tuesday, at(monday, "08:00")))
    }

    @Test fun currentClass() {
        assertEquals("A", ScheduleCalculator.currentClass(listOf(a, b), at(monday, "10:00"))?.id)
        assertNull(ScheduleCalculator.currentClass(listOf(a, b), at(monday, "10:55")))
        assertNull(ScheduleCalculator.currentClass(listOf(a, b), at(tuesday, "10:00")))
    }

    @Test fun nextClassLaterToday() {
        val next = ScheduleCalculator.nextClass(listOf(a, b, c), at(monday, "10:00"))!!
        assertEquals("B", next.session.id)
        assertEquals(monday, next.date)
    }

    @Test fun classStartingExactlyNowIsNotNext() {
        val next = ScheduleCalculator.nextClass(listOf(a, b), at(monday, "09:30"))!!
        assertEquals("B", next.session.id)
    }

    @Test fun nextClassRollsToTomorrow() {
        val tue = s("T", DayOfWeek.TUESDAY, "08:00", "09:00")
        val next = ScheduleCalculator.nextClass(listOf(a, tue), at(monday, "15:00"))!!
        assertEquals("T", next.session.id)
        assertEquals(tuesday, next.date)
    }

    @Test fun nextClassWrapsToNextWeek() {
        val next = ScheduleCalculator.nextClass(listOf(a), at(monday, "12:00"))!!
        assertEquals(monday.plusDays(7), next.date)
    }

    @Test fun nextClassNullWhenNoSessions() {
        assertNull(ScheduleCalculator.nextClass(emptyList(), at(monday, "12:00")))
    }

    @Test fun weekDatesStartOnMonday() {
        val wednesday = monday.plusDays(2)
        val sunday = monday.plusDays(6)
        assertEquals(monday, ScheduleCalculator.weekDates(wednesday).first())
        assertEquals(sunday, ScheduleCalculator.weekDates(wednesday).last())
        assertEquals(monday, ScheduleCalculator.weekDates(sunday).first())
        assertEquals(7, ScheduleCalculator.weekDates(monday).size)
    }
}