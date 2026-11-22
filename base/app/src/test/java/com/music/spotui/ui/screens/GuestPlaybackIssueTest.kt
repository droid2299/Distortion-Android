package com.music.spotui.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class GuestPlaybackIssueTest {
    @Test fun unavailablePlaybackDistinguishesActionableKnownConditions() {
        assertEquals(GuestPlaybackIssue.NO_SOURCE, guestPlaybackIssue(hasSource = false, online = true))
        assertEquals(GuestPlaybackIssue.NO_SOURCE, guestPlaybackIssue(hasSource = false, online = false))
        assertEquals(GuestPlaybackIssue.OFFLINE, guestPlaybackIssue(hasSource = true, online = false))
        assertEquals(GuestPlaybackIssue.SOURCE_UNAVAILABLE, guestPlaybackIssue(hasSource = true, online = true))
    }
}
