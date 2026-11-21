package com.music.spotui.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import app.myspoti.api.Artist
import app.myspoti.api.Playlist
import com.music.spotui.data.api.Response
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ArtistDiscoveryViewModelTest {
    private val store = ViewModelStore()
    private val uri = "spotify:artist:${"a".repeat(22)}"
    private fun playlist(name: String) = Playlist("spotify:playlist:$name", name, null, null, emptyList(), 0)
    private fun keep(model: ArtistDiscoveryViewModel) = model.also { store.put("model", it) }
    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (!condition() && System.nanoTime() < deadline) Thread.sleep(10)
        assertTrue("Discovery state did not settle", condition())
    }
    @After fun clear() = store.clear()

    @Test fun failuresAndRetriesAreIndependentAcrossSections() {
        val reads = AtomicInteger()
        val related = AtomicInteger()
        val model = keep(ArtistDiscoveryViewModel(
            readPlaylists = { if (reads.incrementAndGet() == 1) throw IOException("offline") else listOf(playlist("available")) },
            readRelated = { related.incrementAndGet(); listOf(Artist("spotify:artist:related", "Related", emptyList())) },
        ))
        model.load(uri, true)
        await { model.playlists.value is Response.Error && model.related.value is Response.Success }
        model.retryPlaylists(uri)
        await { model.playlists.value is Response.Success }
        assertEquals(2, reads.get())
        assertEquals(1, related.get())
    }

    @Test fun cachedOverviewRelatedArtistsDoNotCauseAnotherNetworkRequest() {
        val reads = AtomicInteger()
        val related = AtomicInteger()
        var clock = 1000L
        val model = keep(ArtistDiscoveryViewModel(
            readPlaylists = { reads.incrementAndGet(); emptyList() },
            readRelated = { related.incrementAndGet(); emptyList() },
            clock = { clock },
        ))
        model.load(uri, false)
        await { model.playlists.value is Response.Success }
        model.load(uri, false)
        assertEquals(1, reads.get())
        assertEquals(0, related.get())
        clock += 300_001
        model.load(uri, false)
        await { reads.get() == 2 }
    }

    @Test fun aLateOldArtistCannotReplaceTheNewArtist() = runBlocking {
        val started = CountDownLatch(1)
        val release = CompletableDeferred<Unit>()
        val other = "spotify:artist:${"b".repeat(22)}"
        val model = keep(ArtistDiscoveryViewModel(
            readPlaylists = { selected ->
                if (selected == uri) {
                    started.countDown()
                    withContext(NonCancellable) { release.await() }
                    listOf(playlist("old"))
                } else listOf(playlist("new"))
            }, readRelated = { emptyList() },
        ))
        try {
            model.load(uri, false)
            assertTrue(started.await(5, TimeUnit.SECONDS))
            val old = checkNotNull(model.viewModelScope.coroutineContext[Job]).children.first()
            model.load(other, false)
            await { model.playlists.value is Response.Success }
            release.complete(Unit)
            withTimeout(5_000) { old.join() }
            val data = (model.playlists.value as Response.Success).data
            assertEquals("new", data.single()?.name)
        } finally { release.complete(Unit) }
    }

    @Test fun aNestedTimeoutProducesARetryableErrorInsteadOfAnEndlessSpinner() {
        val model = keep(ArtistDiscoveryViewModel(
            readPlaylists = { withTimeout(10) { delay(60_000); emptyList() } },
            readRelated = { emptyList() },
        ))
        model.load(uri, false)
        await { model.playlists.value is Response.Error }
    }
}
