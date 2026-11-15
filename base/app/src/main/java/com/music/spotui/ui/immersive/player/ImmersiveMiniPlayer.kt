/*
 * Adapted from BitChord (https://github.com/kushagrasinghx/BitChord), GPL-3.0-or-later,
 * app/src/main/java/com/music/bitchord/ui/components/MiniPlayer.kt at edc1e6f.
 * Modified for Myspoti, 2026-10.
 *
 * Changes: the 56dp capsule (radius 28) with cover, credits, play/pause and next, a swipe along
 * it to change track and a pull up to open the player. The dock and the
 * loading/locked states of a shared session are left out; the capsule is the Immersive glass
 * (immersiveGlassSurface: Liquid Glass or frost).
 */
package com.music.spotui.ui.immersive.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.myspoti.engine.R
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.music.spotui.di.Haptics
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.components.titleMarquee
import com.music.spotui.ui.immersive.Immersive
import com.music.spotui.ui.navigation.PlayerSheet
import com.music.spotui.ui.viewmodel.PlayerViewModel
import kotlin.math.abs

private val GLYPH_SLOT = 48.dp
private val GLYPH_SIZE = 28.dp
internal val MiniArtworkCorner = 6.dp
internal val MiniPlayerCorner = 24.dp
private val MINI_HEIGHT = 60.dp

/** How far the capsule is swiped along to change track. */
private val TRACK_SWIPE_THRESHOLD = 72.dp

/**
 * The floating capsule over the pages: cover, title and artist, play/pause and next. A tap (or a
 * pull up) opens the player ([PlayerSheet.show]); a swipe along it changes track. Nothing is drawn
 * while nothing is playing. Give it the horizontal margin and the room above the tab bar through
 * [modifier]; it fills the width it is given.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ImmersiveMiniPlayer(modifier: Modifier = Modifier) {
    val vm: PlayerViewModel = viewModel()
    // A crossfade: the words stay the outgoing song's until half way (see SongBlend).
    val blend = com.music.spotui.ui.components.rememberSongBlend()
    val outgoing = blend?.takeIf { it.outgoingWords }?.song
    val title = outgoing?.title ?: vm.currentSongTitle.value
    if (title.isBlank()) return
    val singer = outgoing?.artist ?: vm.currentSongSinger.value
    val cover = vm.currentSongCoverUri.value
    val playing = vm.currentSongPlayingState.value
    val adPlaying = SongPlayer.adPlaying
    val buffering = SongPlayer.isBuffering
    val shape = RoundedCornerShape(MiniPlayerCorner)
    val capsuleSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MINI_HEIGHT)
            // Where the Classic sheet rises from; kept up to date for the pages that read it.
            .onGloballyPositioned {
                val bounds = it.boundsInWindow()
                PlayerSheet.miniTop = bounds.top
                PlayerSheet.miniBounds = bounds
            }
            .miniPlayerGestures()
            .background(Immersive.scheme.secondaryContainer, shape)
            .clip(shape)
            .clickable(interactionSource = capsuleSource, indication = null) { PlayerSheet.show() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DisposableEffect(Unit) {
            onDispose {
                PlayerSheet.miniBounds = null
                PlayerSheet.miniArtworkBounds = null
            }
        }
        Box(Modifier.size(44.dp).onGloballyPositioned {
            PlayerSheet.miniArtworkBounds = it.boundsInWindow()
        }.clip(RoundedCornerShape(MiniArtworkCorner)).background(Color.Black.copy(alpha = 0.18f))) {
            GlideImage(
                model = cover.takeIf { it.isNotBlank() },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                transition = if (app.myspoti.power.LocalLightweightUi.current) null else CrossFade,
                loading = placeholder(R.drawable.placeholder),
                failure = placeholder(R.drawable.placeholder),
                modifier = Modifier.size(44.dp),
            )
            // The outgoing cover dissolving into this one.
            com.music.spotui.ui.components.SongBlendCover(blend, Modifier.size(44.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f).graphicsLayer { alpha = blend?.wordsAlpha() ?: 1f }) {
            Text(
                text = if (adPlaying) stringResource(R.string.immersive_player_ad) else title,
                // Title and artist set close, as the official mini player: the default line heights left a gap.
                style = Immersive.BodyStrong.copy(color = Immersive.scheme.onSecondaryContainer, fontSize = 15.sp, lineHeight = 19.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // Once per song then rests: the mini player is always on screen.
                modifier = Modifier.titleMarquee(iterations = 1),
            )
            if (!adPlaying) {
                Text(
                    text = singer, style = Immersive.Subhead.copy(color = Immersive.scheme.onSecondaryContainer), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.titleMarquee(iterations = 1),
                )
            }
        }
        val iconColors = IconButtonDefaults.iconButtonColors(contentColor = Immersive.scheme.onSecondaryContainer)
        com.music.spotui.ui.components.LoadedFade(buffering) { loading ->
            if (loading) {
                Box(Modifier.size(GLYPH_SLOT), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Immersive.scheme.onSecondaryContainer, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                }
            } else {
                IconButton(
                    onClick = {
                        Haptics.click()
                        if (playing) SongPlayer.pause() else SongPlayer.play()
                        vm.updateSongState(cover, vm.currentSongTitle.value, vm.currentSongSinger.value, !playing, vm.currentSongId.value, vm.currentSongIndex.value, vm.currentSongAlbum.value)
                    },
                    modifier = Modifier.size(GLYPH_SLOT),
                    colors = iconColors,
                ) {
                    com.music.spotui.ui.components.GlyphSwap(playing) { on ->
                        Icon(
                            imageVector = if (on) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(if (on) R.string.immersive_player_pause else R.string.immersive_player_play),
                            modifier = Modifier.size(GLYPH_SIZE),
                        )
                    }
                }
            }
        }
        IconButton(
            onClick = {
                Haptics.click()
                SongPlayer.swipeNext()
            },
            modifier = Modifier.size(GLYPH_SLOT),
            colors = iconColors,
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = stringResource(R.string.immersive_player_next),
                modifier = Modifier.size(GLYPH_SIZE),
            )
        }
    }
}


/**
 * A swipe along the capsule changes track (past [TRACK_SWIPE_THRESHOLD]); a pull up opens the
 * player. A tap is left to the capsule's own click.
 */
