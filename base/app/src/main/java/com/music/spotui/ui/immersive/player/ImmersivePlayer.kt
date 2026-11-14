/*
 * Adapted from BitChord (https://github.com/kushagrasinghx/BitChord), GPL-3.0-or-later,
 * sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/NowPlayingScreen.kt at edc1e6f.
 * Modified for Myspoti, 2026-10.
 *
 * Changes: only the portrait layout is taken: the artwork with its step back on pause, the move
 * of the artwork to a small header (title beside it) when the lyrics or the queue open, the
 * title row with its round glyphs, the scrubber, transport, volume and bottom rows, and the
 * sheet's drag to close. The full-bleed banner, Canvas clips, BitChord's track-swipe on the cover,
 * version switching, the lyric strip and the dock are left out. The landscape layout (cover beside
 * the controls), the Canvas behind the player (with the cover opening into it) and the swipe
 * along the cover that changes track (the mini player's) are Myspoti's own.
 */
package com.music.spotui.ui.immersive.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.spotui.ui.navigation.PlayerSheet
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.State
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Earbuds
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconToggleButton
import com.music.spotui.data.preferences.PlayerCapsulePref
import com.music.spotui.ui.components.AudioRoute
import com.music.spotui.ui.components.rememberAudioRoute
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.navigation.NavController
import app.myspoti.engine.R
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.music.spotui.ui.components.DevicesSheet
import com.music.spotui.ui.immersive.sheets.ImmersiveSongOptionsSheet
import com.music.spotui.ui.immersive.Immersive
import com.music.spotui.ui.immersive.lyrics.ImmersiveLyricsPanel
import com.music.spotui.ui.immersive.lyrics.ImmersiveQueuePanel
import com.music.spotui.ui.look.lerp
import com.music.spotui.ui.components.SongBlendCover
import com.music.spotui.ui.components.mixSqueeze
import com.music.spotui.ui.components.mixWave
import com.music.spotui.ui.look.rememberPauseScale
import com.music.spotui.ui.look.rememberReduceMotion
import kotlinx.coroutines.delay
import kotlin.math.abs

// Sizes and gaps of the portrait layout, as the player this follows has them.
private val THUMB_SIZE = 54.dp
// Covers.large is 640px: the cover is always decoded at that, whatever size the box has right now.
private const val COVER_DECODE_PX = 640
private val ART_BOX_TOP_PAD = 8.dp

/** The player's side margin. Scrollable panels reach back across it. */
internal val PLAYER_GUTTER = 16.dp

/**
 * How wide the player's content is ever allowed to get: a sleeve and a volume slider stretched
 * across a tablet aren't a bigger player, just a coarser one.
 */
internal val PLAYER_MAX_WIDTH = 560.dp

/** How long the sleeve takes to travel between the full player and the header. */
private const val PANEL_TRAVEL_MS = com.music.spotui.ui.immersive.motion.ImmersiveMotion.LONG_MS

private val ArtCorner = RoundedCornerShape(28.dp)
/** How much bigger the cover is on an AutoMix beat (the player draws in around it meanwhile). */
private const val MIX_BEAT_GROW = 0.025f

/** What the middle of the player shows. */
private object Panel {
    const val NONE = 0
    const val LYRICS = 1
    const val QUEUE = 2
}

/**
 * The sheet the player sits in, as the player sees it: dragged down from the top of the player
 * (the handle, the artwork, the credits) it follows the finger, and lets go with its speed.
 */
internal interface PlayerSheetDrag {
    fun drag(delta: Float)
    fun release(velocity: Float)
}

internal val LocalPlayerSheetDrag = staticCompositionLocalOf<PlayerSheetDrag?> { null }

/**
 * The downward half of a vertical drag on this element, handed to the sheet. Upward is not ours:
 * the crossing of the touch slop is left alone and nothing is consumed, so a tap, a scroll or a
 * child's own gesture keeps working.
 */
