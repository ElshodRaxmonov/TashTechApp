package com.mrdev.tashtech.tashbox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrdev.tashtech.tashbox.puzzle.Axis
import com.mrdev.tashtech.tashbox.puzzle.Difficulty
import com.mrdev.tashtech.tashbox.puzzle.Grid
import com.mrdev.tashtech.tashbox.puzzle.Puzzle
import com.mrdev.tashtech.tashbox.puzzle.PuzzleGenerator
import com.mrdev.tashtech.tashbox.puzzle.Rule
import kotlinx.coroutines.delay

private const val STEPS = 7
private const val PRACTICE_STEP = 4

/**
 * First-run tutorial (also reachable from Home > HOW TO PLAY). Seven short steps, one idea each,
 * ending with a real practice puzzle. Skippable at any time.
 */
@Composable
fun TutorialScreen(onDone: () -> Unit) {
    var step by rememberSaveable { mutableStateOf(0) }
    var picked by rememberSaveable { mutableStateOf<Int?>(null) }
    val practice = remember { PuzzleGenerator.generate(1, 20_260_101L, Difficulty.EASY) }
    val pulse = rememberPulse()

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("HOW TO PLAY", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            TextButton(onDone) { Text("Skip") }
        }
        Dots(step)
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (step) {
                0 -> WelcomeStep()
                1 -> BoxStep(pulse)
                2 -> SymbolsStep()
                3 -> ExamplesStep(pulse)
                4 -> PracticeStep(practice, pulse, picked) { picked = it }
                5 -> RoundTypesStep()
                else -> ScoringStep()
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) OutlinedButton({ step-- }) { Text("Back") } else Spacer(Modifier.size(1.dp))
            Spacer(Modifier.weight(1f))
            val canNext = step != PRACTICE_STEP || picked != null
            Button(
                onClick = { if (step == STEPS - 1) onDone() else step++ },
                enabled = canNext
            ) { Text(if (step == STEPS - 1) "LET'S PLAY" else if (step == PRACTICE_STEP && picked == null) "Pick an answer" else "Next") }
        }
    }
}

@Composable
private fun Dots(step: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        repeat(STEPS) { i ->
            Box(
                Modifier.padding(horizontal = 3.dp).size(if (i == step) 10.dp else 7.dp).clip(CircleShape)
                    .background(if (i == step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            )
        }
    }
}

@Composable
private fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
}

@Composable
private fun Body(text: String) {
    Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
}

private val LOGO = Grid(3, listOf(1, 0, 2, 0, 3, 0, 2, 0, 1))

@Composable
private fun WelcomeStep() {
    Spacer(Modifier.height(8.dp))
    GridView(LOGO, Modifier.size(120.dp))
    Title("Welcome to TashBox")
    Body("A 2-minute logic game. No math, no engineering knowledge, no memorizing facts.\n\nJust spot the pattern.")
    InfoCard(
        "YOUR JOB",
        "Every round hides a secret RULE inside a black BOX. You work out what the BOX does, then use it."
    )
}

@Composable
private fun BoxStep(pulse: State<Float>) {
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (true) { delay(1500); revealed = !revealed } }
    val input = Grid(3, listOf(1, 0, 0, 2, 0, 0, 3, 0, 0))
    val output = Rule.Mirror(Axis.LEFT_RIGHT).apply(input)
    Title("Meet the BOX")
    MachineRow(input, if (revealed) output else null, pulse, panelSize = 92.dp)
    Body("A grid goes IN. The BOX changes it by a secret rule. A new grid comes OUT.")
    InfoCard(
        "EXAMPLE",
        "Here the BOX flips the grid left to right. The circle in the left column ends up in the right column."
    )
}

@Composable
private fun SymbolsStep() {
    Title("Read the shapes")
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Legend(1, "circle")
        Legend(2, "square")
        Legend(3, "triangle")
        Legend(0, "empty")
    }
    Body("Each node is a circle ●, a square ■, a triangle ▲ or empty. Shapes also have different colors, so you can use either.")
    InfoCard(
        "RULES CAN",
        "Move nodes, change their shape, flip or turn the whole grid, or make shapes spread and fall."
    )
}

@Composable
private fun Legend(v: Int, name: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        NodeGlyph(v, Modifier.size(48.dp))
        Text(name, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
    }
}

@Composable
private fun ExamplesStep(pulse: State<Float>) {
    var showRule by remember { mutableStateOf(false) }
    val rule = Rule.Rotate(1)
    val inputs = listOf(
        Grid(3, listOf(1, 1, 0, 0, 0, 0, 0, 2, 0)),
        Grid(3, listOf(3, 0, 0, 0, 1, 0, 0, 0, 2)),
        Grid(3, listOf(0, 2, 0, 0, 0, 3, 1, 0, 0))
    )
    Title("Compare IN and OUT")
    inputs.forEachIndexed { i, g ->
        MachineRow(
            g,
            rule.apply(g),
            pulse,
            panelSize = 70.dp,
            tagIn = "IN ${i + 1}",
            tagOut = "OUT ${i + 1}"
        )
    }
    Body("The BOX does the SAME thing every time. Ask yourself: what moved? What changed? What stayed put?")
    if (showRule) InfoCard("THE RULE", rule.explain)
    else OutlinedButton({ showRule = true }) { Text("Show me the rule") }
}

@Composable
private fun PracticeStep(
    practice: Puzzle,
    pulse: State<Float>,
    picked: Int?,
    onPick: (Int) -> Unit
) {
    val answered = picked != null
    fun visual(i: Int): OptionVisual = when {
        answered && i == practice.correctIndex -> OptionVisual.CORRECT
        answered && i == picked -> OptionVisual.WRONG
        answered -> OptionVisual.DIMMED
        else -> OptionVisual.NEUTRAL
    }
    Title("Your turn")
    PredictBody(
        practice,
        pulse,
        answered,
        practice.correctIndex,
        ::visual,
        canAnswer = !answered,
        onAnswer = onPick
    )
    if (answered) {
        val right = picked == practice.correctIndex
        InfoCard(
            if (right) "NICE!" else "NOT QUITE",
            (if (right) "That's it. " else "The right answer is highlighted. ") + practice.metadata["explain"].orEmpty(),
            accent = if (right) CorrectGreen else WrongRed
        )
    }
}

@Composable
private fun RoundTypesStep() {
    Title("Five rounds, three kinds")
    InfoCard(
        "PREDICT  (rounds 1, 2, 5)",
        "Study the examples, find the rule, pick the grid the BOX makes from the new one. Round 5 is the FINAL and worth double."
    )
    InfoCard(
        "MEMORY  (round 3)",
        "A grid flashes for a few seconds, then comes back with ONE node changed. Tap the letter on the changed node."
    )
    InfoCard(
        "FIND THE FAULT  (round 4)",
        "Several examples went through the same BOX, but one was tampered with. Tap the odd one out."
    )
}

@Composable
private fun ScoringStep() {
    Title("Points and friends")
    InfoCard(
        "SCORING",
        "Correct answer = 100 points (the final round = 200). Answer faster for up to +25 bonus. Wrong or no answer = 0. You get one answer per round, so think first."
    )
    InfoCard(
        "DIFFICULTY",
        "Start on EASY: simple rules and more time. MEDIUM is the classic game. HARD has bigger grids and less time."
    )
    InfoCard(
        "WITH FRIENDS",
        "Create a room, friends scan your QR code on the same Wi-Fi. Same puzzles, same timer, everyone answers on their own phone. No internet needed."
    )
    Spacer(Modifier.height(4.dp))
    Text("Tip: the small ? button during a round explains how that round works.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
