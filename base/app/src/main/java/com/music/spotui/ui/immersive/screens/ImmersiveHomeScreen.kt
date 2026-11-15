/*
 * Adapted from BitChord (https://github.com/kushagrasinghx/BitChord), GPL-3.0-or-later,
 * sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/screens/HomeScreen.kt at edc1e6f.
 * Modified for Myspoti, 2026-10.
 */
package com.music.spotui.ui.immersive.screens

import android.os.SystemClock
import android.widget.Toast
import app.myspoti.api.RecentItem
import app.myspoti.host.android.MyspotiRuntime
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.text.style.TextOverflow
import com.music.spotui.ui.immersive.components.SectionHeader
import com.music.spotui.ui.immersive.pages.openRecentContext
import com.music.spotui.ui.navigation.PlayerSheet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.music.spotui.ui.immersive.components.expressivePress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import app.myspoti.engine.R
import com.music.spotui.data.api.Api
import com.music.spotui.data.api.Features
import com.music.spotui.data.api.ProfileCache
import com.music.spotui.data.api.Response
import com.music.spotui.data.entity.HomeFeedModel
import com.music.spotui.data.entity.HomeItem
import com.music.spotui.data.entity.HomeSection
import com.music.spotui.data.entity.withLikedFirst
import com.music.spotui.data.entity.withoutDj
import com.music.spotui.di.Graph
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.components.pressHint
import com.music.spotui.ui.immersive.Immersive
import com.music.spotui.ui.immersive.components.ImmersiveCover
import com.music.spotui.ui.immersive.components.ImmersiveMessageState
import com.music.spotui.ui.immersive.components.ImmersiveShelf
import com.music.spotui.ui.immersive.components.ImmersivePlayingBars
import com.music.spotui.ui.immersive.components.ImmersiveSpotlightCard
import com.music.spotui.ui.immersive.components.immersiveTonalColors
import com.music.spotui.ui.immersive.components.immersiveClickable
import com.music.spotui.ui.immersive.components.immersiveFeedSkeleton
import com.music.spotui.ui.navigation.Routes
import com.music.spotui.ui.navigation.TabReselect
import com.music.spotui.ui.navigation.albumRoute
import com.music.spotui.ui.navigation.artistRoute
import com.music.spotui.ui.navigation.playlistRoute
import com.music.spotui.ui.navigation.showRoute
import com.music.spotui.ui.screens.ContextViewModel
import com.music.spotui.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.firstOrNull
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch

@Composable
fun ImmersiveHomeScreen(navController: NavController) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        ImmersiveHomeFeed(navController, maxWidth)
    }
}

