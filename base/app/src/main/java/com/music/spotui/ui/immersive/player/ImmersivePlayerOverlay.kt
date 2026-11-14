/*
 * Adapted from BitChord (https://github.com/kushagrasinghx/BitChord), GPL-3.0-or-later,
 * app/src/main/java/com/music/bitchord/ui/player/PlayerSheetMotion.kt and
 * sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/PlayerDock.kt at edc1e6f.
 * Modified for Myspoti, 2026-10.
 *
 * Changes: only the sheet's motion is kept: it opens and closes on one critically damped spring
 * (stiffness 195) and a pull down carries the finger's speed into it. The Material sheet window,
 * the window animations and the cover docking into the mini player are left out.
 */
package com.music.spotui.ui.immersive.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.immersive.shell.LocalImmersiveBackdrop
import com.music.spotui.ui.immersive.shell.LocalImmersiveFrostPage
import com.music.spotui.ui.immersive.shell.backdrop.backdrops.LocalBackdropSourceAlpha
import com.music.spotui.ui.immersive.shell.backdrop.backdrops.layerBackdrop
import com.music.spotui.ui.immersive.shell.backdrop.backdrops.rememberLayerBackdrop
import com.music.spotui.ui.navigation.LocalPageSettled
import com.music.spotui.ui.navigation.PlayerSheet
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** A release faster than this (px/s) decides the sheet on its own, either way. */
internal const val PlayerFlingVelocity = 1_000f

private val PlayerCardShape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

private fun slideSpec(reduce: Boolean) = com.music.spotui.ui.immersive.motion.ImmersiveMotion.slideSpring(reduce)

internal class ArtworkDock(
    val record: (LayoutCoordinates) -> Unit,
    val inFlight: () -> Boolean,
)

internal val LocalArtworkDock = staticCompositionLocalOf<ArtworkDock?> { null }

/**
 * The Immersive look's player over the pages, a drop-in for [com.music.spotui.ui.navigation.PlayerOverlay]:
 * driven by [PlayerSheet] (`open`, `show()`, `close()`), it rises on a spring when opened, follows
 * a pull down and closes with the finger's speed, closes on the back button and on any navigation.
 * A podcast episode gets [ImmersivePodcastPlayer].
 */
