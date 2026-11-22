package com.music.spotui.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.music.spotui.ui.immersive.components.ImmersiveSearchField
import com.music.spotui.ui.immersive.components.SectionHeader
import com.music.spotui.ui.immersive.Immersive
import com.music.spotui.ui.immersive.shell.ImmersiveLargeTitle
import com.music.spotui.ui.immersive.shell.ImmersiveTopBar
import com.music.spotui.ui.immersive.shell.ImmersiveBarAction
import com.music.spotui.ui.immersive.shell.immersiveTopContentPadding
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch

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
                composable("guest-new") { GuestCatalogueScreen(onLogin, releases = true, onExtensions = { nav.navigate(Routes.Extensions.route) }) }
                composable("guest-library") { GuestLibrary(onLogin, { nav.navigate(Routes.LocalFiles.route) }, { nav.navigate(Routes.Extensions.route) }, { nav.navigate(com.music.spotui.ui.navigation.localPlaylistRoute(it)) }) }
                composable("${Routes.LocalPlaylist.route}/{id}") { entry ->
                    entry.arguments?.getString("id")?.let { com.music.spotui.ui.immersive.pages.ImmersiveLocalPlaylistScreen(nav, it) }
                }
                composable("guest-catalogue") { GuestCatalogueScreen(onLogin, onExtensions = { nav.navigate(Routes.Extensions.route) }) }
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
internal fun GuestLibrary(onLogin: () -> Unit, onLocal: () -> Unit, onExtensions: () -> Unit, onPlaylist: (String) -> Unit) {
    var creating by remember { mutableStateOf(false) }
    var searchActive by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    if (creating) NameDialog(stringResource(R.string.local_playlist_new), "", stringResource(R.string.create_playlist_create_button),
        onDismiss = { creating = false }) { name ->
        if (name.isNotBlank()) onPlaylist(com.music.spotui.data.local.LocalPlaylists.create(name).id)
        creating = false
    }
    val playlists = com.music.spotui.data.local.LocalPlaylists.list().filter { it.name.contains(query, ignoreCase = true) }
    val libraryTitle = stringResource(R.string.immersive_library_title)
    Box(Modifier.fillMaxSize().background(Immersive.Page)) {
    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(top = immersiveTopContentPadding(), bottom = 24.dp)) {
        item {
            ImmersiveLargeTitle(libraryTitle)
            if (searchActive) ImmersiveSearchField(query = query, onQueryChange = { query = it }, onSubmit = {},
                placeholder = stringResource(R.string.library_search_text_hint), modifier = Modifier.padding(horizontal = Immersive.Gutter))
            GuestLibraryCategory(Icons.Rounded.QueueMusic, R.string.guest_your_playlists) {
                scope.launch { listState.animateScrollToItem(1) }
            }
            GuestLibraryCategory(Icons.Rounded.MusicNote, R.string.local_files_title, onLocal)
            GuestLibraryCategory(Icons.Rounded.Extension, R.string.extensions_title, onExtensions)
        }
        item { SectionHeader(stringResource(R.string.library_sort_recently_added)) }
        if (playlists.isEmpty()) {
            item { Text(stringResource(if (query.isBlank()) R.string.guest_library_empty else R.string.catalogue_empty),
                modifier = Modifier.padding(horizontal = Immersive.Gutter, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(playlists.chunked(2), key = { it.first().id }) { row ->
                Row(Modifier.fillMaxWidth().padding(horizontal = Immersive.Gutter, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { playlist ->
                        Column(Modifier.weight(1f).clickable { onPlaylist(playlist.id) }) {
                            ImmersiveCover(playlist.items.firstOrNull()?.artUri, Modifier.fillMaxWidth().aspectRatio(1f))
                            Text(playlist.name, style = MaterialTheme.typography.titleSmall, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                            Text(stringResource(R.string.local_playlist_subtitle), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        item { TextButton(onClick = onLogin, modifier = Modifier.padding(horizontal = Immersive.Gutter, vertical = 16.dp)) { Text(stringResource(R.string.guest_connect)) } }
    }
    ImmersiveTopBar(libraryTitle, listState) {
        ImmersiveBarAction(Icons.Rounded.Add, stringResource(R.string.create_playlist_content_description), onClick = { creating = true })
        ImmersiveBarAction(Icons.Rounded.Search, stringResource(R.string.library_search_text_hint), onClick = {
            searchActive = !searchActive
            if (!searchActive) query = ""
        })
    }
    }
}

@Composable
private fun GuestLibraryCategory(icon: androidx.compose.ui.graphics.vector.ImageVector, title: Int, onClick: () -> Unit) {
    Column(Modifier.padding(horizontal = Immersive.Gutter)) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(22.dp))
            Text(stringResource(title), modifier = Modifier.weight(1f).padding(start = 14.dp), style = MaterialTheme.typography.bodyLarge)
            Icon(Icons.Rounded.ChevronRight, null, modifier = Modifier.size(20.dp))
        }
        HorizontalDivider(Modifier.padding(start = 36.dp))
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
