package com.music.spotui.ui.screens

import com.music.spotui.data.catalogue.CatalogueEntry
import com.music.spotui.data.catalogue.CatalogueKind
import org.junit.Assert.assertEquals
import org.junit.Test

class GuestPlaybackSongsTest {
    @Test fun `only valid tracks are queued in their original order with metadata`() {
        val first = "spotify:track:${"a".repeat(22)}"
        val second = "spotify:track:${"b".repeat(22)}"
        val songs = guestPlaybackSongs(listOf(
            CatalogueEntry(first, "First", "Artist", "https://example.invalid/art", CatalogueKind.TRACK),
            CatalogueEntry("spotify:album:${"c".repeat(22)}", "Album", "", "", CatalogueKind.ALBUM),
            CatalogueEntry("spotify:track:bad", "Invalid", "", "", CatalogueKind.TRACK),
            CatalogueEntry(second, "Second", "Other", "", CatalogueKind.TRACK),
        ))
        assertEquals(listOf(first, second), songs.map { it.url })
        assertEquals("a".repeat(22), songs.first().spotifyTrackId)
        assertEquals("Artist", songs.first().singer)
        assertEquals("https://example.invalid/art", songs.first().coverUri)
    }
}
