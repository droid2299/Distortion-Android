package com.music.spotui.data.local

import android.content.Context
import android.content.SharedPreferences
import com.music.spotui.data.catalogue.CatalogueEntry
import com.music.spotui.data.catalogue.CatalogueKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Device-only catalogue bookmarks. Stores metadata, never resolved streams or credentials. */
class GuestSavedLibraryStore(private val preferences: SharedPreferences) {
    private val mutex = Mutex()
    private val state = MutableStateFlow<List<CatalogueEntry>>(emptyList())
    val entries: StateFlow<List<CatalogueEntry>> = state
    private var loaded = false

    suspend fun load() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!loaded) {
                state.value = decode(preferences.getString(KEY, "[]").orEmpty())
                loaded = true
            }
        }
    }

    /** Returns false on invalid input or a failed disk write. */
    suspend fun setSaved(entry: CatalogueEntry, saved: Boolean): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!loaded) {
                state.value = decode(preferences.getString(KEY, "[]").orEmpty())
                loaded = true
            }
            if (!valid(entry)) return@withLock false
            val next = state.value.filterNot { it.uri == entry.uri }.let { if (saved) listOf(entry) + it else it }
            if (next.size > LIMIT) return@withLock false
            val written = runCatching { preferences.edit().putString(KEY, encode(next)).commit() }.getOrDefault(false)
            if (written) state.value = next
            written
        }
    }

    companion object {
        private const val KEY = "entries"
        private const val LIMIT = 5000
        private val id = Regex("[A-Za-z0-9]{22}")

        internal fun valid(entry: CatalogueEntry): Boolean {
            val parts = entry.uri.split(':')
            return parts.size == 3 && parts[0] == "spotify" &&
                parts[1] == entry.kind.name.lowercase() && id.matches(parts[2]) &&
                entry.title.isNotBlank() && entry.title.length <= 500 &&
                entry.subtitle.length <= 1000 && entry.image.length <= 2048
        }

        internal fun encode(entries: List<CatalogueEntry>): String = JSONArray().apply {
            entries.forEach { entry -> put(JSONObject().put("uri", entry.uri).put("title", entry.title)
                .put("subtitle", entry.subtitle).put("image", entry.image).put("kind", entry.kind.name)) }
        }.toString()

        internal fun decode(raw: String): List<CatalogueEntry> = runCatching {
            val array = JSONArray(raw)
            (0 until minOf(array.length(), LIMIT)).mapNotNull { index ->
                runCatching {
                    val value = array.getJSONObject(index)
                    CatalogueEntry(value.getString("uri"), value.getString("title"), value.optString("subtitle"),
                        value.optString("image"), CatalogueKind.valueOf(value.getString("kind"))).takeIf(::valid)
                }.getOrNull()
            }.distinctBy { it.uri }
        }.getOrDefault(emptyList())
    }
}

object GuestSavedLibrary {
    lateinit var store: GuestSavedLibraryStore
        private set

    fun init(context: Context) {
        if (!::store.isInitialized) store = GuestSavedLibraryStore(
            context.applicationContext.getSharedPreferences("distortion_guest_saved_library", Context.MODE_PRIVATE),
        )
    }
}
