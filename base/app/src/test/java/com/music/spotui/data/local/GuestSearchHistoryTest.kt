package com.music.spotui.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34], application = android.app.Application::class)
class GuestSearchHistoryTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    @Before fun reset() { context.getSharedPreferences("distortion_guest_search", Context.MODE_PRIVATE).edit().clear().commit() }

    @Test fun historyIsOrderedBoundedAndCaseInsensitive() {
        val history = GuestSearchHistory(context)
        repeat(25) { history.add("Search $it") }
        assertEquals(20, history.read().size)
        history.add("  search 24  ")
        assertEquals("search 24", GuestSearchHistory(context).read().first())
        assertEquals(1, history.read().count { it.equals("search 24", ignoreCase = true) })
        history.remove("SEARCH 24")
        assertFalse(history.read().any { it.equals("search 24", ignoreCase = true) })
        history.clear()
        assertTrue(history.read().isEmpty())
    }

    @Test fun malformedHistoryAndShortTermsAreIgnored() {
        assertTrue(GuestSearchHistory.decode("bad json").isEmpty())
        assertTrue(GuestSearchHistory.merge(emptyList(), " a ").isEmpty())
        assertEquals(listOf("valid"), GuestSearchHistory.decode("[42, null, \"valid\"]"))
    }
}
