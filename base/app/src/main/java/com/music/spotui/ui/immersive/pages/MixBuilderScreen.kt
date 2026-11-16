package com.music.spotui.ui.immersive.pages

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import app.myspoti.engine.R
import app.myspoti.host.android.MyspotiRuntime
import com.music.spotui.data.api.SpotifyPlaylistDraft
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.data.local.ListenHistory
import com.music.spotui.data.local.MixMode
import com.music.spotui.data.local.buildMix
import com.music.spotui.data.local.spotifyId
import com.music.spotui.data.preferences.HiddenSongs
import com.music.spotui.di.Graph
import com.music.spotui.di.SessionActions
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.immersive.components.ImmersiveListRow
import com.music.spotui.ui.immersive.components.ImmersiveMessageState
import com.music.spotui.ui.immersive.components.ImmersiveSearchField
import com.music.spotui.ui.immersive.components.SectionHeader
import com.music.spotui.ui.immersive.screens.ImmersiveChipRow
import com.music.spotui.ui.navigation.playlistRoute
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val MAX_SEEDS = 4

private data class MixSeed(val uri: String, val name: String, val subtitle: String, val image: String?, val song: SongsModel? = null)

private suspend fun searchSeeds(query: String): List<MixSeed>? = withContext(Dispatchers.IO) {
    if (!MyspotiRuntime.api.ready.value) return@withContext null
    recover {
        val found = MyspotiRuntime.api.search.all(query, limit = 8)
        found.artists.items.map { artist ->
            MixSeed(artist.uri, artist.name, "", artist.images.firstOrNull()?.url)
        } + found.tracks.items.map(Graph.api::songOf).filter { it.playable && it.spotifyTrackId.isNotBlank() }.map { song ->
            MixSeed("spotify:track:${song.spotifyTrackId}", song.title, song.singer, song.coverUri, song)
        }
    }?.distinctBy { it.uri }
}

private suspend fun generateMix(seeds: List<MixSeed>, mode: MixMode): List<SongsModel> = withContext(Dispatchers.IO) {
    val session = SessionActions.generation
    val anchorLists = seeds.map { seed ->
        check(session == SessionActions.generation)
        seed.song?.let(::listOf) ?: recover {
            MyspotiRuntime.api.catalog.artistTopTracks(seed.uri).map(Graph.api::songOf)
                .filter { it.playable && it.spotifyTrackId.isNotBlank() }.take(4)
        }.orEmpty()
    }
    val stations = anchorLists.map { tracks ->
        check(session == SessionActions.generation)
        tracks.firstOrNull()?.let { Graph.api.getRecommendations(listOf(it.spotifyTrackId)) }.orEmpty()
    }
    check(session == SessionActions.generation)
    val heard = ListenHistory.all().mapTo(hashSetOf()) { it.spotifyId() }
    buildMix(
        anchorLists.flatten().filterNot { HiddenSongs.isHidden(it.spotifyTrackId) },
        stations.map { tracks -> tracks.filterNot { HiddenSongs.isHidden(it.spotifyTrackId) } },
        heard, mode,
    )
}

internal suspend fun <T> recover(block: suspend () -> T): T? = try {
    block()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    null
}

