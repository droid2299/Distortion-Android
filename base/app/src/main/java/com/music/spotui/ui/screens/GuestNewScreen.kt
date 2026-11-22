package com.music.spotui.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.myspoti.api.Album
import app.myspoti.engine.R
import com.music.spotui.data.catalogue.CatalogueEntry
import com.music.spotui.data.catalogue.GuestRelease
import com.music.spotui.data.catalogue.PublicCatalogueSession
import com.music.spotui.data.catalogue.guestRecentReleases
import com.music.spotui.data.local.GuestListening
import com.music.spotui.di.LocalPlayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.time.LocalDate

@Composable
fun GuestNewScreen(onLogin: () -> Unit, onSearch: () -> Unit) {
    val history by GuestListening.recent.collectAsState()
    var selected by remember { mutableStateOf<CatalogueEntry?>(null) }
    val active = selected
    if (active != null) {
        GuestCatalogueScreen(onLogin, initialEntry = active, onInitialBack = { selected = null })
    } else {
        // Only catalogue tracks have an artist identity we can reliably look up.
        val trackIds = history.map(LocalPlayer::spotifyIdOf).filter { Regex("[A-Za-z0-9]{22}").matches(it) }.distinct().take(5)
        GuestNewFeed(trackIds, onSearch, onOpen = { selected = it })
    }
}

@Composable
private fun GuestNewFeed(trackIds: List<String>, onSearch: () -> Unit, onOpen: (CatalogueEntry) -> Unit) {
    val context = LocalContext.current
    var attempt by remember { mutableIntStateOf(0) }
    val created = remember(attempt, trackIds.isNotEmpty()) {
        if (trackIds.isEmpty()) null else runCatching { PublicCatalogueSession(context) }
    }
    val session = created?.getOrNull()
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var releases by remember { mutableStateOf<List<GuestRelease>>(emptyList()) }

    LaunchedEffect(session, trackIds) {
        releases = emptyList()
        failed = created?.isFailure == true
        if (session == null) return@LaunchedEffect
        loading = true
        try {
            releases = withTimeout(25_000) {
                withContext(Dispatchers.IO) {
                    session.awaitReady()
                    var trackLookupSucceeded = false
                    val artistUris = trackIds.flatMap { id ->
                        try { session.catalog.track("spotify:track:$id").artists.map { it.uri }.also { trackLookupSucceeded = true } }
                        catch (e: CancellationException) { throw e }
                        catch (_: Exception) { emptyList() }
                    }.filter { Regex("spotify:artist:[A-Za-z0-9]{22}").matches(it) }.distinct().take(3)
                    if (!trackLookupSucceeded) error("Recent artists unavailable")
                    val albums = mutableListOf<Album>()
                    var succeeded = false
                    artistUris.forEach { uri ->
                        try { albums += session.catalog.discography(uri, 0, 20).items; succeeded = true }
                        catch (e: CancellationException) { throw e }
                        catch (_: Exception) { }
                    }
                    if (artistUris.isNotEmpty() && !succeeded) error("Artist releases unavailable")
                    guestRecentReleases(albums, LocalDate.now())
                }
            }
        } catch (e: CancellationException) {
            if (e is kotlinx.coroutines.TimeoutCancellationException) failed = true else throw e
        } catch (_: Exception) { failed = true }
        finally { loading = false }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(stringResource(R.string.bottom_tab_new), Modifier.padding(horizontal = 24.dp, vertical = 16.dp), style = MaterialTheme.typography.headlineLarge)
        if (session != null) key(session) {
            AndroidView(factory = { session.view }, modifier = Modifier.size(1.dp).alpha(0f), onReset = null, onRelease = { session.close() })
        }
        when {
            trackIds.isEmpty() -> GuestNewMessage(R.string.guest_new_empty, R.string.navigationbar_musicappitems_search_title, onSearch)
            loading -> CircularProgressIndicator(Modifier.padding(24.dp))
            failed -> GuestNewMessage(R.string.guest_new_failed, R.string.catalogue_retry) { attempt++ }
            releases.isEmpty() -> GuestNewMessage(R.string.guest_new_no_releases, R.string.navigationbar_musicappitems_search_title, onSearch)
            else -> LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                item { Text(stringResource(R.string.guest_new_from_listening), Modifier.padding(8.dp), style = MaterialTheme.typography.titleLarge) }
                items(releases, key = { it.entry.uri }) { release ->
                    CatalogueRow(release.entry.copy(subtitle = listOf(release.entry.subtitle, release.date).filter(String::isNotBlank).joinToString(" · "))) { onOpen(release.entry) }
                }
            }
        }
    }
}

@Composable
private fun GuestNewMessage(message: Int, action: Int, onClick: () -> Unit) {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(message), style = MaterialTheme.typography.bodyLarge)
        FilledTonalButton(onClick = onClick) { Text(stringResource(action)) }
    }
}
