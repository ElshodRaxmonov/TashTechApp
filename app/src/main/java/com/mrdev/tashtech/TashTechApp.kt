package com.mrdev.tashtech

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import coil3.SingletonImageLoader
import com.mrdev.tashtech.data.local.AppDatabase
import com.mrdev.tashtech.data.news.NewsApi
import com.mrdev.tashtech.data.preferences.UserPreferencesRepository
import com.mrdev.tashtech.data.preferences.UserProfile
import com.mrdev.tashtech.data.reminders.AlarmReminderScheduler
import com.mrdev.tashtech.data.reminders.ReminderNotifications
import com.mrdev.tashtech.data.repository.AssetStudentRepository
import com.mrdev.tashtech.data.repository.AssetTimetableRepository
import com.mrdev.tashtech.data.repository.NewsRepository
import com.mrdev.tashtech.data.repository.NoteRepository
import com.mrdev.tashtech.data.repository.ReminderRepository
import com.mrdev.tashtech.data.repository.StudentRepository
import com.mrdev.tashtech.data.repository.StudentSignIn
import com.mrdev.tashtech.data.repository.TimetableRepository
import com.mrdev.tashtech.data.repository.UserEventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

private val Context.userDataStore by preferencesDataStore(name = "user_preferences")

class TashTechApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val container: AppContainer by lazy { AppContainer(this, appScope) }

    override fun onCreate() {
        super.onCreate()
        ReminderNotifications.ensureChannel(this)
        appScope.launch { // overlaps with Activity creation; everything below is cached for later reads
            runCatching { container.userPreferences.profile.first() }
            runCatching { container.profile }                          // starts the shared profile StateFlow
            runCatching { container.timetable.getGroups() }            // parses timetable.json off the main thread
            runCatching { SingletonImageLoader.get(this@TashTechApp) } // Coil's first-use setup is not free
        }
    }
}

/** Hand-rolled DI: a handful of singletons is all this app needs. */
class AppContainer(context: Context, scope: CoroutineScope) {

    val students: StudentRepository = AssetStudentRepository(context)
    val news: NewsRepository by lazy {
        NewsRepository(database.savedArticleDao(), File(appContext.cacheDir, "news/feed.json"),
            NewsApi()
        )
    }
    val studentSignIn: StudentSignIn by lazy { StudentSignIn(students, userPreferences) }
    val userEvents: UserEventRepository by lazy { UserEventRepository(database.userEventDao()) }
    val notes: NoteRepository by lazy { NoteRepository(database.noteDao()) }
    private val appContext = context.applicationContext

    val userPreferences: UserPreferencesRepository =
        UserPreferencesRepository(appContext.userDataStore)

    val timetable: TimetableRepository = AssetTimetableRepository(appContext)
    private val database: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "tashtech.db").build()
    }

    val reminders: ReminderRepository by lazy {
        ReminderRepository(database.reminderDao(), AlarmReminderScheduler(appContext))
    }
    val profile: StateFlow<UserProfile?> by lazy {
        userPreferences.profile.stateIn(scope, SharingStarted.Eagerly, null)
    }
}
