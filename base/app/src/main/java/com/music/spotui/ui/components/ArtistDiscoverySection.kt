package com.music.spotui.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import app.myspoti.api.best
import app.myspoti.engine.R
import com.music.spotui.data.api.Response
import com.music.spotui.ui.immersive.components.ImmersiveCover
import com.music.spotui.ui.navigation.artistRoute
import com.music.spotui.ui.navigation.playlistRoute
import com.music.spotui.ui.viewmodel.ArtistDiscoveryViewModel

@Composable
fun ArtistDiscoverySection(artistId: String, relatedAlreadyShown: Boolean, navController: NavController) {
    if (artistId.isBlank()) return
    val uri = "spotify:artist:$artistId"
    val model: ArtistDiscoveryViewModel = viewModel(key = "artist-discovery-$artistId")
    val playlists by model.playlists.collectAsState()
    val related by model.related.collectAsState()
    LaunchedEffect(uri, relatedAlreadyShown) { model.load(uri, includeRelated = !relatedAlreadyShown) }
    Column(Modifier.fillMaxWidth().padding(vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.artist_discovered_on), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.artist_discovered_on_detail), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        when (val state = playlists) {
            is Response.Loading -> DiscoveryLoading()
            is Response.Error -> DiscoveryFailure(state.error) { model.retryPlaylists(uri) }
            is Response.Success -> {
                val available = state.data.filterNotNull()
                if (available.isEmpty()) Text(stringResource(R.string.artist_discovery_empty), Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.bodyMedium)
                else LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(available, key = { index, playlist -> "${playlist.uri}:$index" }) { _, playlist ->
                        DiscoveryCard(playlist.name, playlist.owner.orEmpty(), playlist.images.best(512)?.url.orEmpty(), false) {
                            navController.navigate(playlistRoute(playlist.uri.substringAfterLast(':'), playlist.name))
                        }
                    }
                }
                if (state.data.any { it == null }) Text(stringResource(R.string.artist_discovery_unavailable_slots), Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (!relatedAlreadyShown) {
            Text(stringResource(R.string.fans_also_like_section_title), Modifier.padding(start = 20.dp, top = 12.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            when (val state = related) {
                is Response.Loading -> DiscoveryLoading()
                is Response.Error -> DiscoveryFailure(state.error) { model.retryRelated(uri) }
                is Response.Success -> {
                    if (state.data.isEmpty()) Text(stringResource(R.string.artist_related_empty), Modifier.padding(horizontal = 20.dp))
                    else LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(state.data, key = { index, artist -> "${artist.uri}:$index" }) { _, artist ->
                            DiscoveryCard(artist.name, stringResource(R.string.artist_subtitle), artist.images.best(512)?.url.orEmpty(), true) {
                                navController.navigate(artistRoute(artist.name, artist.uri.substringAfterLast(':')))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoveryLoading() {
    Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun DiscoveryFailure(reason: String, retry: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(if (reason == "unsupported") R.string.artist_discovery_unsupported else R.string.artist_discovery_failed), style = MaterialTheme.typography.bodyMedium)
        if (reason != "unsupported") OutlinedButton(onClick = retry) { Text(stringResource(R.string.retry_button)) }
    }
}

@Composable
private fun DiscoveryCard(title: String, subtitle: String, image: String, round: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.width(164.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ImmersiveCover(image, Modifier.fillMaxWidth().aspectRatio(1f), shape = if (round) CircleShape else RoundedCornerShape(16.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
