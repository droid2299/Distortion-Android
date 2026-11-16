package com.music.spotui.ui.immersive.pages

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import app.myspoti.engine.R
import com.music.spotui.data.api.SpotifyPlaylistDraft
import com.music.spotui.data.local.ListenHistory
import com.music.spotui.data.local.listensInMonth
import com.music.spotui.data.local.rankedSpotifyIds
import com.music.spotui.ui.immersive.Immersive
import com.music.spotui.ui.immersive.components.ImmersiveMessageState
import com.music.spotui.ui.immersive.components.SectionHeader
import com.music.spotui.ui.navigation.playlistRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class MonthlyStory(val month: YearMonth, val summary: ListenHistory.Recap, val trackIds: List<String>, val activeDays: Int)

@Composable
internal fun MonthlyRecapScreen(nav: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val today = remember { YearMonth.now() }
    var month by remember { mutableStateOf(today) }
    var running by remember { mutableStateOf(false) }
    var error by remember(month) { mutableStateOf<Int?>(null) }
    val draft = remember(month) { SpotifyPlaylistDraft() }
    val story by produceState<MonthlyStory?>(null, month) {
        value = null
        value = withContext(Dispatchers.IO) {
            val listens = listensInMonth(ListenHistory.all(), month)
            val zone = ZoneId.systemDefault()
            val days = listens.mapTo(hashSetOf()) { Instant.ofEpochMilli(it.atMs).atZone(zone).toLocalDate() }.size
            MonthlyStory(month, ListenHistory.recap(listens), rankedSpotifyIds(listens, 30), days)
        }
    }
    val label = month.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()))
    val colors = MaterialTheme.colorScheme
    ImmersivePage(stringResource(R.string.app_recap_monthly), onBack = { if (!running) nav.popBackStack() }) {
        immersiveBlock("months") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = !running, onClick = { month = month.minusMonths(1) }) {
                    Text(stringResource(R.string.app_recap_previous))
                }
                OutlinedButton(enabled = !running && month < today, onClick = { month = month.plusMonths(1) }) {
                    Text(stringResource(R.string.app_recap_next))
                }
            }
        }
        item("month-label") { SectionHeader(label) }
        val loaded = story?.takeIf { it.month == month }
        when {
            loaded == null -> item("loading") { ImmersiveMessageState(stringResource(R.string.app_discovery_working)) }
            loaded.summary.listens == 0 -> item("empty") { ImmersiveMessageState(stringResource(R.string.app_recap_empty)) }
            else -> {
                immersiveBlock("summary") {
                    Column(Modifier.fillMaxWidth().background(colors.primaryContainer, Immersive.Card).padding(20.dp)) {
                        Text(
                            stringResource(R.string.app_recap_minutes, loaded.summary.minutes),
                            style = MaterialTheme.typography.headlineMedium, color = colors.onPrimaryContainer,
                        )
                        Text(
                            stringResource(R.string.app_recap_listens, loaded.summary.listens),
                            style = MaterialTheme.typography.titleMedium, color = colors.onPrimaryContainer,
                        )
                        Text(stringResource(R.string.app_recap_days, loaded.activeDays), color = colors.onPrimaryContainer)
                        loaded.summary.topArtists.firstOrNull()?.let { artist ->
                            Text(stringResource(R.string.app_recap_top_artist, artist.name), color = colors.onPrimaryContainer)
                        }
                    }
                }
                immersiveBlock("share") {
                    Button(enabled = !running, onClick = {
                        running = true
                        error = null
                        scope.launch {
                            try {
                                val ok = recover {
                                    shareRecapCard(context, label, loaded, colors.primaryContainer.toArgb(), colors.onPrimaryContainer.toArgb())
                                    true
                                }
                                if (ok != true) error = R.string.app_recap_share_failed
                            } finally {
                                running = false
                            }
                        }
                    }) { Text(stringResource(R.string.app_recap_share)) }
                }
                if (loaded.trackIds.isNotEmpty()) immersiveBlock("playlist") {
                    Button(enabled = !running, onClick = {
                        val name = context.getString(R.string.app_recap_playlist, label)
                        running = true
                        error = null
                        scope.launch {
                            try {
                                val uri = recover { draft.save(name, loaded.trackIds, makePrivate = true) }
                                if (uri == null) error = R.string.app_discovery_save_failed
                                else nav.navigate(playlistRoute(uri.substringAfterLast(':'), name))
                            } finally {
                                running = false
                            }
                        }
                    }) { Text(stringResource(R.string.app_recap_save)) }
                }
                item("tracks-header") { SectionHeader(stringResource(R.string.recap_top_tracks)) }
                items(loaded.summary.topTracks) { track ->
                    ImmersiveGroupRow(title = track.name, detail = track.subtitle)
                }
            }
        }
        error?.let { id -> item("error") { ImmersiveMessageState(stringResource(id)) } }
    }
}

private suspend fun shareRecapCard(context: Context, month: String, story: MonthlyStory, bg: Int, fg: Int) {
    val recap = story.summary
    val file = withContext(Dispatchers.IO) {
        val out = Bitmap.createBitmap(720, 1024, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(out)
            canvas.drawColor(bg)
            val text = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = fg }
            fun line(value: String, y: Float, size: Float, bold: Boolean = false) {
                text.textSize = size
                text.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                val fitting = TextUtils.ellipsize(value, text, 608f, TextUtils.TruncateAt.END)
                canvas.drawText(fitting.toString(), 56f, y, text)
            }
            line(context.getString(R.string.app_recap_monthly), 106f, 48f, true)
            line(month, 163f, 36f)
            line(context.getString(R.string.app_recap_minutes, recap.minutes), 280f, 62f, true)
            line(context.getString(R.string.app_recap_listens, recap.listens), 340f, 42f)
            line(context.getString(R.string.app_recap_days, story.activeDays), 392f, 33f)
            recap.topArtists.firstOrNull()?.let {
                line(context.getString(R.string.app_recap_top_artist, it.name), 455f, 36f, true)
            }
            line(context.getString(R.string.recap_top_tracks), 540f, 38f, true)
            recap.topTracks.take(4).forEachIndexed { index, track ->
                line("${index + 1}. ${track.name}", 610f + 65f * index, 34f)
            }
            line("Distortion", 960f, 36f, true)
            val folder = File(context.cacheDir, "shared").apply { mkdirs() }
            File(folder, "distortion-recap.png").also { target ->
                target.outputStream().use { check(out.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            }
        } finally {
            out.recycle()
        }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val share = Intent(Intent.ACTION_SEND).setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(share, context.getString(R.string.app_recap_share)))
}
