package app.myspoti.spotify.bridge

import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PublicCatalogueBridgeTest {
    @Test fun `catalogue commands cannot reach playback or arbitrary services`() = runBlocking {
        var calls = 0
        val host = object : BridgeHost {
            override val messages = MutableSharedFlow<String>()
            override suspend fun evaluate(script: String) { calls++ }
        }
        try {
            val bridge = Bridge.publicCatalogue(host, this)
            for (method in listOf("player.play", "service.call", "gql.query", "unknown")) {
                assertThrows(BridgeException::class.java) { runBlocking { bridge.call(method) } }
            }
            assertThrows(IllegalStateException::class.java) { runBlocking { bridge.inject() } }
            assertEquals(0, calls)
        } finally { coroutineContext.cancelChildren() }
    }

    @Test fun `closing the catalogue does not reset the playback device`() = runBlocking {
        val joined = PageDevice.joined.value
        val reported = PageDevice.reported
        val field = PageDevice::class.java.getDeclaredField("reported").apply { isAccessible = true }
        val host = object : BridgeHost {
            override val messages = MutableSharedFlow<String>()
            override suspend fun evaluate(script: String) = Unit
        }
        try {
            PageDevice.joined()
            val bridge = Bridge.publicCatalogue(host, this)
            bridge.pageGone()
            assertTrue(PageDevice.joined.value)
            assertEquals(1, bridge.pagesGone)
        } finally {
            if (joined) PageDevice.joined() else PageDevice.pageGone()
            field.setBoolean(null, reported)
            coroutineContext.cancelChildren()
        }
    }
}