@Composable
internal fun Modifier.dragToClose(enabled: Boolean = true): Modifier {
    if (!enabled) return this
    val sheet = LocalPlayerSheetDrag.current ?: return this
    val current by rememberUpdatedState(sheet)
    return pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var pulled = 0f
            val drag = awaitVerticalTouchSlopOrCancellation(down.id) { change, overSlop ->
                if (overSlop > 0f) {
                    pulled = overSlop
                    change.consume()
                }
            }
            if (drag == null || pulled <= 0f) return@awaitEachGesture
            val velocity = VelocityTracker()
            velocity.addPointerInputChange(down)
            velocity.addPointerInputChange(drag)
            current.drag(pulled)
            var releaseVelocity = 0f
            try {
                val finished = verticalDrag(drag.id) { change ->
                    velocity.addPointerInputChange(change)
                    current.drag(change.positionChange().y)
                    change.consume()
                }
                if (finished) releaseVelocity = velocity.calculateVelocity().y
            } finally {
                current.release(releaseVelocity)
            }
        }
    }
}

/** How far the cover is swiped along to change track (the mini player's distance). */
private val TRACK_SWIPE_THRESHOLD = 72.dp

/**
 * A swipe along the cover changes track, as on the mini player: to the left the next song, to the
 * right the previous one, past [TRACK_SWIPE_THRESHOLD]. Only a mostly sideways move is taken (and
 * consumed); a vertical one is left untouched for [dragToClose] and a tap for the cover's click.
 * Off while [enabled] is false (the cover is the header's thumbnail under an open panel).
 */
@Composable
private fun Modifier.swipeToChangeTrack(enabled: Boolean, onSkip: (Int) -> Unit): Modifier {
    val on by rememberUpdatedState(enabled)
    val current by rememberUpdatedState(onSkip)
    return pointerInput(Unit) {
        val threshold = TRACK_SWIPE_THRESHOLD.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (!on) return@awaitEachGesture
            val slopped = awaitTouchSlopOrCancellation(down.id) { change, _ ->
                val moved = change.position - down.position
                if (abs(moved.x) > abs(moved.y)) change.consume()
            } ?: return@awaitEachGesture
            var dragged = (slopped.position - down.position).x
            val finished = drag(slopped.id) { change ->
                dragged += change.positionChange().x
                change.consume()
            }
            if (!finished) return@awaitEachGesture
            when {
                dragged <= -threshold -> current(1)
                dragged >= threshold -> current(-1)
            }
        }
    }
}

private class CoverSwipe(val from: String, val direction: Int)

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlayerArtwork(
    state: ImmersivePlayerState,
    actions: ImmersivePlayerActions,
    modifier: Modifier,
    overlay: @Composable () -> Unit = {},
) {
    val reduceMotion = rememberReduceMotion()
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    var swipe by remember { mutableStateOf<CoverSwipe?>(null) }
    LaunchedEffect(swipe) {
        if (swipe != null) {
            delay(4_000)
            swipe = null
        }
    }
    Box(
        modifier.swipeToChangeTrack(enabled = !state.adPlaying) { direction ->
            swipe = if (reduceMotion) null else CoverSwipe(state.coverUrl, direction)
            if (direction > 0) actions.next() else actions.previous()
        },
    ) {
        AnimatedContent(
            targetState = state.coverUrl,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                val direction = swipe?.takeIf { it.from == initialState }?.direction
                val transition = if (!reduceMotion && direction != null) {
                    slideInHorizontally(spatial) { it * direction } togetherWith
                        slideOutHorizontally(spatial) { -it * direction }
                } else {
                    EnterTransition.None togetherWith ExitTransition.None
                }
                transition.using(SizeTransform(clip = true))
            },
            label = "playerArtworkSwipe",
        ) { cover ->
            Box(Modifier.fillMaxSize()) {
                GlideImage(
                    model = cover.takeIf { it.isNotBlank() },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    transition = CrossFade,
                    loading = placeholder(R.drawable.placeholder),
                    failure = placeholder(R.drawable.placeholder),
                    modifier = Modifier.fillMaxSize(),
                    requestBuilderTransform = { it.override(COVER_DECODE_PX) },
                )
                if (cover == state.coverUrl) {
                    SongBlendCover(state.blend, Modifier.fillMaxSize()) { it.override(COVER_DECODE_PX) }
                    overlay()
                }
            }
        }
    }
}

