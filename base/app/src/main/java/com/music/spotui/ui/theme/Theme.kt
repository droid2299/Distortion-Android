package com.music.spotui.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.music.spotui.ui.immersive.Immersive
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun SpotuiTheme(
    content: @Composable () -> Unit
) {
    val energy by app.myspoti.power.EnergySettings.state.collectAsStateWithLifecycle()
    val policy = energy.policy
    androidx.compose.runtime.LaunchedEffect(policy.videoMaxHeight, policy.allowBackgroundLookups) {
        com.music.spotui.video.MusicVideoController.onEnergyPolicyChanged()
    }
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val dark = isSystemInDarkTheme() || policy.oledBlack || com.music.spotui.ui.tv.Tv.isTv(context)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var paletteRefresh by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) paletteRefresh++
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val visualScheme = remember(context, configuration, dark, policy.oledBlack, paletteRefresh) {
        val system = when {
            Build.VERSION.SDK_INT >= 31 && dark -> dynamicDarkColorScheme(context)
            Build.VERSION.SDK_INT >= 31 -> dynamicLightColorScheme(context)
            dark -> darkColorScheme()
            else -> lightColorScheme()
        }
        if (policy.oledBlack) system.copy(background = Color.Black, surface = Color.Black) else system
    }
    SideEffect {
        Immersive.scheme = visualScheme
        Immersive.systemDark = dark
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.attributes = window.attributes.apply { preferredRefreshRate = policy.displayRefreshCap }
            window.statusBarColor = Color.Transparent.toArgb()
            // White icons on the dark palette.
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
            // B19: the navigation bar was left to the system — a white band under the always-dark
            // UI (reported in the modal sheets, where the bar was opaque) with dark icons on it.
            window.navigationBarColor = Color.Transparent.toArgb()
            // Without this Android lays its own translucent scrim on a transparent bar (API 29+),
            // which showed as a differently coloured strip under the full-screen Canvas.
            window.isNavigationBarContrastEnforced = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
            // Behind the first frame and under overscroll: the palette's page colour.
            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(visualScheme.background.toArgb()))
        }
    }

    MaterialTheme(colorScheme = visualScheme, content = content)
}