@Composable
private fun ImmersiveHomeFeed(navController: NavController, pageWidth: Dp) {
    val homeViewModel: HomeViewModel = viewModel()
    val home by homeViewModel.home.collectAsState()
    val feed = (home as? Response.Success)?.data
    val context = LocalContext.current
    LaunchedEffect(Unit) { ProfileCache.ensure(context) }
    // An old feed is read again when Home shows again (see HomeViewModel.refreshIfStale).
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) { homeViewModel.refreshIfStale() }

    // Re-tapping the Home tab while on Home scrolls back to the top, as in the Classic screen.
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        TabReselect.home.collect {
            if (listState.firstVisibleItemIndex != 0 || listState.firstVisibleItemScrollOffset != 0) {
                listState.animateScrollToItem(0)
            }
        }
    }

    var showProfile by remember { mutableStateOf(false) }
    if (showProfile) {
        ImmersiveProfileSheet(
            onOpen = { route -> showProfile = false; navController.navigate(route) },
            onDismiss = { showProfile = false },
        )
    }

    // The context playing right now (empty while paused), derived once for every card: a card
    // only recomposes when its own answer flips, not on every change of the player's state.
    val playingContext = remember {
        derivedStateOf { if (Graph.currentSongState.playingState.value) SongPlayer.nowPlaying.value.contextUri else "" }
    }

    // Podcasts can be off: their shows and episodes then leave the feed.
    val hideDj = com.music.spotui.data.preferences.HideDjPref.hidden
    val sections = remember(feed, hideDj) { feed?.let { visibleSections(it) }.orEmpty().let { if (hideDj) it.withoutDj() else it } }
    // Only the untitled section the web home starts with is a shortcut grid; a titled lead
    // section is a carousel like any other.
    val gridSection = sections.firstOrNull()?.takeIf { it.title.isBlank() }
    val carousels = remember(sections, gridSection) { if (gridSection != null) sections.drop(1) else sections }
    // Liked Songs leads the shortcuts when Spotify's grid does not carry it.
    val likedName = stringResource(R.string.collection_liked_songs_title)
    val shortcuts = remember(gridSection, likedName) { gridSection?.items?.withLikedFirst(likedName) }
    // The shortcuts at the top load in the background, ready before a tap.
    LaunchedEffect(shortcuts?.map { it.name }) {
        val top = shortcuts.orEmpty().mapNotNull { item ->
            when (item) {
                is HomeItem.Album -> item.id.takeIf { it.isNotBlank() }?.let { "album" to it }
                is HomeItem.Playlist -> if (item.id == Api.HomeCache.LIKED_SONGS_ID) "liked" to "" else item.id.takeIf { it.isNotBlank() }?.let { "playlist" to it }
                else -> null
            }
        }
        if (top.isNotEmpty()) ContextViewModel.prefetchSoon(top)
    }

    val mix = remember(carousels) {
        carousels.firstNotNullOfOrNull { section ->
            section.items.filterIsInstance<HomeItem.Playlist>()
                .firstOrNull { it.id.isNotBlank() && it.id != Api.HomeCache.LIKED_SONGS_ID }
        }
    }
    val artwork = remember(mix, carousels) {
        (listOfNotNull(mix) + carousels.asSequence().flatMap { it.items.asSequence() }
            .filter { it is HomeItem.Playlist || it is HomeItem.Album }.take(12).toList())
            .filter { it.imageUrl.isNotBlank() }
            .distinctBy { it.imageUrl }
            .take(3)
    }
    val recent = rememberHomeRecents()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(top = topInset + 12.dp, bottom = Immersive.BottomChrome),
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        item(key = "home:actions") {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProfileDisc(onClick = { showProfile = true })
                FilledTonalIconButton(onClick = { navController.navigate(Routes.WhatsNew.route) }) {
                    Icon(Icons.Rounded.Newspaper, stringResource(R.string.whats_new_title))
                }
                FilledTonalIconButton(onClick = { navController.navigate(Routes.Settings.route) }) {
                    Icon(Icons.Rounded.Settings, stringResource(R.string.settings_title))
                }
            }
        }
        when {
            sections.isNotEmpty() -> {
                if (mix != null) {
                    item(key = "home:mix") {
                        HomeMixFeature(navController, mix, artwork, playingContext)
                    }
                }
                item(key = "home:continue") { HomeContinueListening() }
                shortcuts?.takeIf { it.isNotEmpty() }?.let { tiles ->
                    item(key = "home:shortcuts", contentType = "home:shortcuts") {
                        HomeQuickAccess(navController, tiles, playingContext)
                    }
                }
                if (recent.isNotEmpty()) {
                    item(key = "home:recents", contentType = "home:recents") {
                        HomeRecentlyPlayed(navController, recent)
                    }
                }
                carousels.forEachIndexed { index, section ->
                    item(key = "home:section:$index:${section.title}", contentType = "home:recommendations") {
                        HomeRecommendationSection(navController, section, index, mix, playingContext, pageWidth)
                    }
                }
                item(key = "home:extensions") {
                    com.music.spotui.extensions.ExtensionBlocks(app.myspoti.extensions.Hook.HOME, app.myspoti.extensions.Surface.HOME, "{}")
                }
            }
            home is Response.Loading -> immersiveFeedSkeleton(firstIsHero = false)
            else -> item(key = "home:message") {
                ImmersiveMessageState(
                    message = stringResource(if (home is Response.Error) R.string.home_error else R.string.home_empty),
                    actionLabel = if (home is Response.Error) stringResource(R.string.home_retry) else null,
                    onAction = if (home is Response.Error) ({ homeViewModel.retry() }) else null,
                )
            }
        }
    }
}

@Composable
private fun rememberHomeRecents(): List<RecentItem> {
    var recent by remember { mutableStateOf(emptyList<RecentItem>()) }
    LaunchedEffect(Unit) {
        var lastRequest: Long? = null
        snapshotFlow { SongPlayer.nowPlaying.value.uri }.conflate().collect {
            lastRequest?.let { previous ->
                val remaining = 30_000L - (SystemClock.elapsedRealtime() - previous)
                kotlinx.coroutines.delay(remaining.coerceAtLeast(1_000L))
            }
            val session = com.music.spotui.di.SessionActions.generation
            lastRequest = SystemClock.elapsedRealtime()
            try {
                val loaded = withContext(Dispatchers.IO) {
                    MyspotiRuntime.api.recents.recentEntries(0, 40).items.asSequence()
                        .filterNot { it.saved }
                        .map { it.item }
                        .filter { it.uri.isNotBlank() && it.title.isNotBlank() }
                        .distinctBy { it.uri }
                        .take(8)
                        .toList()
                }
                if (session == com.music.spotui.di.SessionActions.generation) recent = loaded
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
            }
        }
    }
    return recent
}

