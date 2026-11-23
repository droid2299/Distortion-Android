package com.music.spotui.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.myspoti.engine.R
import com.music.spotui.data.catalogue.*
import com.music.spotui.data.local.GuestSearchHistory
import com.music.spotui.ui.immersive.components.ImmersiveCover
import com.music.spotui.ui.immersive.components.ImmersiveSearchField
import com.music.spotui.ui.immersive.screens.ImmersiveChip
import com.music.spotui.ui.immersive.screens.ImmersiveChipRow
import com.music.spotui.ui.immersive.Immersive
import com.music.spotui.ui.immersive.shell.ImmersiveLargeTitle
import com.music.spotui.data.local.GuestSavedLibrary

import kotlinx.coroutines.*

private val CataloguePathSaver = listSaver<List<CatalogueEntry>, String>(
    save = { path -> path.flatMap { listOf(it.uri, it.title, it.subtitle, it.image, it.kind.name) } },
    restore = { values -> values.chunked(5).mapNotNull { row ->
        if (row.size != 5) null else CatalogueKind.entries.find { it.name == row[4] }
            ?.let { CatalogueEntry(row[0], row[1], row[2], row[3], it) }
    } },
)

@Composable
fun GuestCatalogueScreen(onLogin: () -> Unit, releases: Boolean = false, initialEntry: CatalogueEntry? = null, onInitialBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val searchFocus = remember { FocusRequester() }
    var query by rememberSaveable { mutableStateOf("") }
    val history = remember(context) { GuestSearchHistory(context) }
    var recentQueries by remember { mutableStateOf(history.read()) }
    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val recognized = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty().take(200)
            if (recognized.isNotBlank()) { query = recognized; recentQueries = history.add(recognized) }
        }
    }
    var category by rememberSaveable { mutableIntStateOf(if (releases) CatalogueKind.ALBUM.ordinal else 0) }
    var path by rememberSaveable(stateSaver = CataloguePathSaver) { mutableStateOf(listOfNotNull(initialEntry)) }
    val saved by GuestSavedLibrary.store.entries.collectAsState()
    LaunchedEffect(Unit) { GuestSavedLibrary.store.load() }
    var attempt by remember { mutableIntStateOf(0) }
    val kind = CatalogueKind.entries[category.coerceIn(CatalogueKind.entries.indices)]
    val creation = remember(attempt) {
        try { Result.success(PublicCatalogueSession(context)) } catch (error: Exception) { Result.failure(error) }
    }
    val session = creation.getOrNull()
    val searchCache = remember(session) { android.util.LruCache<Pair<String, CatalogueKind>, CataloguePage>(8) }
    val detailCache = remember(session) { android.util.LruCache<String, CatalogueDetails>(6) }
    var page by remember { mutableStateOf<CataloguePage?>(null) }
    var detail by remember { mutableStateOf<CatalogueDetails?>(null) }
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var moreFailed by remember { mutableStateOf(false) }
    var moreJob by remember { mutableStateOf<Job?>(null) }
    var playJob by remember { mutableStateOf<Job?>(null) }
    var addTrack by remember { mutableStateOf<com.music.spotui.data.local.LocalTrack?>(null) }
    addTrack?.let { track -> AddLocalToPlaylistSheet(track) { addTrack = null } }
    val scope = rememberCoroutineScope()
    val selected = path.lastOrNull()
    val key = query.trim() to kind

    fun playTracks(entries: List<CatalogueEntry>, picked: CatalogueEntry) {
        val songs = guestPlaybackSongs(entries)
        val song = songs.firstOrNull { it.url == picked.uri } ?: return
        playJob?.cancel()
        playJob = scope.launch {
            val played = try {
                val metadata = try { withContext(Dispatchers.IO) { session?.catalog?.track(picked.uri) } }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { null }
                ensureActive()
                val enriched = if (metadata == null) song else song.copy(
                    album = metadata.album?.name.orEmpty(),
                    durationMs = metadata.durationMs.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
                    explicit = metadata.explicit,
                    albumId = metadata.album?.uri?.substringAfterLast(':').orEmpty(),
                )
                val queue = songs.map { if (it.url == enriched.url) enriched else it }
                com.music.spotui.extensions.ExtensionAudio.playList(queue, enriched, "", false, isLatest = { isActive })
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { false }
            if (!played && isActive) android.widget.Toast.makeText(context, R.string.guest_playback_sign_in, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun back() {
        if (initialEntry != null && path.size == 1 && onInitialBack != null) onInitialBack()
        else path = path.dropLast(1)
    }
    BackHandler(path.isNotEmpty()) { back() }
    LaunchedEffect(key, path, session) {
        moreJob?.cancel()
        loadingMore = false
        moreFailed = false
        failed = false
        loading = false
        if (session == null) return@LaunchedEffect
        try {
            if (selected == null) {
                detail = null
                page = null
                if (key.first.length < 2) return@LaunchedEffect
                searchCache.get(key)?.let { page = it; return@LaunchedEffect }
                loading = true
                delay(400)
                val result = session.searchPage(kind, key.first)
                ensureActive()
                val unique = result.copy(entries = result.entries.distinctBy { it.uri })
                page = unique
                searchCache.put(key, unique)
            } else {
                detail = null
                detailCache.get(selected.uri)?.let { detail = it; return@LaunchedEffect }
                loading = true
                val result = session.details(selected)
                ensureActive()
                detail = result
                detailCache.put(selected.uri, result)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            ensureActive()
            app.myspoti.host.android.Logbook.log("public catalogue read failed: ${error.javaClass.simpleName}")
            failed = true
        } finally { if (isActive) loading = false }
    }

    fun loadMore() {
        if (session == null || loadingMore) return
        val offset = (if (selected == null) page?.nextOffset else detail?.nextOffset)?.takeIf { it < 500 } ?: return
        val expectedPath = path
        val expectedKey = key
        loadingMore = true
        moreFailed = false
        moreJob = scope.launch {
            try {
                val next = if (selected == null) session.searchPage(kind, expectedKey.first, offset) else session.moreTracks(selected, offset)
                ensureActive()
                if (path != expectedPath || (query.trim() to CatalogueKind.entries[category]) != expectedKey) return@launch
                if (selected == null) {
                    val merged = CataloguePage((page.orEmptyEntries() + next.entries).distinctBy { it.uri }.take(500), next.nextOffset)
                    page = merged
                    searchCache.put(expectedKey, merged)
                } else {
                    val current = detail ?: return@launch
                    val merged = current.copy(sections = current.sections.map { section ->
                        if (section.kind == CatalogueSectionKind.SONGS) section.copy(entries = (section.entries + next.entries).take(500)) else section
                    }, nextOffset = next.nextOffset)
                    detail = merged
                    detailCache.put(selected.uri, merged)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { ensureActive(); moreFailed = true }
            finally { if (isActive) loadingMore = false }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        val pageTitle = stringResource(if (releases) R.string.bottom_tab_new else R.string.navigationbar_musicappitems_search_title)
        if (selected == null) ImmersiveLargeTitle(pageTitle)
        else Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { back() }) {

                Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.catalogue_back))
            }
            Text(pageTitle, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
        }
        if (session != null) key(session) {
            AndroidView(factory = { session.view }, modifier = Modifier.size(1.dp).alpha(0f), onReset = null, onRelease = { session.close() })
        }
        if (selected == null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = Immersive.Gutter), verticalAlignment = Alignment.CenterVertically) {
                ImmersiveSearchField(
                    query = query, onQueryChange = { query = it.take(200) },
                    onSubmit = { recentQueries = history.add(query); keyboard?.hide() },
                    placeholder = stringResource(R.string.search_header_field_hint),
                    focusRequester = searchFocus, modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    try {
                        voice.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM))
                    } catch (_: ActivityNotFoundException) {
                        android.widget.Toast.makeText(context, R.string.guest_voice_unavailable, android.widget.Toast.LENGTH_SHORT).show()
                    } catch (_: SecurityException) {
                        android.widget.Toast.makeText(context, R.string.guest_voice_unavailable, android.widget.Toast.LENGTH_SHORT).show()
                    }
                }) { Icon(Icons.Rounded.Mic, stringResource(R.string.guest_voice_search)) }
            }
            if (query.isNotBlank()) ImmersiveChipRow {
                CatalogueKind.entries.forEachIndexed { index, entry ->
                    ImmersiveChip(catalogueKindLabel(entry), selected = category == index) { category = index }
                }
            }
        }
        when {
            session == null -> CatalogueMessage(if (creation.exceptionOrNull() is UnsupportedOperationException) R.string.catalogue_webview_required else R.string.catalogue_failed) { attempt++ }
            loading -> Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Text(stringResource(R.string.catalogue_loading), Modifier.padding(top = 16.dp))
            }
            failed -> CatalogueMessage(R.string.catalogue_failed) { attempt++ }
            selected == null && key.first.length < 2 -> Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Immersive.Gutter, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (recentQueries.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.guest_recent_searches), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { recentQueries = history.clear() }) { Text(stringResource(R.string.guest_clear_searches)) }
                    }
                    recentQueries.forEach { term ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(term, Modifier.weight(1f).clickable { query = term; recentQueries = history.add(term); keyboard?.hide() }.padding(vertical = 12.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            IconButton(onClick = { recentQueries = history.remove(term) }) { Icon(Icons.Rounded.Delete, stringResource(R.string.guest_remove_search, term)) }
                        }
                    }
                }
                Text(stringResource(R.string.browse_all), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.catalogue_intro), style = MaterialTheme.typography.bodyMedium)
                CatalogueKind.entries.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { entry ->
                            ElevatedCard(onClick = { category = entry.ordinal; searchFocus.requestFocus(); keyboard?.show() }, modifier = Modifier.weight(1f)) {
                                Box(Modifier.fillMaxWidth().height(90.dp).padding(16.dp), contentAlignment = Alignment.CenterStart) {
                                    Text(catalogueKindLabel(entry), style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            selected == null && page?.entries.isNullOrEmpty() -> Text(stringResource(R.string.catalogue_empty), Modifier.padding(24.dp))
            else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
                if (selected != null && detail != null) {
                    val current = checkNotNull(detail)
                    item(key = "hero") {
                        Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ImmersiveCover(current.entry.image, Modifier.size(180.dp), shape = if (current.entry.kind == CatalogueKind.ARTIST) CircleShape else RoundedCornerShape(24.dp))
                            Text(current.entry.title, style = MaterialTheme.typography.headlineMedium)
                            if (current.entry.subtitle.isNotBlank()) Text(current.entry.subtitle, style = MaterialTheme.typography.bodyLarge)
                            Text(stringResource(R.string.guest_playback_description), style = MaterialTheme.typography.bodyMedium)
                            val isSaved = saved.any { it.uri == current.entry.uri }
                            OutlinedButton(onClick = {
                                scope.launch {
                                    val ok = GuestSavedLibrary.store.setSaved(current.entry, !isSaved)
                                    if (!ok) android.widget.Toast.makeText(context, R.string.guest_save_failed, android.widget.Toast.LENGTH_LONG).show()
                                }
                            }) { Text(stringResource(if (isSaved) R.string.guest_remove_saved else R.string.guest_save)) }
                            val playable = if (current.entry.kind == CatalogueKind.TRACK) listOf(current.entry) else current.sections.firstOrNull { it.kind == CatalogueSectionKind.SONGS }?.entries.orEmpty()
                            if (playable.isNotEmpty()) FilledTonalButton(onClick = { playTracks(playable, playable.first()) }) { Text(stringResource(R.string.guest_playback_extension)) }
                            if (current.entry.kind == CatalogueKind.TRACK) {
                                guestPlaybackSongs(listOf(current.entry)).firstOrNull()?.let { song ->
                                    OutlinedButton(onClick = { addTrack = com.music.spotui.extensions.ExtensionAudio.localTrack(song) }) {
                                        Text(stringResource(R.string.context_menu_add_to_playlist))
                                    }
                                }
                            }
                            Button(onClick = onLogin) { Text(stringResource(R.string.guest_connect)) }
                        }
                    }
                    current.sections.filter { it.entries.isNotEmpty() }.forEach { section ->
                        item(key = "section-${section.kind}") { Text(catalogueSectionLabel(section.kind), Modifier.padding(horizontal = 8.dp, vertical = 12.dp), style = MaterialTheme.typography.titleLarge) }
                        itemsIndexed(section.entries, key = { index, entry -> "${section.kind}:${entry.uri}:$index" }) { _, entry ->
                            CatalogueRow(entry) { keyboard?.hide(); if (entry.kind == CatalogueKind.TRACK) playTracks(section.entries, entry) else path = (path + entry).takeLast(20) }
                        }
                    }
                } else {
                    itemsIndexed(page.orEmptyEntries(), key = { _, entry -> entry.uri }) { _, entry ->
                        CatalogueRow(entry) { recentQueries = history.add(query); keyboard?.hide(); path = listOf(entry) }
                    }
                }
                val next = if (selected == null) page?.nextOffset else detail?.nextOffset
                if (next != null && next < 500) item(key = "more") {
                    TextButton(onClick = ::loadMore, enabled = !loadingMore, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(if (loadingMore) R.string.catalogue_loading else if (moreFailed) R.string.catalogue_retry else R.string.catalogue_more))
                    }
                }
            }
        }
    }
}

