package com.music.spotui.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.music.spotui.data.catalogue.CatalogueEntry
import com.music.spotui.data.catalogue.CatalogueKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34], application = android.app.Application::class)
class GuestSavedLibraryTest {
    private val prefs get() = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences("guest_saved_test", Context.MODE_PRIVATE)
    private fun entry(kind: CatalogueKind) = CatalogueEntry("spotify:${kind.name.lowercase()}:${"A".repeat(22)}", "Title", "Artist", "", kind)

    @Before fun reset() { prefs.edit().clear().commit() }

    @Test fun saveDeduplicatesAndPersistsAcrossInstances() = runBlocking {
        val first = GuestSavedLibraryStore(prefs)
        val track = entry(CatalogueKind.TRACK)
        assertTrue(first.setSaved(track, true))
        assertTrue(first.setSaved(track, true))
        assertTrue(first.setSaved(entry(CatalogueKind.ALBUM), true))
        val restored = GuestSavedLibraryStore(prefs)
        restored.load()
        assertEquals(listOf(entry(CatalogueKind.ALBUM), track), restored.entries.value)
        assertTrue(restored.setSaved(track, false))
        assertEquals(listOf(entry(CatalogueKind.ALBUM)), restored.entries.value)
    }

    @Test fun rejectsInvalidIdentifiersAndSkipsCorruptEntries() = runBlocking {
        val store = GuestSavedLibraryStore(prefs)
        assertFalse(store.setSaved(entry(CatalogueKind.ARTIST).copy(uri = "https://example.com/audio"), true))
        assertFalse(store.setSaved(entry(CatalogueKind.ARTIST).copy(uri = "spotify:track:${"A".repeat(22)}"), true))
        assertTrue(GuestSavedLibraryStore.decode("not json").isEmpty())
        assertTrue(store.entries.value.isEmpty())
    }
}
