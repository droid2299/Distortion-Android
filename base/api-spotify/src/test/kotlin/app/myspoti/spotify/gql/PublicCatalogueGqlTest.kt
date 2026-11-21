package app.myspoti.spotify.gql

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import app.myspoti.spotify.bridge.Bridge
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PublicCatalogueGqlTest {
    @Test fun `only public reads reach the transport`() = runBlocking {
        var calls = 0
        val gql = PublicCatalogueGql(object : Gql {
            override suspend fun query(name: String, variables: JsonObject) =
                Bridge.json.parseToJsonElement("""{"data":{"searchV2":{"tracksV2":{"items":[]}}}}""").also { calls++ }
        }) { true }
        gql.query("searchTracks", JsonObject(emptyMap()))
        for (name in listOf("addToPlaylist", "fetchLibraryTracks", "fetchRecentSearches", "futureOperation")) {
            assertThrows(IllegalArgumentException::class.java) { runBlocking { gql.query(name, JsonObject(emptyMap())) } }
        }
        assertEquals(1, calls)
    }

    @Test fun `signed-in sessions are rejected rather than borrowed`() {
        var calls = 0
        val gql = PublicCatalogueGql(object : Gql {
            override suspend fun query(name: String, variables: JsonObject) = JsonObject(emptyMap()).also { calls++ }
        }) { false }
        assertThrows(IllegalStateException::class.java) { runBlocking { gql.query("searchTracks", JsonObject(emptyMap())) } }
        assertEquals(0, calls)
    }

    @Test fun `missing search collections are failures rather than empty successful results`() {
        val gql = PublicCatalogueGql(object : Gql {
            override suspend fun query(name: String, variables: JsonObject) =
                Bridge.json.parseToJsonElement("""{"data":{"searchV2":null},"errors":[{"message":"unavailable"}]}""")
        }) { true }
        assertThrows(GqlException::class.java) { runBlocking { gql.query("searchTracks", JsonObject(emptyMap())) } }
    }
}
