package com.mrdev.tashtech.ui.profile

import androidx.lifecycle.ViewModel
import com.mrdev.tashtech.data.preferences.UserProfile
import com.mrdev.tashtech.data.repository.StudentSignIn
import com.mrdev.tashtech.domain.student.StudentIdError
import kotlinx.coroutines.flow.StateFlow

class ProfileViewModel(
    val profile: StateFlow<UserProfile?>, // the shared in-memory profile: non-null from frame 0
    private val signIn: StudentSignIn,
) : ViewModel() {
    suspend fun changeStudentId(raw: String): StudentIdError? = signIn.signIn(raw)
}