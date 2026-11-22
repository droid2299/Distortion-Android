package com.music.spotui.data.catalogue

import app.myspoti.api.Album
import app.myspoti.api.best
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth

data class GuestRelease(val entry: CatalogueEntry, val date: String, internal val sortDate: LocalDate)

/** Uses supplied catalogue dates only; future and undated releases are not called recent. */
internal fun guestRecentReleases(albums: List<Album>, today: LocalDate): List<GuestRelease> = albums.mapNotNull { album ->
    val raw = album.releaseDate?.trim().orEmpty()
    val date = runCatching {
        when (raw.length) {
            4 -> Year.parse(raw).atDay(1)
            7 -> YearMonth.parse(raw).atDay(1)
            else -> LocalDate.parse(raw.take(10))
        }
    }.getOrNull() ?: return@mapNotNull null
    if (date.isAfter(today) || date.isBefore(today.minusYears(1))) return@mapNotNull null
    if (!Regex("spotify:album:[A-Za-z0-9]{22}").matches(album.uri) || album.name.isBlank()) return@mapNotNull null
    GuestRelease(CatalogueEntry(album.uri, album.name, album.artists.joinToString { it.name },
        album.images.best(256)?.url.orEmpty(), CatalogueKind.ALBUM), raw, date)
}.distinctBy { it.entry.uri }.sortedByDescending { it.sortDate }.take(30)
