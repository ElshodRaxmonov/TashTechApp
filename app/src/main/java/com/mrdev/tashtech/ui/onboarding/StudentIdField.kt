package com.mrdev.tashtech.ui.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.mrdev.tashtech.R
import com.mrdev.tashtech.domain.student.StudentIdError
import com.mrdev.tashtech.domain.student.StudentIds

@StringRes
internal fun StudentIdError.messageRes(): Int = when (this) {
    StudentIdError.Empty -> R.string.student_id_error_empty
    StudentIdError.InvalidFormat -> R.string.student_id_error_format
    StudentIdError.NotFound -> R.string.student_id_error_not_found
    StudentIdError.Unavailable -> R.string.student_id_error_unavailable
    StudentIdError.SaveFailed -> R.string.onboarding_save_error
}

@Composable
fun StudentIdField(
    value: String,
    onValueChange: (String) -> Unit,
    problem: StudentIdError?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val message = problem?.let { stringResource(it.messageRes()) }
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(StudentIds.sanitize(it)) }, // "ttb2600088" shows as "TTB2600088"
        label = { Text(stringResource(R.string.student_id_label)) },
        placeholder = { Text(stringResource(R.string.student_id_placeholder)) },
        leadingIcon = { Icon(Icons.Outlined.Badge, contentDescription = null) },
        isError = problem != null,
        supportingText = {
            Text(
                text = message ?: stringResource(R.string.student_id_helper),
                modifier = Modifier.semantics { if (message != null) liveRegion = LiveRegionMode.Polite },
            )
        },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            keyboardType = KeyboardType.Ascii,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier.fillMaxWidth(),
    )
}