private fun Modifier.miniPlayerGestures(): Modifier = pointerInput(Unit) {
    val threshold = TRACK_SWIPE_THRESHOLD.toPx()
    val expandDistance = 72.dp.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val slopped = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() } ?: return@awaitEachGesture
        val moved = slopped.position - down.position
        if (abs(moved.y) > abs(moved.x)) {
            if (moved.y >= 0f || PlayerSheet.open.value) return@awaitEachGesture
            val velocity = VelocityTracker()
            velocity.addPointerInputChange(down)
            velocity.addPointerInputChange(slopped)
            var distance = -moved.y
            var expand = false
            PlayerSheet.pullFromMini(distance)
            try {
                val finished = drag(slopped.id) { change ->
                    velocity.addPointerInputChange(change)
                    distance -= change.positionChange().y
                    PlayerSheet.pullFromMini(distance)
                    change.consume()
                }
                val speed = velocity.calculateVelocity().y
                expand = finished && speed < PlayerFlingVelocity &&
                    (distance >= expandDistance || speed <= -PlayerFlingVelocity)
            } finally {
                PlayerSheet.releaseMiniPull(expand)
            }
            return@awaitEachGesture
        }
        var dragged = moved.x
        val finished = drag(slopped.id) { change ->
            dragged += change.positionChange().x
            change.consume()
        }
        if (!finished) return@awaitEachGesture
        when {
            dragged <= -threshold -> SongPlayer.swipeNext()
            dragged >= threshold -> SongPlayer.previous()
        }
    }
}
