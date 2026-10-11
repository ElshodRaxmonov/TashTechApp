package com.mrdev.tashtech.tashbox.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrdev.tashtech.tashbox.puzzle.Difficulty
import com.mrdev.tashtech.tashbox.puzzle.Example
import com.mrdev.tashtech.tashbox.puzzle.Puzzle
import com.mrdev.tashtech.tashbox.puzzle.QuestionKind
import com.mrdev.tashtech.tashbox.puzzle.RoundType
import com.mrdev.tashtech.tashbox.session.GameUiState
import com.mrdev.tashtech.tashbox.session.SessionRole
import com.mrdev.tashtech.tashbox.session.UiPhase
import com.mrdev.tashtech.tashbox.session.UiPlayer

/** Plain-English instructions for each kind of round. Used by the coach line and the "?" help dialog. */
object RoundHelp {
    fun title(type: RoundType) = when (type) {
        RoundType.PREDICT -> "PREDICT"
        RoundType.ATTRIBUTE -> "PREDICT"
        RoundType.SNAPSHOT -> "MEMORY"
        RoundType.FAULT -> "FIND THE FAULT"
        RoundType.FINAL -> "FINAL x2"
    }

    fun coach(p: Puzzle): String = when (p.question) {
        QuestionKind.PREDICT_OUTPUT ->
            if (p.roundType == RoundType.FINAL && p.level != Difficulty.EASY)
                "Final: TWO rules happen one after the other. Find both, then predict."
            else "The BOX changes every grid by the same secret rule. Spot it, then predict."
        QuestionKind.FIND_CHANGED_NODE -> "Memorize the grid. Then one node will be different. Which one?"
        QuestionKind.FIND_FAULTY_EXAMPLE -> "The BOX followed one rule every time except once. Find the odd example."
    }

    fun help(p: Puzzle): String = when (p.question) {
        QuestionKind.PREDICT_OUTPUT ->
            "1. Look at each IN and OUT pair. The BOX does the SAME thing every time.\n" +
                "2. Ask: what moved? What changed shape? Was it flipped or turned?\n" +
                "3. Imagine the BOX working on the new grid, then tap the matching result.\n\n" +
                "Shapes: ● circle, ■ square, ▲ triangle. A grey dot is an empty node." +
                if (p.roundType == RoundType.FINAL && p.level != Difficulty.EASY) "\n\nFinal round: two rules are chained. The first one happens, then the second works on its result." else ""
        QuestionKind.FIND_CHANGED_NODE ->
            "The grid shows for a few seconds. Remember where the shapes are. When it comes back one node has changed. " +
                "Tap the letter that marks it."
        QuestionKind.FIND_FAULTY_EXAMPLE ->
            "Every example went through the same BOX, but one of them was tampered with. " +
                "Find the rule from the examples that agree, then tap the one that does not fit."
    }
}

/** One round: header, optional player strip, coach line, the puzzle, and the round-result panel. */
@Composable
fun GameScreen(state: GameUiState, onAnswer: (Int) -> Unit, onNext: () -> Unit) {
    var showHelp by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        GameHeader(state, onHelp = { showHelp = true })
        if (state.role != SessionRole.SOLO) {
            Spacer(Modifier.height(8.dp))
            PlayersStrip(state)
        }
        state.notice?.let {
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                Text(it, Modifier.fillMaxWidth().padding(10.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val puzzle = state.puzzle
            if (state.phase == UiPhase.COUNTDOWN || puzzle == null) CountdownView(state)
            else PuzzleBody(state, puzzle, onAnswer)
        }
        if (state.phase == UiPhase.ROUND_RESULT) {
            Spacer(Modifier.height(8.dp))
            RoundResultPanel(state, onNext)
        }
    }
    val p = state.puzzle
    if (showHelp && p != null) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text(RoundHelp.title(p.roundType)) },
            text = { Text(RoundHelp.help(p)) },
            confirmButton = { TextButton({ showHelp = false }) { Text("Got it") } }
        )
    }
}