/**
 * The Immersive look's player: the cover's colours drifting behind a large cover, the credits,
 * a scrubber, the transport, the volume and a row of lyrics, devices and queue.
 *
 * Opening the lyrics or the queue moves the cover up into a small header with the credits beside
 * it; [ImmersiveLyricsPanel] or [ImmersiveQueuePanel] takes the middle and the controls below
 * stay where they are. [navController] is for the "more" sheet (artist, album, playlists…):
 * without it the button is not shown.
 */
@OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@Composable
fun ImmersivePlayer(
    state: ImmersivePlayerState,
    actions: ImmersivePlayerActions,
    modifier: Modifier = Modifier,
    navController: NavController? = null,
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val tint = scheme.onPrimaryContainer

    var panel by rememberSaveable { mutableIntStateOf(Panel.NONE) }
    var showMenu by remember { mutableStateOf(false) }
    var showDevices by remember { mutableStateOf(false) }
    var showQuality by remember { mutableStateOf(false) }
    var showNerd by remember { mutableStateOf(false) }
    val lyricsOpen = panel == Panel.LYRICS
    val queueOpen = panel == Panel.QUEUE

    // 0 with the full player, 1 with the cover in the header. Read at measure, placement and draw.
    val collapse = animateFloatAsState(
        targetValue = if (panel == Panel.NONE) 0f else 1f,
        animationSpec = tween(PANEL_TRAVEL_MS, easing = FastOutSlowInEasing),
        label = "playerCollapse",
    )
    val pauseScale = rememberPauseScale(state.playing)
    val volume = rememberPlayerVolume()

    val canvasUrl = rememberImmersiveCanvas(state)
    var canvasShown by remember(canvasUrl) {
        mutableStateOf(canvasUrl != null && com.music.spotui.ui.components.CanvasPlayer.isReady(canvasUrl))
    }
    val canvasOn = canvasUrl != null && canvasShown && !state.adPlaying && panel == Panel.NONE
    // Opening on a clip already showing starts at the end; every later change animates from where it is.
    val canvasOpen = remember { androidx.compose.animation.core.Animatable(if (canvasOn) 1f else 0f) }
    // Opening the player: the heart of the song shown is checked again if its state is old.
    LaunchedEffect(Unit) { com.music.spotui.di.SongPlayer.refreshLikedIfStale() }
    LaunchedEffect(canvasOn) {
        val target = if (canvasOn) 1f else 0f
        val left = kotlin.math.abs(target - canvasOpen.value)
        // A panel opening: the clip leaves with the cover's travel to the header, not after it.
        val ms = if (!canvasOn && panel != Panel.NONE) PANEL_TRAVEL_MS else CANVAS_OPEN_MS
        if (left > 0f) canvasOpen.animateTo(target, tween((ms * left).toInt(), easing = FastOutSlowInEasing))
    }

    // Back leaves the panel first; the sheet (outside) closes on the next one.
    BackHandler(enabled = panel != Panel.NONE) { panel = Panel.NONE }

    // The wave that leaves the cover when a mix is over, over everything.
    // Not over an open panel: the lyrics or the queue drawing in and springing back with the mix
    // moved the lines being read (reported as particularly bad on the lyrics). Nor over a Canvas:
    // the clip stays full screen, so the controls sprang about over it and the wave crossed the
    // video (reported as the end of the mix bugging on a Canvas).
    val mixMotion = state.mixPulse.takeIf { panel == Panel.NONE && !canvasOn }
    BoxWithConstraints(modifier.fillMaxSize().background(scheme.primaryContainer).mixWave(mixMotion, scheme.primary)) {
        // Wider than tall (a phone on its side, a tablet), or a large portrait tablet: the cover
        // beside the controls. Very wide windows (DeX, large tablets): the lyrics and the queue
        // open in a third pane instead of over the cover.
        val window = com.music.spotui.ui.components.windowSize()
        val landscape = maxWidth > maxHeight || window.twoPane
        val sidePane = window.large && maxWidth >= SIDE_PANE_MIN_WIDTH
        // An AutoMix mix: the content draws in with it and springs back after it, the backdrop stays.
        Box(Modifier.fillMaxSize().mixSqueeze(mixMotion)) {
        CompositionLocalProvider(LocalPlayerTint provides tint) {
            if (lyricsOpen) {
                FullPlayerLyrics(state, actions, onBack = { panel = Panel.NONE })
            } else if (landscape) LandscapePlayer(
                state = state,
                actions = actions,
                panel = panel,
                pauseScale = pauseScale,
                volume = volume,
                onToggleLyrics = { panel = if (lyricsOpen) Panel.NONE else Panel.LYRICS },
                onToggleQueue = { panel = if (queueOpen) Panel.NONE else Panel.QUEUE },
                onOpenDevices = { showDevices = true },
                onMore = if (navController != null && state.song != null) ({ showMenu = true }) else null,
                onSource = { showQuality = true },
                onSourceLong = { showNerd = true },
                sidePane = sidePane,
            ) else Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().dragToClose(enabled = panel == Panel.NONE)) {
                PlayerHeader(
                    actions = actions,
                    state = state,
                    lyricsOpen = lyricsOpen,
                    queueOpen = queueOpen,
                    onToggleLyrics = { panel = if (lyricsOpen) Panel.NONE else Panel.LYRICS },
                    onToggleQueue = { panel = if (queueOpen) Panel.NONE else Panel.QUEUE },
                    onOpenDevices = { showDevices = true },
                    onMore = if (navController != null && state.song != null) ({ showMenu = true }) else null,
                )

                // Everything that changes between the full player and the panels lives in this
                // one weighted box, so the controls below it never move.
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val contentWidth = minOf(maxWidth - PLAYER_GUTTER * 2, PLAYER_MAX_WIDTH)
                    val left = (maxWidth - contentWidth) / 2
                    val fullArt = minOf(contentWidth * 0.86f, maxHeight - ART_BOX_TOP_PAD - 16.dp)
                        .coerceAtLeast(THUMB_SIZE)
                    val spare = (maxHeight - ART_BOX_TOP_PAD - fullArt - 16.dp)
                        .coerceAtLeast(0.dp)
                    val groupTop = spare / 2
                    // Functions of the collapse, called at placement and measure: composition never reads them.
                    fun p() = collapse.value
                    fun artSize(): Dp = lerp(fullArt, THUMB_SIZE, p())
                    fun artTop(): Dp = ART_BOX_TOP_PAD + lerp(groupTop, 0.dp, p())
                    fun artStart(): Dp = left + lerp((contentWidth - fullArt) / 2, 0.dp, p())

                    // ---- The panel, under the header ----
                    val panelAlpha = { ((p() - 0.6f) / 0.4f).coerceIn(0f, 1f) }
                    Crossfade(
                        targetState = panel,
                        animationSpec = tween(250),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 4.dp)
                            .graphicsLayer { alpha = panelAlpha() },
                        label = "playerPanel",
                    ) { shown ->
                        val inset = PaddingValues(start = left, end = left, top = 12.dp, bottom = 12.dp)
                        when (shown) {
                            Panel.QUEUE -> ImmersiveQueuePanel(Modifier.fillMaxSize(), inset, showCurrent = false)
                            else -> Box(Modifier.fillMaxSize())
                        }
                    }

                    if (panel == Panel.NONE) PlayerArtwork(
                        state = state,
                        actions = actions,
                        modifier = Modifier
                            .offset { IntOffset(artStart().roundToPx(), artTop().roundToPx()) }
                            .layout { measurable, _ ->
                                val side = artSize().roundToPx()
                                val placeable = measurable.measure(Constraints.fixed(side, side))
                                layout(side, side) { placeable.place(0, 0) }
                            }
                            .graphicsLayer {
                                val beat = 1f + MIX_BEAT_GROW * (state.mixPulse?.level() ?: 0f) * (1f - p())
                                val scale = (pauseScale.value + (1f - pauseScale.value) * p()) * beat
                                scaleX = scale
                                scaleY = scale
                                shadowElevation = 10.dp.toPx() * (1f - p())
                                shape = ArtCorner
                                clip = true
                            }
                            .background(Color.Black.copy(alpha = 0.18f)),
                    ) {
                        if (canvasUrl != null) {
                            com.music.spotui.ui.screens.CanvasVideo(
                                url = canvasUrl,
                                onFirstFrame = { canvasShown = true },
                                playing = PlayerSheet.open.value && state.playing && !state.buffering,
                                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = canvasOpen.value },
                            )
                        }
                    }


                }

                // ---- Bottom: scrubber, transport, volume, toggles ----
                // One block, measured at its natural height and pinned to the foot of the player.
                Column(
                    modifier = Modifier
                        .widthIn(max = PLAYER_MAX_WIDTH + PLAYER_GUTTER * 2)
                        .fillMaxWidth()
                        .padding(horizontal = PLAYER_GUTTER)
                        .align(Alignment.CenterHorizontally),
                ) {
                    PlayerControlsBlock(
                        state = state, actions = actions, volume = volume,
                        showVolume = com.music.spotui.data.preferences.PlayerVolumePref.on,
                        onSource = { showQuality = true },
                        onSourceLong = { showNerd = true },
                    )
                }
            }
        }
        }
    }

    if (showDevices) DevicesSheet { showDevices = false }
    if (showQuality) com.music.spotui.ui.components.QualitySheet { showQuality = false }
    if (showNerd) com.music.spotui.ui.components.StatsForNerdsSheet { showNerd = false }
    val song = state.song
    if (showMenu && song != null && navController != null) {
        ImmersiveSongOptionsSheet(
            song = song,
            navController = navController,
            context = context,
            onDismiss = { showMenu = false },
            nowPlaying = true,
            onOpenQueue = { panel = Panel.QUEUE },
            lyricsOpen = lyricsOpen,
        )
    }
}

