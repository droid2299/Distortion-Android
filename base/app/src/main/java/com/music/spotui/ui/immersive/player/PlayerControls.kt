/*
 * Adapted from BitChord (https://github.com/kushagrasinghx/BitChord), GPL-3.0-or-later,
 * sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/PlayerControls.kt and
 * sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/theme/ArtworkPalette.kt at edc1e6f.
 * Modified for Myspoti, 2026-10.
 *
 * Changes: the scrubber, transport, volume row, round glyph discs and the bottom row. Quality
 * badges, Automix, lossless, the output/party capsule and the autoplay toggle are left out;
 * the glyphs are Material Icons Rounded; the pale player tint is read from the cover colours
 * the mesh backdrop already extracted, and the part of the palette code made of colours
 * published by a streaming service is dropped.
 */
package com.music.spotui.ui.immersive.player

import com.music.spotui.ui.theme.AppSpacing
import com.music.spotui.ui.components.zoneOn
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.material3.ripple
import com.music.spotui.ui.immersive.components.expressivePress
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.myspoti.engine.R
import app.myspoti.power.EnergySettings
import com.music.spotui.di.Haptics
import com.music.spotui.ui.immersive.Immersive
import com.music.spotui.ui.look.rememberReduceMotion
import com.music.spotui.ui.components.drawMixZone
import com.music.spotui.ui.components.RepeatIcons
import com.music.spotui.ui.components.rememberFrameClock

internal val LocalPlayerTint = compositionLocalOf { Color.White }

/** m:ss. */
internal fun formatTime(ms: Long): String {
    val total = (ms.coerceAtLeast(0L) / 1000L).toInt()
    return "%d:%02d".format(total / 60, total % 60)
}