@Composable
private fun GameHeader(state: GameUiState, onHelp: () -> Unit) {
    val p = state.puzzle
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(
                if (state.round == 0) "GET READY" else "ROUND ${state.round}/${state.totalRounds}",
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 1.sp
            )
            if (p != null && state.round > 0) {
                Text(RoundHelp.title(p.roundType), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("SCORE ${state.score}")
            val seconds = ((state.remainingMillis + 999) / 1000).toInt()
            Pill(if (state.phase == UiPhase.ANSWERING) "%02d s".format(seconds) else "-- s")
            if (p != null) TextButton(onHelp, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("?", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        }
    }
    Spacer(Modifier.height(6.dp))
    SegmentedRoundBar(state.round, state.totalRounds, state.roundHistory)
    if (state.phase == UiPhase.ANSWERING && p != null) {
        Spacer(Modifier.height(5.dp))
        ThinProgress(state.remainingMillis / p.answerMillis.toFloat())
    }
}

@Composable
private fun Pill(text: String) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun PlayersStrip(state: GameUiState) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        state.players.forEach { p -> PlayerChip(p, state.phase) }
    }
}

@Composable
private fun PlayerChip(p: UiPlayer, phase: UiPhase) {
    val symbol = when {
        !p.connected -> "⚠"
        phase == UiPhase.ROUND_RESULT -> when (p.lastCorrect) { true -> "✓"; false -> "✗"; null -> "–" }
        phase == UiPhase.ANSWERING || phase == UiPhase.FLASH -> if (p.answered) "✓" else "…"
        else -> ""
    }
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (p.isYou) "You" else p.name, style = MaterialTheme.typography.labelMedium, fontWeight = if (p.isYou) FontWeight.Bold else FontWeight.Normal)
            Spacer(Modifier.size(6.dp))
            Text(symbol, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.size(6.dp))
            Text("${p.score}", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CountdownView(state: GameUiState) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (state.countdownSeconds > 0) "${state.countdownSeconds}" else "GO",
            fontSize = 110.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text("5 rounds. Find the rule each time.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${state.level.label} mode", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PuzzleBody(state: GameUiState, puzzle: Puzzle, onAnswer: (Int) -> Unit) {
    val reveal = state.phase == UiPhase.ROUND_RESULT
    val correct = state.roundResult?.correctIndex
    val canAnswer = state.phase == UiPhase.ANSWERING && state.selectedAnswer == null
    val pulse = rememberPulse()

    fun visual(i: Int): OptionVisual = when {
        reveal && i == correct -> OptionVisual.CORRECT
        reveal && i == state.selectedAnswer -> OptionVisual.WRONG
        reveal -> OptionVisual.DIMMED
        state.selectedAnswer == i -> OptionVisual.SELECTED
        state.selectedAnswer != null -> OptionVisual.DIMMED
        else -> OptionVisual.NEUTRAL
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.level != Difficulty.HARD && state.phase != UiPhase.FLASH) {
            InfoCard("HOW IT WORKS", RoundHelp.coach(puzzle))
        }
        when (puzzle.question) {
            QuestionKind.PREDICT_OUTPUT -> PredictBody(puzzle, pulse, reveal, correct, ::visual, canAnswer, onAnswer)
            QuestionKind.FIND_CHANGED_NODE -> SnapshotBody(state, puzzle, ::visual, canAnswer, correct, onAnswer)
            QuestionKind.FIND_FAULTY_EXAMPLE -> FaultBody(puzzle, pulse, ::visual, canAnswer, onAnswer)
        }
        if (state.phase == UiPhase.ANSWERING && state.selectedAnswer != null && state.role != SessionRole.SOLO) {
            Text("Answer locked. Waiting for the others...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
fun PredictBody(
    p: Puzzle,
    pulse: State<Float>,
    reveal: Boolean,
    correct: Int?,
    visual: (Int) -> OptionVisual,
    canAnswer: Boolean,
    onAnswer: (Int) -> Unit
) {
    StepLabel("1", "WATCH what the BOX does")
    p.examples.forEachIndexed { i, e ->
        MachineRow(
            e.input,
            e.output,
            pulse,
            tagIn = "IN ${i + 1}",
            tagOut = "OUT ${i + 1}"
        )
    }
    StepLabel("2", "YOUR TURN: same rule, new grid")
    p.queryInput?.let { q ->
        val answer = if (reveal && correct != null) p.options.getOrNull(correct)?.grid else null
        MachineRow(
            q,
            answer,
            pulse,
            panelSize = 84.dp,
            tagIn = "NEW IN",
            tagOut = if (answer != null) "ANSWER" else "OUT ?",
            emphasize = true,
            outputOutline = if (answer != null) CorrectGreen else null
        )
    }
    StepLabel("3", "PICK the grid the BOX makes")
    p.options.chunked(2).forEachIndexed { rowIdx, row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEachIndexed { colIdx, option ->
                val i = rowIdx * 2 + colIdx
                OptionCard(
                    option.label,
                    visual(i),
                    canAnswer,
                    { onAnswer(i) },
                    Modifier.weight(1f)
                ) {
                    option.grid?.let { GridView(it, Modifier.fillMaxWidth(0.82f)) }
                }
            }
        }
    }
}

@Composable
private fun SnapshotBody(
    state: GameUiState, p: Puzzle, visual: (Int) -> OptionVisual,
    canAnswer: Boolean, correct: Int?, onAnswer: (Int) -> Unit
) {
    if (state.phase == UiPhase.FLASH) {
        InfoCard("MEMORIZE", "Remember where every shape is. It disappears soon.")
        p.flashGrid?.let { GridView(it, Modifier.fillMaxWidth(0.9f)) }
        ThinProgress(
            state.flashRemainingMillis / p.flashMillis.toFloat().coerceAtLeast(1f),
            Modifier.fillMaxWidth(0.9f)
        )
        return
    }
    StepLabel("1", "ONE node changed. Which one?")
    val reveal = state.phase == UiPhase.ROUND_RESULT
    val markers = p.options.mapNotNull { o -> o.node?.let { it to o.label } }.toMap()
    val highlight = if (reveal && correct != null) p.options.getOrNull(correct)?.node else null
    p.shownGrid?.let {
        GridView(
            it,
            Modifier.fillMaxWidth(0.9f),
            markers = markers,
            highlight = highlight
        )
    }
    StepLabel("2", "TAP the letter on that node")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        p.options.forEachIndexed { i, o ->
            OptionCard(
                o.label,
                visual(i),
                canAnswer,
                { onAnswer(i) },
                Modifier.weight(1f)
            ) { Spacer(Modifier.height(10.dp)) }
        }
    }
}

@Composable
private fun FaultBody(
    p: Puzzle, pulse: State<Float>, visual: (Int) -> OptionVisual,
    canAnswer: Boolean, onAnswer: (Int) -> Unit
) {
    StepLabel("1", "FIND the example that breaks the rule")
    p.examples.chunked(2).forEachIndexed { rowIdx, row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEachIndexed { colIdx, e: Example ->
                val i = rowIdx * 2 + colIdx
                OptionCard(
                    "EXAMPLE ${i + 1}",
                    visual(i),
                    canAnswer,
                    { onAnswer(i) },
                    Modifier.weight(1f)
                ) {
                    GridPanel(e.input, "IN", 72.dp)
                    Text(
                        "↓ BOX ↓",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    GridPanel(e.output, "OUT", 72.dp)
                }
            }
        }
    }
    Text("Tap the example that does NOT match the others.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun RoundResultPanel(state: GameUiState, onNext: () -> Unit) {
    val data = state.roundResult ?: return
    val mine = data.results.firstOrNull { it.playerId == state.myPlayerId }
    val missed = mine?.correct != true
    val headline = when {
        mine == null || !mine.answered -> "Time's up"
        mine.correct -> "Correct!  +${mine.points}" + if (mine.speedBonus > 0) "  (${mine.speedBonus} speed)" else ""
        else -> "Not this time"
    }
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                headline, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium,
                color = if (mine?.correct == true) CorrectGreen else MaterialTheme.colorScheme.onSurface
            )
            if (missed) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Look at the green answer above. Apply the rule below to the new grid and you get it.",
                    textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (data.explanation.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text("THE RULE", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                Text(data.explanation, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
            }
            if (state.role == SessionRole.SOLO) {
                Spacer(Modifier.height(10.dp))
                Button(onNext, Modifier.fillMaxWidth().height(48.dp)) {
                    Text(
                        if (state.round >= state.totalRounds) "SEE RESULTS" else if (missed) "I UNDERSTAND, NEXT" else "NEXT",
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (missed) {
                Spacer(Modifier.height(4.dp))
                Text("Next round starts in a few seconds.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.role != SessionRole.SOLO) {
                Spacer(Modifier.height(8.dp))
                data.results.sortedByDescending { it.totalScore }.forEach { r ->
                    val name = state.players.firstOrNull { it.id == r.playerId }?.let { if (it.isYou) "You" else it.name } ?: "?"
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(if (r.points > 0) "+${r.points}" else "–", Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.bodyMedium)
                        Text("${r.totalScore}", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
