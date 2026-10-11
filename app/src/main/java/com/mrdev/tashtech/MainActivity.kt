package com.mrdev.tashtech

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.mrdev.tashtech.ui.AppViewModelProvider
import com.mrdev.tashtech.ui.MainViewModel
import com.mrdev.tashtech.ui.motion.LocalAppReady
import com.mrdev.tashtech.ui.navigation.AppNavHost
import com.mrdev.tashtech.ui.splash.LottieSplash
import com.mrdev.tashtech.ui.theme.TashTechTheme
import com.mrdev.tashtech.ui.theme.toNightMode
import com.mrdev.tashtech.widget.HubWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

// AppCompatActivity (not ComponentActivity) so AppCompatDelegate.setApplicationLocales
// works below API 33.
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels { AppViewModelProvider.Factory }
    private var themeApplied = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) viewModel.handleIntent(intent) // not again after rotation


        splash.setKeepOnScreenCondition { !themeApplied }
        enableEdgeToEdge()

        // No runBlocking: the stored theme is read off the main thread, then applied while the system splash is still up.
        lifecycleScope.launch {
            var first = true
            (application as TashTechApp).container.userPreferences.themeMode.collect { mode ->
                // Idempotent: AppCompat only recreates when the resolved mode actually changes.
                AppCompatDelegate.setDefaultNightMode(mode.toNightMode())
                if (first) { first = false; themeApplied = true } // releases the system splash
            }
        }
        lifecycleScope.launch(Dispatchers.IO) { // self-heal alarms
            val appContainer = (application as TashTechApp).container
            launch {
                @OptIn(FlowPreview::class)
                combine(
                    appContainer.reminders.observeAll(),
                    appContainer.userEvents.observeAll(),
                    appContainer.profile
                ) { r, e, p -> Triple(r, e, p) }
                    .drop(1) // the first emission is just the current state
                    .debounce(750)
                    .collect {
                        try {
                            val context = this@MainActivity
                            val hasHub = GlanceAppWidgetManager(context).getGlanceIds(HubWidget::class.java).isNotEmpty()
                            if (hasHub) HubWidget().updateAll(context)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                        }
                    }
            }
            runCatching { appContainer.reminders.rescheduleAll() }
        }


        setContent {
            TashTechTheme {
                val boot by viewModel.bootstrap.collectAsStateWithLifecycle()
                val contentReady by viewModel.contentReady.collectAsStateWithLifecycle()
                var splashDone by rememberSaveable { mutableStateOf(false) }
                CompositionLocalProvider(LocalAppReady provides splashDone) {
                    Box(Modifier.fillMaxSize()) {
                        val launchTarget by viewModel.launchTarget.collectAsStateWithLifecycle()
                        boot?.let {
                            AppNavHost(
                                startDestination = it.start,
                                launchTarget = launchTarget,
                                onLaunchTargetConsumed = viewModel::consumeLaunchTarget,
                            )
                        }
                        AnimatedVisibility(visible = !splashDone, exit = fadeOut(tween(250))) {
                            LottieSplash(ready = contentReady, onFinished = { splashDone = true })
                        }
                    }
                }
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.handleIntent(intent)
    }
}

