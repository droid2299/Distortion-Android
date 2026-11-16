package com.music.spotui.di

import android.os.SystemClock
import android.widget.Toast
import app.myspoti.engine.R
import androidx.compose.runtime.mutableStateOf
import androidx.media3.common.Player
import com.music.spotui.cast.CastPlayback
import com.music.spotui.data.local.LocalTrack
import com.music.spotui.data.local.SetCurve
import com.music.spotui.data.local.SetTrack
import com.music.spotui.data.local.planAdaptiveSet
import com.music.spotui.data.offline.OfflineStore
import com.music.spotui.extensions.ExtensionAudio
import com.music.spotui.extensions.Extensions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal object AdaptiveSetSession {
    data class Status(val targetMs: Long, val projectedMs: Long, val remainingMs: Long, val closerUnavailable: Boolean)

    private class Run(
        val targetMs: Long,
        val curve: SetCurve,
        val candidates: List<SetTrack<LocalTrack>>,
        val closerId: String?,
        val offlineOnly: Boolean,
    ) {
        val generation = SessionActions.generation
        val keys = candidates.associate { it.track.uri to it.id }
        fun key(track: LocalTrack) = keys[track.uri] ?: track.uri.toString()
        val heard = mutableSetOf<String>()
        var playing = false
        var stamp = SystemClock.elapsedRealtime()
        var elapsed = 0L
        var online = !offlineOnly && streamingAvailable()
        var rate = LocalPlayer.rate()
        val changed = Channel<Unit>(Channel.CONFLATED)
        var revision = 0
        fun requestPlan() {
            revision++
            changed.trySend(Unit)
        }
        var skipRequested = false
        var applied = 0
        var failed = false
        var consecutiveFailures = 0
        var job: Job? = null

        fun sample(nextPlaying: Boolean) {
            val now = SystemClock.elapsedRealtime()
            if (playing) elapsed += now - stamp
            stamp = now
            playing = nextPlaying
        }
    }

    private var run: Run? = null
    private val _status = mutableStateOf<Status?>(null)
    val status: androidx.compose.runtime.State<Status?> get() = _status

    fun streamingAvailable(): Boolean = Extensions.hasAudioSource() && OfflineStore.isOnline(Graph.app)

    fun available(candidate: SetTrack<LocalTrack>, online: Boolean): Boolean {
        if (!candidate.id.startsWith("spotify:track:")) return true
        val id = candidate.id.substringAfterLast(':')
        if (com.music.spotui.data.preferences.HiddenSongs.isHidden(id)) return false
        if (candidate.track.uri.scheme == ExtensionAudio.SCHEME) return online
        val download = OfflineStore.tracks[id] ?: return false
        return OfflineStore.audioFile(download).path == candidate.track.uri.path
    }

    val active: Boolean get() = run?.let(::ownsPlayback) == true

    fun start(
        title: String,
        targetMs: Long,
        curve: SetCurve,
        candidates: List<SetTrack<LocalTrack>>,
        closerId: String?,
        offlineOnly: Boolean,
        initial: List<SetTrack<LocalTrack>>,
    ): Boolean {
        if (initial.isEmpty() || CastPlayback.active) return false
        val online = !offlineOnly && streamingAvailable()
        if (initial.any { !available(it, online) }) return false
        stop()
        if (SongPlayer.repeatMode.value != LocalRepeat.OFF) SongPlayer.setRepeatMode(LocalRepeat.OFF)
        val session = Run(targetMs, curve, candidates, closerId, offlineOnly)
        run = session
        LocalPlayer.play(initial.map { it.track }, 0, context = title, adaptiveSet = true)
        LocalPlayer.current()?.let { session.heard += session.key(it) }
        updateStatus(session)
        session.job = CoroutineScope(Dispatchers.Main).launch {
            while (run === session) {
                if (!ownsPlayback(session) || CastPlayback.active) {
                    stop()
                    break
                }
                session.sample(LocalPlayer.isPlaying())
                if (session.playing && LocalPlayer.position() > 1_000) session.consecutiveFailures = 0
                val onlineNow = !session.offlineOnly && streamingAvailable()
                val rateNow = LocalPlayer.rate()
                if (onlineNow != session.online || rateNow != session.rate) {
                    session.online = onlineNow
                    session.rate = rateNow
                    session.requestPlan()
                }
                if (session.revision != session.applied) replan(session)
                if (run === session) updateStatus(session)
                withTimeoutOrNull(1_000) { session.changed.receive() }
            }
        }
        return true
    }

    private fun ownsPlayback(session: Run) = LocalPlayer.active.value && LocalPlayer.adaptiveQueue &&
        session.generation == SessionActions.generation

    fun playingChanged(playing: Boolean) {
        run?.takeIf(::ownsPlayback)?.sample(playing)
    }

    fun transitioned(track: LocalTrack, reason: Int) {
        val session = run?.takeIf(::ownsPlayback) ?: return
        val key = session.key(track)
        session.heard += key
        session.failed = false
        session.skipRequested = false
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK || session.revision != session.applied) session.requestPlan()
    }

    fun next(): Boolean {
        val session = run?.takeIf(::ownsPlayback) ?: return false
        val next = LocalPlayer.upcoming().firstOrNull()?.second ?: return false
        if (session.key(next) != session.closerId) return false
        session.skipRequested = true
        session.requestPlan()
        return true
    }

    fun failed(track: LocalTrack): Boolean {
        val session = run?.takeIf(::ownsPlayback) ?: return false
        session.heard += session.key(track)
        session.sample(false)
        if (!session.failed) session.consecutiveFailures++
        session.failed = true
        session.requestPlan()
        return true
    }

    fun invalidate(): Boolean {
        val session = run?.takeIf(::ownsPlayback) ?: return false
        session.requestPlan()
        return true
    }

    fun ended() {
        val session = run?.takeIf(::ownsPlayback) ?: return
        if (session.closerId != null && session.closerId !in session.heard) {
            notify(R.string.app_set_closer_unavailable)
        }
        stop()
    }

    private fun notify(message: Int) {
        Toast.makeText(Graph.app, message, Toast.LENGTH_LONG).show()
    }

    fun stop() {
        run?.job?.cancel()
        run = null
        _status.value = null
    }

    private suspend fun replan(session: Run) {
        val revision = session.revision
        val current = LocalPlayer.current() ?: return
        val currentId = session.key(current)
        val skip = session.skipRequested
        val rate = LocalPlayer.rate().toDouble()
        val currentMediaMs = if (skip) 0 else currentRemaining(session, current)
        val remaining = ((session.targetMs - session.elapsed) * rate).toLong() - currentMediaMs
        val options = session.candidates.filter { it.id !in session.heard && available(it, session.online) }
        val closer = options.firstOrNull { it.id == session.closerId }
        val keepFilling = currentId != session.closerId && session.closerId !in session.heard
        val exhausted = session.consecutiveFailures >= 4
        val next = withContext(Dispatchers.Default) {
            if (exhausted) emptyList()
            else planAdaptiveSet(options, remaining, session.curve, closer, fill = keepFilling)
        }
        if (run !== session || !ownsPlayback(session) || revision != session.revision || LocalPlayer.current() !== current) return
        if (!LocalPlayer.replaceAdaptiveTail(next.map { it.track })) return
        session.applied = revision
        session.skipRequested = false
        if (skip && next.isNotEmpty() && !session.failed) LocalPlayer.advanceAdaptive()
        if (session.failed) {
            if (next.isEmpty()) {
                LocalPlayer.pause()
                val message = if (currentId == session.closerId) R.string.app_set_closer_unavailable else R.string.app_set_unavailable
                notify(message)
                stop()
            } else LocalPlayer.advanceAdaptive(prepare = true)
        }
    }

    private fun currentRemaining(session: Run, track: LocalTrack): Long {
        if (session.failed) return 0
        val duration = LocalPlayer.duration().takeIf { it > 0 } ?: track.durationMs
        return (duration - LocalPlayer.position()).coerceAtLeast(0)
    }

    private fun updateStatus(session: Run) {
        val current = LocalPlayer.current() ?: return
        val queue = LocalPlayer.upcoming()
        val mediaLeft = currentRemaining(session, current) + queue.sumOf { it.second.durationMs }
        val remaining = (mediaLeft / LocalPlayer.rate().toDouble()).toLong()
        _status.value = Status(
            session.targetMs, session.elapsed + remaining, remaining,
            session.closerId != null && session.closerId !in session.heard &&
                queue.none { session.key(it.second) == session.closerId },
        )
    }
}
