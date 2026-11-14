/*
 * Adapted from BitChord (https://github.com/kushagrasinghx/BitChord), GPL-3.0-or-later,
 * app/src/main/java/com/music/bitchord/ui/components/GlassNavBar.kt at edc1e6f
 * (and app/src/main/java/com/music/bitchord/ui/components/FloatingBottomBar.kt). Modified for Myspoti, 2026-10.
 */
package com.music.spotui.ui.immersive.shell

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.ui.graphics.graphicsLayer
import com.music.spotui.ui.look.rememberReduceMotion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.music.spotui.ui.navigation.Routes
import com.music.spotui.ui.navigation.TabReselect

/** The tab the user is in, how to switch tabs, and whether the bar hides (player, queue). */
internal class TabNavigation(
    val selectedTab: String,
    val onTabClick: (Routes) -> Unit,
    val fullScreen: Boolean,
)

/** Shared navigation behavior for the bottom bar and navigation rail. */
@Composable
internal fun rememberTabNavigation(navController: NavController): TabNavigation {
    // Visibility follows the screen actually shown, not flags each screen sets.
    val navStack by navController.currentBackStackEntryAsState()
    val currentRoute = navStack?.destination?.route
    val fullScreen = currentRoute == Routes.Player.route ||
        currentRoute == Routes.Queue.route

    // The tab the user is in, even on one of its sub-pages (album, artist...). Kept whether or
    // not the bar shows, so it survives the bar being hidden on the player.
    var selectedTab by rememberSaveable { mutableStateOf(Routes.Home.route) }
    if (currentRoute in TabRoutes) selectedTab = currentRoute!!

    val onTabClick: (Routes) -> Unit = { item ->
        // Standard bottom-tab navigation: each tab keeps its state (no reload, scroll kept).
        if (item.route == selectedTab) {
            if (currentRoute == item.route) {
                // Already on the tab's own page: nothing to reload, only the re-tap shortcut.
                TabReselect.emit(item.route)
            } else if (!navController.popBackStack(item.route, inclusive = false)) {
                // On one of its sub-pages: back to its root page first.
                navController.navigate(item.route) { launchSingleTop = true }
            }
        } else {
            selectedTab = item.route
            navController.navigate(item.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                // Home always opens Home itself (its scroll kept), not the page last left
                // open from it (a playlist, an artist...).
                restoreState = item.route != Routes.Home.route
            }
        }
    }
    return TabNavigation(selectedTab, onTabClick, fullScreen)
}

// Friend activity is not a tab any more: it opens from Home's header, as in Spotify.
private val TabRoutes = listOf(Routes.Home.route, Routes.NewReleases.route, Routes.LiveRadio.route, Routes.Library.route, Routes.Search.route)

/** The tabs the bar shows. */
internal val TabItems = listOf(Routes.Home, Routes.NewReleases, Routes.LiveRadio, Routes.Library, Routes.Search)


@Composable
internal fun TabBarContent(selectedTab: String, onTabClick: (Routes) -> Unit, modifier: Modifier) {
    NavigationBar(modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        TabItems.forEach { item ->
            NavigationBarItem(
                selected = selectedTab == item.route,
                onClick = { onTabClick(item) },
                icon = { TabIcon(item, selectedTab == item.route) },
                label = { TabLabel(item) },
            )
        }
    }
}

@Composable
internal fun DistortionNavigationRail(navController: NavController) {
    val tabs = rememberTabNavigation(navController)
    if (tabs.fullScreen) return
    NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        TabItems.forEach { item ->
            NavigationRailItem(
                selected = tabs.selectedTab == item.route,
                onClick = { tabs.onTabClick(item) },
                icon = { TabIcon(item, tabs.selectedTab == item.route) },
                label = { TabLabel(item) },
            )
        }
    }
}

@Composable
private fun TabLabel(item: Routes) {
    Text(
        if (item.labelRes != 0) stringResource(item.labelRes) else item.label,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TabIcon(item: Routes, selected: Boolean) {
    val icon = when (item) {
        Routes.Home -> Icons.Rounded.Home
        Routes.NewReleases -> Icons.Rounded.GridView
        Routes.LiveRadio -> Icons.Rounded.Podcasts
        Routes.Library -> Icons.Rounded.LibraryMusic
        Routes.Search -> Icons.Rounded.Search
        else -> Icons.Rounded.Album
    }
    val scale = animateFloatAsState(
        if (selected) 1.13f else 1f,
        if (rememberReduceMotion()) snap() else MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "tabIconScale",
    )
    Icon(icon, contentDescription = null, modifier = Modifier.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    })
}
