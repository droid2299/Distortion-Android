package com.music.spotui.ui.screens

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-port-xhdpi", application = Application::class)
class GuestHomeTest {
    @get:Rule val rule = createComposeRule()

    @Test fun listeningChoicesAndSignInRemainReachableOnSmallScreens() {
        var local = 0
        var radio = 0
        var login = 0
        var browse = 0
        rule.setContent {
            MaterialTheme { GuestHome({ login++ }, { local++ }, { radio++ }, { browse++ }) }
        }
        rule.onNodeWithText("Listen to music on this device, including FLAC files.").performClick()
        rule.onNodeWithText("Explore internet stations. Browsing and playback use your connection.").performScrollTo().performClick()
        rule.onNodeWithText("Discover songs, albums, artists and public playlists without signing in.").performScrollTo().performClick()
        // The final item can be outside LazyColumn's composition on small screens.
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Connect Spotify"))
        rule.onNodeWithText("Connect Spotify").performClick()
        rule.runOnIdle {
            assertEquals(1, local)
            assertEquals(1, radio)
            assertEquals(1, login)
            assertEquals(1, browse)
        }
    }

    @Test fun homeDoesNotRestoreTheRadioOrLocalPageItJustLeft() {
        rule.setContent {
            val nav = rememberNavController()
            val entry by nav.currentBackStackEntryAsState()
            Column {
                Button(onClick = { nav.openGuestTab("guest-home") }) { Text("Home tab") }
                Button(onClick = { nav.openGuestTab("localfiles") }) { Text("Local tab") }
                Button(onClick = { nav.openGuestTab("liveradio") }) { Text("Radio tab") }
                Text("Selected: ${entry?.destination?.route}")
                NavHost(nav, startDestination = "guest-home") {
                    composable("guest-home") { Text("Guest home content") }
                    composable("localfiles") { Text("Local content") }
                    composable("liveradio") { Text("Radio content") }
                }
            }
        }
        for (tab in listOf("Radio tab", "Local tab", "Radio tab")) {
            rule.onNodeWithText(tab).performClick()
            rule.onNodeWithText("Home tab").performClick()
            rule.onNodeWithText("Selected: guest-home").assertExists()
            rule.onNodeWithText("Guest home content").assertExists()
        }
    }
}
