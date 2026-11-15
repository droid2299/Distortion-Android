package com.music.spotui.ui.viewmodel

import com.music.spotui.data.api.Response
import com.music.spotui.data.entity.PodcastModel
import com.music.spotui.data.entity.SearchResults
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.di.CurrentSongState
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FeedReliabilityTest {
    @Test fun identicalHomeFailuresStillRetryAndRecover() = runBlocking {
        var attempts = 0
        val states = retryHomeFeed(longArrayOf(0, 0, 0)) {
            attempts++
            flowOf(Response.Loading(), if (attempts == 3) Response.Success("fresh") else Response.Error("offline"))
        }.toList()

        assertEquals(3, attempts)
        assertEquals(1, states.count { it is Response.Loading })
        assertEquals(Response.Success("fresh"), states.last())
    }

    @Test fun permanentHomeFailureExhaustsItsRetryBudget() = runBlocking {
        val error = Response.Error<String>("offline")
        assertEquals(listOf(error, error, error), retryHomeFeed(longArrayOf(0, 0)) { flowOf(error) }.toList())
    }

    @Test fun searchCacheRejectsLateWritesFromAnOlderSession() {
        val cache = SearchViewModel.RecentSearches()
        val results = SearchResults(shows = listOf(PodcastModel("show", "Show", "Publisher", "")))

        cache.put(1, " query ", results, now = 10)
        assertEquals(results, cache.get(1, "QUERY", now = 11))
        assertNull(cache.get(2, "query", now = 12))
        cache.put(1, "query", results, now = 13)
        assertNull(cache.get(2, "query", now = 14))

        cache.put(2, "query", results, now = 15)
        assertEquals(results, cache.get(2, "query", now = 16))
        assertNull(cache.get(2, "query", now = 180_015))
    }

    @Test fun oldRadioCannotReplaceRecreatedQueueWithSameSeed() {
        val state = CurrentSongState()
        val seed = SongsModel().copy(id = 1)
        val next = SongsModel().copy(id = 2)
        val stale = state.startRadioQueue(seed)
        val current = state.startRadioQueue(seed)

        state.updateQueueIfCurrent(stale, listOf(seed, next))
        assertEquals(listOf(seed), state.queue.value)
        state.updateQueueIfCurrent(current, listOf(seed, next))
        assertEquals(listOf(seed, next), state.queue.value)
    }
}