/** The cover opening into the Canvas, and the clip's fade in. */
private const val CANVAS_OPEN_MS = 720
/** How much closer the clip starts as it opens. */
/** How much the sleeve grows as it opens into the clip. */
/**
 * The Canvas of the song shown, as the Classic player finds it (same view model, cache and
 * settings: Canvases off, data saver, Wi-Fi only, energy saving); null when there is none.
 */
@Composable
private fun rememberImmersiveCanvas(state: ImmersivePlayerState): String? {
    val context = LocalContext.current
    val vm: com.music.spotui.ui.viewmodel.PlayerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val energy by app.myspoti.power.EnergySettings.state.collectAsStateWithLifecycle()
    val allowed = energy.policy.allowCanvas && !com.music.spotui.data.DataSaver.active &&
        !(com.music.spotui.data.DataSaver.metered && com.music.spotui.data.preferences.isCanvasWifiOnly(context))
    val trackId = state.trackId
    LaunchedEffect(trackId, allowed, state.buffering) {
        vm.loadCanvas(if (allowed) trackId else "", allowFetch = !state.buffering)
    }
    val canvas = vm.canvasState.value
    // Known already (looked up when the song started): this very frame, not one later.
    val known = remember(trackId) { com.music.spotui.di.Graph.api.knownCanvas(trackId)?.getOrNull() }
    return (if (canvas.trackId == trackId) canvas.url else known).takeIf { allowed && trackId.isNotBlank() && !state.adPlaying }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayerCredits(
    state: ImmersivePlayerState,
    actions: ImmersivePlayerActions,
    tint: Color,
    onSource: () -> Unit,
    onSourceLong: () -> Unit,
) {
    val sourceLabel = com.music.spotui.ui.components.playingSourceLabel()
    val sourceShown = com.music.spotui.data.preferences.PlayerSourceLinePref.shown
    Column(
        Modifier.fillMaxWidth().graphicsLayer { alpha = state.blend?.wordsAlpha() ?: 1f },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (state.adPlaying) stringResource(R.string.immersive_player_ad) else state.title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false).clickable(onClick = actions.openAlbum),
            )
            if (state.explicit && !state.adPlaying) {
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.explicit_badge), style = MaterialTheme.typography.labelSmall, color = tint)
            }
        }
        if (!state.adPlaying) {
            val creditColor = tint.copy(alpha = 0.87f)
            val credit = com.music.spotui.ui.components.rememberArtistCredit(state.trackId, state.artist, creditColor) { actions.openArtistRef(it) }
            Text(
                text = credit ?: androidx.compose.ui.text.AnnotatedString(state.artist),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = creditColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(onClick = actions.openArtist),
            )
            if (sourceShown && sourceLabel.isNotBlank()) {
                Text(
                    text = sourceLabel,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = tint.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.combinedClickable(onClick = onSource, onLongClick = onSourceLong),
                )
            }
        }
    }
}

