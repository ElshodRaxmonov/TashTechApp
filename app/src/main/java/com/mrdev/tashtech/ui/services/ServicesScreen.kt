package com.mrdev.tashtech.ui.services

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrdev.tashtech.R
import com.mrdev.tashtech.data.config.AcademicService
import com.mrdev.tashtech.ui.AppViewModelProvider
import com.mrdev.tashtech.ui.components.GlassSurface
import com.mrdev.tashtech.ui.components.ScreenFrame
import com.mrdev.tashtech.ui.components.adaptiveScreenPadding
import com.mrdev.tashtech.ui.motion.Settle
import com.mrdev.tashtech.ui.motion.pressScale
import com.mrdev.tashtech.ui.motion.rememberPageEntrance
import com.mrdev.tashtech.ui.navigation.LocalBottomBarPadding
import com.mrdev.tashtech.ui.theme.Dimens

// 1. Data Model
data class UniversityClub(
    val id: String,
    val name: String,
    @DrawableRes val logoRes: Int,
    val telegramUrl: String?
)

// 2. Dummy Data (Replace with actual data source/ViewModel state)
val dummyClubs = listOf(
    UniversityClub("1", "IT Hub", R.drawable.ithub, "https://t.me/+O9vneLX1gJhlNmZi"),
    UniversityClub(
        "2",
        "CS2",
        R.drawable.cs2,
        "https://t.me/+dfEI7xz1yZk5YmFi"
    ), // Missing link example
    UniversityClub("3", "Zakovat", R.drawable.zakovat, "https://t.me/ttzakovat"),
    UniversityClub("4", "MLBB", R.drawable.mlbb, "https://t.me/+oQjrRGfsMHkwYzYy"),
    UniversityClub("5", "Physics", R.drawable.physics, "https://t.me/+7pyjkRbGY-QzNTdi"),
    UniversityClub("6", "Soccer", R.drawable.football, "https://t.me/+EPdrR2TD6r05MTUy"),
)

// 3. UI Component to be added to ToolsScreen
@Composable
fun ClubsSection(
    clubs: List<UniversityClub>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Student Clubs",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(clubs, key = { it.id }) { club ->
                ClubItemFloatingGlass(club = club) {
                    handleTelegramClick(context, club.telegramUrl)
                }
            }
        }
    }
}

@Composable
private fun ClubItemFloatingGlass(
    club: UniversityClub,
    onClick: () -> Unit
) {
    // Outer Box: 90.dp
    Box(
        modifier = Modifier
            .width(88.dp)
            .height(100.dp) // Overall height
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        // 1. The Glass Background Card
        GlassSurface(
            shape = RoundedCornerShape(
                topEnd = 16.dp,
                topStart = 16.dp,
                bottomEnd = 32.dp,
                bottomStart = 32.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
                // 90dp (total) - 28dp (half the image) = 72.dp
                .height(72.dp)
                .align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = club.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    // Reduced bottom padding slightly to center it in the remaining space
                    modifier = Modifier.padding(bottom = 20.dp, start = 4.dp, end = 4.dp)
                )
            }
        }

        // 2. The Floating Logo
        Image(
            painter = painterResource(id = club.logoRes),
            contentDescription = "${club.name} logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.background), CircleShape)
        )
    }
}

