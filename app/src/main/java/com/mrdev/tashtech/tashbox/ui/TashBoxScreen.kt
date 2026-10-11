package com.mrdev.tashtech.tashbox.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrdev.tashtech.tashbox.leaderboard.GameResult
import com.mrdev.tashtech.tashbox.leaderboard.TashBoxStats
import com.mrdev.tashtech.tashbox.puzzle.Difficulty
import com.mrdev.tashtech.tashbox.session.GameUiState
import com.mrdev.tashtech.tashbox.session.SessionRole
import com.mrdev.tashtech.tashbox.session.UiPhase

private val IN_MATCH = setOf(UiPhase.COUNTDOWN, UiPhase.FLASH, UiPhase.ANSWERING, UiPhase.ROUND_RESULT)

/**
 * THE entry point of the whole feature. Drop this into your Tools route:
 *
 *     composable("tashbox") { TashBoxScreen(onExit = { navController.popBackStack() }) }
 *
 * Everything else (home, rooms, QR, lobby, game, results) is internal. It inherits whatever
 * MaterialTheme is active around it.
 *
 * @param playerName optional: pre-fills the player's name from your app's profile (only used until
 *                   the player types their own).
 * @param onMatchFinished called once per finished match with the clean result model and the id of the
 *                   local player inside it (use it to feed your university leaderboard).
 */
@Composable
fun TashBoxScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    playerName: String? = null,
    onMatchFinished: (result: GameResult, localPlayerId: String) -> Unit = { _, _ -> },
    viewModel: TashBoxViewModel = viewModel()
) {
    val route by viewModel.route.collectAsState()
    val game by viewModel.game.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val name by viewModel.playerName.collectAsState()
    val level by viewModel.level.collectAsState()
    val bestByLevel by viewModel.bestByLevel.collectAsState()
    val scanError by viewModel.scanError.collectAsState()

    LaunchedEffect(playerName) { if (!playerName.isNullOrBlank()) viewModel.seedPlayerName(playerName) }
    val finishedCallback by rememberUpdatedState(onMatchFinished)
    LaunchedEffect(viewModel) {
        viewModel.finishedEvents.collect { (result, id) -> finishedCallback(result, id) }
    }

    var confirmLeave by remember { mutableStateOf(false) }
    val inMatch = route == TashBoxViewModel.Route.SESSION && game?.phase?.let { it in IN_MATCH } == true
    BackHandler {
        if (inMatch) confirmLeave = true else viewModel.onBack(onExit)
    }
    KeepScreenOn(active = route == TashBoxViewModel.Route.SESSION)

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            BlueprintBackground(Modifier.fillMaxSize())
            // Keeps content clear of the status bar, notch, navigation bar and keyboard on edge-to-edge screens.
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(top = 4.dp)) {
                when (route) {
                    TashBoxViewModel.Route.TUTORIAL -> TutorialScreen(onDone = viewModel::finishTutorial)
                    TashBoxViewModel.Route.HOME -> TashBoxHomeScreen(
                        level = level, bestByLevel = bestByLevel, onLevel = viewModel::setLevel,
                        onSolo = viewModel::startSolo, onFriends = viewModel::openFriends,
                        onHowTo = viewModel::openTutorial, onExit = onExit
                    )
                    TashBoxViewModel.Route.FRIENDS -> FriendsMenuScreen(
                        name = name,
                        onNameChange = viewModel::setPlayerName,
                        onCreate = viewModel::createRoom,
                        onJoin = viewModel::openScan,
                        onBack = { viewModel.onBack(onExit) }
                    )
                    TashBoxViewModel.Route.SCAN -> ScanScreen(
                        error = scanError, onCode = viewModel::joinFromQr, onBack = { viewModel.onBack(onExit) }
                    )
                    TashBoxViewModel.Route.SESSION -> SessionContent(
                        state = game, summary = summary, onLevel = viewModel::setLevel,
                        onStart = viewModel::startMatch, onAnswer = viewModel::submitAnswer, onNext = viewModel::continueNext,
                        onPlayAgain = viewModel::playAgain, onLeave = viewModel::goHome,
                        onExit = { viewModel.goHome(); onExit() }
                    )
                }
            }
        }
    }

    if (confirmLeave) {
        val isHost = game?.role == SessionRole.HOST
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Leave the match?") },
            text = { Text(if (isHost) "You are the host. The match will end for everyone." else "Your progress in this match will be lost.") },
            confirmButton = { TextButton({ confirmLeave = false; viewModel.goHome() }) { Text("Leave") } },
            dismissButton = { TextButton({ confirmLeave = false }) { Text("Keep playing") } }
        )
    }
}

@Composable
private fun SessionContent(
    state: GameUiState?,
    summary: TashBoxStats.Summary?,
    onLevel: (Difficulty) -> Unit,
    onStart: () -> Unit,
    onAnswer: (Int) -> Unit,
    onNext: () -> Unit,
    onPlayAgain: () -> Unit,
    onLeave: () -> Unit,
    onExit: () -> Unit
) {
    if (state == null) {
        ConnectingScreen(); return }
    when (state.phase) {
        UiPhase.CONNECTING -> ConnectingScreen()
        UiPhase.CONNECT_FAILED -> MessageScreen(
            "Unable to connect.",
            state.errorMessage.orEmpty(),
            "Back",
            onLeave
        )
        UiPhase.LOBBY -> LobbyScreen(state, onLevel, onStart, onLeave)
        UiPhase.COUNTDOWN, UiPhase.FLASH, UiPhase.ANSWERING, UiPhase.ROUND_RESULT -> GameScreen(
            state,
            onAnswer,
            onNext
        )
        UiPhase.FINISHED -> ResultScreen(state, summary, onPlayAgain, onExit)
        UiPhase.HOST_LEFT ->
            if (state.result != null) ResultScreen(state, null, onPlayAgain, onExit)
            else MessageScreen(
                "The host left the game.",
                "Ask your friend to create a new room.",
                "Back",
                onLeave
            )
    }
}

@Composable
private fun ScanScreen(error: String?, onCode: (String) -> Unit, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        QrScanner(onResult = onCode, modifier = Modifier.fillMaxSize())
        Column(
            Modifier.align(Alignment.BottomCenter).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                error ?: "Point your camera at the host's QR code",
                color = if (error != null) WrongRed else MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall
            )
            TextButton(onBack) { Text("Back") }
        }
    }
}

@Composable
private fun KeepScreenOn(active: Boolean) {
    val view = LocalView.current
    DisposableEffect(active) {
        view.keepScreenOn = active
        onDispose { view.keepScreenOn = false }
    }
}
