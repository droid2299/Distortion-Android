package com.music.spotui.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Text(stringResource(R.string.guest_title), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.guest_description), modifier = Modifier.padding(top = 12.dp))
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
