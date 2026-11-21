package com.music.spotui.data.catalogue

import app.myspoti.api.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

enum class CatalogueKind { TRACK, ALBUM, ARTIST, PLAYLIST }
enum class CatalogueSectionKind { SONGS, ALBUMS, ARTISTS, RELATED_ARTISTS }

data class CatalogueEntry(val uri: String, val title: String, val subtitle: String, val image: String, val kind: CatalogueKind)
data class CataloguePage(val entries: List<CatalogueEntry>, val nextOffset: Int?)
data class CatalogueSection(val kind: CatalogueSectionKind, val entries: List<CatalogueEntry>)
data class CatalogueDetails(val entry: CatalogueEntry, val sections: List<CatalogueSection>, val nextOffset: Int? = null)

private fun List<Image>.artwork() = best(256)?.url.orEmpty()
internal fun Track.catalogueEntry() = CatalogueEntry(uri, name, artists.joinToString { it.name }, album?.images?.artwork().orEmpty(), CatalogueKind.TRACK)
internal fun Album.catalogueEntry() = CatalogueEntry(uri, name, artists.joinToString { it.name }, images.artwork(), CatalogueKind.ALBUM)
internal fun Artist.catalogueEntry() = CatalogueEntry(uri, name, "", images.artwork(), CatalogueKind.ARTIST)
internal fun Playlist.catalogueEntry() = CatalogueEntry(uri, name, owner.orEmpty(), images.artwork(), CatalogueKind.PLAYLIST)

suspend fun PublicCatalogueSession.searchPage(kind: CatalogueKind, query: String, offset: Int = 0): CataloguePage {
    require(query.trim().length in 2..200 && offset in 0..500)
    awaitReady()
    return when (kind) {
        CatalogueKind.TRACK -> search.tracks(query.trim(), offset, 20).let { CataloguePage(it.items.map(Track::catalogueEntry), it.nextOffset) }
        CatalogueKind.ALBUM -> search.albums(query.trim(), offset, 20).let { CataloguePage(it.items.map(Album::catalogueEntry), it.nextOffset) }
        CatalogueKind.ARTIST -> search.artists(query.trim(), offset, 20).let { CataloguePage(it.items.map(Artist::catalogueEntry), it.nextOffset) }
        CatalogueKind.PLAYLIST -> search.playlists(query.trim(), offset, 20).let { CataloguePage(it.items.map(Playlist::catalogueEntry), it.nextOffset) }
    }
}

suspend fun PublicCatalogueSession.details(entry: CatalogueEntry): CatalogueDetails {
    awaitReady()
    return when (entry.kind) {
        CatalogueKind.TRACK -> {
            val track = catalog.track(entry.uri)
            val album = track.album?.let { CatalogueEntry(it.uri, it.name, "", it.images.artwork(), CatalogueKind.ALBUM) }
            val artists = track.artists.map { CatalogueEntry(it.uri, it.name, "", "", CatalogueKind.ARTIST) }
            CatalogueDetails(track.catalogueEntry(), listOf(
                CatalogueSection(CatalogueSectionKind.ALBUMS, listOfNotNull(album)),
                CatalogueSection(CatalogueSectionKind.ARTISTS, artists),
            ))
        }
        CatalogueKind.ALBUM -> coroutineScope {
            val album = async { catalog.album(entry.uri) }
            val tracks = catalog.albumTracks(entry.uri, 0, 20)
            CatalogueDetails(album.await().catalogueEntry(), listOf(CatalogueSection(CatalogueSectionKind.SONGS, tracks.items.map(Track::catalogueEntry))), tracks.nextOffset)
        }
        CatalogueKind.PLAYLIST -> coroutineScope {
            val playlist = async { playlists.playlist(entry.uri) }
            val tracks = playlists.tracks(entry.uri, 0, 20)
            CatalogueDetails(playlist.await().catalogueEntry(), listOf(CatalogueSection(CatalogueSectionKind.SONGS, tracks.items.map(Track::catalogueEntry))), tracks.nextOffset)
        }
        CatalogueKind.ARTIST -> {
            val artist = catalog.artistPage(entry.uri)
            CatalogueDetails(artist.artist.catalogueEntry(), listOf(
                CatalogueSection(CatalogueSectionKind.SONGS, artist.topTracks.map { it.track.catalogueEntry() }),
                CatalogueSection(CatalogueSectionKind.ALBUMS, artist.popularReleases.map(Album::catalogueEntry)),
                CatalogueSection(CatalogueSectionKind.RELATED_ARTISTS, artist.related.map(Artist::catalogueEntry)),
            ))
        }
    }
}

suspend fun PublicCatalogueSession.moreTracks(entry: CatalogueEntry, offset: Int): CataloguePage {
    require(offset in 0..500)
    awaitReady()
    val page = when (entry.kind) {
        CatalogueKind.ALBUM -> catalog.albumTracks(entry.uri, offset, 20)
        CatalogueKind.PLAYLIST -> playlists.tracks(entry.uri, offset, 20)
        else -> error("This catalogue item has no track pages")
    }
    return CataloguePage(page.items.map(Track::catalogueEntry), page.nextOffset)
}
