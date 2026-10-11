package com.mrdev.tashtech.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrdev.tashtech.data.preferences.UserPreferencesRepository
import com.mrdev.tashtech.data.repository.ReminderRepository
import com.mrdev.tashtech.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.System,
    val remindersEnabled: Boolean = true,
)

class SettingsViewModel(
    private val prefs: UserPreferencesRepository,
    private val reminders: ReminderRepository,
) : ViewModel() {

    private val groups = MutableStateFlow<List<String>>(emptyList())


    val uiState: StateFlow<SettingsUiState> =
        combine(prefs.themeMode, prefs.remindersEnabled) { theme, on ->
            SettingsUiState(theme, on)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())



    fun scheduleTestAlert() = reminders.scheduleTestAlert()


    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { prefs.setThemeMode(mode) } // MainActivity's collector applies it, immediately
    }

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setRemindersEnabled(enabled) }
    }
}