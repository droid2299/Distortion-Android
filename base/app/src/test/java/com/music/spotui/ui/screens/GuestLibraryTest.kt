package com.music.spotui.ui.screens

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
class GuestLibraryTest {
    @get:Rule val rule = createComposeRule()

    @Test fun localFilesAndExtensionsRemainReachable() {
        var local = 0
        var extensions = 0
        rule.setContent { MaterialTheme { GuestLibrary({}, { local++ }, { extensions++ }, {}) } }
        rule.onNodeWithText("Local Files").performClick()
        rule.onNodeWithText("Extensions").performClick()
        assertEquals(1, local)
        assertEquals(1, extensions)
    }
}
