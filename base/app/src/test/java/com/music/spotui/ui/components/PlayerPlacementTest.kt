package com.music.spotui.ui.components

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerPlacementTest {
    @Test fun portraitPhoneIgnoresTransientWidePlayerMeasurements() {
        assertFalse(WindowSize(411, 891).playerSideBySide(411.dp, 300.dp))
    }

    @Test fun landscapePhoneAndLargeTabletCanUseTwoPanes() {
        assertTrue(WindowSize(891, 411).playerSideBySide(891.dp, 411.dp))
        assertTrue(WindowSize(900, 1280).playerSideBySide(900.dp, 1280.dp))
    }
}
