package com.mrdev.tashtech

import com.mrdev.tashtech.data.local.TimetableParseException
import com.mrdev.tashtech.data.local.TimetableParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class TimetableParserTest {

    private fun json(lesson: String) = """{ "groups": { "SE-24-01": [ $lesson ] } }"""

    private val valid = """
        { "day": "Monday", "startTime": "09:30", "endTime": "10:50",
          "subject": "Data Structures", "room": "B-204", "teacher": "Dr. Karimov" }
    """.trimIndent()

    @Test fun parsesAllFields() {
        val s = TimetableParser.parse(json(valid)).groups.getValue("SE-24-01").single()
        assertEquals(DayOfWeek.MONDAY, s.day)
        assertEquals(LocalTime.of(9, 30), s.start)
        assertEquals(LocalTime.of(10, 50), s.end)
        assertEquals("Data Structures", s.subject)
        assertEquals("B-204", s.room)
        assertEquals("Dr. Karimov", s.teacher)
        assertEquals("SE-24-01#0", s.id)
        assertEquals(80, s.duration.toMinutes())
    }

    @Test fun ignoresUnknownKeys() {
        val text = """{ "version": 2, "groups": { "G": [ { "day": "friday", "startTime": "08:00",
            "endTime": "09:00", "subject": "S", "type": "lab" } ] } }"""
        val s = TimetableParser.parse(text).groups.getValue("G").single()
        assertEquals(DayOfWeek.FRIDAY, s.day)
        assertEquals("", s.room)
        assertEquals("", s.teacher)
    }

    @Test fun sortsByDayThenStart() {
        val text = """{ "groups": { "G": [
            { "day": "Tuesday", "startTime": "09:00", "endTime": "10:00", "subject": "C" },
            { "day": "Monday", "startTime": "11:00", "endTime": "12:00", "subject": "B" },
            { "day": "Monday", "startTime": "09:00", "endTime": "10:00", "subject": "A" } ] } }"""
        val subjects = TimetableParser.parse(text).groups.getValue("G").map { it.subject }
        assertEquals(listOf("A", "B", "C"), subjects)
    }

    @Test fun keepsMultipleGroups() {
        val text = """{ "groups": { "A": [], "B": [] } }"""
        assertEquals(setOf("A", "B"), TimetableParser.parse(text).groups.keys)
    }

    @Test fun rejectsUnknownDay() = assertFails(valid.replace("Monday", "Funday"))

    @Test fun rejectsBadTime() = assertFails(valid.replace("09:30", "9.30"))

    @Test fun rejectsEndBeforeStart() = assertFails(valid.replace("10:50", "09:00"))

    @Test fun rejectsMissingRequiredField() = assertFails(valid.replace("\"subject\": \"Data Structures\",", ""))

    @Test fun rejectsMalformedJson() {
        try {
            TimetableParser.parse("{ not json")
            throw AssertionError("expected TimetableParseException")
        } catch (e: TimetableParseException) {
            assertTrue(e.message!!.startsWith("Invalid timetable JSON"))
        }
    }

    private fun assertFails(lesson: String) {
        try {
            TimetableParser.parse(json(lesson))
            throw AssertionError("expected TimetableParseException")
        } catch (_: TimetableParseException) {
        }
    }
}