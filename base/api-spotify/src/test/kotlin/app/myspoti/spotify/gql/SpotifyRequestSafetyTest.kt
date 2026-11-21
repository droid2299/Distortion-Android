package app.myspoti.spotify.gql

import app.myspoti.spotify.bridge.Bridge
import app.myspoti.spotify.bridge.BridgeHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicInteger

class SpotifyRequestSafetyTest {
    private class Session(scope: CoroutineScope, client: OkHttpClient) {
        val reads = AtomicInteger()
        val bridge = Bridge(object : BridgeHost {
            override val messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
            override suspend fun evaluate(script: String) {
                val id = Regex("""Myspoti\.call\((\d+), "token\.get"""").find(script)?.groupValues?.get(1)
                    ?: error("Unexpected bridge call")
                val token = reads.incrementAndGet()
                messages.emit("""{"type":"result","id":$id,"ok":true,"value":{"accessToken":"token-$token","expiresAtMs":${System.currentTimeMillis() + 3_600_000},"clientId":"client","appVersion":"web"}}""")
            }
        }, scope)
        val http = SpotifyHttp(client, bridge)
    }

    private fun client(reply: (Request) -> Pair<Int, String>): OkHttpClient = OkHttpClient.Builder().addInterceptor { chain ->
        val request = chain.request()
        val (status, body) = if (request.url.host == "clienttoken.spotify.com")
            200 to """{"granted_token":{"token":"client","expires_after_seconds":7200}}"""
        else reply(request)
        Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(status).message("test")
            .body(body.toResponseBody()).build()
    }.build()

    @Test fun `invalid destinations fail before any credential is obtained`() = runBlocking {
        try {
            val session = Session(this, client { error("Network must not be used") })
            for (url in listOf("http://api.spotify.com/me", "https://spotify.com.evil.example/me", "https://user@api.spotify.com/me")) {
                assertThrows(IllegalArgumentException::class.java) { runBlocking { session.http.send("GET", url) } }
                assertThrows(IllegalArgumentException::class.java) { runBlocking { session.http.sendProto("GET", url, null) } }
                assertThrows(IllegalArgumentException::class.java) { runBlocking { session.http.sendRaw("GET", url, null, emptyMap()) } }
            }
            assertEquals(0, session.reads.get())
        } finally { coroutineContext.cancelChildren() }
    }

    @Test fun `concurrent rejected reads share one credential renewal`() = runBlocking {
        try {
            val requests = AtomicInteger()
            val session = Session(this, client { request ->
                requests.incrementAndGet()
                if (request.header("authorization") == "Bearer token-1") 401 to "{}" else 200 to "{}"
            })
            val results = List(8) { async { session.http.send("GET", "https://api.spotify.com/me") } }.awaitAll()
            assertTrue(results.all { it.first == 200 })
            assertEquals(2, session.reads.get())
            assertTrue(requests.get() in 9..16)
        } finally { coroutineContext.cancelChildren() }
    }

    @Test fun `read renewal is bounded and writes are not repeated`() = runBlocking {
        try {
            for ((method, expected) in listOf("GET" to 2, "POST" to 1, "PUT" to 1, "DELETE" to 1)) {
                val requests = AtomicInteger()
                val session = Session(this, client { requests.incrementAndGet(); 401 to "{}" })
                assertEquals(401, session.http.send(method, "https://api.spotify.com/me").first)
                assertEquals(expected, requests.get())
            }
        } finally { coroutineContext.cancelChildren() }
    }

    @Test fun `unknown operations writes and refusals cannot fall back to a second execution`() = runBlocking {
        try {
            for (name in listOf("addToPlaylist", "futureUnknownOperation", "fetchLibraryTracks")) {
                for (status in listOf(302, 401, 403, 429, 500)) {
                    if (name == "fetchLibraryTracks" && status == 500) continue
                    var fallbacks = 0
                    val session = Session(this, client { status to "{}" })
                    val gql = HttpGql(session.http, object : OpsSource {
                        override val ops = GqlOps(mapOf(name to "0".repeat(64)))
                    }, object : Gql {
                        override suspend fun query(name: String, variables: JsonObject): kotlinx.serialization.json.JsonElement {
                            fallbacks++
                            error("Unexpected fallback")
                        }
                    })
                    assertThrows(Exception::class.java) { runBlocking { gql.query(name, JsonObject(emptyMap())) } }
                    assertEquals(0, fallbacks, "$name HTTP $status")
                }
            }
        } finally { coroutineContext.cancelChildren() }
    }

