package com.mrdev.tashtech.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mrdev.tashtech.domain.model.Sex
import com.mrdev.tashtech.domain.model.Student
import com.mrdev.tashtech.domain.student.StudentIds
import com.mrdev.tashtech.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

/** All DataStore keys live here. */

object PreferenceKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val STUDENT_NAME = stringPreferencesKey("student_name")
    val STUDENT_GROUP = stringPreferencesKey("student_group")
    val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
    val NOTIFICATION_PROMPT_SHOWN = booleanPreferencesKey("notification_prompt_shown")
    val STUDENT_ID = stringPreferencesKey("student_id")
    val STUDENT_EMAIL = stringPreferencesKey("student_email")
    val STUDENT_SEX = intPreferencesKey("student_sex")
}
data class UserProfile(
    val name: String,
    val group: String, // the timetable key, e.g. "BS26-3"
    val studentId: String = "",
    val sex: Sex = Sex.Unspecified,
    val email: String = "",
)

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    private val prefs: Flow<Preferences> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    val themeMode: Flow<ThemeMode> = prefs
        .map { p -> ThemeMode.entries.firstOrNull { it.name == p[PreferenceKeys.THEME_MODE] } ?: ThemeMode.System }
        .distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[PreferenceKeys.THEME_MODE] = mode.name }
    }
    suspend fun saveStudent(student: Student) {
        dataStore.edit {
            it[PreferenceKeys.STUDENT_ID] = student.id
            it[PreferenceKeys.STUDENT_NAME] = student.name
            it[PreferenceKeys.STUDENT_GROUP] = student.timetableGroup
            it[PreferenceKeys.STUDENT_SEX] = student.sex.code
            it[PreferenceKeys.STUDENT_EMAIL] = student.email
        }
    }
    suspend fun setName(name: String) {
        dataStore.edit { it[PreferenceKeys.STUDENT_NAME] = name }
    }

    /** Emits null until onboarding has been completed. */
    val profile: Flow<UserProfile?> = prefs
        .map { p ->
            val id = p[PreferenceKeys.STUDENT_ID]
            val name = p[PreferenceKeys.STUDENT_NAME]
            val group = p[PreferenceKeys.STUDENT_GROUP]
            // No student ID means onboarding hasn't been done in the new format.
            if (id != null && name != null && group != null) {
                UserProfile(
                    name = name,
                    group = group,
                    studentId = id,
                    sex = Sex.fromCode(p[PreferenceKeys.STUDENT_SEX]),
                    email = p[PreferenceKeys.STUDENT_EMAIL] ?: StudentIds.email(id),
                )
            } else null
        }
        .distinctUntilChanged()
    val remindersEnabled: Flow<Boolean> =
        prefs.map { it[PreferenceKeys.REMINDERS_ENABLED] ?: true }.distinctUntilChanged()

    /** True once we have shown the system notification-permission dialog. */
    val notificationPromptShown: Flow<Boolean> =
        prefs.map { it[PreferenceKeys.NOTIFICATION_PROMPT_SHOWN] ?: false }.distinctUntilChanged()

    suspend fun setRemindersEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferenceKeys.REMINDERS_ENABLED] = enabled }
    }

    suspend fun markNotificationPromptShown() {
        dataStore.edit { it[PreferenceKeys.NOTIFICATION_PROMPT_SHOWN] = true }
    }
    suspend fun saveProfile(name: String, group: String) {
        dataStore.edit {
            it[PreferenceKeys.STUDENT_NAME] = name
            it[PreferenceKeys.STUDENT_GROUP] = group
        }
    }

    suspend fun setGroup(group: String) {
        dataStore.edit { it[PreferenceKeys.STUDENT_GROUP] = group }
    }
}
