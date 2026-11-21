package com.music.spotui.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.myspoti.api.Artist
import app.myspoti.api.Playlist
import app.myspoti.host.android.MyspotiRuntime
import com.music.spotui.data.api.Response
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ArtistDiscoveryViewModel(
    private val readPlaylists: suspend (String) -> List<Playlist?> = { MyspotiRuntime.api.catalog.discoveredOn(it) },
    private val readRelated: suspend (String) -> List<Artist> = { MyspotiRuntime.api.catalog.relatedArtists(it) },
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val playlistState = MutableStateFlow<Response<List<Playlist?>>>(Response.Loading())
    private val relatedState = MutableStateFlow<Response<List<Artist>>>(Response.Loading())
    val playlists: StateFlow<Response<List<Playlist?>>> = playlistState
    val related: StateFlow<Response<List<Artist>>> = relatedState
    private var playlistJob: Job? = null
    private var relatedJob: Job? = null
    private var currentUri: String? = null
    private var requestedRelated = false
    private var loadedAt = 0L

    @Synchronized fun load(uri: String, includeRelated: Boolean) {
        if (uri == currentUri && clock() - loadedAt in 0L until 300_000L) {
            if (includeRelated && !requestedRelated) {
                requestedRelated = true
                startRelated(uri)
            }
            return
        }
        playlistJob?.cancel()
        relatedJob?.cancel()
        currentUri = uri
        requestedRelated = includeRelated
        loadedAt = clock()
        startPlaylists(uri)
        if (includeRelated) startRelated(uri) else relatedState.value = Response.Success(emptyList())
    }

    @Synchronized fun retryPlaylists(uri: String) { if (uri == currentUri) startPlaylists(uri) }
    @Synchronized fun retryRelated(uri: String) { if (uri == currentUri && requestedRelated) startRelated(uri) }

    private fun startPlaylists(uri: String) {
        if (playlistJob?.isActive == true) return
        playlistState.value = Response.Loading()
        playlistJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = readPlaylists(uri)
                ensureActive()
                synchronized(this@ArtistDiscoveryViewModel) {
                    if (currentUri == uri) playlistState.value = Response.Success(result)
                }
            } catch (error: Exception) {
                ensureActive()
                synchronized(this@ArtistDiscoveryViewModel) {
                    if (currentUri == uri) playlistState.value = Response.Error(if (error is UnsupportedOperationException) "unsupported" else "unavailable")
                }
            }
        }
    }

    private fun startRelated(uri: String) {
        if (relatedJob?.isActive == true) return
        relatedState.value = Response.Loading()
        relatedJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = readRelated(uri)
                ensureActive()
                synchronized(this@ArtistDiscoveryViewModel) {
                    if (currentUri == uri) relatedState.value = Response.Success(result)
                }
            } catch (error: Exception) {
                ensureActive()
                synchronized(this@ArtistDiscoveryViewModel) {
                    if (currentUri == uri) relatedState.value = Response.Error(if (error is UnsupportedOperationException) "unsupported" else "unavailable")
                }
            }
        }
    }
}
