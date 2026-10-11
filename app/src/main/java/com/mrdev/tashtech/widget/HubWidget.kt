package com.mrdev.tashtech.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.Text
import com.mrdev.tashtech.R
import com.mrdev.tashtech.ui.home.HeroState
import com.mrdev.tashtech.ui.navigation.AppLinks
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class HubWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(180.dp, 110.dp), DpSize(250.dp, 110.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadHubData(context) // local data only, never the network
        WidgetScheduler.scheduleHub(context, (data as? HubData.Ready)?.let { nextWidgetRefresh(it.content) })
        provideContent { HubContent(context, data) }
    }
}

class HubWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HubWidget()
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetScheduler.cancelHub(context)
    }
}


@Composable
private fun HubContent(context: Context, data: HubData) {
    val wide = LocalSize.current.width >= 240.dp
    Row(
        modifier = GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.widget_bg)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LessonBlock(context, data)
        Spacer(GlanceModifier.width(10.dp))
        QrButton(context, wide)
    }
}

private class Lesson(val label: String, val live: Boolean, val title: String, val detail: String)

private fun lesson(context: Context, data: HubData): Lesson {
    val today = context.getString(R.string.widget_today)
    if (data !is HubData.Ready) return Lesson(today, false, context.getString(R.string.widget_setup), "")
    val clock = DateTimeFormatter.ofPattern("HH:mm")
    fun join(vararg parts: String) = parts.filter { it.isNotBlank() }.joinToString(" · ")
    return when (val hero = data.content.hero) {
        is HeroState.Current -> Lesson(
            context.getString(R.string.widget_now), true, hero.session.subject,
            join(context.getString(R.string.widget_until, hero.session.end.format(clock)), hero.session.room),
        )
        is HeroState.Next ->
            if (hero.dayOffset == 0) Lesson(
                context.getString(R.string.widget_next), false, hero.session.subject,
                join("${hero.session.start.format(clock)} – ${hero.session.end.format(clock)}", hero.session.room),
            ) else Lesson( // nothing left today: say so briefly, hint at the next day
                today, false, context.getString(R.string.home_today_empty_title),
                context.getString(
                    R.string.widget_next_on,
                    hero.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    hero.session.start.format(clock),
                ),
            )
        HeroState.None -> Lesson(
            today, false,
            context.getString(if (data.content.semesterEnded) R.string.hero_semester_ended_title else R.string.home_today_empty_title),
            "",
        )
    }
}

@Composable
private fun RowScope.LessonBlock(context: Context, data: HubData) {
    val lesson = lesson(context, data)
    Column(
        modifier = GlanceModifier.defaultWeight().fillMaxHeight()
            .clickable(actionStartActivity(AppLinks.intentFor(context, Intent.ACTION_MAIN))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(ImageProvider(R.drawable.ic_widget_logo), contentDescription = null, modifier = GlanceModifier.size(16.dp))
            Spacer(GlanceModifier.width(6.dp))
            if (lesson.live) {
                Image(ImageProvider(R.drawable.widget_dot), contentDescription = null, modifier = GlanceModifier.size(7.dp))
                Spacer(GlanceModifier.width(4.dp))
            }
            Text(lesson.label, style = if (lesson.live) WidgetText.labelLive else WidgetText.label, maxLines = 1)
        }
        Spacer(GlanceModifier.height(6.dp))
        Text(lesson.title, style = WidgetText.title, maxLines = 2)
        if (lesson.detail.isNotEmpty()) {
            Spacer(GlanceModifier.height(2.dp))
            Text(lesson.detail, style = WidgetText.detail, maxLines = 1)
        }
    }
}

@Composable
private fun QrButton(context: Context, wide: Boolean) {
    val label = context.getString(R.string.widget_qr_cd)
    Column(
        modifier = GlanceModifier.width(if (wide) 64.dp else 56.dp).fillMaxHeight()
            .background(ImageProvider(R.drawable.widget_bg_accent))
            .clickable(actionStartActivity(AppLinks.intentFor(context, AppLinks.ACTION_OPEN_SCANNER)))
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            ImageProvider(R.drawable.ic_widget_qr), contentDescription = null,
            colorFilter = ColorFilter.tint(WidgetColors.onAccent), modifier = GlanceModifier.size(26.dp),
        )
        Spacer(GlanceModifier.height(2.dp))
        Text(context.getString(R.string.widget_qr_label), style = WidgetText.qr)
    }
}