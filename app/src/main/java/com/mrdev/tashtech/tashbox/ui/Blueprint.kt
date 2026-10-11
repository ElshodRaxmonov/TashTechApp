package com.mrdev.tashtech.tashbox.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrdev.tashtech.tashbox.puzzle.Grid
import kotlin.math.abs

/** Faint engineering-paper grid with a few circuit traces. Sits behind every TashBox screen. */
@Composable
fun BlueprintBackground(modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    val accent = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    Canvas(modifier) {
        val step = 28.dp.toPx()
        var x = 0f
        while (x < size.width) { drawLine(line, Offset(x, 0f), Offset(x, size.height)); x += step }
        var y = 0f
        while (y < size.height) { drawLine(line, Offset(0f, y), Offset(size.width, y)); y += step }

        val w = size.width
        val h = size.height
        val a = Path().apply {
            moveTo(w, h * 0.14f); lineTo(w * 0.80f, h * 0.14f); lineTo(w * 0.80f, h * 0.20f); lineTo(w * 0.64f, h * 0.20f)
        }
        drawPath(a, accent, style = Stroke(2.dp.toPx()))
        drawCircle(accent, 4.dp.toPx(), Offset(w * 0.64f, h * 0.20f))
        val b = Path().apply {
            moveTo(0f, h * 0.86f); lineTo(w * 0.18f, h * 0.86f); lineTo(w * 0.18f, h * 0.80f); lineTo(w * 0.34f, h * 0.80f)
        }
        drawPath(b, accent, style = Stroke(2.dp.toPx()))
        drawCircle(accent, 4.dp.toPx(), Offset(w * 0.34f, h * 0.80f))
    }
}

/** One shared 0..1 loop for every signal dot on screen (read it inside draw scopes to avoid recomposition). */
@Composable
fun rememberPulse(periodMillis: Int = 1800): State<Float> {
    val t = rememberInfiniteTransition(label = "tashbox-pulse")
    return t.animateFloat(0f, 1f, infiniteRepeatable(tween(periodMillis, easing = LinearEasing), RepeatMode.Restart), label = "p")
}

/** A grid inside a technical "panel" with corner ticks and a small IN / OUT tag. */
@Composable
fun GridPanel(
    grid: Grid?,
    tag: String,
    panelSize: Dp,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false,
    highlightOutline: Color? = null
) {
    val border = highlightOutline ?: if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(tag, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.5.sp)
        Box(
            Modifier
                .size(panelSize)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
        ) {
            Box(Modifier.padding(5.dp).size(panelSize - 10.dp), contentAlignment = Alignment.Center) {
                if (grid != null) GridView(grid, Modifier.size(panelSize - 10.dp))
                else Text("?", fontSize = (panelSize.value * 0.5f).sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Canvas(Modifier.size(panelSize)) {
                val t = 10.dp.toPx(); val sw = 2.dp.toPx(); val pad = 1.dp.toPx()
                val w = this.size.width; val h = this.size.height
                fun corner(x: Float, y: Float, dx: Float, dy: Float) {
                    drawLine(border, Offset(x, y), Offset(x + dx * t, y), sw)
                    drawLine(border, Offset(x, y), Offset(x, y + dy * t), sw)
                }
                corner(pad, pad, 1f, 1f); corner(w - pad, pad, -1f, 1f)
                corner(pad, h - pad, 1f, -1f); corner(w - pad, h - pad, -1f, -1f)
            }
        }
    }
}

/** A wire with an arrow head and a signal dot travelling along it. */
@Composable
fun Wire(pulse: State<Float>, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.outline
    val dot = MaterialTheme.colorScheme.primary
    Canvas(modifier.height(20.dp)) {
        val y = size.height / 2
        val sw = 2.dp.toPx()
        drawLine(color, Offset(0f, y), Offset(size.width - 4.dp.toPx(), y), sw)
        val a = Path().apply {
            moveTo(size.width, y); lineTo(size.width - 8.dp.toPx(), y - 5.dp.toPx()); lineTo(size.width - 8.dp.toPx(), y + 5.dp.toPx()); close()
        }
        drawPath(a, color)
        drawCircle(dot, 4.dp.toPx(), Offset((size.width - 8.dp.toPx()) * pulse.value, y))
    }
}

/** The machine in the middle. Glows while a signal passes through. */
@Composable
fun BoxMachine(pulse: State<Float>, modifier: Modifier = Modifier) {
    val glow = MaterialTheme.colorScheme.primary
    Canvas(modifier.size(46.dp)) {
        val r = CornerRadius(10.dp.toPx())
        val p = pulse.value
        val intensity = (1f - abs(p - 0.5f) * 2f).coerceIn(0f, 1f)
        drawRoundRect(glow.copy(alpha = 0.10f + 0.25f * intensity), cornerRadius = r)
        drawRoundRect(glow, cornerRadius = r, style = Stroke(2.dp.toPx()))
        val bar = 3.dp.toPx()
        for (i in 0..2) {
            val y = size.height * (0.30f + 0.20f * i)
            drawLine(glow.copy(alpha = 0.7f), Offset(size.width * 0.25f, y), Offset(size.width * 0.75f, y), bar)
        }
    }
}

/**
 * IN -> [BOX] -> OUT. The core picture of the whole game. [output] == null shows a "?" panel.
 * [tagIn] / [tagOut] let rows be numbered ("IN 1").
 */
@Composable
fun MachineRow(
    input: Grid,
    output: Grid?,
    pulse: State<Float>,
    modifier: Modifier = Modifier,
    panelSize: Dp = 76.dp,
    tagIn: String = "IN",
    tagOut: String = "OUT",
    emphasize: Boolean = false,
    outputOutline: Color? = null
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        GridPanel(input, tagIn, panelSize, emphasize = emphasize)
        Wire(pulse, Modifier.weight(1f).padding(horizontal = 4.dp).padding(top = 14.dp))
        BoxMachine(pulse, Modifier.padding(top = 14.dp))
        Wire(pulse, Modifier.weight(1f).padding(horizontal = 4.dp).padding(top = 14.dp))
        GridPanel(output, tagOut, panelSize, emphasize = emphasize && output == null, highlightOutline = outputOutline)
    }
}

/** "1  WATCH what the box does": numbered section label that explains the screen. */
@Composable
fun StepLabel(number: String, text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {
            Text(number, Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Text(text, Modifier.padding(start = 8.dp), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.5.sp)
    }
}

/** Five segments: green = got it, red = missed, bright = current round. */
@Composable
fun SegmentedRoundBar(current: Int, total: Int, history: List<Boolean>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 1..total) {
            val color = when {
                i <= history.size -> if (history[i - 1]) CorrectGreen else WrongRed.copy(alpha = 0.75f)
                i == current -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            Box(
                Modifier
                    .weight(if (i == total) 1.5f else 1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun InfoCard(title: String, body: String, modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.primary) {
    Surface(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = accent, fontSize = 13.sp, letterSpacing = 1.sp)
            Text(body, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