internal fun guestPlaybackSongs(entries: List<CatalogueEntry>): List<com.music.spotui.data.entity.SongsModel> {
    val validTrack = Regex("spotify:track:[A-Za-z0-9]{22}")
    return entries.filter { it.kind == CatalogueKind.TRACK && validTrack.matches(it.uri) }.map { entry ->
        com.music.spotui.data.entity.SongsModel(
            id = 0, title = entry.title, album = "", singer = entry.subtitle,
            coverUri = entry.image, url = entry.uri, spotifyTrackId = entry.uri.substringAfterLast(':'),
        )
    }
}

private fun CataloguePage?.orEmptyEntries() = this?.entries.orEmpty()

@Composable
private fun CatalogueMessage(message: Int, retry: () -> Unit) {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(message), style = MaterialTheme.typography.bodyLarge)
        OutlinedButton(onClick = retry) { Text(stringResource(R.string.catalogue_retry)) }
    }
}

@Composable
internal fun CatalogueRow(entry: CatalogueEntry, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(entry.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(entry.subtitle.ifBlank { catalogueKindLabel(entry.kind) }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = { ImmersiveCover(entry.image, Modifier.size(56.dp), shape = if (entry.kind == CatalogueKind.ARTIST) CircleShape else RoundedCornerShape(12.dp)) },
        colors = ListItemDefaults.colors(containerColor = Immersive.Page),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}

@Composable
private fun catalogueKindLabel(kind: CatalogueKind): String = stringResource(when (kind) {
    CatalogueKind.TRACK -> R.string.catalogue_songs
    CatalogueKind.ALBUM -> R.string.catalogue_albums
    CatalogueKind.ARTIST -> R.string.catalogue_artists
    CatalogueKind.PLAYLIST -> R.string.catalogue_playlists
})

@Composable
private fun catalogueSectionLabel(kind: CatalogueSectionKind): String = stringResource(when (kind) {
    CatalogueSectionKind.SONGS -> R.string.catalogue_songs
    CatalogueSectionKind.ALBUMS -> R.string.catalogue_albums
    CatalogueSectionKind.ARTISTS -> R.string.catalogue_artists
    CatalogueSectionKind.RELATED_ARTISTS -> R.string.catalogue_related
})
