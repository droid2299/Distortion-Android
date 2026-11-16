package com.music.spotui.data.local

import com.music.spotui.data.entity.SongsModel
import java.time.YearMonth
import java.time.ZoneId

internal enum class MixMode { FAMILIAR, BALANCED, DISCOVER }

internal fun buildMix(
    seeds: List<SongsModel>,
    stations: List<List<SongsModel>>,
    heard: Set<String>,
    mode: MixMode,
    limit: Int = 30,
): List<SongsModel> {
    val selected = seeds.mapTo(hashSetOf()) { it.spotifyTrackId }
    val candidates = seeds + (0 until (stations.maxOfOrNull { it.size } ?: 0)).flatMap { index ->
        stations.mapNotNull { it.getOrNull(index) }
    }
    val (familiar, discovery) = candidates
        .filter { it.playable && !it.isEpisode && it.spotifyTrackId.isNotBlank() }
        .distinctBy { it.spotifyTrackId }
        .partition { it.spotifyTrackId in selected || it.spotifyTrackId in heard }
    val pattern = when (mode) {
        MixMode.FAMILIAR -> listOf(true, true, false)
        MixMode.BALANCED -> listOf(true, false)
        MixMode.DISCOVER -> listOf(false, false, true)
    }
    var known = 0
    var new = 0
    return buildList {
        while (size < limit && (known < familiar.size || new < discovery.size)) {
            val preferKnown = pattern[size % pattern.size]
            add(
                if (known < familiar.size && (preferKnown || new == discovery.size)) familiar[known++]
                else discovery[new++],
            )
        }
    }
}

internal fun ListenHistory.Listen.spotifyId(): String = spotifyTrackId
    .ifBlank { key.removePrefix("spotify:track:").takeIf { key.startsWith("spotify:track:") }.orEmpty() }
    .takeIf { it.isNotEmpty() && it.all { c -> c in '0'..'9' || c in 'A'..'Z' || c in 'a'..'z' } }
    .orEmpty()

internal fun rankedSpotifyIds(listens: List<ListenHistory.Listen>, limit: Int): List<String> =
    listens.groupBy { it.spotifyId() }.filterKeys { it.isNotBlank() }.entries
        .sortedWith(compareByDescending<Map.Entry<String, List<ListenHistory.Listen>>> { it.value.size }
            .thenByDescending { it.value.sumOf { listen -> listen.playedMs } }
            .thenByDescending { it.value.maxOf { listen -> listen.atMs } }
            .thenBy { it.key })
        .take(limit).map { it.key }

internal fun listensInMonth(
    listens: List<ListenHistory.Listen>,
    month: YearMonth,
    zone: ZoneId = ZoneId.systemDefault(),
): List<ListenHistory.Listen> {
    val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return listens.filter { it.atMs in start until end }
}
