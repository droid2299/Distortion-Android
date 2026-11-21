package com.music.spotui.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34], application = android.app.Application::class)
class GuestSessionTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before fun reset() {
        context.getSharedPreferences("myspoti_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        GuestSession.set(context, false)
    }

    @After fun cleanUp() = reset()

    @Test fun firstLaunchRemainsOnWelcome() {
        GuestSession.init(context)
        assertFalse(GuestSession.active)
    }

    @Test fun guestChoiceSurvivesRestartWithoutClaimingSpotifyLogin() {
        GuestSession.set(context, true)
        GuestSession.init(context)
        assertTrue(GuestSession.active)
        assertFalse(context.getSharedPreferences("myspoti_prefs", Context.MODE_PRIVATE).getBoolean("LoggedIn", false))
    }

    @Test fun existingSpotifyAccountTakesPriorityOverOldGuestChoice() {
        GuestSession.set(context, true)
        context.getSharedPreferences("myspoti_prefs", Context.MODE_PRIVATE).edit().putBoolean("LoggedIn", true).commit()
        GuestSession.init(context)
        assertFalse(GuestSession.active)
    }

    @Test fun successfulSignInClearsPersistedGuestChoice() {
        GuestSession.set(context, true)
        GuestSession.set(context, false)
        GuestSession.init(context)
        assertFalse(GuestSession.active)
    }
}
