package com.music.spotui.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveSetPlanTest {
    private fun song(id: String, seconds: Long, energy: Double? = null) =
        SetTrack(id, id, seconds * 1_000, energy)

    @Test fun fitsTheTargetWithTheCloserReservedAtTheEnd() {
        val closer = song("closer", 90, 0.0)
        val tracks = listOf(song("long", 250), song("a", 120), song("b", 90), closer)
        val plan = planAdaptiveSet(tracks, 300_000, SetCurve.STEADY, closer)

        assertEquals(listOf("a", "b", "closer"), plan.map { it.id })
        assertEquals(300_000L, plan.sumOf { it.durationMs })
    }

    @Test fun reordersMeasuredTracksAroundUnmeasuredTracks() {
        val tracks = listOf(song("loud", 60, 0.9), song("unknown", 60), song("quiet", 60, 0.2))
        assertEquals(
            listOf("quiet", "unknown", "loud"),
            planAdaptiveSet(tracks, 180_000, SetCurve.BUILD, null).map { it.id },
        )
        assertEquals(
            listOf("loud", "unknown", "quiet"),
            planAdaptiveSet(tracks.reversed(), 180_000, SetCurve.WIND_DOWN, null).map { it.id },
        )
    }

    @Test fun replansWithinNewBudgetWhenACandidateIsLost() {
        val closer = song("closer", 90)
        val tracks = listOf(song("stream", 180), song("file", 115), song("download", 60))
        val afterLoss = tracks.drop(1)
        val plan = planAdaptiveSet(afterLoss, 270_000, SetCurve.STEADY, closer)

        assertEquals(listOf("file", "download", "closer"), plan.map { it.id })
        assertEquals(265_000L, plan.sumOf { it.durationMs })
    }

    @Test fun supportsAnHourAtDoublePlaybackSpeed() {
        val tracks = listOf(song("a", 2_400), song("b", 2_400), song("c", 2_400))
        assertEquals(7_200_000L, planAdaptiveSet(tracks, 7_200_000, SetCurve.STEADY, null).sumOf { it.durationMs })
    }

    @Test fun excludesDuplicatesAndDoesNotFillSmallGapsOrExtendACloserAlreadyStarted() {
        val available = listOf(song("duplicate", 60), song("duplicate", 60), song("invalid", 0))
        assertEquals(listOf("duplicate"), planAdaptiveSet(available, 120_000, SetCurve.STEADY, null).map { it.id })
        assertTrue(planAdaptiveSet(available, 20_000, SetCurve.STEADY, null).isEmpty())
        assertTrue(planAdaptiveSet(available, 120_000, SetCurve.STEADY, null, fill = false).isEmpty())
    }
}
