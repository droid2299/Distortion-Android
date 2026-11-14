/*
 * Adapted from BitChord (https://github.com/kushagrasinghx/BitChord), GPL-3.0-or-later,
 * sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/screens/ExploreScreen.kt at edc1e6f.
 * Modified for Myspoti, 2026-10.
 */
package com.music.spotui.ui.immersive.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.myspoti.engine.R
import app.myspoti.host.android.MyspotiRuntime
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.music.spotui.data.api.Features
import com.music.spotui.ui.components.spotifyImageUrl
import com.music.spotui.ui.immersive.Immersive
import com.music.spotui.ui.immersive.components.immersiveClickable

/** Genre key of the "New releases" tile, opened in its own screen (same key as the Classic screen). */
internal const val ImmersiveNewReleasesGenre = "new-releases"

/** One "Browse all" tile: a category's title, colour and artwork. */
internal data class BrowseTile(val key: String, val title: String, val color: Color, val image: String)

/** The catalogue's categories, "New releases" first. Empty until they have loaded. */
@Composable
internal fun rememberBrowseTiles(): List<BrowseTile> {
    val newReleases = stringResource(R.string.browse_new_releases)
    val tiles by produceState(initialValue = emptyList<BrowseTile>()) {
        val categories = runCatching { MyspotiRuntime.api.home.categories() }.getOrDefault(emptyList())
        value = listOf(BrowseTile(ImmersiveNewReleasesGenre, newReleases, Color(0xFFE8115B), "")) +
            categories
                .filter { Features.PODCASTS || !it.title.contains("odcast", ignoreCase = true) }
                .map { c ->
                    val color = runCatching { Color(android.graphics.Color.parseColor(c.color ?: "#333333")) }
                        .getOrDefault(Color(0xFF333333))
                    BrowseTile(c.uri, c.title, color, c.images.firstOrNull()?.url.orEmpty())
                }
    }
    return tiles
}

/** A row of [columns] browse tiles; a short last row keeps its tiles the size of a full row's. */
@Composable
internal fun BrowseTileRow(
    row: List<BrowseTile>,
    columns: Int,
    onClick: (BrowseTile) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(TileSpacing),
        modifier = Modifier.padding(horizontal = Immersive.Gutter).padding(bottom = TileSpacing),
    ) {
        row.forEach { tile ->
            BrowseTileCard(
                tile = tile,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(TileAspect)
                    .clip(TileShape)
                    .immersiveClickable(onClick = { onClick(tile) }),
            )
        }
        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
internal fun BrowseTileCard(tile: BrowseTile, modifier: Modifier = Modifier) {
    val artwork = spotifyImageUrl(tile.image)
    Box(modifier = modifier.background(Immersive.scheme.secondaryContainer)) {
        if (artwork != null) {
            GlideImage(
                model = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                transition = CrossFade,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (artwork != null) Box(Modifier.matchParentSize().background(Brush.verticalGradient(
            0f to Color.Transparent,
            0.48f to Color.Black.copy(alpha = 0.12f),
            1f to Color.Black.copy(alpha = 0.72f),
        )))
        Text(
            text = tile.title,
            style = Immersive.Headline.copy(color = if (artwork == null) Immersive.scheme.onSecondaryContainer else Color.White, fontWeight = FontWeight.Bold),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

private val TileSpacing = 12.dp
private val TileShape = RoundedCornerShape(10.dp)
private const val TileAspect = 1.72f
