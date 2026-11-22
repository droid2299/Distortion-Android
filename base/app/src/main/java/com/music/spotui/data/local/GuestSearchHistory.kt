package com.music.spotui.data.local

import android.content.Context
import org.json.JSONArray

/** Search terms remain on this device and are separate from Spotify account history. */
class GuestSearchHistory(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("distortion_guest_search", Context.MODE_PRIVATE)

    fun read(): List<String> = decode(preferences.getString(KEY, "[]").orEmpty())
    fun add(query: String): List<String> = update(merge(read(), query))
    fun remove(query: String): List<String> = update(read().filterNot { it.equals(query, ignoreCase = true) })
    fun clear(): List<String> = update(emptyList())

    private fun update(terms: List<String>): List<String> {
        preferences.edit().putString(KEY, JSONArray(terms).toString()).apply()
        return terms
    }

    companion object {
        private const val KEY = "queries"
        private const val LIMIT = 20
        internal fun merge(current: List<String>, query: String): List<String> {
            val value = query.trim().take(200)
            if (value.length < 2) return current
            return (listOf(value) + current.filterNot { it.equals(value, ignoreCase = true) }).take(LIMIT)
        }
        internal fun decode(raw: String): List<String> = runCatching {
            val array = JSONArray(raw)
            (0 until minOf(array.length(), LIMIT)).mapNotNull { i ->
                (array.opt(i) as? String)?.trim()?.takeIf { it.length in 2..200 }
            }.distinctBy { it.lowercase() }
        }.getOrDefault(emptyList())
    }
}
