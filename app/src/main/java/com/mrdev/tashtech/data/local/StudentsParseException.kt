package com.mrdev.tashtech.data.local

import com.mrdev.tashtech.domain.model.Sex
import com.mrdev.tashtech.domain.model.Student
import com.mrdev.tashtech.domain.student.StudentIds
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class StudentsParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

@Serializable
private data class StudentsFileDto(val students: Map<String, StudentDto>)

@Serializable
private data class StudentDto(val name: String, val group: Int, val sex: Int = -1)

object StudentsParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): Map<String, Student> {
        val dto = try {
            json.decodeFromString<StudentsFileDto>(text)
        } catch (e: SerializationException) {
            throw StudentsParseException("Invalid students JSON: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            throw StudentsParseException("Invalid students JSON: ${e.message}", e)
        }
        return buildMap {
            for ((rawId, s) in dto.students) {
                val id = StudentIds.normalize(rawId)
                if (!StudentIds.isValidFormat(id) || s.name.isBlank() || s.group <= 0) continue
                put(id, Student(id, s.name.trim(), s.group, Sex.fromCode(s.sex)))
            }
        }
    }
}