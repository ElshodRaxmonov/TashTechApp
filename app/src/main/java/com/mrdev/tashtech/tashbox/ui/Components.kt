package com.mrdev.tashtech.tashbox.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.mrdev.tashtech.tashbox.puzzle.Grid

/** Node palette. Each color also has its own SHAPE (circle / square / triangle) for color-blind players. */
val NodeColors = listOf(Color.Transparent, Color(0xFF22B8CF), Color(0xFFFFB020), Color(0xFFB197FC))
val CorrectGreen = Color(0xFF2FB344)
val WrongRed = Color(0xFFE03131)

/** Draws one node: ● (1), ■ (2), ▲ (3) or a small dark dot (0), with a soft glow when lit. */
fun DrawScope.drawNodeShape(value: Int, c: Offset, r: Float, off: Color) {
    if (value != 0) drawCircle(NodeColors[value].copy(alpha = 0.16f), r * 1.55f, c)
    when (value) {
        0 -> drawCircle(off, r * 0.45f, c)
        1 -> drawCircle(NodeColors[1], r, c)
        2 -> drawRoundRect(NodeColors[2], Offset(c.x - r, c.y - r), Size(2 * r, 2 * r), CornerRadius(r * 0.25f))
        else -> {
            val p = Path().apply {
                moveTo(c.x, c.y - r * 1.1f)
                lineTo(c.x + r * 1.05f, c.y + r * 0.85f)
                lineTo(c.x - r * 1.05f, c.y + r * 0.85f)
                close()
            }
            drawPath(p, NodeColors[3])
        }
    }
}

/** A single node symbol, e.g. for the tutorial legend. */
@Composable
fun NodeGlyph(value: Int, modifier: Modifier = Modifier.size(36.dp)) {
    val off = MaterialTheme.colorScheme.outline.copy(alpha = 0.40f)
    Canvas(modifier) { drawNodeShape(value, Offset(size.width / 2, size.height / 2), size.width * 0.30f, off) }
}

/**
 * A small node grid: nodes joined by faint lines. [markers] draws letter badges on nodes (used by the
 * Snapshot round), [highlight] rings one node, [hidden] shows an empty "unknown" grid.
 */
@Composable
fun GridView(
    grid: Grid,
    modifier: Modifier = Modifier,
    hidden: Boolean = false,
    markers: Map<Int, String> = emptyMap(),
    highlight: Int? = null
) {
    val line = MaterialTheme.colorScheme.outlineVariant
    val off = MaterialTheme.colorScheme.outline.copy(alpha = 0.40f)
    val badge = MaterialTheme.colorScheme.inverseSurface
    val badgeText = MaterialTheme.colorScheme.inverseOnSurface
    val measurer = rememberTextMeasurer()

    Canvas(modifier.aspectRatio(1f)) {
        val n = grid.size
        val cell = size.width / n
        fun center(i: Int) = Offset((i % n + 0.5f) * cell, (i / n + 0.5f) * cell)
        val stroke = (cell * 0.05f).coerceAtLeast(1f)

        for (i in 0 until n * n) {
            if (i % n + 1 < n) drawLine(line, center(i), center(i + 1), stroke)
            if (i / n + 1 < n) drawLine(line, center(i), center(i + n), stroke)
        }
        val r = cell * 0.30f
        for (i in 0 until n * n) {
            val c = center(i)
            drawNodeShape(if (hidden) 0 else grid.cells[i], c, r, off)
            if (highlight == i) drawCircle(CorrectGreen, r * 1.55f, c, style = Stroke(stroke * 2f))
            markers[i]?.let { label ->
                drawCircle(badge, r * 1.35f, c, style = Stroke(stroke * 1.5f))
                val br = cell * 0.17f
                val bc = Offset(c.x + r * 1.0f, c.y - r * 1.0f)
                drawCircle(badge, br, bc)
                val layout = measurer.measure(
                    label,
                    TextStyle(color = badgeText, fontSize = (br * 1.15f).toSp(), fontWeight = FontWeight.Bold)
                )
                drawText(layout, topLeft = Offset(bc.x - layout.size.width / 2f, bc.y - layout.size.height / 2f))
            }
        }
    }
}

enum class OptionVisual { NEUTRAL, SELECTED, CORRECT, WRONG, DIMMED }

/** Tappable answer card. [onClick] only fires when [enabled]. */
@Composable
fun OptionCard(
    label: String,
    visual: OptionVisual,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val borderColor = when (visual) {
        OptionVisual.CORRECT -> CorrectGreen
        OptionVisual.WRONG -> WrongRed
        OptionVisual.SELECTED -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = modifier
            .alpha(if (visual == OptionVisual.DIMMED) 0.55f else 1f)
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(if (visual == OptionVisual.NEUTRAL || visual == OptionVisual.DIMMED) 1.dp else 2.5.dp, borderColor)
    ) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
fun ThinProgress(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

/** QR code drawn straight from the ZXing bit matrix (no bitmaps). Always black on white so any scanner can read it. */
@Composable
fun QrCode(text: String, modifier: Modifier = Modifier.size(220.dp)) {
    val matrix = remember(text) {
        QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 0))
    }
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(Color.White).padding(12.dp)) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
            val n = matrix.width
            val cell = size.width / n
            for (y in 0 until matrix.height) for (x in 0 until n) {
                if (matrix.get(x, y)) {
                    drawRect(Color.Black, Offset(x * cell, y * cell), Size(cell + 0.6f, cell + 0.6f))
                }
            }
        }
    }
}
