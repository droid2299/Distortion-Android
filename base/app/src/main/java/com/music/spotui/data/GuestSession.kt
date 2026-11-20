package com.music.spotui.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object GuestSession {
    var active by mutableStateOf(false)
        private set

    fun init(context: Context) {
        active = restoreGuest(
            context.getSharedPreferences("distortion_session", Context.MODE_PRIVATE).getBoolean("guest", false),
            context.getSharedPreferences("myspoti_prefs", Context.MODE_PRIVATE).getBoolean("LoggedIn", false),
        )
    }

    fun set(context: Context, enabled: Boolean) {
        active = enabled
        context.getSharedPreferences("distortion_session", Context.MODE_PRIVATE).edit().putBoolean("guest", enabled).apply()
    }
}

internal fun restoreGuest(requested: Boolean, signedIn: Boolean): Boolean = requested && !signedIn
