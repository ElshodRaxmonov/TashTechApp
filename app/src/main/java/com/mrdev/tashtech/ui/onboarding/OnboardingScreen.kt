package com.mrdev.tashtech.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrdev.tashtech.R
import com.mrdev.tashtech.ui.AppViewModelProvider
import com.mrdev.tashtech.ui.components.ScreenFrame
import com.mrdev.tashtech.ui.components.adaptiveScreenPadding
import com.mrdev.tashtech.ui.motion.Settle
import com.mrdev.tashtech.ui.motion.rememberPageEntrance
import com.mrdev.tashtech.ui.theme.Dimens

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val entrance = rememberPageEntrance()
    val pad = adaptiveScreenPadding()

    LaunchedEffect(state.completed) { if (state.completed) onFinished() }

    ScreenFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = pad, vertical = Dimens.SpaceLg),
        ) {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Settle(entrance, 0, distance = 8.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                        Text(
                            stringResource(R.string.app_name),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Text(
                            stringResource(R.string.onboarding_welcome_title),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            stringResource(R.string.onboarding_welcome_subtitle),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(Dimens.SpaceXl))
                Settle(entrance, 1) {
                    StudentIdField(
                        value = state.input,
                        onValueChange = viewModel::onInputChange,
                        problem = state.error,
                        onDone = viewModel::onContinue,
                    )
                }
            }
            Spacer(Modifier.height(Dimens.SpaceMd))
            Button(
                onClick = viewModel::onContinue,
                enabled = !state.checking, // validation happens on tap, so an empty field still explains itself
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                if (state.checking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                    )
                    Spacer(Modifier.width(Dimens.SpaceSm))
                    Text(stringResource(R.string.student_id_checking), style = MaterialTheme.typography.labelLarge)
                } else {
                    Text(stringResource(R.string.onboarding_continue), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

