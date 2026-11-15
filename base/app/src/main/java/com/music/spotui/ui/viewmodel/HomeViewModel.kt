package com.music.spotui.ui.viewmodel

import android.os.SystemClock
import com.music.spotui.di.Graph
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.spotui.data.api.Response
import com.music.spotui.data.entity.HomeFeedModel
import com.music.spotui.ui.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel constructor(private val repository: AppRepository = Graph.repository)  : ViewModel() {

    private val _home : MutableStateFlow<Response<HomeFeedModel>> = MutableStateFlow(Response.Loading())
    val home : StateFlow<Response<HomeFeedModel>> = _home

    /** Selected home chip ("" = All). */
    val facet = androidx.compose.runtime.mutableStateOf("")
    private var homeJob: kotlinx.coroutines.Job? = null
    /** When the feed was last asked for. */
    private var fetchedAt = 0L

    init {
        fetchHome()
        // A Home that failed (player still signing in, network back) loads again by itself:
        // when the player is ready again, then a few times with growing pauses.
        viewModelScope.launch {
            app.myspoti.host.android.MyspotiRuntime.api.ready.collect { ready ->
                if (ready && _home.value is Response.Error && homeJob?.isActive != true) fetchHome()
            }
        }
    }

    /** Tries again now (the Home error's button). */
    fun retry() {
        fetchHome(reset = true)
    }

    /**
     * Home shown again (back on its tab, the app reopened): read again once it is older than
     * [STALE_MS], behind the feed shown. It was read once per launch, and an app kept open for
     * days showed the same releases and picks (reported: nothing new to discover).
     */
    fun refreshIfStale() {
        if (SystemClock.elapsedRealtime() - fetchedAt >= STALE_MS && homeJob?.isActive != true) fetchHome()
    }

    /**
     * The chip [f] tapped: it is picked, or unpicked when it already was: a sub-chip (Following) goes
     * back to its parent (Music), a top chip back to everything.
     */
    fun selectFacet(f: String) {
        facet.value = if (facet.value != f) f
            else com.music.spotui.data.entity.HomeSubChips.parentOf(f)?.takeIf { it != f } ?: ""
        fetchHome(reset = true)
    }

    private companion object {
        val RETRY_DELAYS_MS = longArrayOf(2_000, 5_000, 15_000, 30_000)
        const val STALE_MS = 30 * 60_000L
    }

    private fun fetchHome(reset: Boolean = false) {
        homeJob?.cancel()
        val selectedFacet = facet.value
        if (reset) _home.value = Response.Loading()
        fetchedAt = SystemClock.elapsedRealtime()
        homeJob = viewModelScope.launch {
            retryHomeFeed(RETRY_DELAYS_MS) { repository.provideHomeFeed(selectedFacet) }
                .flowOn(Dispatchers.IO)
                .collect { feed ->
                    if (feed !is Response.Loading || _home.value !is Response.Success) {
                        _home.value = feed
                    }
                }
        }
    }
}

/** A failed attempt gets its own retry even when the UI already shows the same error. */
internal fun <T> retryHomeFeed(
    delaysMs: LongArray,
    request: suspend () -> Flow<Response<T>>,
): Flow<Response<T>> = flow {
    for (attempt in 0..delaysMs.size) {
        var failed = false
        request().collect { result ->
            failed = result is Response.Error
            if (attempt == 0 || result !is Response.Loading) emit(result)
        }
        if (!failed || attempt == delaysMs.size) break
        delay(delaysMs[attempt])
    }
}
