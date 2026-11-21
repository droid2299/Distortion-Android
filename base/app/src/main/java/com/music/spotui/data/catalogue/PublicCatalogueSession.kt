package com.music.spotui.data.catalogue

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import app.myspoti.spotify.SpotifyCatalog
import app.myspoti.spotify.SpotifyPlaylists
import app.myspoti.spotify.SpotifySearch
import app.myspoti.spotify.bridge.Bridge
import app.myspoti.spotify.bridge.BridgeHost
import app.myspoti.spotify.gql.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import java.io.Closeable
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

@SuppressLint("SetJavaScriptEnabled")
class PublicCatalogueSession(context: Context) : BridgeHost, Closeable {
    init {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE) ||
            !WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            throw UnsupportedOperationException("Update Android System WebView to browse the public catalogue")
        }
    }
    private val bootstrap = PublicCatalogueSession::class.java.getResourceAsStream("/bridge/catalogue-bridge.js")
        ?.bufferedReader()?.use { it.readText() } ?: throw IOException("Public catalogue resources unavailable")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val failure = MutableStateFlow(false)
    private val incoming = MutableSharedFlow<String>(extraBufferCapacity = 16)
    override val messages: SharedFlow<String> = incoming
    private val bridge = Bridge(this, scope)
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS).callTimeout(25, TimeUnit.SECONDS).build()
    private var closed = false
    val view: WebView
    private val http: SpotifyHttp
    val search: SpotifySearch
    val catalog: SpotifyCatalog
    val playlists: SpotifyPlaylists

    init {
        view = WebView(context)
        androidx.webkit.ProfileStore.getInstance().getOrCreateProfile("distortion_public_catalogue")
        WebViewCompat.setProfile(view, "distortion_public_catalogue")
        view.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = true
        }
        android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(view, false)
        WebViewCompat.addWebMessageListener(view, "DistortionCatalogue", setOf(ORIGIN)) { _, message, origin, mainFrame, _ ->
            if (mainFrame && origin.scheme == "https" && origin.host == "open.spotify.com" && origin.port in listOf(-1, 443) && !closed) {
                message.data?.takeIf { it.length <= 1_048_576 }?.let(incoming::tryEmit)
            }
        }
        view.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) = request.deny()
        }
        view.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!request.isForMainFrame) return false
                val allowed = request.url.scheme == "https" && request.url.host == "open.spotify.com"
                if (!allowed) failure.value = true
                return !allowed
            }
            override fun onPageFinished(view: WebView, url: String) {
                val target = android.net.Uri.parse(url)
                if (!closed && target.scheme == "https" && target.host == "open.spotify.com") {
                    view.evaluateJavascript(bootstrap, null)
                }
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
                if (request.isForMainFrame) failure.value = true
            }
            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceResponse) {
                if (request.isForMainFrame && error.statusCode >= 400) failure.value = true
            }
            override fun onRenderProcessGone(view: WebView, detail: android.webkit.RenderProcessGoneDetail): Boolean {
                failure.value = true
                close()
                return true
            }
        }
        http = SpotifyHttp(client, bridge, Locale.getDefault().language, view.settings.userAgentString)
        val source = object : OpsSource {
            override val ops get() = app.myspoti.host.android.MyspotiRuntime.spotify.opsInUse
        }
        val direct = HttpGql(http, source, object : Gql {
            override suspend fun query(name: String, variables: kotlinx.serialization.json.JsonObject): kotlinx.serialization.json.JsonElement {
                throw IOException("Public catalogue temporarily unavailable")
            }
        })
        direct.refreshOperations = app.myspoti.host.android.MyspotiRuntime.spotify.httpGql.refreshOperations
        val public = PublicCatalogueGql(direct) { !http.isSignedIn() }
        search = SpotifySearch(public)
        catalog = SpotifyCatalog(public, http)
        playlists = SpotifyPlaylists(public, bridge, http)
        view.loadUrl("$ORIGIN/search")
    }

    suspend fun awaitReady() = withTimeout(30_000) {
        val state = combine(bridge.ready, failure) { ready, failed -> ready to failed }.first { it.first || it.second }
        if (state.second || closed) throw IOException("Public catalogue could not be loaded")
    }

    override suspend fun evaluate(script: String) = withContext(Dispatchers.Main.immediate) {
        check(!closed)
        view.evaluateJavascript(script, null)
    }

    override fun close() {
        if (closed) return
        closed = true
        failure.value = true
        bridge.pageGone()
        scope.cancel()
        client.dispatcher.cancelAll()
        client.connectionPool.evictAll()
        client.dispatcher.executorService.shutdown()
        runCatching { view.stopLoading() }
        (view.parent as? android.view.ViewGroup)?.removeView(view)
        view.destroy()
    }

    companion object { private const val ORIGIN = "https://open.spotify.com" }
}
