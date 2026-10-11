package com.mrdev.tashtech.ui.profile

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrdev.tashtech.R
import com.mrdev.tashtech.domain.student.StudentIdError
import com.mrdev.tashtech.ui.AppViewModelProvider
import com.mrdev.tashtech.ui.components.GlassSurface
import com.mrdev.tashtech.ui.components.ProfileAvatar
import com.mrdev.tashtech.ui.components.ScreenFrame
import com.mrdev.tashtech.ui.components.adaptiveScreenPadding
import com.mrdev.tashtech.ui.motion.Settle
import com.mrdev.tashtech.ui.motion.rememberPageEntrance
import com.mrdev.tashtech.ui.motion.sharedProfile
import com.mrdev.tashtech.ui.onboarding.StudentIdField
import com.mrdev.tashtech.ui.theme.Dimens
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    var changeId by rememberSaveable { mutableStateOf(false) }
    val entrance = rememberPageEntrance()
    val colors = MaterialTheme.colorScheme
    val pad = adaptiveScreenPadding()

    ScreenFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = pad),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    stringResource(R.string.common_back),
                    tint = colors.onBackground,
                )
            }
            androidx.compose.foundation.layout.Spacer(Modifier.size(Dimens.SpaceSm))
            profile?.let { p ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = Dimens.SpaceXl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
                ) {
                    ProfileAvatar(p.sex, Modifier
                        .sharedProfile("profile-avatar")
                        .size(120.dp))
                    Text(
                        p.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .sharedProfile("profile-name", text = true)
                            .fillMaxWidth(),
                    )

                    Settle(entrance, 1) {
                        GlassSurface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            alpha = 0.72f,
                            shadow = 0.dp
                        ) {
                            Column(
                                Modifier
                                    .padding(Dimens.CardPadding)
                                    .background(Color.Transparent),
                                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)
                            ) {
                                ProfileInfo(
                                    Icons.Outlined.Badge,
                                    R.string.student_id_label,
                                    p.studentId
                                )
                                HorizontalDivider(color = colors.outlineVariant)
                                ProfileInfo(
                                    Icons.Outlined.Groups,
                                    R.string.settings_group_label,
                                    p.group
                                )
                                HorizontalDivider(color = colors.outlineVariant)
                                ProfileInfo(
                                    Icons.Outlined.Email,
                                    R.string.settings_email,
                                    p.email,
                                    isEmail = true
                                )
                            }
                        }
                    }
                    TextButton(onClick = {
                        changeId = true
                    }) { Text(stringResource(R.string.settings_change_student_id)) }

                }

            }
            if (changeId) StudentIdDialog(
                onSubmit = viewModel::changeStudentId,
                onDismiss = { changeId = false })
        }
    }
}

@Composable
internal fun StudentIdDialog(onSubmit: suspend (String) -> StudentIdError?, onDismiss: () -> Unit) {

    var text by rememberSaveable { mutableStateOf("") }
    var problem by remember { mutableStateOf<StudentIdError?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit() {
        if (busy) return
        busy = true
        scope.launch {
            val result = onSubmit(text)
            busy = false
            if (result == null) onDismiss() else problem = result
        }
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(R.string.settings_change_student_id)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
                Text(
                    stringResource(R.string.settings_change_student_id_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StudentIdField(
                    value = text,
                    onValueChange = { text = it; problem = null },
                    problem = problem,
                    onDone = { submit() },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { submit() },
                enabled = !busy
            ) { Text(stringResource(R.string.onboarding_continue)) }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !busy
            ) { Text(stringResource(R.string.common_cancel)) }
        }
    )

}

@Composable
private fun ProfileInfo(
    icon: ImageVector,
    @StringRes label: Int,
    value: String,
    isEmail: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = colors.secondary,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(label),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
            )
            Text(
                value,
                style = if (isEmail) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}