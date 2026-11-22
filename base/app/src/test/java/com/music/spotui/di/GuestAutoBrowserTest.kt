package com.music.spotui.di

import android.net.Uri
import com.music.spotui.data.local.LocalTrack
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34], application = android.app.Application::class)
class GuestAutoBrowserTest {
    private fun track(uri: String, title: String, artist: String) = LocalTrack(1, Uri.parse(uri), title, artist, "", 0, Uri.EMPTY)

    @Test fun searchUsesOnlyMatchingRecentTracksAndDeduplicates() {
        val a = track("content://media/audio/1", "Night Drive", "Artist")
        val b = track("content://media/audio/2", "Daylight", "Night Band")
        assertEquals(listOf(a, b), guestFindRecent(listOf(a, a, b), "night"))
        assertTrue(guestFindRecent(listOf(a), " ").isEmpty())
        assertTrue(guestFindRecent(listOf(a), "unknown").isEmpty())
    }

    @Test fun mediaKeysAreStableOpaqueAndDifferentForDifferentUris() {
        val a = track("content://media/audio/private-path", "A", "Artist")
        val b = track("content://media/audio/other", "A", "Artist")
        assertEquals(guestTrackKey(a), guestTrackKey(a.copy(title = "Renamed")))
        assertNotEquals(guestTrackKey(a), guestTrackKey(b))
        assertFalse(guestTrackKey(a).contains("private-path"))
    }
}
