package com.music.spotui.data.engine

import app.myspoti.api.NowPlaying
import app.myspoti.api.PlayerState
import app.myspoti.api.Queue
import app.myspoti.api.Repeat
import kotlinx.coroutines.flow.StateFlow

interface DistortionPlayback {
    val state: StateFlow<PlayerState?>
    val queue: StateFlow<Queue?>
    val localMute: Boolean

    suspend fun play(
        contextUri: String,
        trackUri: String? = null,
        index: Int? = null,
        positionMs: Long? = null,
        order: List<String>? = null,
    )
    suspend fun pause()
    suspend fun pauseSlowly(fadeMs: Long)
    suspend fun resume()
    suspend fun seek(positionMs: Long)
    suspend fun next()
    suspend fun previous()
    suspend fun setShuffle(on: Boolean)
    suspend fun setRepeat(mode: Repeat)
    suspend fun addToQueue(uris: List<String>)
    suspend fun removeFromQueue(items: List<NowPlaying>)
    suspend fun clearQueue()
    suspend fun skipTo(item: NowPlaying)
    suspend fun playNext(uris: List<String>)
    suspend fun insertInQueue(uris: List<String>, before: NowPlaying)
    suspend fun moveInQueue(item: NowPlaying, target: NowPlaying, after: Boolean)
    suspend fun volume(): Double
    suspend fun setVolume(level: Double)
    suspend fun setSpeed(speed: Double)
    suspend fun setLocalMute(on: Boolean)
}
