package com.music.spotui.data.catalogue

import app.myspoti.api.Album
import app.myspoti.api.ArtistRef
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class GuestReleasesTest {
    private fun album(id: Char, date: String?) = Album("spotify:album:${id.toString().repeat(22)}", "Album $id", listOf(ArtistRef("artist", "Artist")), emptyList(), "album", date, 1)

    @Test fun recentResultsUseDatedPastReleasesAndDeduplicate() {
        val today = LocalDate.parse("2026-11-22")
        val older = album('A', "2026-01-15")
        val newer = album('B', "2026-11-01")
        val result = guestRecentReleases(listOf(older, newer, newer, album('C', "2027-01-01"), album('D', "2020"), album('E', null)), today)
        assertEquals(listOf(newer.uri, older.uri), result.map { it.entry.uri })
    }

    @Test fun invalidIdentifiersAndDatesAreExcluded() {
        val result = guestRecentReleases(listOf(album('A', "bad"), album('B', "2026-10-10").copy(uri = "https://example.org")), LocalDate.parse("2026-11-22"))
        assertEquals(emptyList<GuestRelease>(), result)
    }
}
