package com.music.spotui.data.catalogue

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.webkit.WebViewCompat
import app.myspoti.host.android.MyspotiRuntime
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class PublicCatalogueSessionTest {
    @Test fun accountCookiesAreNotVisibleToThePublicProfile() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val origin = "https://catalogue-isolation.invalid/"
        val cookie = "distortion_catalogue_test=present; Secure; Path=/; SameSite=Strict"
        val ready = CountDownLatch(1)
        var session: PublicCatalogueSession? = null
        instrumentation.runOnMainSync {
            MyspotiRuntime.host.cookies().setCookie(origin, cookie) { ready.countDown() }
        }
        assertTrue(ready.await(5, TimeUnit.SECONDS))
        try {
            instrumentation.runOnMainSync {
                assertTrue(MyspotiRuntime.host.cookies().getCookie(origin).orEmpty().contains("distortion_catalogue_test=present"))
                session = PublicCatalogueSession(instrumentation.targetContext)
                val public = WebViewCompat.getProfile(checkNotNull(session).view)
                assertNotEquals(MyspotiRuntime.host.profile, public.name)
                assertNull(public.cookieManager.getCookie(origin))
            }
        } finally {
            instrumentation.runOnMainSync {
                session?.close()
                MyspotiRuntime.host.cookies().setCookie(origin, "distortion_catalogue_test=; Max-Age=0; Secure; Path=/")
            }
        }
    }

    @Test fun publicSearchAndMetadataWorkWithoutAnAccount() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var session: PublicCatalogueSession
        instrumentation.runOnMainSync { session = PublicCatalogueSession(instrumentation.targetContext) }
        try {
            withTimeout(90_000) {
                for (kind in CatalogueKind.entries) {
                    val result = session.searchPage(kind, "Daft Punk")
                    assertTrue("No public results for $kind", result.entries.isNotEmpty())
                    assertTrue(result.entries.all { it.kind == kind && it.title.isNotBlank() })
                    if (kind == CatalogueKind.TRACK) {
                        val offset = checkNotNull(result.nextOffset)
                        val next = session.searchPage(kind, "Daft Punk", offset)
                        assertTrue("The next song page was empty", next.entries.isNotEmpty())
                        assertNotEquals(result.entries.map { it.uri }, next.entries.map { it.uri })
                    }
                    val detail = session.details(result.entries.first())
                    assertEquals(result.entries.first().uri, detail.entry.uri)
                    assertTrue(detail.entry.title.isNotBlank())
                    detail.nextOffset?.let { offset ->
                        val next = session.moreTracks(detail.entry, offset)
                        assertTrue("The next track page was empty for $kind", next.entries.isNotEmpty())
                        assertTrue(next.entries.all { it.kind == CatalogueKind.TRACK })
                    }
                }
            }
        } finally { instrumentation.runOnMainSync { session.close() } }
    }
}
