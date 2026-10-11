package com.mrdev.tashtech.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrdev.tashtech.data.repository.StudentSignIn
import com.mrdev.tashtech.domain.student.StudentIdError
import com.mrdev.tashtech.domain.student.StudentIds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val input: String = "",
    val error: StudentIdError? = null,
    val checking: Boolean = false,
    val completed: Boolean = false,
)

class OnboardingViewModel(private val signIn: StudentSignIn) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onInputChange(raw: String) =
        _uiState.update { it.copy(input = StudentIds.sanitize(raw), error = null) }

    fun onContinue() {
        val current = _uiState.value
        if (current.checking || current.completed) return
        _uiState.update { it.copy(checking = true, error = null) }
        viewModelScope.launch {
            val error = signIn.signIn(current.input) // normalize → validate → lookup → save
            _uiState.update {
                if (error == null) it.copy(checking = false, completed = true)
                else it.copy(checking = false, error = error)
            }
        }
    }
}