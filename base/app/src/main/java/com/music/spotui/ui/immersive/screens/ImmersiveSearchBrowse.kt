/*
 * Adapted from BitChord (https://github.com/kushagrasinghx/BitChord), GPL-3.0-or-later,
 * sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/screens/ExploreScreen.kt at edc1e6f.
 * Modified for Myspoti, 2026-10.
 */
package com.music.spotui.ui.immersive.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.myspoti.engine.R
import app.myspoti.host.android.MyspotiRuntime
import com.music.spotui.data.api.Features
import com.music.spotui.ui.immersive.Immersive

/** Genre key of the "New releases" tile, opened in its own screen (same key as the Classic screen). */
internal const val ImmersiveNewReleasesGenre = "new-releases"

/** One "Browse all" tile: a category's title, colour and artwork. */
internal data class BrowseTile(val key: String, val title: String, val image: String)

/** The catalogue's categories, "New releases" first. Empty until they have loaded. */
@Composable
internal fun rememberBrowseTiles(): List<BrowseTile> {
    val newReleases = stringResource(R.string.browse_new_releases)
    val tiles by produceState(initialValue = emptyList<BrowseTile>()) {
        val categories = runCatching { MyspotiRuntime.api.home.categories() }.getOrDefault(emptyList())
        value = listOf(BrowseTile(ImmersiveNewReleasesGenre, newReleases, "")) +
            categories
                .filter { Features.PODCASTS || !it.title.contains("odcast", ignoreCase = true) }
                .map { c -> BrowseTile(c.uri, c.title, c.images.firstOrNull()?.url.orEmpty()) }
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
            com.music.spotui.ui.immersive.components.ImmersiveSpotlightCard(
                title = tile.title,
                subtitle = null,
                imageUrl = tile.image,
                tone = tile.key.hashCode(),
                modifier = Modifier.weight(1f),
                onClick = { onClick(tile) },
            )
        }
        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
    }
}

private val TileSpacing = 12.dp
