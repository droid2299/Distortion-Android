package com.music.spotui.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import app.myspoti.engine.R
import com.music.spotui.di.LocalPlayer
import com.music.spotui.ui.immersive.pages.ImmersiveEqualizerScreen
import com.music.spotui.ui.immersive.pages.ImmersiveLocalFilesScreen
import com.music.spotui.ui.immersive.pages.ImmersiveQueueScreen
import com.music.spotui.ui.immersive.player.ImmersiveMiniPlayer
import com.music.spotui.ui.immersive.player.ImmersivePlayerOverlay
import com.music.spotui.ui.navigation.Routes
import com.music.spotui.data.local.GuestListening
import com.music.spotui.ui.immersive.components.ImmersiveCover
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun GuestScreen(onLogin: () -> Unit) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val tabs = listOf(
        Triple("guest-home", R.string.navigationbar_musicappitems_home_title, Icons.Rounded.Home),
        Triple(Routes.LocalFiles.route, R.string.local_files_title, Icons.Rounded.LibraryMusic),
        Triple(Routes.LiveRadio.route, R.string.bottom_tab_radio, Icons.Rounded.Radio),
        Triple("guest-catalogue", R.string.catalogue_title, Icons.Rounded.Search),
    )
    Box(Modifier.fillMaxSize()) {
        Scaffold(bottomBar = {
            Column {
                if (LocalPlayer.active.value) ImmersiveMiniPlayer()
                NavigationBar {
                    tabs.forEach { (route, title, icon) ->
                        NavigationBarItem(
                            selected = entry?.destination?.route == route,
                            onClick = { nav.openGuestTab(route) },
                            icon = { Icon(icon, contentDescription = null) },
                            label = { Text(stringResource(title)) },
                        )
                    }
                }
            }
        }) { padding ->
            NavHost(nav, startDestination = "guest-home", modifier = Modifier.padding(padding)) {
                composable("guest-home") {
                    GuestHome(onLogin, { nav.openGuestTab(Routes.LocalFiles.route) }, { nav.openGuestTab(Routes.LiveRadio.route) }, { nav.openGuestTab("guest-catalogue") })
                }
                composable(Routes.LocalFiles.route) { ImmersiveLocalFilesScreen(nav, allowPlaylistEdits = false) }
                composable(Routes.LiveRadio.route) { com.music.spotui.radio.ui.ImmersiveLiveRadioScreen(nav) }
                composable("guest-catalogue") { GuestCatalogueScreen(onLogin) }
                composable(Routes.Queue.route) { ImmersiveQueueScreen(nav) }
                composable(Routes.Equalizer.route) { ImmersiveEqualizerScreen(nav) }
            }
        }
        ImmersivePlayerOverlay(nav)
    }
}

internal fun androidx.navigation.NavHostController.openGuestTab(route: String) {
    navigate(route) {
        popUpTo("guest-home")
        launchSingleTop = true
    }
}

@Composable
internal fun GuestHome(onLogin: () -> Unit, onLocal: () -> Unit, onRadio: () -> Unit, onBrowse: () -> Unit = {}) {
    val recent by GuestListening.recent.collectAsState()
    val mixTitle = stringResource(R.string.home_your_mix)
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Text(stringResource(R.string.guest_title), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.guest_description), modifier = Modifier.padding(top = 12.dp))
        }
        if (recent.isNotEmpty()) {
            item {
                ElevatedCard(onClick = { LocalPlayer.play(recent, 0, shuffle = true, context = mixTitle) },
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        ImmersiveCover(recent.first().artUri.toString(), Modifier.size(88.dp))
                        Column(Modifier.weight(1f)) {
                            Text(mixTitle, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(stringResource(R.string.guest_mix_description), style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.guest_play_mix),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(40.dp))
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.guest_recently_played), style = MaterialTheme.typography.titleLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(recent, key = { it.uri.toString() }) { track ->
                            Column(Modifier.width(136.dp).clickable {
                                LocalPlayer.play(recent, recent.indexOf(track), context = mixTitle)
                            }) {
                                ImmersiveCover(track.artUri.toString(), Modifier.size(136.dp))
                                Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(track.artist, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
        item { GuestCard(R.string.local_files_title, R.string.guest_local_description, onLocal) }
        item { GuestCard(R.string.bottom_tab_radio, R.string.guest_radio_description, onRadio) }
        item { GuestCard(R.string.catalogue_title, R.string.catalogue_home_description, onBrowse) }
        item {
            Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.guest_connect)) }
        }
    }
}

@Composable
private fun GuestCard(title: Int, description: Int, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(description), style = MaterialTheme.typography.bodyLarge)
        }
    }
}
