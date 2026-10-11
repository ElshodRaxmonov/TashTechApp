package com.mrdev.tashtech.data.local

import com.mrdev.tashtech.domain.model.ClassSession
import com.mrdev.tashtech.domain.model.LessonType
import com.mrdev.tashtech.domain.model.Timetable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNames
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeParseException

class TimetableParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

@Serializable
private data class TimetableDto(val groups: Map<String, List<LessonDto>>)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
private data class LessonDto(
    val day: String,
    val startTime: String,
    val endTime: String,
    val subject: String,
    @JsonNames("type", "lesson_type", "lessontype") val lessonType: String = "",
    val room: String = "",
    val teacher: String = "",
)

/** Strict about structure and values, lenient about unknown keys (so the dataset can grow). */
object TimetableParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): Timetable {
        val dto = try {
            json.decodeFromString<TimetableDto>(text)
        } catch (e: SerializationException) {
            throw TimetableParseException("Invalid timetable JSON: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            throw TimetableParseException("Invalid timetable JSON: ${e.message}", e)
        }
        return Timetable(
            dto.groups.mapValues { (group, lessons) ->
                lessons
                    .mapIndexed { index, lesson -> lesson.toSession(group, index) }
                    .sortedWith(compareBy({ it.day }, { it.start }, { it.end }))
            },
        )
    }

    private fun LessonDto.toSession(group: String, index: Int): ClassSession {

        val where = "group $group, lesson #${index + 1}"
        val dayOfWeek = DayOfWeek.entries.firstOrNull { it.name.equals(day.trim(), ignoreCase = true) }
            ?: throw TimetableParseException("Unknown day '$day' ($where)")
        val start = parseTime(startTime, where)
        val end = parseTime(endTime, where)
        if (!end.isAfter(start)) {
            throw TimetableParseException("endTime must be after startTime ($where)")
        }
        return ClassSession(
            id = "$group#$index",
            day = dayOfWeek,
            start = start,
            end = end,
            subject = subject.trim(),
            room = room.trim(),
            teacher = teacher.trim(),
            lessonType = LessonType.parse(lessonType),
        )
    }

    private fun parseTime(value: String, where: String): LocalTime = try {
        LocalTime.parse(value.trim())
    } catch (e: DateTimeParseException) {
        throw TimetableParseException("Bad time '$value' ($where); expected HH:mm", e)
    }
}