@Composable
private fun HomeContinueListening() {
    val song = Graph.currentSongState
    val title = song.title.value
    if (title.isBlank()) return
    val playing = song.playingState.value
    Column(Modifier.padding(bottom = 12.dp)) {
        SectionHeader(stringResource(R.string.home_continue_listening))
        Surface(
            onClick = { PlayerSheet.show() },
            modifier = Modifier.fillMaxWidth().padding(horizontal = Immersive.Gutter),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ImmersiveCover(song.coverUri.value, Modifier.size(52.dp), border = false)
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.singer.value, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FilledTonalIconButton(onClick = { if (playing) SongPlayer.pause() else SongPlayer.play() }) {
                    Icon(
                        if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        stringResource(if (playing) R.string.immersive_player_pause else R.string.immersive_player_play),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeMixFeature(
    navController: NavController,
    mix: HomeItem.Playlist,
    artwork: List<HomeItem>,
    playingContext: State<String>,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val playback: ContextViewModel = viewModel()
    val scheme = MaterialTheme.colorScheme
    val mixInteraction = remember { MutableInteractionSource() }
    var loading by remember(mix.id) { mutableStateOf(false) }
    val playing = mix.isPlayingIn(playingContext)
    val unavailable = stringResource(R.string.home_mix_unavailable)

    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val heroWidth = minOf(maxWidth, 540.dp)
            val titleSize = (heroWidth.value * 0.175f).coerceIn(38f, 84f)
            val playSize = (heroWidth * 0.30f).coerceIn(72.dp, 118.dp)
            Column(Modifier.width(heroWidth).align(Alignment.TopCenter)) {
                Box(Modifier.fillMaxWidth().height(heroWidth * 0.66f)) {
                    Column(
                        Modifier.align(Alignment.CenterStart).padding(bottom = 16.dp)
                            .width(heroWidth - playSize - 8.dp)
                            .immersiveClickable(onClick = { onHomeItemClick(navController, mix) }),
                    ) {
                        Text(
                            stringResource(R.string.home_your_mix),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = titleSize.sp,
                                lineHeight = (titleSize * 0.94f).sp,
                                fontWeight = FontWeight.Bold,
                                textGeometricTransform = TextGeometricTransform(scaleX = 1.08f),
                            ),
                            color = scheme.onBackground,
                        )
                        Text(
                            mix.name,
                            modifier = Modifier.padding(top = 10.dp),
                            style = MaterialTheme.typography.titleSmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                    FilledIconButton(
                        enabled = !loading,
                        onClick = {
                            when {
                                playing -> SongPlayer.pause()
                                SongPlayer.nowPlaying.value.contextUri == mix.contextUri() -> SongPlayer.play()
                                else -> {
                                    loading = true
                                    scope.launch {
                                        try {
                                            val result = Graph.api.getPlaylistPage(mix.id).firstOrNull {
                                                it is Response.Error || it is Response.Success && (it.data.tracks.isNotEmpty() || it.data.complete)
                                            }
                                            val page = (result as? Response.Success)?.data
                                            val first = page?.tracks?.firstOrNull()
                                            if (page != null && first != null) playback.play(page, first)
                                            else Toast.makeText(context, unavailable, Toast.LENGTH_SHORT).show()
                                        } catch (cancelled: CancellationException) {
                                            throw cancelled
                                        } catch (_: Exception) {
                                            Toast.makeText(context, unavailable, Toast.LENGTH_SHORT).show()
                                        } finally {
                                            loading = false
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomEnd).size(playSize).expressivePress(mixInteraction, enabled = !loading),
                        interactionSource = mixInteraction,
                        shape = CircleShape,
                    ) {
                        if (loading) {
                            CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                        } else {
                            Icon(
                                if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                stringResource(if (playing) R.string.immersive_player_pause else R.string.home_play_mix),
                                Modifier.size(playSize * 0.34f),
                            )
                        }
                    }
                }
                if (artwork.isNotEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(top = 38.dp).aspectRatio(1.48f)) {
                        MixArtwork(
                            artwork.first(), navController,
                            Modifier.align(Alignment.Center).size(heroWidth * 0.48f, heroWidth * 0.70f).rotate(39f),
                        )
                        artwork.getOrNull(1)?.let { item ->
                            MixArtwork(item, navController, Modifier.align(Alignment.TopStart).offset(y = 8.dp).size(heroWidth * 0.23f))
                        }
                        artwork.getOrNull(2)?.let { item ->
                            MixArtwork(item, navController, Modifier.align(Alignment.BottomEnd).offset(y = (-4).dp).size(heroWidth * 0.23f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MixArtwork(item: HomeItem, navController: NavController, modifier: Modifier) {
    ImmersiveCover(
        url = item.imageUrl,
        modifier = modifier.clip(CircleShape).pressHint { prefetchHomeItem(item) }
            .immersiveClickable(onClick = { onHomeItemClick(navController, item) }),
        shape = CircleShape,
        contentDescription = item.name,
        border = false,
    )
}

private fun visibleSections(feed: HomeFeedModel): List<HomeSection> =
    if (Features.PODCASTS) feed.sections
    else feed.sections.map { s -> s.copy(items = s.items.filterNot { it is HomeItem.Show }) }.filter { it.items.isNotEmpty() }

/** Albums and playlists start loading under the finger. */
private fun prefetchHomeItem(item: HomeItem) {
    when (item) {
        is HomeItem.Album -> if (item.id.isNotBlank()) ContextViewModel.prefetch("album", item.id)
        is HomeItem.Playlist ->
            if (item.id == Api.HomeCache.LIKED_SONGS_ID) ContextViewModel.prefetch("liked", "")
            else if (item.id.isNotBlank()) ContextViewModel.prefetch("playlist", item.id)
        else -> {}
    }
}

private fun onHomeItemClick(navController: NavController, item: HomeItem) {
    when (item) {
        is HomeItem.Album -> navController.navigate(albumRoute(item.name, item.artists.ifBlank { item.subtitle }, item.id))
        is HomeItem.Artist -> navController.navigate(artistRoute(item.name, item.id))
        is HomeItem.Show ->
            if (item.isEpisode) SongPlayer.playSong("episode:${item.id}")
            else navController.navigate(showRoute(item.id, item.name))
        // The real playlist content by its id (daily mixes, etc).
        is HomeItem.Playlist ->
            if (item.id == Api.HomeCache.LIKED_SONGS_ID) navController.navigate(Routes.Liked.route)
            else if (item.id.isNotBlank()) navController.navigate(playlistRoute(item.id, item.name))
            else navController.navigate(albumRoute(item.name))
    }
}

/** The uri of the thing this card opens, to tell whether it is what is playing. */
private fun HomeItem.contextUri(): String = when (this) {
    is HomeItem.Album -> if (id.isNotBlank()) "spotify:album:$id" else ""
    is HomeItem.Playlist -> if (id.isNotBlank()) "spotify:playlist:$id" else ""
    is HomeItem.Artist -> if (id.isNotBlank()) "spotify:artist:$id" else ""
    is HomeItem.Show -> if (id.isNotBlank() && !isEpisode) "spotify:show:$id" else ""
}

/** True while this card's album, playlist or artist is the playing context, [playingContext]. */
@Composable
private fun HomeItem.isPlayingIn(playingContext: State<String>): Boolean {
    val uri = contextUri()
    if (uri.isEmpty()) return false
    val playing by remember(uri, playingContext) { derivedStateOf { playingContext.value == uri } }
    return playing
}

/**
 * Keys for the cards of a shelf: the uri they open, or their kind and name for those with none
 * (an episode). A repeat within one shelf gets a suffix, as a lazy list refuses two equal keys.
 */
private fun homeItemKeys(items: List<HomeItem>): List<String> {
    val seen = HashMap<String, Int>()
    return items.map { item ->
        val base = item.contextUri().ifEmpty { "${item::class.simpleName}:${item.name}:${item.imageUrl}" }
        val count = seen.merge(base, 1, Int::plus) ?: 1
        if (count == 1) base else "$base#$count"
    }
}

@Composable
private fun HomeItem.cardSubtitle(): String = when (this) {
    is HomeItem.Album -> subtitle
    is HomeItem.Playlist -> subtitle
    is HomeItem.Artist -> stringResource(R.string.artist_subtitle)
    is HomeItem.Show -> subtitle
}

@Composable
private fun HomeRecentlyPlayed(navController: NavController, recent: List<RecentItem>) {
    val stacks = remember(recent) { recent.chunked(2) }
    ImmersiveShelf(
        title = stringResource(R.string.home_recently_played),
        onSeeAll = { navController.navigate(Routes.Recent.route) },
    ) {
        items(stacks, key = { it.first().uri }) { entries ->
            Column(Modifier.width(252.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                entries.forEachIndexed { index, entry ->
                    HomeListeningTile(
                        title = entry.title,
                        subtitle = entry.subtitle,
                        imageUrl = entry.images.firstOrNull()?.url,
                        tone = index + 1,
                        onClick = { openRecentContext(entry, navController) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeQuickAccess(navController: NavController, tiles: List<HomeItem>, playingContext: State<String>) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(bottom = 26.dp)) {
        val columns = (maxWidth.value / 160f).toInt().coerceIn(2, 4)
        Column {
            SectionHeader(stringResource(R.string.home_quick_access))
            Column(
                Modifier.padding(horizontal = Immersive.Gutter),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tiles.chunked(columns).forEachIndexed { rowIndex, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEachIndexed { columnIndex, item ->
                            HomeItemTile(
                                navController, item, playingContext, rowIndex + columnIndex,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeRecommendationSection(
    navController: NavController,
    section: HomeSection,
    sectionIndex: Int,
    mix: HomeItem.Playlist?,
    playingContext: State<String>,
    pageWidth: Dp,
) {
    val entries = remember(section.items, mix) {
        section.items.filterNot { item -> item is HomeItem.Playlist && item.id == mix?.id }
    }
    if (entries.isEmpty()) return
    val keys = remember(entries) { homeItemKeys(entries) }
    val featureWidth = (pageWidth * 0.63f).coerceIn(156.dp, 296.dp)
    val tileWidth = (pageWidth * 0.72f).coerceIn(156.dp, 280.dp)
    ImmersiveShelf(title = section.title) {
        if (sectionIndex % 2 == 0) {
            items(entries.size, key = { keys[it] }) { index ->
                HomeSpotlightCard(
                    navController, entries[index], playingContext, index + sectionIndex,
                    Modifier.width(featureWidth),
                )
            }
        } else {
            items((entries.size + 1) / 2, key = { keys[it * 2] }) { stack ->
                Column(Modifier.width(tileWidth), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val first = stack * 2
                    for (index in first until minOf(first + 2, entries.size)) {
                        HomeItemTile(navController, entries[index], playingContext, index + sectionIndex)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSpotlightCard(
    navController: NavController,
    item: HomeItem,
    playingContext: State<String>,
    tone: Int,
    modifier: Modifier,
) {
    ImmersiveSpotlightCard(
        title = item.name,
        subtitle = item.cardSubtitle(),
        imageUrl = item.imageUrl,
        tone = tone,
        onClick = { onHomeItemClick(navController, item) },
        modifier = modifier.pressHint { prefetchHomeItem(item) },
        roundArtwork = item is HomeItem.Artist,
        playing = item.isPlayingIn(playingContext),
        badge = { HomeEpisodeBadge(item, Modifier.align(Alignment.TopEnd)) },
    )
}

@Composable
private fun HomeItemTile(
    navController: NavController,
    item: HomeItem,
    playingContext: State<String>,
    tone: Int,
    modifier: Modifier = Modifier,
) {
    HomeListeningTile(
        title = item.name,
        subtitle = item.cardSubtitle(),
        imageUrl = item.imageUrl,
        tone = tone,
        modifier = modifier.pressHint { prefetchHomeItem(item) },
        round = item is HomeItem.Artist,
        playing = item.isPlayingIn(playingContext),
        badge = { HomeEpisodeBadge(item, Modifier.align(Alignment.TopEnd)) },
        onClick = { onHomeItemClick(navController, item) },
    )
}

@Composable
private fun HomeListeningTile(
    title: String,
    subtitle: String?,
    imageUrl: String?,
    tone: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    round: Boolean = false,
    playing: Boolean = false,
    badge: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {},
) {
    val (container, content) = immersiveTonalColors(tone)
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier.fillMaxWidth().immersiveClickable(onClick = onClick)
            .clip(shape).background(container).heightIn(min = 70.dp).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp)) {
            ImmersiveCover(
                imageUrl, Modifier.fillMaxSize(),
                shape = if (round) CircleShape else RoundedCornerShape(14.dp), border = false,
            )
            if (playing) ImmersivePlayingBars(Modifier.align(Alignment.Center))
            badge()
        }
        Column(Modifier.weight(1f)) {
            Text(
                title, style = MaterialTheme.typography.labelLarge, color = content,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle, style = MaterialTheme.typography.bodySmall, color = content,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HomeEpisodeBadge(item: HomeItem, modifier: Modifier) {
    if (item is HomeItem.Show && !item.isEpisode) {
        com.music.spotui.ui.components.NewEpisodeDot(item.id, MaterialTheme.colorScheme.primary, modifier)
    }
}
