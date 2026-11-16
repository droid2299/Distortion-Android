package com.music.spotui.ui.immersive.pages

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import app.myspoti.engine.R
import com.music.spotui.data.api.ProfileCache
import com.music.spotui.data.local.ListenHistory
import com.music.spotui.data.local.LocalMusic
import com.music.spotui.data.local.LocalTrack
import com.music.spotui.data.local.rankedSpotifyIds
import com.music.spotui.data.offline.OfflineFavorites
import com.music.spotui.data.offline.OfflineStore
import com.music.spotui.di.LocalPlayer
import com.music.spotui.ui.immersive.components.ImmersiveListRow
import com.music.spotui.ui.immersive.components.ImmersiveMessageState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun OfflineFavoritesScreen(nav: NavController) {
    val context = LocalContext.current
    val profile = ProfileCache.name
    val account = ProfileCache.id
    val preference = OfflineFavorites.revision.intValue
    val limit = androidx.compose.runtime.remember(profile, account, preference) { OfflineFavorites.limitMb() }
    val recent by produceState<List<String>>(emptyList(), account) {
        value = withContext(Dispatchers.IO) { rankedSpotifyIds(ListenHistory.all(), 100) }
    }
    var hasAudioPermission by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(LocalMusic.hasPermission(context)) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasAudioPermission = granted
    }
    val local by produceState<List<LocalTrack>>(emptyList(), hasAudioPermission) {
        value = if (hasAudioPermission) LocalMusic.tracks(context) else emptyList()
    }
    val saved = OfflineStore.tracks.values.filterNot { it.isEpisode }
    val savedById = saved.associateBy { it.trackId }
    val fromHistory = recent.mapNotNull(savedById::get)
    val fromDownloads = (fromHistory + saved.sortedByDescending { it.addedAt }).distinctBy { it.trackId }
    val queue = (fromDownloads.map(OfflineStore::toLocalTrack) + local).distinctBy { it.uri }.take(50)
    val title = stringResource(R.string.app_offline_favorites)
    val autoMb = saved.filter { it.automaticFavorite }.sumOf { it.bytes } / OfflineFavorites.MIB
    LaunchedEffect(account) { OfflineFavorites.refresh() }
    ImmersivePage(title, onBack = { nav.popBackStack() }) {
        immersiveGroup("automatic", headerRes = R.string.app_offline_auto, footerRes = R.string.app_offline_limit_help) {
            row {
                ImmersiveGroupRow(
                    title = stringResource(R.string.app_offline_storage, autoMb),
                    detail = stringResource(R.string.app_offline_help),
                )
            }
            listOf(0, 128, 256, 512).forEach { mb ->
                row {
                    val label = if (mb == 0) stringResource(R.string.app_offline_off) else stringResource(R.string.app_offline_mb, mb)
                    if (account == null) ImmersiveGroupRow(title = label, enabled = false)
                    else ImmersiveChoiceRow(title = label, selected = limit == mb, onClick = { OfflineFavorites.setLimitMb(mb) })
                }
            }
        }
        if (limit > 0) immersiveBlock("refresh") {
            Button(onClick = { OfflineFavorites.refresh(force = true) }) {
                androidx.compose.material3.Text(stringResource(R.string.app_offline_refresh))
            }
        }
        if (!hasAudioPermission) immersiveBlock("audio-permission") {
            androidx.compose.material3.OutlinedButton(onClick = { permission.launch(LocalMusic.permission) }) {
                androidx.compose.material3.Text(stringResource(R.string.app_offline_add_device))
            }
        }
        if (queue.isEmpty()) item("empty") { ImmersiveMessageState(stringResource(R.string.app_offline_empty)) }
        else {
            immersiveBlock("play") {
                Button(onClick = { LocalPlayer.play(queue, 0, context = title) }) {
                    androidx.compose.material3.Text(stringResource(R.string.immersive_shell_play))
                }
            }
            itemsIndexed(queue, key = { _, track -> track.uri.toString() }) { index, track ->
                ImmersiveListRow(track.title, track.artist, track.artUri.toString(), round = false,
                    onClick = { LocalPlayer.play(queue, index, context = title) })
            }
        }
    }
}
