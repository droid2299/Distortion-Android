package com.music.spotui.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.myspoti.engine.R
import com.music.spotui.ui.components.LoginReportLink
import com.music.spotui.ui.tv.Tv

/** Signed-out entry point. Account and guest transitions remain owned by the activity. */
@Composable
fun WelcomeScreen(onLogin: () -> Unit, onGuest: () -> Unit = {}) {
    val context = LocalContext.current
    val tv = remember(context) { Tv.isTv(context) }
    val loginFocus = remember { FocusRequester() }
    var loginFocused by remember { mutableStateOf(false) }
    LaunchedEffect(tv) {
        if (tv) {
            withFrameNanos { }
            loginFocus.requestFocus()
        }
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { insets ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets)) {
            // The minimum height anchors actions on roomy displays. The whole
            // page scrolls when content grows, including landscape/large fonts.
            Column(
                Modifier.align(Alignment.TopCenter).widthIn(max = 560.dp).fillMaxWidth()
                    .verticalScroll(rememberScrollState()).heightIn(min = maxHeight)
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Surface(Modifier.size(72.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
                            Icon(painterResource(R.drawable.ic_launcher_foreground), null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Text(stringResource(R.string.onboarding_brand), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.onboarding_intro), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(Modifier.padding(vertical = 8.dp)) {
                            WelcomeFeature(Icons.Rounded.LibraryMusic, stringResource(R.string.onboarding_local_title), stringResource(R.string.onboarding_local_detail))
                            WelcomeFeature(Icons.Rounded.Radio, stringResource(R.string.onboarding_radio_title), stringResource(R.string.onboarding_radio_detail))
                            WelcomeFeature(Icons.Rounded.Search, stringResource(R.string.onboarding_browse_title), stringResource(R.string.onboarding_browse_detail))
                        }
                    }
                }
                Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onLogin,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                            .then(if (tv) Modifier.focusRequester(loginFocus).onFocusChanged { loginFocused = it.isFocused } else Modifier),
                        border = if (tv && loginFocused) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null,
                    ) { Text(stringResource(R.string.welcome_login)) }
                    FilledTonalButton(onClick = onGuest, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                        Text(stringResource(R.string.guest_continue))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Lock, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(R.string.welcome_privacy), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeFeature(icon: ImageVector, title: String, detail: String) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(detail, style = MaterialTheme.typography.bodyMedium) },
        leadingContent = {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondaryContainer) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
internal fun WelcomeProblemScreen(onRetry: () -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { insets ->
        Box(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets), contentAlignment = Alignment.Center) {
            Column(
                Modifier.widthIn(max = 560.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.errorContainer) {
                    Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.WifiOff, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                Text(stringResource(R.string.login_failed_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.login_failed_detail), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(stringResource(R.string.retry_button))
                }
                LoginReportLink()
            }
        }
    }
}