@Composable
internal fun MixBuilderScreen(nav: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val account = com.music.spotui.data.api.ProfileCache.id
    val selected = remember(account) { mutableStateListOf<MixSeed>() }
    var query by remember { mutableStateOf("") }
    var adaptive by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var mode by remember { mutableStateOf(MixMode.BALANCED) }
    var result by remember(account) { mutableStateOf(emptyList<SongsModel>()) }
    var running by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }
    var title by remember { mutableStateOf(context.getString(R.string.app_mix_title)) }
    var draft by remember { mutableStateOf(SpotifyPlaylistDraft()) }
    val candidates by produceState<List<MixSeed>?>(emptyList(), query) {
        value = emptyList()
        if (query.trim().length < 2) return@produceState
        delay(300)
        value = withTimeoutOrNull(12_000) { searchSeeds(query.trim()) }
    }

    fun resetPreview() {
        result = emptyList()
        error = null
        draft = SpotifyPlaylistDraft()
    }

    androidx.activity.compose.BackHandler(enabled = adaptive) { adaptive = false }
    if (adaptive) {
        AdaptiveSetScreen(result, onBack = { adaptive = false })
        return
    }
    ImmersivePage(stringResource(R.string.app_mix_title), onBack = { if (!running) nav.popBackStack() }) {
        immersiveBlock("adaptive-set") {
            androidx.compose.material3.OutlinedButton(enabled = !running, onClick = { adaptive = true }) {
                Text(stringResource(R.string.app_set_title))
            }
        }
        immersiveBlock("seed-search") {
            ImmersiveSearchField(
                query = query, onQueryChange = { if (!running) query = it }, onSubmit = {},
                placeholder = stringResource(R.string.app_mix_search),
            )
        }
        if (selected.isNotEmpty()) item("picked") {
            ImmersiveChipRow {
                selected.forEach { seed ->
                    FilterChip(
                        selected = true, enabled = !running,
                        onClick = { selected.remove(seed); resetPreview() },
                        label = { Text(seed.name, maxLines = 1) },
                    )
                }
            }
        }
        if (candidates == null) item("search-error") {
            ImmersiveMessageState(stringResource(R.string.app_mix_search_failed))
        }
        items(candidates.orEmpty().filterNot { candidate -> selected.any { it.uri == candidate.uri } }, key = { "seed:${it.uri}" }) { seed ->
            ImmersiveListRow(
                seed.name, seed.subtitle, seed.image, round = seed.song == null,
                onClick = {
                    if (!running && selected.size < MAX_SEEDS) {
                        selected.add(seed)
                        query = ""
                        resetPreview()
                    }
                },
            )
        }
        item("mix-controls") {
            ImmersiveChipRow {
                MixMode.entries.forEach { option ->
                    FilterChip(
                        selected = mode == option, enabled = !running,
                        onClick = { mode = option; resetPreview() },
                        label = { Text(stringResource(when (option) {
                            MixMode.FAMILIAR -> R.string.app_mix_familiar
                            MixMode.BALANCED -> R.string.app_mix_balanced
                            MixMode.DISCOVER -> R.string.app_mix_discover
                        })) },
                    )
                }
            }
        }
        immersiveBlock("generate") {
            Button(enabled = !running && selected.isNotEmpty(), onClick = {
                val seeds = selected.toList()
                val chosenMode = mode
                resetPreview()
                running = true
                scope.launch {
                    try {
                        result = withTimeoutOrNull(35_000) { recover { generateMix(seeds, chosenMode) } }.orEmpty()
                        if (result.isEmpty()) error = R.string.app_mix_generate_failed
                    } finally {
                        running = false
                    }
                }
            }) {
                Text(stringResource(R.string.app_mix_generate))
            }
        }
        if (running) item("busy") { ImmersiveMessageState(stringResource(R.string.app_discovery_working)) }
        error?.let { message -> item("error") { ImmersiveMessageState(stringResource(message)) } }
        if (result.isNotEmpty()) {
            item("preview-header") { SectionHeader(stringResource(R.string.app_mix_preview)) }
            immersiveBlock("playlist-name") {
                OutlinedTextField(
                    title, { title = it.take(100) }, modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.app_discovery_playlist_name)) }, enabled = !running, singleLine = true,
                )
            }
            immersiveBlock("save") {
                Button(enabled = !running && title.isNotBlank(), onClick = {
                    running = true
                    error = null
                    val name = title.trim()
                    scope.launch {
                        try {
                            val uri = recover { draft.save(name, result.map { it.spotifyTrackId }) }
                            if (uri == null) error = R.string.app_discovery_save_failed
                            else nav.navigate(playlistRoute(uri.substringAfterLast(':'), name))
                        } finally {
                            running = false
                        }
                    }
                }) { Text(stringResource(R.string.app_discovery_save_playlist)) }
            }
            items(result, key = { "result:${it.spotifyTrackId}" }) { song ->
                ImmersiveListRow(
                    song.title, song.singer, song.coverUri, round = false,
                    onClick = { SongPlayer.playSong(song.url, context, songs = listOf(song), alone = true) },
                )
            }
        }
    }
}
