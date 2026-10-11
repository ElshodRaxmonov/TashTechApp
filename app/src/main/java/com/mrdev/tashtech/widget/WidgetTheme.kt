package com.mrdev.tashtech.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.glance.color.ColorProvider
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import com.mrdev.tashtech.TashTechApp


internal object WidgetColors {
    val onSurface = ColorProvider(day = Color(0xFF211A18), night = Color(0xFFF1DFDA))
    val muted = ColorProvider(day = Color(0xFF53433E), night = Color(0xFFD8C2BB))
    val accent = ColorProvider(day = Color(0xFFB72F0C), night = Color(0xFFFFB59F))
    val teal = ColorProvider(day = Color(0xFF006B5B), night = Color(0xFF80D5C1))
    val onAccent = ColorProvider(day = Color.White, night = Color(0xFF611300))
    val onPill = ColorProvider(day = Color(0xFF00382E), night = Color(0xFF00382E))
}


internal object WidgetText {
    val label = TextStyle(color = WidgetColors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    val labelLive = TextStyle(color = WidgetColors.teal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    val title = TextStyle(color = WidgetColors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    val section = TextStyle(color = WidgetColors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    val detail = TextStyle(color = WidgetColors.muted, fontSize = 12.sp)
    val chip = TextStyle(color = WidgetColors.onAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    val qr = TextStyle(color = WidgetColors.onAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    val pill = TextStyle(color = WidgetColors.onPill, fontSize = 12.sp, fontWeight = FontWeight.Bold)
}
internal fun container(context: Context) = (context.applicationContext as TashTechApp).container