    @Test fun `ambiguous mutation failures never execute through the bridge fallback`() = runBlocking {
        try {
            for (failure in listOf(java.io.IOException("connection lost"), IllegalStateException("transport failure"))) {
                var requests = 0
                var fallbacks = 0
                val session = Session(this, client { requests++; throw failure })
                val gql = HttpGql(session.http, object : OpsSource {
                    override val ops = GqlOps(mapOf("addToPlaylist" to "0".repeat(64)))
                }, object : Gql {
                    override suspend fun query(name: String, variables: JsonObject): kotlinx.serialization.json.JsonElement {
                        fallbacks++
                        error("Unexpected fallback")
                    }
                })
                assertThrows(Exception::class.java) { runBlocking { gql.query("addToPlaylist", JsonObject(emptyMap())) } }
                assertEquals(1, requests)
                assertEquals(0, fallbacks)
            }
        } finally { coroutineContext.cancelChildren() }
    }

    @Test fun `raw and protobuf reads renew once while writes do not renew`() = runBlocking {
        try {
            for (format in listOf("raw", "protobuf")) for (method in listOf("GET", "POST")) {
                val requests = AtomicInteger()
                val session = Session(this, client { requests.incrementAndGet(); 401 to "{}" })
                val status = if (format == "raw") session.http.sendRaw(method, "https://api.spotify.com/me", null, emptyMap()).status
                    else session.http.sendProto(method, "https://api.spotify.com/me", null).first
                assertEquals(401, status)
                assertEquals(if (method == "GET") 2 else 1, requests.get(), "$format $method")
            }
        } finally { coroutineContext.cancelChildren() }
    }

    @Test fun `raw public requests cannot receive credentials from caller headers`() = runBlocking {
        try {
            var seen: Request? = null
            val session = Session(this, client { seen = it; 200 to "{}" })
            session.http.sendRaw("GET", "https://open.spotifycdn.com/public", null,
                mapOf("AuThOrIzAtIoN" to "caller", "CLIENT-TOKEN" to "caller", "Cookie" to "caller"))
            assertEquals(0, session.reads.get())
            for (header in listOf("authorization", "client-token", "cookie")) assertNull(checkNotNull(seen).header(header))
        } finally { coroutineContext.cancelChildren() }
    }

    @Test fun `credentialed redirect is returned without contacting the destination`() = runBlocking {
        val certificate = HeldCertificate.Builder().addSubjectAlternativeName("api.spotify.com")
            .addSubjectAlternativeName("clienttoken.spotify.com").addSubjectAlternativeName("other.example").build()
        val serverTls = HandshakeCertificates.Builder().heldCertificate(certificate).build()
        val clientTls = HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
        MockWebServer().use { server ->
            server.useHttps(serverTls.sslSocketFactory())
            server.start()
            val client = OkHttpClient.Builder().dns { listOf(InetAddress.getLoopbackAddress()) }
                .sslSocketFactory(clientTls.sslSocketFactory(), clientTls.trustManager)
                .addInterceptor { chain -> chain.proceed(chain.request().newBuilder().url(chain.request().url.newBuilder().port(server.port).build()).build()) }
                .build()
            server.enqueue(MockResponse.Builder().body("""{"granted_token":{"token":"client","expires_after_seconds":7200}}""").build())
            server.enqueue(MockResponse.Builder().code(302).addHeader("Location", "https://other.example:${server.port}/destination").build())
            server.enqueue(MockResponse.Builder().body("Unexpected redirect").build())
            try {
                val session = Session(this, client)
                assertEquals(302, session.http.send("GET", "https://api.spotify.com/me").first)
                assertEquals(2, server.requestCount)
            } finally {
                coroutineContext.cancelChildren()
                client.connectionPool.evictAll()
                client.dispatcher.executorService.shutdown()
            }
        }
    }
}
