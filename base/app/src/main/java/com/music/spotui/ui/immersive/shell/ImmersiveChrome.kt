/*
 * The minimizing chrome follows BitChord (https://github.com/kushagrasinghx/BitChord),
 * GPL-3.0-or-later, app/src/main/java/com/music/bitchord/ui/components/floatingtabbar/FloatingTabBar.kt
 * (FloatingTabBarScrollConnection, the inline bar) and GlassNavBar.kt at edc1e6f.
 * Written anew for Myspoti, 2026-10: a scroll connection pages attach to, and one layout that
 * morphs the tab bar into a pill with the mini player beside it.
 */
package com.music.spotui.ui.immersive.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun ImmersiveBottomChrome(
    navController: NavController,
    miniPlayer: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = rememberTabNavigation(navController)
    if (tabs.fullScreen || WindowInsets.ime.getBottom(LocalDensity.current) > 0) return
    val large = com.music.spotui.ui.components.windowSize().large
    Column(
        modifier.fillMaxWidth().then(if (large) Modifier.windowInsetsPadding(WindowInsets.navigationBars) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        miniPlayer(Modifier.widthIn(max = 560.dp).padding(horizontal = 12.dp, vertical = 6.dp))
        if (!large) TabBarContent(tabs.selectedTab, tabs.onTabClick, Modifier)
    }
}
