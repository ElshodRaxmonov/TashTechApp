package com.mrdev.tashtech.data.repository

import com.mrdev.tashtech.data.local.ReminderDao
import com.mrdev.tashtech.data.local.toDomain
import com.mrdev.tashtech.data.local.toEntity
import com.mrdev.tashtech.data.reminders.ReminderScheduler
import com.mrdev.tashtech.domain.model.Reminder
import com.mrdev.tashtech.domain.reminder.ReminderRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room is the source of truth; every write keeps the OS alarms in step with it. */
class ReminderRepository(
    private val dao: ReminderDao,
    private val scheduler: ReminderScheduler,
) {
    fun observeAll(): Flow<List<Reminder>> = dao.observeAll().map { list -> list.map { it.toDomain() } }


    fun observeActiveCount(): Flow<Int> = dao.observeActiveCount()
    suspend fun get(id: Long): Reminder? = dao.get(id)?.toDomain()

    /** Inserts (id = 0) or updates, then re-arms alarms. Returns the saved id. */
    suspend fun save(reminder: Reminder): Long {
        val normalized = reminder.copy(offsetsMinutes = ReminderRules.normalizeOffsets(reminder.offsetsMinutes))
        val id = dao.save(normalized.toEntity())
        scheduler.schedule(normalized.copy(id = id))
        return id
    }

    suspend fun setCompleted(id: Long, completed: Boolean) {
        dao.setCompleted(id, completed)
        dao.get(id)?.let { scheduler.schedule(it.toDomain()) } // cancels when completed, re-arms otherwise
    }

    suspend fun delete(id: Long) {
        scheduler.cancel(id)
        dao.delete(id)
    }

    suspend fun deleteCompleted() = dao.deleteCompleted() // completed reminders have no alarms left
    fun scheduleTestAlert(delayMillis: Long = 30_000) = scheduler.scheduleTest(delayMillis)
    suspend fun rescheduleAll() = dao.getActive().forEach { scheduler.schedule(it.toDomain()) }
}