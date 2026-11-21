package app.myspoti.host.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrowserPresentationTest {
    @Test fun `account and challenge pages use platform sign-in presentation`() {
        for (url in listOf(
            "https://accounts.spotify.com/login",
            "https://accounts.spotify.com/en/login/otp?continue=https%3A%2F%2Fopen.spotify.com%2F",
            "https://challenge.spotify.com/c/example",
            "https://open.spotify.com/login",
        )) assertEquals(url, BrowserPresentation.SIGN_IN, BrowserPresentation.forUrl(url))
    }

    @Test fun `return to player restores desktop presentation without reading continue query`() {
        for (url in listOf(
            "https://open.spotify.com/",
            "https://open.spotify.com/intl-en/?next=https://accounts.spotify.com/login",
            "https://open.spotify.com/preferences",
        )) assertEquals(url, BrowserPresentation.PLAYER, BrowserPresentation.forUrl(url))
    }

    @Test fun `external insecure and misleading addresses cannot switch presentations`() {
        for (url in listOf(
            "http://accounts.spotify.com/login",
            "https://accounts.spotify.com.evil.example/login",
            "https://open.spotify.com@evil.example/",
            "https://example@open.spotify.com/",
            "https://accounts.google.com/o/oauth2/auth",
            "about:blank",
            "not a url",
        )) assertNull(url, BrowserPresentation.forUrl(url))
    }
}
