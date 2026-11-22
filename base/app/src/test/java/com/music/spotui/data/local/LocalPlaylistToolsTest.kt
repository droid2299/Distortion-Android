package com.music.spotui.data.local

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34], application = android.app.Application::class)
class LocalPlaylistToolsTest {
    private fun item(key: String, uri: String) = LocalItem(key, uri, key, "", "", 0, "")

    @Test fun moveChangesOnlyTheChosenOccurrenceAndRespectsBounds() {
        val rows = listOf(item("a", "content://media/1"), item("b", "content://media/1"), item("c", "content://media/2"))
        assertEquals(listOf("b", "a", "c"), movePlaylistItem(rows, "b", -1).map { it.key })
        assertEquals(rows, movePlaylistItem(rows, "a", -1))
        assertEquals(rows, movePlaylistItem(rows, "missing", 1))
        assertEquals(rows, movePlaylistItem(rows, "b", 2))
    }

    @Test fun duplicateRemovalKeepsFirstAndUsesCatalogueIdentity() {
        val id = "A".repeat(22)
        val rows = listOf(item("a", "myspoti-ext://track/$id?title=One"), item("b", "myspoti-ext://track/$id?title=Two"),
            item("c", "content://media/1"), item("d", "content://media/1"), item("e", ""), item("f", ""))
        assertEquals(listOf("a", "c", "e", "f"), distinctPlaylistItems(rows).map { it.key })
    }
}
