package com.music.spotui.data.api

import app.myspoti.api.PlaylistAccess
import app.myspoti.host.android.MyspotiRuntime
import com.music.spotui.di.SessionActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Keeps the created playlist if adding tracks needs a retry. */
internal class SpotifyPlaylistDraft(private val session: Long = SessionActions.generation) {
    private var uri: String? = null
    private var privateConfirmed = false

    suspend fun save(name: String, ids: List<String>, makePrivate: Boolean = false): String = withContext(Dispatchers.IO) {
        require(ids.isNotEmpty() && ids.size <= 50)
        checkSession()
        check(MyspotiRuntime.api.ready.value)
        val playlists = MyspotiRuntime.api.playlists
        val existing = uri
        val destination = existing ?: playlists.create(name.trim()).also {
            uri = it
            Api.HomeCache.libraryRoot = null
            LibraryEdits.reloadSoon()
        }
        checkSession()
        if (makePrivate && !privateConfirmed) {
            playlists.setAccess(destination, PlaylistAccess.PRIVATE)
            privateConfirmed = true
        }
        checkSession()
        val current = if (existing == null) emptySet() else playlists.tracks(destination, 0, 100).items.mapTo(hashSetOf()) { it.uri }
        val missing = ids.distinct().map { "spotify:track:$it" }.filterNot(current::contains)
        checkSession()
        if (missing.isNotEmpty()) playlists.addTracks(destination, missing)
        checkSession()
        Api.HomeCache.invalidatePlaylist(destination.substringAfterLast(':'))
        destination
    }

    private fun checkSession() = check(session == SessionActions.generation)
}