@Composable
private fun FullPlayerLyrics(
    state: ImmersivePlayerState,
    actions: ImmersivePlayerActions,
    onBack: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).height(52.dp).dragToClose(),
            contentAlignment = Alignment.Center,
        ) {
            FilledTonalIconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = scheme.surfaceContainerLowest,
                    contentColor = scheme.onSurface,
                ),
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.immersive_player_lyrics_close))
            }
            Text(
                stringResource(R.string.lyrics_card_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = scheme.onPrimaryContainer,
            )
        }
        ImmersiveLyricsPanel(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            playbackOverlay = { LyricsPlaybackControls(state, actions) },
        )
    }
}

@Composable
private fun PlayerHeader(
    actions: ImmersivePlayerActions,
    state: ImmersivePlayerState,
    lyricsOpen: Boolean,
    queueOpen: Boolean,
    onToggleLyrics: () -> Unit,
    onToggleQueue: () -> Unit,
    onOpenDevices: () -> Unit,
    onMore: (() -> Unit)?,
) {
    val scheme = MaterialTheme.colorScheme
    val route = rememberAudioRoute()
    val container = scheme.onPrimaryContainer.copy(alpha = 0.13f)
    val iconColors = IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = container,
        contentColor = scheme.onPrimaryContainer,
    )
    val toggleColors = IconButtonDefaults.iconToggleButtonColors(
        containerColor = container,
        contentColor = scheme.onPrimaryContainer,
        checkedContainerColor = scheme.primary,
        checkedContentColor = scheme.onPrimary,
    )
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp).dragToClose(enabled = queueOpen),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(onClick = actions.close, colors = iconColors) {
            Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.a11y_close_player))
        }
        Text(
            text = stringResource(R.string.immersive_shell_now_playing),
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onPrimaryContainer,
            modifier = Modifier.weight(1f).padding(start = 6.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (PlayerCapsulePref.shown) {
            FilledTonalIconButton(onClick = onOpenDevices, colors = iconColors) {
                val airPods = state.deviceName == null && route.kind == AudioRoute.Kind.BLUETOOTH &&
                    route.name.contains("airpods", ignoreCase = true)
                Icon(
                    if (airPods) Icons.Rounded.Earbuds else Icons.Rounded.Cast,
                    stringResource(R.string.immersive_player_devices),
                )
            }
        }
        if (onMore != null) {
            FilledTonalIconButton(onClick = onMore, colors = iconColors) {
                Icon(Icons.Rounded.MoreHoriz, stringResource(R.string.immersive_player_more))
            }
        }
        IconToggleButton(checked = lyricsOpen, onCheckedChange = { onToggleLyrics() }, colors = toggleColors) {
            Icon(
                Icons.Rounded.Lyrics,
                stringResource(if (lyricsOpen) R.string.immersive_player_lyrics_close else R.string.immersive_player_lyrics),
            )
        }
        IconToggleButton(checked = queueOpen, onCheckedChange = { onToggleQueue() }, colors = toggleColors) {
            Icon(
                Icons.AutoMirrored.Rounded.QueueMusic,
                stringResource(if (queueOpen) R.string.immersive_player_queue_close else R.string.immersive_player_queue),
            )
        }
    }
}

