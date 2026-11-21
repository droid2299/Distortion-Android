package app.myspoti.spotify

import app.myspoti.spotify.bridge.Bridge
import app.myspoti.spotify.gql.Gql
import app.myspoti.spotify.gql.GqlException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ArtistDiscoveredOnTest {
    private val artist = "spotify:artist:${"a".repeat(22)}"
    private val first = "spotify:playlist:${"b".repeat(22)}"
    private val second = "spotify:playlist:${"c".repeat(22)}"

    private fun catalog(items: String?, responseUri: String = artist) = SpotifyCatalog(object : Gql {
        override suspend fun query(name: String, variables: JsonObject) = Bridge.json.parseToJsonElement(
            """{"data":{"artistUnion":{"__typename":"Artist","uri":"$responseUri","relatedContent":{"discoveredOnV2":{${items?.let { "\"items\":$it" }.orEmpty()}}}}}}"""
        )
    })

    @Test fun `server order and unavailable slots are preserved`() = runBlocking {
        val value = catalog("""[{"data":{"__typename":"Playlist","uri":"$first","name":"First"}},{"data":{"__typename":"GenericError"}},{"data":{"__typename":"Playlist","uri":"$second","name":"Second"}}]""").discoveredOn(artist)
        assertEquals(listOf(first, null, second), value.map { it?.uri })
        assertEquals(listOf("First", null, "Second"), value.map { it?.name })
    }

    @Test fun `an explicitly empty selection is valid`() = runBlocking {
        assertTrue(catalog("[]").discoveredOn(artist).isEmpty())
    }

    @Test fun `changed missing or mismatched data never becomes an empty success`() {
        for (items in listOf(null, "null", "{}", "[{}]", """[{"data":{"__typename":"Playlist","uri":"$first","name":""}}]""", """[{"data":{"__typename":"Artist","uri":"$artist","name":"Wrong type"}}]""")) {
            assertThrows(GqlException::class.java) { runBlocking { catalog(items).discoveredOn(artist) } }
        }
        assertThrows(GqlException::class.java) { runBlocking { catalog("[]", "spotify:artist:${"d".repeat(22)}").discoveredOn(artist) } }
    }

    @Test fun `non-artist input fails before any request`() {
        var requests = 0
        val catalog = SpotifyCatalog(object : Gql {
            override suspend fun query(name: String, variables: JsonObject): kotlinx.serialization.json.JsonElement {
                requests++
                error("Unexpected request")
            }
        })
        for (uri in listOf("", "spotify:track:${"a".repeat(22)}", "https://example.com/artist")) {
            assertThrows(IllegalArgumentException::class.java) { runBlocking { catalog.discoveredOn(uri) } }
        }
        assertEquals(0, requests)
    }
}
