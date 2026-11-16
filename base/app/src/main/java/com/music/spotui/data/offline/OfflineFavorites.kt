package com.music.spotui.data.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import androidx.compose.runtime.mutableIntStateOf
import app.myspoti.host.android.MyspotiRuntime
import com.music.spotui.data.api.ProfileCache
import com.music.spotui.data.local.ListenHistory
import com.music.spotui.data.local.rankedSpotifyIds
import com.music.spotui.data.preferences.HiddenSongs
import com.music.spotui.data.preferences.isDownloadsEnabled
import com.music.spotui.di.Graph
import com.music.spotui.di.SessionActions
import com.music.spotui.extensions.Extensions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull

internal object OfflineFavorites {
    const val MIB = 1024L * 1024
    const val PER_TRACK_BYTES = 24 * MIB
    const val MAX_TRACKS = 20
    private const val REFRESH_MS = 6 * 60 * 60_000L
    private const val RECENT_MS = 30 * 24 * 60 * 60_000L
    private val limits = setOf(0, 128, 256, 512)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshLock = Mutex()
    val revision = mutableIntStateOf(0)
    private var lastRun: Pair<String, Long>? = null

    private fun prefs() = Graph.app.getSharedPreferences("offline_favorites", Context.MODE_PRIVATE)
    private fun key() = ProfileCache.id?.let(OfflineAccounts::keyOf)
    fun limitMb(): Int = key()?.let { prefs().getInt(it, 0) }?.takeIf { it in limits } ?: 0

    fun setLimitMb(value: Int) {
        val owner = key() ?: return
        require(value in limits)
        prefs().edit().putInt(owner, value).apply()
        revision.intValue++
        if (value != 0) refresh(force = true)
    }

    fun canRun(owner: String): Boolean {
        if (owner != key() || limitMb() == 0 || !isDownloadsEnabled(Graph.app)) return false
        val cm = Graph.app.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun refresh(force: Boolean = false) {
        val owner = key() ?: return
        if (limitMb() == 0) return
        val session = SessionActions.generation
        scope.launch {
            if (!refreshLock.tryLock()) return@launch
            try {
                if (!canRun(owner) || !MyspotiRuntime.api.ready.value || !Extensions.hasDownloadSource()) return@launch
                if (!OfflineAccounts.isOwnedBy(Graph.app, owner)) return@launch
                val at = SystemClock.elapsedRealtime()
                if (!force && lastRun?.let { it.first == owner && at - it.second < REFRESH_MS } == true) return@launch
                lastRun = owner to at
                val since = System.currentTimeMillis() - RECENT_MS
                val ids = rankedSpotifyIds(ListenHistory.all().filter { it.atMs >= since }, 60)
                val candidates = ids.filterNot { OfflineStore.isDownloaded(it) || HiddenSongs.isHidden(it) || OfflineStore.status[it] != null }.take(4)
                val songs = withTimeoutOrNull(25_000) {
                    candidates.mapNotNull { id ->
                        if (session != SessionActions.generation || !canRun(owner)) return@withTimeoutOrNull emptyList()
                        try {
                            Graph.api.songOf(MyspotiRuntime.api.catalog.track("spotify:track:$id")).takeIf { it.playable }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            null
                        }
                    }
                }.orEmpty()
                if (session == SessionActions.generation && canRun(owner) && OfflineAccounts.isOwnedBy(Graph.app, owner)) {
                    OfflineDownloader.enqueueFavorites(songs, owner)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                android.util.Log.w("OfflineFavorites", "Could not refresh offline favorites", e)
            } finally {
                refreshLock.unlock()
            }
        }
    }
}

internal fun favoriteReservations(
    limitBytes: Long,
    occupiedBytes: Long,
    occupiedSlots: Int,
    candidateCount: Int,
    usableBytes: Long,
): List<Long> {
    val remaining = minOf(limitBytes - occupiedBytes, usableBytes - 64 * OfflineFavorites.MIB)
    val slots = (OfflineFavorites.MAX_TRACKS - occupiedSlots).coerceAtLeast(0)
    val count = minOf(candidateCount, slots, (remaining.coerceAtLeast(0) / OfflineFavorites.PER_TRACK_BYTES).toInt())
    return List(count) { OfflineFavorites.PER_TRACK_BYTES }
}
