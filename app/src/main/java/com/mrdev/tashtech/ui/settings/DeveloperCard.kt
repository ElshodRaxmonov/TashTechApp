package com.mrdev.tashtech.ui.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mrdev.tashtech.R
import com.mrdev.tashtech.ui.components.GlassSurface
import com.mrdev.tashtech.ui.services.openLinkExternally
import com.mrdev.tashtech.ui.theme.Dimens

/** Fill the URLs in. A null entry is not shown: nothing here is guessed. */
internal object DeveloperLinks {
    const val NAME = "Elshod Rakhmonov"
    const val HANDLE = "Mr.ElshodDev"
    val telegram: String = "https://t.me/MrElshodDev"
    val linkedin: String = "https://www.linkedin.com/in/elshod-rakhmonov/"
    val instagram: String = "https://www.instagram.com/mr.elshoddev"
    val github: String = "https://github.com/ElshodRaxmonov"
    val itHub: String = "https://t.me/+O9vneLX1gJhlNmZi" // official TashTech IT Hub group
}

private class Social(@DrawableRes val iconRes: Int, @StringRes val label: Int, val url: String)

@Composable
internal fun DeveloperCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val socials = listOf(
        Social(R.drawable.ic_telegram, R.string.social_telegram, DeveloperLinks.telegram),
        Social(R.drawable.ic_linkedin, R.string.social_linkedin, DeveloperLinks.linkedin),
        Social(R.drawable.ic_instagram, R.string.social_instagram, DeveloperLinks.instagram),
        Social(R.drawable.ic_github, R.string.social_github, DeveloperLinks.github),
    )
    GlassSurface(modifier = modifier.fillMaxWidth(), alpha = 0.7f) {
        Column(
            Modifier.padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)
        ) {
            Text(
                stringResource(R.string.settings_developed_by, DeveloperLinks.NAME),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                DeveloperLinks.HANDLE,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                socials.forEach { s ->
                    val name = stringResource(s.label)
                    FilledTonalIconButton(
                        onClick = { openLinkExternally(context, s.url) },
                        modifier = Modifier
                            .size(42.dp)
                            .semantics { contentDescription = name },
                    ) {
                        Image(
                            painter = painterResource(s.iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            DeveloperLinks.itHub?.let { hub ->
                HorizontalDivider(color = colors.outlineVariant)
                Text(
                    stringResource(R.string.settings_hub_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    stringResource(R.string.settings_hub_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
                Button(
                    onClick = { openLinkExternally(context, hub) },
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(stringResource(R.string.settings_hub_join))
                }
            }
        }
    }
}