// 4. Intent Handler
private fun handleTelegramClick(context: Context, url: String?) {
    if (url.isNullOrBlank()) {
        Toast.makeText(context, "Telegram group not yet available", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        // "t.me" URLs automatically route to the Telegram app if installed.
        // If not installed, standard URL resolution safely falls back to the browser.
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
    }
}

private class ServiceStyle(
    val icon: ImageVector,
    @StringRes val title: Int,
    @StringRes val description: Int,
    val container: Color,
    val content: Color,
)

@Composable
private fun AcademicService.style(): ServiceStyle {
    val c = MaterialTheme.colorScheme
    return when (this) {
        AcademicService.Sis -> ServiceStyle(
            Icons.Outlined.School, R.string.home_sis_title, R.string.services_sis_desc,
            c.tertiaryContainer, c.onTertiaryContainer,
        )

        AcademicService.Lms -> ServiceStyle(
            Icons.AutoMirrored.Outlined.MenuBook,
            R.string.home_lms_title,
            R.string.services_lms_desc,
            c.secondaryContainer,
            c.onSecondaryContainer,
        )
    }
}

@Composable
fun ServicesScreen(
    onOpen: (AcademicService) -> Unit,
    onOpenNotes: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenNews: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGame: () -> Unit, // null hides the tile until the game route is supplied
    viewModel: ToolsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val colors = MaterialTheme.colorScheme
    val counts by viewModel.counts.collectAsStateWithLifecycle()
    val entrance = rememberPageEntrance()
    val pad = adaptiveScreenPadding()

    ScreenFrame {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
            contentPadding = PaddingValues(
                start = pad,
                end = pad,
                top = Dimens.SpaceMd,
                bottom = Dimens.SpaceLg + LocalBottomBarPadding.current
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        ) {
            item(key = "header") {
                Settle(entrance, 0, distance = 8.dp) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.services_title),
                                style = MaterialTheme.typography.headlineMedium,
                                color = colors.onBackground
                            )
                            Text(
                                stringResource(R.string.services_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                Icons.Outlined.Settings,
                                stringResource(R.string.tool_settings_cd),
                                tint = colors.onBackground,
                            )
                        }
                    }
                }
            }
            item(key = "academic") {
                Settle(entrance, 1) {
                    Row(
                        Modifier.height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)
                    ) {
                        ToolTile(
                            icon = Icons.Outlined.School,
                            tile = colors.tertiaryContainer,
                            onTile = colors.onTertiaryContainer,
                            title = R.string.home_sis_title,
                            subtitle = R.string.home_sis_subtitle,
                            onClick = { onOpen(AcademicService.Sis) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                        ToolTile(
                            icon = Icons.AutoMirrored.Outlined.MenuBook,
                            tile = colors.secondaryContainer,
                            onTile = colors.onSecondaryContainer,
                            title = R.string.home_lms_title,
                            subtitle = R.string.home_lms_subtitle,
                            onClick = { onOpen(AcademicService.Lms) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
            item(key = "personal") {
                Settle(entrance, 2) {
                    Row(
                        Modifier.height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)
                    ) {
                        val remindersTitle = stringResource(R.string.tool_reminders_title)
                        val notesTitle = stringResource(R.string.tool_notes_title)
                        CompactToolTile(
                            icon = Icons.Outlined.Notifications,
                            tile = colors.secondaryContainer,
                            onTile = colors.onSecondaryContainer,
                            title = R.string.tool_reminders_title,
                            subtitle = R.string.tool_reminders_short,
                            count = counts.activeReminders,
                            description = "$remindersTitle, " + pluralStringResource(
                                R.plurals.tool_reminders_active,
                                counts.activeReminders,
                                counts.activeReminders
                            ),
                            onClick = onOpenReminders,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                        CompactToolTile(
                            icon = Icons.Outlined.EditNote,
                            tile = colors.surfaceVariant,
                            onTile = colors.onSurfaceVariant,
                            title = R.string.tool_notes_title,
                            subtitle = R.string.tool_notes_short,
                            count = counts.notes,
                            description = "$notesTitle, " + pluralStringResource(
                                R.plurals.tool_notes_saved,
                                counts.notes,
                                counts.notes
                            ),
                            onClick = onOpenNotes,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
            item(key = "explore") {
                Settle(entrance, 3) {
                    Row(
                        Modifier.height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)
                    ) {
                        CompactToolTile(
                            painter = painterResource(R.drawable.technews),
                            tile = colors.primaryContainer,
                            onTile = colors.onPrimaryContainer,
                            title = R.string.tool_news_title,
                            subtitle = R.string.tool_news_short,
                            count = null,
                            description = null,
                            onClick = onOpenNews,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )

                        CompactToolTile(
                            painter = painterResource(R.drawable.tashbox_icon),
                            tile = colors.tertiaryContainer,
                            onTile = colors.onTertiaryContainer,
                            title = R.string.tool_game_title,
                            subtitle = R.string.tool_game_short,
                            count = null,
                            description = null,
                            onClick = onOpenGame,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )

                    }
                }
            }
            item(key = "note") { Settle(entrance, 4) { SignInNote() } }
            item(key = "clubs") { Settle(entrance, 4) { ClubsSection(dummyClubs) } }
        }
    }
}

@Composable
private fun CompactToolTile(
    icon: ImageVector? = null, painter: Painter? = null, tile: Color, onTile: Color,
    @StringRes title: Int, @StringRes subtitle: Int,
    count: Int?, description: String?,
    onClick: () -> Unit, modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    GlassSurface(
        onClick = onClick,
        modifier = modifier.semantics(mergeDescendants = true) {
            role = Role.Button
            if (description != null) contentDescription = description
        },
        shape = MaterialTheme.shapes.large,
        alpha = 0.7f,
        shadow = 0.dp,
    ) {
        Box {
            Row(
                modifier = Modifier
                    .heightIn(min = 64.dp)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = tile,
                    contentColor = onTile,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        if (painter != null) {
                            Image(
                                painter = painter,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(MaterialTheme.shapes.small)
                            )
                        } else {
                            Icon(
                                imageVector = icon!!,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(title),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        stringResource(subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (count != null && count > 0) { // nothing at zero
                    Surface(
                        shape = CircleShape,
                        color = colors.primary,
                        contentColor = colors.onPrimary
                    ) {
                        Text(
                            if (count > 99) "99+" else count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .widthIn(min = 22.dp)
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolTile(
    icon: ImageVector, tile: Color, onTile: Color,
    @StringRes title: Int, @StringRes subtitle: Int,
    onClick: () -> Unit, modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainer,
        border = BorderStroke(Dimens.BorderThin, colors.outlineVariant),
        modifier = modifier.semantics(mergeDescendants = true) { role = Role.Button },
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)
        ) {
            Surface(shape = MaterialTheme.shapes.small, color = tile, contentColor = onTile) {
                Icon(
                    icon, contentDescription = null, modifier = Modifier
                        .padding(8.dp)
                        .size(22.dp)
                )
            }
            Spacer(Modifier.height(Dimens.SpaceXs))
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun ToolRow(
    icon: ImageVector,
    @StringRes title: Int,
    @StringRes desc: Int,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    Surface(
        interactionSource = source,
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainer,
        border = BorderStroke(Dimens.BorderThin, colors.outlineVariant),
        modifier = Modifier
            .pressScale(source)
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { role = Role.Button },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
            modifier = Modifier
                .heightIn(min = Dimens.MinTouchTarget)
                .padding(14.dp),
        ) {
            Icon(icon, contentDescription = null, tint = colors.secondary)
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun SignInNote() {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = colors.secondaryContainer.copy(alpha = 0.5f),
        contentColor = colors.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
            modifier = Modifier.padding(Dimens.CardPadding),
        ) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = colors.secondary)
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
                Text(
                    stringResource(R.string.services_note_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    stringResource(R.string.services_note_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}