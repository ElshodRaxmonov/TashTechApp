package com.mrdev.tashtech.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import com.mrdev.tashtech.R
import com.mrdev.tashtech.domain.model.Sex

/** Male/female/neutral avatar from the student's saved sex (0 = female, 1 = male). */
@Composable
fun ProfileAvatar(
    sex: Sex,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
) {
    val res = when (sex) {
        Sex.Female -> R.drawable.avatar_female
        Sex.Male -> R.drawable.avatar_male
        Sex.Unspecified -> R.drawable.ic_splash_logo
    }
    Image(
        painter = painterResource(res),
        contentDescription = null, // decorative: the name sits right next to it
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick) else Modifier),
    )
}