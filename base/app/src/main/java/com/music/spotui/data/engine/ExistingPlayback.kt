package com.music.spotui.data.engine

import app.myspoti.api.NowPlaying
import app.myspoti.api.Player
import app.myspoti.api.Repeat

internal class ExistingPlayback(private val current: () -> Player) : DistortionPlayback {
    override val state get() = current().state
    override val queue get() = current().queue
    override val localMute get() = current().localMute

    override suspend fun play(
        contextUri: String,
        trackUri: String?,
        index: Int?,
        positionMs: Long?,
        order: List<String>?,
    ) = current().play(contextUri, trackUri, index, positionMs, order)

    override suspend fun pause() = current().pause()
    override suspend fun pauseSlowly(fadeMs: Long) = current().pauseSlowly(fadeMs)
    override suspend fun resume() = current().resume()
    override suspend fun seek(positionMs: Long) = current().seek(positionMs)
    override suspend fun next() = current().next()
    override suspend fun previous() = current().previous()
    override suspend fun setShuffle(on: Boolean) = current().setShuffle(on)
    override suspend fun setRepeat(mode: Repeat) = current().setRepeat(mode)
    override suspend fun addToQueue(uris: List<String>) = current().addToQueue(uris)
    override suspend fun removeFromQueue(items: List<NowPlaying>) = current().removeFromQueue(items)
    override suspend fun clearQueue() = current().clearQueue()
    override suspend fun skipTo(item: NowPlaying) = current().skipTo(item)
    override suspend fun playNext(uris: List<String>) = current().playNext(uris)
    override suspend fun insertInQueue(uris: List<String>, before: NowPlaying) = current().insertInQueue(uris, before)
    override suspend fun moveInQueue(item: NowPlaying, target: NowPlaying, after: Boolean) = current().moveInQueue(item, target, after)
    override suspend fun volume() = current().volume()
    override suspend fun setVolume(level: Double) = current().setVolume(level)
    override suspend fun setSpeed(speed: Double) = current().setSpeed(speed)
    override suspend fun setLocalMute(on: Boolean) = current().setLocalMute(on)
}
