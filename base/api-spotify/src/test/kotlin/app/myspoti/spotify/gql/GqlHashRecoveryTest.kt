package app.myspoti.spotify.gql

import app.myspoti.spotify.bridge.Bridge
import app.myspoti.spotify.bridge.BridgeHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class GqlHashRecoveryTest {
    private class Session(scope: CoroutineScope, var status: Int = 200) {
        private val host = object : BridgeHost {
            override val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
            override suspend fun evaluate(script: String) {
                val id = Regex("""Myspoti\.call\((\d+), "token\.get"""").find(script)?.groupValues?.get(1)
                    ?: error("Unexpected bridge call")
                val token = """{"accessToken":"test","expiresAtMs":${System.currentTimeMillis() + 3_600_000},"clientId":"client","appVersion":"web"}"""
                messages.emit("""{"type":"result","id":$id,"ok":true,"value":$token}""")
            }
        }
        private val old = "0".repeat(64)
        private val updated = "1".repeat(64)
        private val names = listOf("fetchLibraryTracks", "addToPlaylist")
        private val source = object : OpsSource {
            override var ops = GqlOps(names.associateWith { old })
        }
        val hashes = mutableListOf<String>()
        var refreshes = 0

        private val http = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val isClientToken = request.url.host == "clienttoken.spotify.com"
            val statusCode = if (isClientToken) 200 else status
            val body = if (isClientToken) {
                """{"granted_token":{"token":"client-token","expires_after_seconds":3600}}"""
            } else {
                val buffer = okio.Buffer()
                checkNotNull(request.body).writeTo(buffer)
                val body = Bridge.json.parseToJsonElement(buffer.readUtf8()).jsonObject
                val hash = body.getValue("extensions").jsonObject.getValue("persistedQuery")
                    .jsonObject.getValue("sha256Hash").jsonPrimitive.content
                hashes += hash
                if (hash == updated && statusCode == 200) """{"data":{"ok":true}}"""
                else """{"data":null,"errors":[{"extensions":{"code":"PERSISTED_QUERY_NOT_FOUND"}}]}"""
            }
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(statusCode)
                .message("test").body(body.toResponseBody()).build()
        }.build()

        val gql = HttpGql(SpotifyHttp(http, Bridge(host, scope)), source, object : Gql {
            override suspend fun query(name: String, variables: JsonObject) = error("Unexpected fallback")
        }).apply {
            refreshOperations = { _, rejected ->
                assertEquals(old, rejected)
                refreshes++
                source.ops = GqlOps(names.associateWith { updated })
            }
        }
    }

    @Test fun rejectedReadUsesTheNewHashOnce() {
        runBlocking {
            try {
                val session = Session(this)
                session.gql.query("fetchLibraryTracks", JsonObject(emptyMap()))
                assertEquals(listOf("0".repeat(64), "1".repeat(64)), session.hashes)
                assertEquals(1, session.refreshes)
            } finally { coroutineContext.cancelChildren() }
        }
    }

    @Test fun rejectedMutationAndRateLimitedReadAreNotRetried() {
        for ((name, status) in listOf("addToPlaylist" to 200, "fetchLibraryTracks" to 429)) {
            assertThrows(GqlException::class.java) {
                runBlocking {
                    try {
                        val session = Session(this, status)
                        try { session.gql.query(name, JsonObject(emptyMap())) }
                        finally {
                            assertEquals(1, session.hashes.size)
                            assertEquals(0, session.refreshes)
                        }
                    } finally { coroutineContext.cancelChildren() }
                }
            }
        }
    }
}
