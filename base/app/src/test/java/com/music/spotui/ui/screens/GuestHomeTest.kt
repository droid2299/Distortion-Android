package com.music.spotui.ui.screens

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
        rule.setContent {
            MaterialTheme { GuestHome({ login++ }, { local++ }, { radio++ }) }
        }
        rule.onNodeWithText("Listen to music on this device, including FLAC files.").performClick()
        rule.onNodeWithText("Explore internet stations. Browsing and playback use your connection.").performScrollTo().performClick()
        rule.onNodeWithText("Connect Spotify").performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals(1, local)
            assertEquals(1, radio)
            assertEquals(1, login)
        }
    }
}
