package com.music.spotui.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
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
        Triple("guest-new", R.string.bottom_tab_new, Icons.Rounded.GridView),
        Triple(Routes.LiveRadio.route, R.string.bottom_tab_radio, Icons.Rounded.Radio),
        Triple("guest-library", R.string.immersive_library_title, Icons.Rounded.LibraryMusic),
        Triple("guest-catalogue", R.string.navigationbar_musicappitems_search_title, Icons.Rounded.Search),
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
                    GuestHome(onLogin, { nav.openGuestTab(Routes.LocalFiles.route) }, { nav.openGuestTab(Routes.LiveRadio.route) }, { nav.openGuestTab("guest-catalogue") }, { nav.navigate(com.music.spotui.ui.navigation.localPlaylistRoute(it)) })
                }
                composable(Routes.LocalFiles.route) { ImmersiveLocalFilesScreen(nav, allowPlaylistEdits = true) }
                composable(Routes.LiveRadio.route) { com.music.spotui.radio.ui.ImmersiveLiveRadioScreen(nav) }
                composable("guest-new") { GuestCatalogueScreen(onLogin, releases = true) }
                composable("guest-library") { GuestLibrary(onLogin, { nav.navigate(Routes.LocalFiles.route) }, { nav.navigate(Routes.Extensions.route) }, { nav.navigate(com.music.spotui.ui.navigation.localPlaylistRoute(it)) }) }
                composable("${Routes.LocalPlaylist.route}/{id}") { entry ->
                    entry.arguments?.getString("id")?.let { com.music.spotui.ui.immersive.pages.ImmersiveLocalPlaylistScreen(nav, it) }
                }
                composable("guest-catalogue") { GuestCatalogueScreen(onLogin) }
                composable(Routes.Extensions.route) { com.music.spotui.ui.immersive.pages.ImmersiveExtensionsScreen(nav) }
                composable("${Routes.Extension.route}/{id}") { com.music.spotui.ui.immersive.pages.ImmersiveExtensionScreen(nav, it.arguments?.getString("id").orEmpty()) }
                composable("${Routes.ExtensionEntry.route}/{id}") { com.music.spotui.ui.immersive.pages.ImmersiveExtensionEntryScreen(nav, it.arguments?.getString("id").orEmpty()) }
                composable("extension-page/{id}") { com.music.spotui.ui.screens.ExtensionPageScreen(nav, it.arguments?.getString("id").orEmpty()) }
                composable(Routes.Queue.route) { ImmersiveQueueScreen(nav) }
                composable(Routes.Equalizer.route) { ImmersiveEqualizerScreen(nav) }
            }
        }
        ImmersivePlayerOverlay(nav)
    }
}

@Composable
private fun GuestLibrary(onLogin: () -> Unit, onLocal: () -> Unit, onExtensions: () -> Unit, onPlaylist: (String) -> Unit) {
    var creating by remember { mutableStateOf(false) }
    if (creating) NameDialog(stringResource(R.string.local_playlist_new), "", stringResource(R.string.create_playlist_create_button),
        onDismiss = { creating = false }) { name ->
        if (name.isNotBlank()) onPlaylist(com.music.spotui.data.local.LocalPlaylists.create(name).id)
        creating = false
    }
    val playlists = com.music.spotui.data.local.LocalPlaylists.list()
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text(stringResource(R.string.immersive_library_title), style = MaterialTheme.typography.headlineLarge) }
        item { GuestCard(R.string.local_files_title, R.string.guest_local_description, onLocal) }
        item { GuestCard(R.string.extensions_title, R.string.guest_playback_description, onExtensions) }
        item { FilledTonalButton(onClick = { creating = true }) { Text(stringResource(R.string.local_playlist_new)) } }
        items(playlists, key = { it.id }) { playlist ->
            Card(onClick = { onPlaylist(playlist.id) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    ImmersiveCover(playlist.items.firstOrNull()?.artUri, Modifier.size(64.dp))
                    Text(playlist.name, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        item { Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.guest_connect)) } }
    }
}

internal fun androidx.navigation.NavHostController.openGuestTab(route: String) {
    navigate(route) {
        popUpTo("guest-home")
        launchSingleTop = true
    }
}

@Composable
internal fun GuestHome(onLogin: () -> Unit, onLocal: () -> Unit, onRadio: () -> Unit, onBrowse: () -> Unit = {}, onPlaylist: (String) -> Unit = {}) {
    val recent by GuestListening.recent.collectAsState()
    val mixTitle = stringResource(R.string.home_your_mix)
    val playlists = com.music.spotui.data.local.LocalPlaylists.list()
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (recent.isEmpty()) item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = MaterialTheme.shapes.extraLarge) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.LibraryMusic, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(stringResource(R.string.guest_title), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(stringResource(R.string.guest_description), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        if (recent.isNotEmpty()) {
            item {
                BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                    val heroWidth = minOf(maxWidth, 540.dp)
                    Column(Modifier.width(heroWidth).align(Alignment.Center)) {
                        Box(Modifier.fillMaxWidth().height(heroWidth * 0.60f)) {
                            Column(Modifier.align(Alignment.CenterStart).fillMaxWidth(0.68f)) {
                                Text(mixTitle, style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = (heroWidth.value * 0.175f).coerceIn(38f, 84f).sp,
                                    fontWeight = FontWeight.Bold))
                                Text(stringResource(R.string.guest_mix_description), style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            FilledIconButton(onClick = { LocalPlayer.play(recent, 0, shuffle = true, context = mixTitle) },
                                modifier = Modifier.align(Alignment.BottomEnd).size((heroWidth * 0.30f).coerceIn(72.dp, 118.dp))) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.guest_play_mix), modifier = Modifier.size(40.dp))
                            }
                        }
                        Box(Modifier.fillMaxWidth().padding(top = 28.dp).aspectRatio(1.48f)) {
                            ImmersiveCover(recent.first().artUri.toString(), Modifier.align(Alignment.Center)
                                .size(heroWidth * 0.48f, heroWidth * 0.70f).rotate(39f), shape = CircleShape, border = false)
                            recent.getOrNull(1)?.let { track -> ImmersiveCover(track.artUri.toString(),
                                Modifier.align(Alignment.TopStart).size(heroWidth * 0.23f), shape = CircleShape, border = false) }
                            recent.getOrNull(2)?.let { track -> ImmersiveCover(track.artUri.toString(),
                                Modifier.align(Alignment.BottomEnd).size(heroWidth * 0.23f), shape = CircleShape, border = false) }
                        }
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
            val artists = recent.filter { it.artist.isNotBlank() }.groupBy { it.artist }
            if (artists.isNotEmpty()) item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.guest_artist_mixes), style = MaterialTheme.typography.titleLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(artists.entries.toList(), key = { it.key }) { (artist, tracks) ->
                            ElevatedCard(onClick = { LocalPlayer.play(tracks, 0, shuffle = true, context = artist) }, modifier = Modifier.width(160.dp)) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ImmersiveCover(tracks.first().artUri.toString(), Modifier.size(136.dp))
                                    Text(artist, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(stringResource(R.string.guest_artist_mix_description), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (playlists.isNotEmpty()) item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.guest_your_playlists), style = MaterialTheme.typography.titleLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(playlists, key = { it.id }) { playlist ->
                        Column(Modifier.width(136.dp).clickable { onPlaylist(playlist.id) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ImmersiveCover(playlist.items.firstOrNull()?.artUri, Modifier.size(136.dp))
                            Text(playlist.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
