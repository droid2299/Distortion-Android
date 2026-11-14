package com.music.spotui.data.preferences

import com.music.spotui.data.api.GeniusMeaning
import com.music.spotui.data.api.LyricsApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class GeniusPrefTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val track = LyricsApi.Track("test-track", "Song", "Artist", "Album", 200_000)

    @After fun resetSetting() {
        GeniusPref.set(app, true)
        GeniusMeaning.forget()
    }

    @Test fun disabledGeniusHidesAnnotationsAndDisablesBothLookupPaths() = runBlocking {
        GeniusPref.set(app, true)
        val meaning = GeniusMeaning.Meaning("https://example.invalid", "Song", "Artist", "Meaning", emptyList())
        GeniusMeaning.current.value = track.trackId to meaning
        assertEquals(meaning, GeniusMeaning.currentFor(track.trackId))
        assertTrue(LyricsApi.sources(track).any { it.id == "genius" })

        GeniusPref.set(app, false)
        assertNull(GeniusMeaning.currentFor(track.trackId))
        assertFalse(LyricsApi.sources(track).any { it.id == "genius" })
        assertNull(GeniusMeaning.find(track.title, track.artist))
        assertNull(LyricsApi.fetchFrom(track, "genius"))

        GeniusPref.set(app, true)
        assertTrue(LyricsApi.sources(track).any { it.id == "genius" })
    }
}
