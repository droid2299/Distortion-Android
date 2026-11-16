package com.music.spotui.ui.immersive.pages

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.myspoti.engine.R
import com.music.spotui.automix.AutoMix
import com.music.spotui.data.api.ProfileCache
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.data.local.LocalMusic
import com.music.spotui.data.local.LocalTrack
import com.music.spotui.data.local.SetCurve
import com.music.spotui.data.local.SetTrack
import com.music.spotui.data.local.planAdaptiveSet
import com.music.spotui.data.offline.OfflineStore
import com.music.spotui.data.preferences.HiddenSongs
import com.music.spotui.di.AdaptiveSetSession
import com.music.spotui.di.LocalPlayer
import com.music.spotui.extensions.ExtensionAudio
import com.music.spotui.data.offline.OfflineTrack
import com.music.spotui.ui.immersive.components.ImmersiveListRow
import com.music.spotui.ui.immersive.screens.ImmersiveChipRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

@Composable
internal fun AdaptiveSetScreen(mix: List<SongsModel>, onBack: () -> Unit) {
    val context = LocalContext.current
    val title = stringResource(R.string.app_set_title)
    val account = ProfileCache.id
    var minutes by rememberSaveable { mutableIntStateOf(35) }
    var curve by rememberSaveable { mutableStateOf(SetCurve.STEADY) }
    var offline by rememberSaveable { mutableStateOf(false) }
    var closerId by rememberSaveable(account) { mutableStateOf<String?>(null) }
    var choosing by rememberSaveable { mutableStateOf(false) }
    var started by remember { mutableStateOf<Boolean?>(null) }

    var canReadAudio by remember { mutableStateOf(LocalMusic.hasPermission(context)) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { canReadAudio = it }
    val device by produceState<List<LocalTrack>>(emptyList(), canReadAudio) {
        value = if (canReadAudio) LocalMusic.tracks(context) else emptyList()
    }
    val online by produceState(AdaptiveSetSession.streamingAvailable()) {
        while (true) {
            value = AdaptiveSetSession.streamingAvailable()
            delay(1_000)
        }
    }
    val downloads = OfflineStore.tracks.toMap()
    val source = remember(account, downloads, mix, device) { SetSources(account, downloads, mix, device) }
    val analysis by produceState<Pair<SetSources, List<SetTrack<LocalTrack>>>?>(null, source) {
        value = source to withContext(Dispatchers.IO) {
            loadSetCandidates(source)
        }
    }
    val loaded = analysis?.first == source
    val pool = analysis?.takeIf { loaded }?.second.orEmpty()
    val sourceOnline = online && !offline
    val available = remember(pool, sourceOnline, downloads) {
        pool.filter { AdaptiveSetSession.available(it, sourceOnline) }
    }
    val target = minutes * 60_000L
    val rate = LocalPlayer.rate().toDouble()
    val mediaBudget = (target * rate).toLong()
    val closer = available.firstOrNull { it.id == closerId }
    val validCloser = closerId == null || (closer != null && closer.durationMs <= mediaBudget)
    val previewInput = remember(available, mediaBudget, curve, closer, validCloser) {
        PreviewInput(available, mediaBudget, curve, closer, validCloser)
    }
    val planned by produceState<Pair<PreviewInput, List<SetTrack<LocalTrack>>>?>(null, previewInput) {
        value = previewInput to withContext(Dispatchers.Default) {
            if (validCloser) planAdaptiveSet(available, mediaBudget, curve, closer) else emptyList()
        }
    }
    val preview = planned?.takeIf { it.first == previewInput }?.second.orEmpty()
    val measured = preview.count { it.energy != null }

    if (choosing) {
        CloserPicker(available, closerId, onSelect = { closerId = it; choosing = false; started = null }, onClose = { choosing = false })
    }
    ImmersivePage(title, onBack = onBack) {
        immersiveBlock("duration-label") { Text(stringResource(R.string.app_set_duration)) }
        item("durations") {
            ImmersiveChipRow {
                listOf(15, 25, 35, 45, 60).forEach { value ->
                    FilterChip(selected = value == minutes, onClick = { minutes = value; started = null },
                        label = { Text(stringResource(R.string.app_set_minutes, value)) })
                }
            }
        }
        immersiveBlock("energy-label") { Text(stringResource(R.string.app_set_energy)) }
        item("energy") {
            ImmersiveChipRow {
                SetCurve.entries.forEach { value ->
                    val label = when (value) {
                        SetCurve.STEADY -> R.string.app_set_steady
                        SetCurve.BUILD -> R.string.app_set_build
                        SetCurve.WIND_DOWN -> R.string.app_set_wind_down
                    }
                    FilterChip(selected = value == curve, onClick = { curve = value; started = null },
                        label = { Text(stringResource(label)) })
                }
            }
        }
        immersiveBlock("options") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.app_set_offline))
                    Switch(checked = offline, onCheckedChange = { offline = it; started = null },
                        modifier = Modifier.semantics { contentDescription = context.getString(R.string.app_set_offline) })
                }
                Text(stringResource(if (sourceOnline) R.string.app_set_sources_online else R.string.app_set_sources_offline))
                OutlinedButton(onClick = { choosing = true }, enabled = available.isNotEmpty() || closerId != null) {
                    Text(stringResource(R.string.app_set_choose_closer))
                }
                closer?.let { Text(stringResource(R.string.app_set_pinned, it.track.title)) }
                if (!canReadAudio) OutlinedButton(onClick = { permission.launch(LocalMusic.permission) }) {
                    Text(stringResource(R.string.app_offline_add_device))
                }
            }
        }
        immersiveBlock("preview") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    !loaded || planned?.first != previewInput -> Text(stringResource(R.string.app_set_planning))
                    !validCloser -> Text(stringResource(if (closer == null) R.string.app_set_closer_unavailable else R.string.app_set_closer_too_long))
                    preview.isEmpty() -> Text(stringResource(R.string.app_set_empty))
                    else -> {
                        Text(stringResource(R.string.app_set_preview, setTime((preview.sumOf { it.durationMs } / rate).toLong()), preview.size))
                        if (curve != SetCurve.STEADY) Text(stringResource(R.string.app_set_energy_coverage, measured, preview.size))
                    }
                }
                Button(enabled = preview.isNotEmpty(), onClick = {
                    started = AdaptiveSetSession.start(title, target, curve, pool, closerId, offline, preview)
                }) { Text(stringResource(R.string.app_set_play)) }
                started?.let { Text(stringResource(if (it) R.string.app_set_started else R.string.app_set_start_failed)) }
            }
        }
        items(preview, key = { it.id }) { entry ->
            ImmersiveGroupRow(title = entry.track.title, detail = entry.track.artist, modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

private data class SetSources(
    val account: String?,
    val downloads: Map<String, OfflineTrack>,
    val mix: List<SongsModel>,
    val device: List<LocalTrack>,
)

private suspend fun loadSetCandidates(source: SetSources): List<SetTrack<LocalTrack>> {
    val downloads = source.downloads.values.asSequence()
        .filterNot { it.isEpisode || HiddenSongs.isHidden(it.trackId) }
        .sortedByDescending { it.addedAt }
        .map { "spotify:track:${it.trackId}" to OfflineStore.toLocalTrack(it) }
    val mix = source.mix.asSequence()
        .filter { it.playable && it.spotifyTrackId.isNotBlank() && !HiddenSongs.isHidden(it.spotifyTrackId) }
        .filter { it.durationMs.toLong() in 1..3_600_000L }
        .take(60)
        .map { "spotify:track:${it.spotifyTrackId}" to ExtensionAudio.localTrack(it) }
        .toList()
    val files = source.device.asSequence().map { it.uri.toString() to it }
    val local = (downloads + files)
        .filter { (_, track) -> track.durationMs in 1..3_600_000L }
        .take(180)
    val job = kotlinx.coroutines.currentCoroutineContext()
    return (mix.asSequence() + local).distinctBy { it.first }.map { (id, track) ->
        job.ensureActive()
        val energy = AutoMix.cachedFileAnalysis(track.uri)?.let { analysis ->
            val level = ((analysis.loudnessDb + 30) / 24).coerceIn(0.0, 1.0)
            val tempo = ((analysis.bpm - 60) / 110).coerceIn(0.0, 1.0)
            (0.65 * level + 0.35 * tempo).takeIf { it.isFinite() }
        }
        SetTrack(id, track, track.durationMs, energy)
    }.toList()
}

private data class PreviewInput(
    val pool: List<SetTrack<LocalTrack>>, val target: Long, val curve: SetCurve,
    val closer: SetTrack<LocalTrack>?, val valid: Boolean,
)

@Composable
private fun CloserPicker(
    tracks: List<SetTrack<LocalTrack>>, selected: String?, onSelect: (String?) -> Unit, onClose: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val shown = remember(tracks, query) {
        tracks.filter { it.track.title.contains(query.trim(), true) || it.track.artist.contains(query.trim(), true) }
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.app_set_choose_closer)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(query, { query = it }, label = { Text(stringResource(R.string.app_set_filter)) }, singleLine = true)
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(shown, key = { it.id }) { track ->
                        ImmersiveListRow(track.track.title, track.track.artist, track.track.artUri.toString(), round = false,
                            onClick = { onSelect(track.id) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(android.R.string.cancel)) } },
        dismissButton = {
            if (selected != null) TextButton(onClick = { onSelect(null) }) { Text(stringResource(R.string.app_set_clear_closer)) }
        },
    )
}

internal fun setTime(ms: Long): String {
    val seconds = ms.coerceAtLeast(0) / 1_000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
