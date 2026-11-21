package app.myspoti.host.android

internal enum class BrowserPresentation {
    PLAYER, SIGN_IN;

    companion object {
        fun forUrl(url: String): BrowserPresentation? {
            val uri = runCatching { java.net.URI(url) }.getOrNull() ?: return null
            if (!uri.scheme.equals("https", ignoreCase = true) || uri.rawUserInfo != null) return null
            return when (uri.host?.lowercase()) {
                "accounts.spotify.com", "challenge.spotify.com" -> SIGN_IN
                "open.spotify.com" -> if (LoginLoad.isLoginUrl(url)) SIGN_IN else PLAYER
                else -> null
            }
        }
    }
}
