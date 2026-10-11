package com.mrdev.tashtech.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ReminderEntity::class, NoteEntity::class, UserEventEntity::class, SavedArticleEntity::class],
    version = 4,
    autoMigrations = [
        AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(
            from = 3,
            to = 4
        ),
    ],
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun noteDao(): NoteDao
    abstract fun savedArticleDao(): SavedArticleDao
    abstract fun userEventDao(): UserEventDao
}