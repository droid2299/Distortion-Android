package com.music.spotui.data.local

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Bounded, device-only guest history. Never stores resolved streaming URLs or account data. */
object GuestListening {
    private const val LIMIT = 50
    private val lock = Any()
    private var preferences: android.content.SharedPreferences? = null
    private val mutableRecent = MutableStateFlow<List<LocalTrack>>(emptyList())
    val recent: StateFlow<List<LocalTrack>> = mutableRecent

    fun init(context: Context) = synchronized(lock) {
        if (preferences != null) return@synchronized
        val prefs = context.applicationContext.getSharedPreferences("distortion_guest_history", Context.MODE_PRIVATE)
        preferences = prefs
        mutableRecent.value = decode(prefs.getString("recent", "[]").orEmpty())
    }

    fun record(track: LocalTrack) = synchronized(lock) {
        if (track.isRadio || track.title.isBlank() || !allowed(track.uri)) return@synchronized
        val tracks = (listOf(track) + mutableRecent.value.filterNot { identity(it.uri) == identity(track.uri) }).take(LIMIT)
        mutableRecent.value = tracks
        preferences?.edit()?.putString("recent", encode(tracks))?.apply()
    }

    internal fun allowed(uri: Uri): Boolean = when (uri.scheme) {
        "content", "file" -> true
        "myspoti-ext" -> uri.authority == "track" && uri.lastPathSegment.orEmpty().matches(Regex("[A-Za-z0-9]{22}"))
        else -> false
    }

    private fun identity(uri: Uri): String = if (uri.scheme == "myspoti-ext") "track:${uri.lastPathSegment}" else uri.toString()

    internal fun encode(tracks: List<LocalTrack>): String = JSONArray().apply {
        tracks.take(LIMIT).forEach { t -> put(JSONObject().put("id", t.id).put("uri", t.uri.toString())
            .put("title", t.title).put("artist", t.artist).put("album", t.album)
            .put("duration", t.durationMs).put("art", t.artUri.toString())) }
    }.toString()

    internal fun decode(text: String): List<LocalTrack> = runCatching {
        val array = JSONArray(text)
        (0 until minOf(array.length(), LIMIT)).mapNotNull { i -> runCatching {
            val o = array.getJSONObject(i)
            val uri = Uri.parse(o.getString("uri"))
            if (!allowed(uri)) return@runCatching null
            LocalTrack(o.getLong("id"), uri, o.getString("title"), o.optString("artist"),
                o.optString("album"), o.optLong("duration"), Uri.parse(o.optString("art")))
        }.getOrNull() }
    }.getOrDefault(emptyList())
}
