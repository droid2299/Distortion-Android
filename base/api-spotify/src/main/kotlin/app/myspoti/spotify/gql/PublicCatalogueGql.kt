package app.myspoti.spotify.gql

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray

class PublicCatalogueGql(
    private val delegate: Gql,
    private val anonymous: suspend () -> Boolean,
) : Gql {
    override suspend fun query(name: String, variables: JsonObject): JsonElement {
        require(name in OPERATIONS) { "Operation is not available in the public catalogue" }
        check(anonymous()) { "Public catalogue requires an anonymous session" }
        val result = delegate.query(name, variables)
        val data = (result as? JsonObject)?.get("data") as? JsonObject
            ?: throw GqlException(name, "Public catalogue response has no data")
        val searchCollection = when (name) {
            "searchTracks" -> "tracksV2"
            "searchArtists" -> "artists"
            "searchAlbums" -> "albumsV2"
            "searchPlaylists" -> "playlists"
            else -> null
        }
        if (searchCollection != null) {
            val collection = (data["searchV2"] as? JsonObject)?.get(searchCollection) as? JsonObject
            if (collection?.get("items") !is JsonArray) throw GqlException(name, "Public catalogue search response changed")
        }
        return result
    }

    companion object {
        private val OPERATIONS = setOf(
            "searchTracks", "searchArtists", "searchAlbums", "searchPlaylists",
            "getTrack", "getAlbum", "fetchPlaylist", "fetchPlaylistContents", "queryArtistOverview",
            "queryArtistDiscographyAll", "queryArtistRelated", "queryArtistDiscoveredOn",
        )
    }
}
