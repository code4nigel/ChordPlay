/*
 * Copyright (C) 2026 Shivanshu (code4nigel) <https://github.com/code4nigel/ChordPlay>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
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