@Composable
internal fun PlayerScrubber(
    state: ImmersivePlayerState,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    expressive: Boolean = false,
) {
    val scrub = rememberPlayerScrub(state.title, state.positionMs, state.durationMs)
    val frame = rememberFrameClock(state.playing && !state.buffering, fps = 15)
    Column(modifier.fillMaxWidth().graphicsLayer { alpha = state.blend?.let { 0.3f + 0.7f * it.wordsAlpha() } ?: 1f }) {
        PlaybackSlider(state, scrub, frame, onSeek, expressive)
        ScrubTimes(frame, scrub, state, expressive)
        com.music.spotui.ui.components.PlaybackSpeedNote(
            LocalPlayerTint.current.copy(alpha = 0.8f),
            Immersive.Accent,
            Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LyricsPlaybackControls(state: ImmersivePlayerState, actions: ImmersivePlayerActions) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val corner by animateDpAsState(
        if (state.playing) 30.dp else 58.dp,
        animationSpec = if (rememberReduceMotion()) snap() else MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "lyricsPlayPauseCorners",
    )
    val scrub = rememberPlayerScrub(state.title, state.positionMs, state.durationMs)
    val frame = rememberFrameClock(state.playing && !state.buffering, fps = 15)
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Gutter),
    ) {
        FilledIconButton(
            onClick = actions.playPause,
            enabled = !state.buffering,
            modifier = Modifier.size(116.dp).expressivePress(interaction, enabled = !state.buffering),
            interactionSource = interaction,
            shape = RoundedCornerShape(corner.coerceAtLeast(0.dp)),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = scheme.tertiaryFixed,
                contentColor = scheme.onTertiaryFixed,
            ),
        ) {
            if (state.buffering) {
                CircularProgressIndicator(Modifier.size(32.dp), color = scheme.onPrimaryContainer)
            } else {
                Icon(
                    if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    stringResource(if (state.playing) R.string.immersive_player_pause else R.string.immersive_player_play),
                    Modifier.size(40.dp),
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().height(62.dp)
                .background(scheme.surfaceContainerLowest, CircleShape)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LyricsElapsed(state, scrub, frame)
            Box(Modifier.weight(1f)) {
                PlaybackSlider(state, scrub, frame, actions.seekTo, expressive = true, waveColor = scheme.primaryFixed)
            }
            Text(
                formatTime(state.durationMs).padStart(5, '0'),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurface,
            )
        }
    }
}

@Composable
private fun LyricsElapsed(state: ImmersivePlayerState, scrub: PlayerScrub, frame: State<Long>) {
    frame.value
    val elapsed = (scrub.shown(state.positionMs(), state.durationMs) * state.durationMs).toLong()
    Text(
        formatTime(elapsed).padStart(5, '0'),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlaybackSlider(
    state: ImmersivePlayerState,
    scrub: PlayerScrub,
    frame: State<Long>,
    onSeek: (Long) -> Unit,
    expressive: Boolean,
    waveColor: Color = Color.Unspecified,
) {
    frame.value
    val position = scrub.shown(state.positionMs(), state.durationMs)
    val slider = rememberSliderState(value = position)
    SideEffect { slider.value = position }
    val seeking = scrub.scrubbing
    val energy by EnergySettings.state.collectAsStateWithLifecycle()
    val reduceMotion = rememberReduceMotion()
    val animateWave = !reduceMotion && !energy.manualSaver
    val loading = state.buffering && !seeking
    val mixCue = state.mixCue
    val mixColor = MaterialTheme.colorScheme.tertiary
    val interactionSource = remember { MutableInteractionSource() }
    val enabled = state.durationMs > 0
    val accent = when {
        waveColor != Color.Unspecified -> waveColor
        expressive -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.primary
    }
    val inactive = if (expressive) accent.copy(alpha = 0.26f) else MaterialTheme.colorScheme.secondaryContainer
    val sliderColors = SliderDefaults.colors(thumbColor = accent)
    val expressiveStroke = with(LocalDensity.current) { Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round) }
    val seekDescription = stringResource(R.string.playback_seek_description)
    Slider(
        state = slider,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = seekDescription },
        enabled = enabled,
        interactionSource = interactionSource,
        thumb = {
            if (expressive) {
                Box(Modifier.size(18.dp).background(accent.copy(alpha = if (enabled) 1f else 0.38f), CircleShape))
            } else {
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    colors = sliderColors,
                    enabled = enabled,
                )
            }
        },
        onValueChange = {
            slider.value = it
            scrub.drag(it)
        },
        onValueChangeFinished = {
            if (scrub.scrubbing) {
                scrub.release { onSeek((it * (state.seekLengthMs?.invoke() ?: state.durationMs)).toLong()) }
            }
        },
        track = { trackState ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val decoration = Modifier.fillMaxWidth().clearAndSetSemantics {}
                if (loading) {
                    LinearWavyProgressIndicator(
                        modifier = decoration,
                        color = accent,
                        trackColor = inactive,
                        waveSpeed = if (animateWave) 24.dp else 0.dp,
                    )
                } else {
                    LinearWavyProgressIndicator(
                        progress = { trackState.value },
                        modifier = decoration,
                        color = accent,
                        trackColor = inactive,
                        stroke = if (expressive) expressiveStroke else WavyProgressIndicatorDefaults.linearIndicatorStroke,
                        trackStroke = if (expressive) expressiveStroke else WavyProgressIndicatorDefaults.linearTrackStroke,
                        stopSize = if (expressive) 0.dp else 4.dp,
                        gapSize = if (expressive) 0.dp else 4.dp,
                        amplitude = { 1f },
                        wavelength = if (expressive) 30.dp else 24.dp,
                        waveSpeed = if (animateWave && state.playing && !seeking) 24.dp else 0.dp,
                    )
                }
                if (mixCue != null) {
                    Canvas(Modifier.matchParentSize()) {
                        val zone = mixCue.zoneOn(state.durationMs, incomingShown = state.blend?.outgoingWords == false)
                        if (zone != null) {
                            val direction = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
                            scale(scaleX = direction, scaleY = 1f) {
                                drawMixZone(zone, trackState.value, size.height - 1.dp.toPx(), 2.dp.toPx(), mixColor)
                            }
                        }
                    }
                }
            }
        },
    )
}

/** The elapsed and remaining times under the seek bar, read per tick of [frame]. */
@Composable
private fun ScrubTimes(frame: State<Long>, scrub: PlayerScrub, state: ImmersivePlayerState, expressive: Boolean) {
    frame.value
    val shown = scrub.shown(state.positionMs(), state.durationMs)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // The slider's touch target extends well past the drawn bar: pull the labels back up under it.
            .offset(y = if (expressive) (-6).dp else (-9).dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val duration = state.durationMs.coerceAtLeast(0L)
        val elapsed = (shown * duration).toLong()
        Text(
            text = formatTime(elapsed),
            style = Immersive.Caption,
            color = LocalPlayerTint.current.copy(alpha = 0.78f),
        )
        Text(
            text = if (expressive) formatTime(duration) else "-" + formatTime(duration - elapsed),
            style = Immersive.Caption,
            color = LocalPlayerTint.current.copy(alpha = 0.78f),
        )
    }
}

