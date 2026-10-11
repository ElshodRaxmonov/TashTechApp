package com.mrdev.tashtech.tashbox.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrdev.tashtech.tashbox.leaderboard.GameMode
import com.mrdev.tashtech.tashbox.leaderboard.TashBoxStats
import com.mrdev.tashtech.tashbox.puzzle.Difficulty
import com.mrdev.tashtech.tashbox.puzzle.Grid
import com.mrdev.tashtech.tashbox.room.RoomManager
import com.mrdev.tashtech.tashbox.session.GameUiState
import com.mrdev.tashtech.tashbox.session.SessionRole
import com.mrdev.tashtech.tashbox.session.UiPhase
import com.mrdev.tashtech.tashbox.session.UiPlayer

private val LogoGrid = Grid(3, listOf(1, 0, 2, 0, 3, 0, 2, 0, 1))

private fun dots(level: Difficulty) = when (level) {
    Difficulty.EASY -> "●○○"
    Difficulty.MEDIUM -> "●●○"
    Difficulty.HARD -> "●●●"
}

/** Tappable difficulty card. [best] is shown on Home (null in lobbies). */
@Composable
fun LevelCard(level: Difficulty, selected: Boolean, enabled: Boolean, best: Int?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        modifier = modifier.clip(shape).clickable(enabled = enabled, onClick = onClick),
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        border = BorderStroke(if (selected) 2.5.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(horizontal = 6.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(dots(level), color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, letterSpacing = 2.sp)
            Text(level.label.uppercase(), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            if (best != null) {
                Text(if (best > 0) "best $best" else "new", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun TashBoxHomeScreen(
    level: Difficulty,
    bestByLevel: Map<Difficulty, Int>,
    onLevel: (Difficulty) -> Unit,
    onSolo: () -> Unit,
    onFriends: () -> Unit,
    onHowTo: () -> Unit,
    onExit: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GridView(LogoGrid, Modifier.size(96.dp))
        Spacer(Modifier.height(10.dp))
        Text("TashBox", fontSize = 38.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Text("Find the rule. Beat your friends.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))

        StepLabel("1", "CHOOSE A LEVEL")
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Difficulty.values().forEach { l ->
                LevelCard(l, l == level, true, bestByLevel[l] ?: 0, { onLevel(l) }, Modifier.weight(1f))
            }
        }
        Text(level.tagline, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(18.dp))
        Button(onSolo, Modifier.fillMaxWidth().height(56.dp)) { Text("SOLO CHALLENGE", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onFriends, Modifier.fillMaxWidth().height(56.dp)) { Text("PLAY WITH FRIENDS", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(6.dp))
        Row {
            TextButton(onHowTo) { Text("How to play") }
            TextButton(onExit) { Text("Back") }
        }
    }
}

@Composable
fun FriendsMenuScreen(
    name: String,
    onNameChange: (String) -> Unit,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("PLAY WITH FRIENDS", style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = name, onValueChange = onNameChange, singleLine = true,
            label = { Text("Your name") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        Button(onCreate, Modifier.fillMaxWidth().height(56.dp), enabled = name.isNotBlank()) { Text("CREATE ROOM", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onJoin, Modifier.fillMaxWidth().height(56.dp), enabled = name.isNotBlank()) { Text("JOIN ROOM", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(20.dp))
        InfoCard(
            "HOW IT WORKS",
            "One phone creates a room and shows a QR code. Everyone else taps JOIN ROOM and scans it. You all need the same Wi-Fi. No internet required."
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onBack) { Text("Back") }
    }
}

@Composable
fun MessageScreen(title: String, body: String, buttonText: String = "OK", onButton: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(body, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onButton) { Text(buttonText) }
    }
}

@Composable
fun ConnectingScreen() {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Connecting...", style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun LobbyScreen(state: GameUiState, onLevel: (Difficulty) -> Unit, onStart: () -> Unit, onLeave: () -> Unit) {
    val isHost = state.role == SessionRole.HOST
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("TashBox", style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (isHost) {
            Text(if (state.players.size < 2) "WAITING FOR PLAYERS" else "READY WHEN YOU ARE", style = MaterialTheme.typography.labelLarge, letterSpacing = 1.sp)
            Text("Room ${state.roomId}", style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            state.qrPayload?.let { QrCode(it, Modifier.size(230.dp)) }
            Spacer(Modifier.height(8.dp))
            Text("Friends: tap JOIN ROOM and scan this code.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        } else {
            Text("Connected to:", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(state.roomName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        Text("DIFFICULTY", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Difficulty.values().forEach { l ->
                LevelCard(l, l == state.level, isHost, null, { onLevel(l) }, Modifier.weight(1f))
            }
        }
        if (!isHost) Text("The host picks the difficulty.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(16.dp))
        Text("Players: ${state.players.size} / ${RoomManager.MAX_PLAYERS}", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        state.players.forEach { PlayerRow(it) }
        Spacer(Modifier.height(20.dp))
        if (isHost) {
            Button(onStart, Modifier.fillMaxWidth().height(52.dp), enabled = state.canStart) { Text("START GAME", fontWeight = FontWeight.Bold) }
            if (!state.canStart) {
                Text("Need at least ${RoomManager.MIN_PLAYERS} players", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Text("Waiting for host...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onLeave) { Text("Leave") }
    }
}

@Composable
private fun PlayerRow(p: UiPlayer) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(if (p.ready || p.isHost) "✓" else "…", Modifier.width(24.dp), color = MaterialTheme.colorScheme.primary)
        Text(if (p.isYou) "${p.name} (you)" else p.name, fontWeight = if (p.isYou) FontWeight.Bold else FontWeight.Normal)
        if (p.isHost) Text("  host", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ResultScreen(
    state: GameUiState,
    summary: TashBoxStats.Summary?,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    val result = state.result
    val solo = result?.mode == GameMode.SOLO
    val canPlayAgain = state.phase == UiPhase.FINISHED && state.role != SessionRole.CLIENT
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state.phase == UiPhase.HOST_LEFT) {
            Text("The host left the game.", fontWeight = FontWeight.Bold)
            Text("Standings so far:", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
        }
        if (solo) {
            val mine = result?.playerResult(state.myPlayerId)
            Text("TASHBOX COMPLETE", style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text("${state.level.label} mode", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            SegmentedRoundBar(state.totalRounds + 1, state.totalRounds, state.roundHistory)
            Spacer(Modifier.height(16.dp))
            Text("Score", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${mine?.score ?: 0}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Accuracy", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${((mine?.accuracy ?: 0f) * 100).toInt()}%", style = MaterialTheme.typography.headlineMedium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Best (${state.level.label})", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${summary?.bestScore ?: mine?.score ?: 0}", style = MaterialTheme.typography.headlineMedium)
                }
            }
            if (summary?.isNewBest == true && summary.previousBest > 0) {
                Spacer(Modifier.height(6.dp))
                Text("New personal best!", color = CorrectGreen, fontWeight = FontWeight.Bold)
            }
            if ((mine?.accuracy ?: 1f) < 0.4f && state.level != Difficulty.EASY) {
                Spacer(Modifier.height(10.dp))
                Text("Tip: try EASY mode to practise spotting rules.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Text("FINAL RESULTS", style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            result?.standings?.forEach { p ->
                val medal = when (p.rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "   " }
                val you = p.playerId == state.myPlayerId
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(medal, Modifier.width(36.dp), style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (you) "You" else p.name, Modifier.weight(1f),
                        fontWeight = if (you) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("${p.score}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        if (canPlayAgain) {
            Button(onPlayAgain, Modifier.fillMaxWidth().height(52.dp)) { Text("PLAY AGAIN", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(10.dp))
        } else if (state.role == SessionRole.CLIENT && state.phase == UiPhase.FINISHED) {
            Text("Waiting for the host to play again...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
        }
        FilledTonalButton(onExit, Modifier.fillMaxWidth().height(52.dp)) { Text("EXIT", fontWeight = FontWeight.Bold) }
    }
}
