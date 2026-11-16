package com.music.spotui.data.local

internal enum class SetCurve { STEADY, BUILD, WIND_DOWN }

internal data class SetTrack<T>(val id: String, val track: T, val durationMs: Long, val energy: Double?)

internal fun <T> planAdaptiveSet(
    pool: List<SetTrack<T>>,
    targetMs: Long,
    curve: SetCurve,
    closer: SetTrack<T>?,
    fill: Boolean = true,
): List<SetTrack<T>> {
    if (!fill) return listOfNotNull(closer)
    val options = pool.asSequence()
        .filter { it.id != closer?.id && it.durationMs in 1..MAX_SET_MS }
        .distinctBy { it.id }
        .take(180)
        .toList()
    val budget = (targetMs.coerceIn(0, MAX_SET_MS * 2) - (closer?.durationMs ?: 0))
    if (budget <= 0 || options.isEmpty()) return listOfNotNull(closer)

    val cap = minOf(MAX_SET_MS * 3 / STEP_MS, (budget + options.maxOf { it.durationMs }) / STEP_MS + 1).toInt()
    val choice = IntArray(cap + 1) { -1 }
    val previous = IntArray(cap + 1)
    val actual = LongArray(cap + 1)
    choice[0] = options.size
    options.forEachIndexed { index, track ->
        val units = ((track.durationMs + STEP_MS / 2) / STEP_MS).toInt().coerceAtLeast(1)
        for (sum in cap downTo units) {
            val before = sum - units
            if (choice[sum] >= 0 || choice[before] < 0) continue
            choice[sum] = index
            previous[sum] = before
            actual[sum] = actual[before] + track.durationMs
        }
    }
    var best = 0
    for (sum in 1..cap) {
        if (choice[sum] < 0) continue
        val distance = kotlin.math.abs(actual[sum] - budget)
        val bestDistance = kotlin.math.abs(actual[best] - budget)
        if (distance < bestDistance || (distance == bestDistance && actual[sum] < actual[best])) best = sum
    }
    val selected = buildList {
        while (best > 0) {
            add(options[choice[best]])
            best = previous[best]
        }
    }.asReversed()
    val sorted = when (curve) {
        SetCurve.STEADY -> selected
        SetCurve.BUILD, SetCurve.WIND_DOWN -> {
            val known = selected.filter { it.energy != null }
            val ordered = if (curve == SetCurve.BUILD) known.sortedBy { it.energy } else known.sortedByDescending { it.energy }
            val available = ordered.iterator()
            selected.map { if (it.energy == null) it else available.next() }
        }
    }
    return sorted + listOfNotNull(closer)
}

private const val STEP_MS = 1_000L
private const val MAX_SET_MS = 60 * 60_000L