@Composable
internal fun CircleGlyph(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    active: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    FilledTonalIconButton(
        onClick = {
            Haptics.click()
            onClick()
        },
        colors = if (active) {
            IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = scheme.primaryContainer,
                contentColor = scheme.onPrimaryContainer,
            )
        } else IconButtonDefaults.filledTonalIconButtonColors(),
    ) {
        Icon(icon, contentDescription)
    }
}

@Composable
internal fun TransportGlyph(
    icon: ImageVector,
    contentDescription: String,
    size: Dp,
    touchSize: Dp,
    onClick: () -> Unit,
    prominent: Boolean = false,
) {
    val click = {
        Haptics.click()
        onClick()
    }
    val glyph: @Composable () -> Unit = {
        com.music.spotui.ui.components.GlyphSwap(icon) { current ->
            Icon(current, contentDescription, modifier = Modifier.size(size))
        }
    }
    if (prominent) {
        FilledIconButton(
            onClick = click,
            modifier = Modifier.size(touchSize),
            shape = RoundedCornerShape(28.dp),
            content = glyph,
        )
    } else {
        FilledTonalIconButton(onClick = click, modifier = Modifier.size(touchSize), content = glyph)
    }
}

internal val VOLUME_ROW_HEIGHT = AppSpacing.TouchTarget
private val MusicTransportHeight = 76.dp

/** The volume bar between its two speaker glyphs. */
@Composable
internal fun VolumeRow(volume: PlayerVolume) {
    Row(
        modifier = Modifier.fillMaxWidth().height(VOLUME_ROW_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.S),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.VolumeDown,
            contentDescription = stringResource(R.string.immersive_player_volume_down),
            tint = LocalPlayerTint.current.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp),
        )
        val level = volume.level.value
        val slider = rememberSliderState(value = level)
        SideEffect { slider.value = level }
        val volumeDescription = stringResource(R.string.immersive_player_volume)
        Slider(
            state = slider,
            onValueChange = {
                slider.value = it
                volume.drag(it)
            },
            onValueChangeFinished = volume::release,
            modifier = Modifier.weight(1f).semantics { contentDescription = volumeDescription },
        )
        Icon(
            Icons.AutoMirrored.Rounded.VolumeUp,
            contentDescription = stringResource(R.string.immersive_player_volume_up),
            tint = LocalPlayerTint.current.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MusicTransportRow(state: ImmersivePlayerState, actions: ImmersivePlayerActions) {
    val scheme = MaterialTheme.colorScheme
    val previousInteraction = remember { MutableInteractionSource() }
    val playInteraction = remember { MutableInteractionSource() }
    val nextInteraction = remember { MutableInteractionSource() }
    val corner by animateDpAsState(
        if (state.playing) 26.dp else 40.dp,
        animationSpec = if (rememberReduceMotion()) snap() else MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "playPauseCorners",
    )
    val skipColors = IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = scheme.onPrimaryContainer.copy(alpha = 0.14f),
        contentColor = scheme.onPrimaryContainer,
    )
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.S),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(
            onClick = {
                Haptics.click()
                actions.previous()
            },
            modifier = Modifier.weight(1f).height(MusicTransportHeight).expressivePress(previousInteraction),
            interactionSource = previousInteraction,
            colors = skipColors,
        ) {
            Icon(Icons.Rounded.SkipPrevious, stringResource(R.string.immersive_player_previous), Modifier.size(28.dp))
        }
        FilledIconButton(
            onClick = {
                Haptics.click()
                actions.playPause()
            },
            modifier = Modifier.weight(1f).height(MusicTransportHeight).expressivePress(playInteraction, enabled = !state.buffering),
            interactionSource = playInteraction,
            enabled = !state.buffering,
            shape = RoundedCornerShape(corner.coerceAtLeast(0.dp)),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = scheme.primary,
                contentColor = scheme.onPrimary,
                disabledContainerColor = scheme.onPrimaryContainer.copy(alpha = 0.14f),
            ),
        ) {
            if (state.buffering) {
                CircularProgressIndicator(Modifier.size(28.dp), color = scheme.onPrimaryContainer, strokeWidth = 3.dp)
            } else {
                val icon = if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow
                Icon(
                    icon,
                    stringResource(if (state.playing) R.string.immersive_player_pause else R.string.immersive_player_play),
                    Modifier.size(34.dp),
                )
            }
        }
        FilledTonalIconButton(
            onClick = {
                Haptics.click()
                actions.next()
            },
            modifier = Modifier.weight(1f).height(MusicTransportHeight).expressivePress(nextInteraction),
            interactionSource = nextInteraction,
            colors = skipColors,
        ) {
            Icon(Icons.Rounded.SkipNext, stringResource(R.string.immersive_player_next), Modifier.size(28.dp))
        }
    }
}

