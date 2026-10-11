package com.mrdev.tashtech.data.repository

import android.content.Context
import com.mrdev.tashtech.data.local.TimetableParser
import com.mrdev.tashtech.domain.model.ClassSession
import com.mrdev.tashtech.domain.model.Timetable
import com.mrdev.tashtech.domain.util.NaturalOrder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** The seam for a future remote source: the UI only ever sees this interface. */
interface TimetableRepository {
    suspend fun getGroups(): List<String>

    /** Sessions for a group, sorted by day then start time. Unknown group → empty list. */
    suspend fun getSessions(group: String): List<ClassSession>
}

private const val TIMETABLE_ASSET = "timetable.json"

class AssetTimetableRepository(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TimetableRepository {

    private val appContext = context.applicationContext
    private val mutex = Mutex()
    private var cached: Timetable? = null

    override suspend fun getGroups(): List<String> =
        timetable().groups.keys.sortedWith(NaturalOrder)

    override suspend fun getSessions(group: String): List<ClassSession> =
        timetable().groups[group].orEmpty()

    private suspend fun timetable(): Timetable = mutex.withLock {
        cached ?: load().also { cached = it }
    }

    private suspend fun load(): Timetable = withContext(ioDispatcher) {
        val text = appContext.assets.open(TIMETABLE_ASSET).bufferedReader().use { it.readText() }
        TimetableParser.parse(text)
    }
}