@Composable
private fun PlayerControlsBlock(
    state: ImmersivePlayerState,
    actions: ImmersivePlayerActions,
    volume: PlayerVolume,
    showVolume: Boolean,
    onSource: () -> Unit,
    onSourceLong: () -> Unit,
) {
    PlayerCredits(state, actions, MaterialTheme.colorScheme.onPrimaryContainer, onSource, onSourceLong)
    Spacer(Modifier.height(6.dp))
    PlayerScrubber(state, actions.seekTo, expressive = true)
    Spacer(Modifier.height(8.dp))
    MusicTransportRow(state, actions)
    if (showVolume) {
        VolumeRow(volume)
    }
    Spacer(Modifier.height(if (showVolume) 6.dp else 22.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        PlayerActionRow(state, actions)
    }
    val extTrack = remember(state.title, state.artist) { com.music.spotui.extensions.Extensions.currentTrack() }
    com.music.spotui.extensions.MirrorIndicator()
    com.music.spotui.extensions.ExtensionPlayerButtons(extTrack)
    Spacer(Modifier.height(12.dp))
}

/**
 * The landscape layout: the cover on the left (the lyrics or the queue take its place when they
 * are up), on the right the credits, the scrubber, every transport control and the bottom row.
 * The right side scrolls if a small window cannot hold it, so no control is out of reach.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun LandscapePlayer(
    state: ImmersivePlayerState,
    actions: ImmersivePlayerActions,
    panel: Int,
    pauseScale: State<Float>,
    volume: PlayerVolume,
    onToggleLyrics: () -> Unit,
    onToggleQueue: () -> Unit,
    onOpenDevices: () -> Unit,
    onMore: (() -> Unit)?,
    onSource: () -> Unit,
    onSourceLong: () -> Unit = {},
    sidePane: Boolean = false,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().dragToClose(enabled = panel == Panel.NONE)) {
        PlayerHeader(
            actions, state, panel == Panel.LYRICS, panel == Panel.QUEUE,
            onToggleLyrics, onToggleQueue, onOpenDevices, onMore,
        )
        Row(
            Modifier.weight(1f).fillMaxWidth().padding(start = PLAYER_GUTTER, end = PLAYER_GUTTER, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(PLAYER_GUTTER),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BoxWithConstraints(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                val side = minOf(maxWidth, maxHeight)
                // With a side pane the cover stays: the panels open in the third column.
                Crossfade(targetState = if (sidePane) Panel.NONE else panel, animationSpec = tween(250), modifier = Modifier.fillMaxSize(), label = "playerLandscapePanel") { shown ->
                    when (shown) {
                        Panel.QUEUE -> ImmersiveQueuePanel(Modifier.fillMaxSize(), PaddingValues(vertical = 4.dp), showCurrent = false)
                        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            PlayerArtwork(
                                state = state,
                                actions = actions,
                                modifier = Modifier
                                    .size(side)
                                    .graphicsLayer {
                                        val beat = 1f + MIX_BEAT_GROW * (state.mixPulse?.level() ?: 0f)
                                        scaleX = pauseScale.value * beat
                                        scaleY = pauseScale.value * beat
                                        shadowElevation = 10.dp.toPx()
                                        shape = ArtCorner
                                        clip = true
                                    }
                                    .background(Color.Black.copy(alpha = 0.18f))
                                    .dragToClose(),
                            )
                        }
                    }
                }
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                // The volume bar is the first thing to go when the window is low: the hardware keys do it too.
                val showVolume = com.music.spotui.data.preferences.PlayerVolumePref.on && maxHeight >= LANDSCAPE_VOLUME_MIN_HEIGHT
                Column(Modifier.widthIn(max = PLAYER_MAX_WIDTH).verticalScroll(rememberScrollState())) {
                    PlayerControlsBlock(
                        state = state, actions = actions, volume = volume,
                        showVolume = showVolume, onSource = onSource, onSourceLong = onSourceLong,
                    )
                }
            }
            if (sidePane && panel != Panel.NONE) Box(Modifier.weight(1.1f).fillMaxHeight()) {
                Crossfade(targetState = panel, animationSpec = tween(250), modifier = Modifier.fillMaxSize(), label = "playerSidePane") { shown ->
                    when (shown) {
                        Panel.QUEUE -> ImmersiveQueuePanel(Modifier.fillMaxSize(), PaddingValues(vertical = 4.dp), showCurrent = false)
                        else -> Box(Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

/** A window at least this wide shows the lyrics and the queue beside the cover and the controls. */
private val SIDE_PANE_MIN_WIDTH = 1100.dp

/** The right-hand column needs about this much height to show the volume bar as well. */
private val LANDSCAPE_VOLUME_MIN_HEIGHT = 400.dp