@Composable
internal fun PlayerActionRow(state: ImmersivePlayerState, actions: ImmersivePlayerActions) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(0.78f)
            .height(64.dp)
            .clip(CircleShape)
            .background(scheme.surfaceContainerLowest.copy(alpha = 0.70f))
            .padding(AppSpacing.S)
            .clip(CircleShape),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.S),
    ) {
        val segment = Modifier.weight(1f).height(AppSpacing.TouchTarget)
        if (com.music.spotui.data.preferences.PlayerModesPref.on) {
            val shuffleLabel = when {
                state.smart -> R.string.smart_shuffle_title
                state.shuffle -> R.string.immersive_player_shuffle_on
                else -> R.string.immersive_player_shuffle_off
            }
            PlayerModeSegment(
                icon = if (state.smart) Icons.Rounded.AutoAwesome else Icons.Rounded.Shuffle,
                label = stringResource(shuffleLabel),
                active = state.shuffle || state.smart,
                activeBackground = scheme.primaryFixed,
                activeForeground = scheme.onPrimaryFixed,
                onClick = actions.toggleShuffle,
                modifier = segment,
            )
            val repeatLabel = when (state.repeatMode) {
                ImmersiveRepeat.ONE -> R.string.immersive_player_repeat_one
                ImmersiveRepeat.ONCE -> R.string.immersive_player_repeat_once
                ImmersiveRepeat.ALL -> R.string.immersive_player_repeat_all
                else -> R.string.immersive_player_repeat_off
            }
            val repeatIcon = when (state.repeatMode) {
                ImmersiveRepeat.ONE -> Icons.Rounded.RepeatOne
                ImmersiveRepeat.ONCE -> RepeatIcons.once
                else -> Icons.Rounded.Repeat
            }
            PlayerModeSegment(
                icon = repeatIcon,
                label = stringResource(repeatLabel),
                active = state.repeatMode != ImmersiveRepeat.OFF,
                activeBackground = scheme.secondaryFixed,
                activeForeground = scheme.onSecondaryFixed,
                onClick = actions.cycleRepeat,
                modifier = segment,
                cyclesModes = true,
            )
        }
        PlayerModeSegment(
            icon = Icons.Rounded.Favorite,
            label = stringResource(if (state.liked) R.string.immersive_player_unlike else R.string.immersive_player_like),
            active = state.liked,
            activeBackground = scheme.tertiaryFixed,
            activeForeground = scheme.onTertiaryFixed,
            onClick = actions.toggleLike,
            modifier = segment,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlayerModeSegment(
    icon: ImageVector,
    label: String,
    active: Boolean,
    activeBackground: Color,
    activeForeground: Color,
    onClick: () -> Unit,
    modifier: Modifier,
    cyclesModes: Boolean = false,
) {
    val foreground = MaterialTheme.colorScheme.onSurface
    val targetBackground = if (active) activeBackground else foreground.copy(alpha = 0.07f)
    val targetRadius = if (active) 24.dp else 8.dp
    val motion = MaterialTheme.motionScheme
    val reduceMotion = rememberReduceMotion()
    val animatedBackground by animateColorAsState(
        targetBackground, if (reduceMotion) snap() else motion.fastEffectsSpec(), label = "modeBackground",
    )
    val animatedRadius by animateDpAsState(
        targetRadius, if (reduceMotion) snap() else motion.fastSpatialSpec(), label = "modeCorners",
    )
    val source = remember { MutableInteractionSource() }
    val activate = {
        Haptics.click()
        onClick()
    }
    val interaction = if (cyclesModes) {
        Modifier.clickable(interactionSource = source, indication = ripple(), role = Role.Button, onClick = activate)
    } else {
        Modifier.toggleable(value = active, interactionSource = source, indication = ripple(), role = Role.Checkbox, onValueChange = { activate() })
    }
    Box(
        modifier = modifier
            .expressivePress(source)
            .clip(RoundedCornerShape(animatedRadius.coerceAtLeast(0.dp)))
            .background(animatedBackground)
            .then(interaction),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = if (active) activeForeground else foreground, modifier = Modifier.size(24.dp))
    }
}
