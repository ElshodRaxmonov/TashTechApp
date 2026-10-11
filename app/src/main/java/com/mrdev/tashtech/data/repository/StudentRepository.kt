package com.mrdev.tashtech.data.repository

import android.content.Context
import com.mrdev.tashtech.data.local.StudentsParser
import com.mrdev.tashtech.domain.model.Student
import com.mrdev.tashtech.domain.student.StudentIdError
import com.mrdev.tashtech.domain.student.StudentIds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

sealed interface StudentLookup {
    data class Found(val student: Student) : StudentLookup
    data class Failed(val error: StudentIdError) : StudentLookup
}

/** Validation and lookup in one place, shared by onboarding and Settings. */
interface StudentRepository {
    suspend fun lookup(rawId: String): StudentLookup
}

internal fun lookupStudent(table: Map<String, Student>, rawId: String): StudentLookup {
    val id = StudentIds.normalize(rawId)
    return when {
        id.isEmpty() -> StudentLookup.Failed(StudentIdError.Empty)
        !StudentIds.isValidFormat(id) -> StudentLookup.Failed(StudentIdError.InvalidFormat)
        else -> table[id]?.let { StudentLookup.Found(it) } ?: StudentLookup.Failed(StudentIdError.NotFound)
    }
}

private const val STUDENTS_ASSET = "students.json"

class AssetStudentRepository(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : StudentRepository {

    private val appContext = context.applicationContext
    private val mutex = Mutex()
    private var cache: Map<String, Student>? = null // only successes are cached, so Retry can work

    override suspend fun lookup(rawId: String): StudentLookup {
        val id = StudentIds.normalize(rawId)
        // Cheap checks first: no need to open the file for an empty or malformed ID.
        if (id.isEmpty() || !StudentIds.isValidFormat(id)) return lookupStudent(emptyMap(), rawId)
        val table = try {
            table()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) { // missing asset, invalid JSON, unreadable file
            return StudentLookup.Failed(StudentIdError.Unavailable)
        }
        return lookupStudent(table, rawId)
    }

    private suspend fun table(): Map<String, Student> = mutex.withLock {
        cache ?: load().also { cache = it }
    }

    private suspend fun load(): Map<String, Student> = withContext(ioDispatcher) {
        val text = appContext.assets.open(STUDENTS_ASSET).bufferedReader().use { it.readText() }
        StudentsParser.parse(text)
    }
}