@Composable
fun ImmersivePlayerOverlay(navController: NavController) {
    // Any navigation from the player (album, artist, playlist…) lands on that page.
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.drop(1).collect { PlayerSheet.close() }
    }
    val open = PlayerSheet.open.value
    val pulling = PlayerSheet.miniPullActive.value
    val scope = rememberCoroutineScope()
    val dismissDistance = with(androidx.compose.ui.platform.LocalDensity.current) { 96.dp.toPx() }
    val currentDismissDistance = androidx.compose.runtime.rememberUpdatedState(dismissDistance)
    val reduce = com.music.spotui.ui.look.rememberReduceMotion()
    var heightPx by remember { mutableIntStateOf(0) }
    // How far down the sheet is (px): 0 open, the window's height away.
    val offset = remember { Animatable(0f) }
    var shown by remember { mutableStateOf(false) }
    // Invisible until it has been put below the window: no frame of it at rest before the rise.
    var primed by remember { mutableStateOf(false) }
    // The finger's hold on the sheet, or NaN when it has none.
    var held by remember { mutableFloatStateOf(Float.NaN) }
    var closeVelocity by remember { mutableFloatStateOf(0f) }
    val settled = remember { mutableStateOf(false) }

    LaunchedEffect(open, pulling, reduce) {
        if (pulling && reduce) return@LaunchedEffect
        if (open || pulling || shown) {
            val wasShown = shown
            shown = true
            if (!wasShown) {
                settled.value = false
                snapshotFlow { heightPx }.first { it > 0 }
                offset.snapTo(heightPx.toFloat())
                primed = true
            }
            if (pulling) return@LaunchedEffect
            if (PlayerSheet.miniPullDistance > 0f) {
                offset.snapTo((heightPx - PlayerSheet.miniPullDistance).coerceAtLeast(0f))
                PlayerSheet.miniPullDistance = 0f
            }
            if (open) {
                offset.animateTo(0f, slideSpec(reduce))
                settled.value = true
            } else {
                settled.value = false
                val velocity = closeVelocity.coerceAtLeast(0f)
                closeVelocity = 0f
                offset.animateTo(heightPx.toFloat(), slideSpec(reduce), initialVelocity = velocity)
                shown = false
                primed = false
            }
        } else {
            PlayerSheet.miniPullDistance = 0f
        }
    }
    if (!shown) return
    BackHandler(enabled = open) { PlayerSheet.close() }
    // Compose's clickable asks the view for the system "click" sound on every tap; the Classic player
    // is silent, so the window's sound effects are off while this player is up (haptics are separate).
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(view) {
        val was = view.isSoundEffectsEnabled
        view.isSoundEffectsEnabled = false
        onDispose { view.isSoundEffectsEnabled = was }
    }

    val sheetDrag = remember {
        object : PlayerSheetDrag {
            override fun drag(delta: Float) {
                if (!PlayerSheet.open.value) return
                if (held.isNaN()) held = offset.value
                held = (held + delta).coerceIn(0f, heightPx.toFloat())
            }

            override fun release(velocity: Float) {
                if (held.isNaN()) return
                val from = held
                val close = when {
                    velocity >= PlayerFlingVelocity -> true
                    velocity <= -PlayerFlingVelocity -> false
                    else -> from >= minOf(currentDismissDistance.value, heightPx * 0.22f)
                }
                scope.launch {
                    offset.snapTo(from)
                    held = Float.NaN
                    if (close) {
                        // The finger's own speed goes into the spring: the sheet carries on as it was moving.
                        closeVelocity = velocity
                        PlayerSheet.close()
                    } else {
                        offset.animateTo(0f, slideSpec(reduce), initialVelocity = velocity)
                    }
                }
            }
        }
    }

    val music = !SongPlayer.isEpisode
    val bridge = if (music) rememberImmersivePlayer(navController) else null
    val destination = PlayerSheet.miniArtworkBounds?.takeIf { it.width > 0f && it.height > 0f }
    var origin by remember { mutableStateOf(Offset.Zero) }
    var artworkBounds by remember { mutableStateOf<Rect?>(null) }
    val miniBounds = PlayerSheet.miniBounds?.takeIf { it.width > 0f && it.height > 0f }
    val canMorph = !reduce && miniBounds != null
    val canDock = music && canMorph && destination != null
    val scheme = MaterialTheme.colorScheme
    val travel = {
        when {
            PlayerSheet.miniPullDistance > 0f -> (heightPx - PlayerSheet.miniPullDistance).coerceAtLeast(0f)
            held.isNaN() -> offset.value
            else -> held
        }
    }
    var sheetCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val dock = if (canDock) ArtworkDock(
        record = { cover ->
            val sheet = sheetCoordinates
            if (sheet != null && sheet.isAttached && cover.isAttached) {
                val localTop = sheet.localPositionOf(cover, Offset.Zero)
                val localBottom = sheet.localPositionOf(cover, Offset(cover.size.width.toFloat(), cover.size.height.toFloat()))
                val bounds = Rect(origin + localTop, origin + localBottom)
                if (bounds.width > 0f && bounds.height > 0f) artworkBounds = bounds
            }
        },
        inFlight = { artworkBounds != null && travel() > 1f },
    ) else null

    val expandedFraction = { (1f - travel() / heightPx.coerceAtLeast(1)).coerceIn(0f, 1f) }
    // Reveal the mounted mini player while the sheet is still moving into its bounds.
    val handoffAlpha = { (expandedFraction() / 0.30f).coerceIn(0f, 1f) }
    Box(Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInWindow() }) {
        Box(
            Modifier.fillMaxSize()
                .onSizeChanged { heightPx = it.height }
                .graphicsLayer {
                    val fraction = expandedFraction()
                    alpha = when {
                        !primed -> 0f
                        canMorph -> handoffAlpha()
                        else -> 1f
                    }
                    if (miniBounds != null && canMorph && fraction < 1f) {
                        val anchor = miniBounds.translate(-origin)
                        val rect = lerp(anchor, Rect(Offset.Zero, size), fraction)
                        val corner = MiniPlayerCorner.toPx() * (1f - fraction)
                        shape = GenericShape { _, _ ->
                            addRoundRect(RoundRect(rect, CornerRadius(corner)))
                        }
                        clip = true
                    } else if (!canMorph && travel() > 0.5f) {
                        translationY = travel()
                        shape = PlayerCardShape
                        clip = true
                    }
                }
                .drawBehind {
                    if (canMorph) {
                        drawRect(lerpColor(scheme.secondaryContainer, scheme.primaryContainer, expandedFraction()))
                    }
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = open,
                ) {},
        ) {
            // The glass sheets opened from the player (sound quality, devices…) sample the player, not
            // the page under it: the player records itself and stands in for the page recording.
            // Nothing recorded while neither Liquid Glass nor the recorded frost reads it.
            val outerGlass = LocalImmersiveBackdrop.current
            val outerFrost = LocalImmersiveFrostPage.current
            val playerPage = if (outerGlass != null || outerFrost != null) rememberLayerBackdrop() else null
            CompositionLocalProvider(
                LocalPageSettled provides settled,
                LocalPlayerSheetDrag provides sheetDrag,
                LocalArtworkDock provides dock,
                LocalImmersiveBackdrop provides outerGlass?.let { playerPage },
                LocalImmersiveFrostPage provides outerFrost?.let { playerPage },
                // Hidden until primed, like the player itself: no frame of it at rest in the glass.
                LocalBackdropSourceAlpha provides { if (primed) 1f else 0f },
            ) {
                Box(
                    Modifier.fillMaxSize()
                        .onGloballyPositioned { sheetCoordinates = it }
                        .graphicsLayer {
                            if (canMorph) {
                                val fraction = expandedFraction()
                                alpha = ((fraction - 0.22f) / 0.68f).coerceIn(0f, 1f)
                            }
                        }
                        .then(if (playerPage != null) Modifier.layerBackdrop(playerPage) else Modifier),
                ) {
                    // A podcast episode gets the podcast player (15 s skips, speed); everything else the music one.
                    if (bridge == null) {
                        ImmersivePodcastPlayer(navController)
                    } else {
                        ImmersivePlayer(bridge.state, bridge.actions, navController = navController)
                    }
                }
            }
        }
        val start = artworkBounds
        if (dock != null && start != null && destination != null && bridge != null) {
            val density = LocalDensity.current
            ArtworkImage(
                bridge.state.coverUrl,
                Modifier.size(
                    with(density) { start.width.toDp() },
                    with(density) { start.height.toDp() },
                ).graphicsLayer {
                    val progress = (travel() / heightPx.coerceAtLeast(1)).coerceIn(0f, 1f)
                    val rect = lerp(start, destination, progress)
                    transformOrigin = TransformOrigin(0f, 0f)
                    translationX = rect.left - origin.x
                    translationY = rect.top - origin.y
                    scaleX = rect.width / start.width
                    scaleY = rect.height / start.height
                    val corner = androidx.compose.ui.unit.lerp(28.dp, MiniArtworkCorner / scaleX, progress)
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(corner)
                    clip = true
                    alpha = if (primed && dock.inFlight()) handoffAlpha() else 0f
                },
            )
        }
    }
}
