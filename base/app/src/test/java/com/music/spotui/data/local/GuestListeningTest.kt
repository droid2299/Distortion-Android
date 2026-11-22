package com.music.spotui.data.local

import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34], application = android.app.Application::class)
class GuestListeningTest {
    private fun track(uri: String) = LocalTrack(1, Uri.parse(uri), "Song", "Artist", "Album", 1234, Uri.parse("https://example.org/art"))

    @Test fun `history round trips local and catalogue tracks`() {
        val tracks = listOf(track("content://media/audio/1"), track("myspoti-ext://track/${"a".repeat(22)}?title=Song"))
        assertEquals(tracks, GuestListening.decode(GuestListening.encode(tracks)))
    }

    @Test fun `history excludes streaming urls and malformed catalogue ids`() {
        assertFalse(GuestListening.allowed(Uri.parse("https://example.org/private-audio")))
        assertFalse(GuestListening.allowed(Uri.parse("myspoti-ext://track/not-an-id")))
        val stored = GuestListening.encode(listOf(track("https://example.org/private-audio")))
        assertTrue(GuestListening.decode(stored).isEmpty())
        assertTrue(GuestListening.decode("bad json").isEmpty())
    }
}
