package com.music.spotui.data.local

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34], application = android.app.Application::class)
class GuestListenHistoryTest {
    private lateinit var context: Context

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ListenHistory.init(context)
        ListenHistory.clear()
        context.filesDir.resolve("guest_listen_history.jsonl").delete()
    }

    @After fun tearDown() {
        ListenHistory.clear()
        context.filesDir.resolve("guest_listen_history.jsonl").delete()
        context.filesDir.resolve("listen-history/test-account.jsonl").delete()
    }

    private fun listen(title: String, duration: Long = 30_000) = ListenHistory.Listen(
        1, title, title, "Artist", "Album", "", duration, ListenHistory.Source.SPOTIFY
    )

    @Test fun guestHistoryIsNotClaimedOrClearedWithAccount() {
        ListenHistory.record(listen("guest"), guest = true)
        ListenHistory.claim("test-account")
        ListenHistory.record(listen("account"))
        assertEquals(listOf("account"), ListenHistory.all().map { it.title })
        assertEquals(listOf("guest"), ListenHistory.allGuest().map { it.title })
        ListenHistory.park("test-account")
        ListenHistory.claim("test-account")
        assertEquals(listOf("account"), ListenHistory.all().map { it.title })
        ListenHistory.clear()
        assertEquals(listOf("guest"), ListenHistory.allGuest().map { it.title })
    }

    @Test fun guestHistoryUsesSameMinimumListeningThreshold() {
        ListenHistory.record(listen("short", 29_999), guest = true)
        ListenHistory.record(listen("eligible"), guest = true)
        assertEquals(listOf("eligible"), ListenHistory.allGuest().map { it.title })
        assertEquals(emptyList<ListenHistory.Listen>(), ListenHistory.all())
    }
}
