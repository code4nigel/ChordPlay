package com.music.bitchord.data.innertube

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlaylistMembershipTest {
    @Test
    fun actionEndpointsDoNotProveMembership() {
        val response = Json.parseToJsonElement(
            """
            {"contents":{"addToPlaylistRenderer":{"playlists":[
              {"playlistAddToOptionRenderer":{"playlistId":"VLpresent","addToPlaylistServiceEndpoint":{},"removeFromPlaylistServiceEndpoint":{}}},
              {"playlistAddToOptionRenderer":{"playlistId":"absent","addToPlaylistServiceEndpoint":{}}},
              {"playlistAddToOptionRenderer":{"playlistId":"legacy","containsSelectedVideos":"ALL"}},
              {"playlistAddToOptionRenderer":{"playlistId":"legacyAbsent","containsSelectedVideos":"NONE","removeFromPlaylistServiceEndpoint":{}}},
              {"playlistAddToOptionRenderer":{"playlistId":"unknown","containsSelectedVideos":"SOME"}}
            ]}}}
            """.trimIndent(),
        )
        assertEquals(
            mapOf("present" to null, "absent" to null, "legacy" to true, "legacyAbsent" to false, "unknown" to null),
            InnertubeParser.parsePlaylistMembership(response),
        )
    }

    @Test
    fun malformedResponseDoesNotBecomeAnEmptyMembershipSuccess() {
        assertFailsWith<IllegalStateException> {
            InnertubeParser.parsePlaylistMembership(Json.parseToJsonElement("""{"contents":{}}"""))
        }
    }
}
