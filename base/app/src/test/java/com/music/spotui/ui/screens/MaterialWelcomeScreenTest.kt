package com.music.spotui.ui.screens

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import app.myspoti.engine.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w320dp-h480dp-port-xhdpi", application = Application::class)
class MaterialWelcomeScreenTest {
    @get:Rule val rule = createComposeRule()

    private fun verifyChoices(dark: Boolean, fontScale: Float) {
        var login = 0
        var guest = 0
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                    WelcomeScreen({ login++ }, { guest++ })
                }
            }
        }
        rule.onNodeWithText("Log in with Spotify").performScrollTo().assertIsDisplayed().performClick()
        rule.onNodeWithText("Continue without an account").performScrollTo().assertIsDisplayed().performClick()
        rule.runOnIdle {
            assertEquals(1, login)
            assertEquals(1, guest)
        }
    }

    @Test fun bothChoicesRemainReachableOnSmallDarkScreens() = verifyChoices(dark = true, fontScale = 1f)

    @Test
    @Config(qualifiers = "w640dp-h360dp-land-xhdpi")
    fun bothChoicesRemainReachableInLandscapeWithLargeText() = verifyChoices(dark = false, fontScale = 2f)

    @Test fun loginFailureCanBeRetriedWithLargeText() {
        var retries = 0
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
                MaterialTheme { WelcomeProblemScreen { retries++ } }
            }
        }
        val label = RuntimeEnvironment.getApplication().getString(R.string.retry_button)
        rule.onNodeWithText(label).performScrollTo().assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals(1, retries) }
    }
}
