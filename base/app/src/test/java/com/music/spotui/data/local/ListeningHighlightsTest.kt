package com.music.spotui.data.local

import com.music.spotui.data.entity.SongsModel
import com.music.spotui.data.offline.OfflineFavorites
import com.music.spotui.data.offline.favoriteReservations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

class ListeningHighlightsTest {
    private fun song(id: String, playable: Boolean = true) =
        SongsModel().copy(spotifyTrackId = id, title = id, playable = playable)

    private fun listen(at: String, key: String, duration: Long = 90_000, id: String = "") =
        ListenHistory.Listen(Instant.parse(at).toEpochMilli(), key, key, "Artist", "", "", duration,
            ListenHistory.Source.SPOTIFY, spotifyTrackId = id)

    @Test fun mixesInterleaveStationsAndOnlyUseUniquePlayableTracks() {
        val seed = song("seed")
        val left = listOf(seed, song("a"), song("c"), song("unavailable", false), song(""))
        val right = listOf(song("b"), song("a"), song("d"))
        val balanced = buildMix(listOf(seed), listOf(left, right), setOf("c"), MixMode.BALANCED)
        assertEquals(listOf("seed", "b", "c", "a", "d"), balanced.map { it.spotifyTrackId })
        assertEquals(listOf("b", "a", "seed"), buildMix(listOf(seed), listOf(left, right), setOf("c"), MixMode.DISCOVER, 3).map { it.spotifyTrackId })
        assertEquals(listOf("seed", "c", "b"), buildMix(listOf(seed), listOf(left, right), setOf("c"), MixMode.FAMILIAR, 3).map { it.spotifyTrackId })
    }

    @Test fun monthRespectsLocalBoundaryAndRankingSupportsOldRecords() {
        val march = YearMonth.of(2026, 3)
        val zone = ZoneId.of("America/New_York")
        val before = listen("2026-03-01T04:59:59Z", "spotify:track:before")
        val first = listen("2026-03-01T05:00:00Z", "spotify:track:old")
        val second = listen("2026-03-20T17:00:00Z", "local:101", id = "old")
        val local = listen("2026-03-28T22:00:00Z", "local:202")
        val next = listen("2026-04-01T04:00:00Z", "spotify:track:next")
        val matched = listensInMonth(listOf(before, first, second, local, next), march, zone)
        assertEquals(listOf(first, second, local), matched)
        assertEquals(listOf("old"), rankedSpotifyIds(matched, 30))
    }

    @Test fun automaticDownloadsReserveSpaceAndTrackSlots() {
        val mb = OfflineFavorites.MIB
        assertEquals(listOf(24 * mb), favoriteReservations(128 * mb, 100 * mb, 19, 4, 1_000 * mb))
        assertTrue(favoriteReservations(128 * mb, 106 * mb, 12, 4, 1_000 * mb).isEmpty())
        assertTrue(favoriteReservations(512 * mb, 0, 0, 4, 70 * mb).isEmpty())
        assertTrue(favoriteReservations(512 * mb, 0, 20, 4, 1_000 * mb).isEmpty())
    }
}
