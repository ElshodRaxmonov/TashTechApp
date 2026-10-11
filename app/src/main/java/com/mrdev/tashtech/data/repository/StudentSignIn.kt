package com.mrdev.tashtech.data.repository

import com.mrdev.tashtech.data.preferences.UserPreferencesRepository
import com.mrdev.tashtech.domain.student.StudentIdError
import java.io.IOException

class StudentSignIn(
    private val students: StudentRepository,
    private val prefs: UserPreferencesRepository,
) {
    /** Validates, looks up and saves the student. Returns null on success, otherwise the reason. */
    suspend fun signIn(rawId: String): StudentIdError? {
        val student = when (val result = students.lookup(rawId)) {
            is StudentLookup.Found -> result.student
            is StudentLookup.Failed -> return result.error
        }
        return try {
            prefs.saveStudent(student)
            null
        } catch (e: IOException) {
            StudentIdError.SaveFailed
        }
    }
}