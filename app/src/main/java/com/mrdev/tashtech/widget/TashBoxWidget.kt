package com.mrdev.tashtech.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.Text
import com.mrdev.tashtech.R
import com.mrdev.tashtech.ui.navigation.AppLinks

class TashBoxWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Single
    override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent { TashBoxContent(context) }
}

class TashBoxWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TashBoxWidget()
}

@SuppressLint("DiscouragedApi") // the logo is optional, so it is looked up by name
@Composable
private fun TashBoxContent(context: Context) {
    val logo = remember {
        context.resources.getIdentifier("tashbox_logo", "drawable", context.packageName)
            .takeIf { it != 0 } ?: R.drawable.tashbox_icon
    }
    val play = actionStartActivity(AppLinks.intentFor(context, AppLinks.ACTION_OPEN_GAME))
    val label = context.getString(R.string.widget_tashbox_cd)
    Column(
        modifier = GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.widget_bg_box)).padding(10.dp)
            .clickable(play).semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(ImageProvider(logo), contentDescription = null, contentScale = ContentScale.Fit, modifier = GlanceModifier.size(52.dp))
        Spacer(GlanceModifier.height(8.dp))
        Row(
            modifier = GlanceModifier.height(30.dp).background(ImageProvider(R.drawable.widget_bg_pill)).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                ImageProvider(R.drawable.ic_widget_play), contentDescription = null,
                colorFilter = ColorFilter.tint(WidgetColors.onPill), modifier = GlanceModifier.size(14.dp),
            )
            Spacer(GlanceModifier.width(4.dp))
            Text(context.getString(R.string.widget_tashbox_play), style = WidgetText.pill, maxLines = 1)
        }
    }
}