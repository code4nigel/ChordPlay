package com.music.bitchord.data.innertube

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PlaylistEntriesTest {
    @Test
    fun playlistShelfKeepsRepeatedVideosInTheirOwnRows() {
        fun row(videoId: String, rowId: String) = """
            {"musicResponsiveListItemRenderer": {
              "playlistItemData": {"videoId": "$videoId", "playlistSetVideoId": "$rowId"},
              "flexColumns": [{"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "Song"}]}}}]
            }}
        """.trimIndent()
        val rows = listOf(row("abcdefghijk", "one"), row("mnopqrstuvw", "two"), row("abcdefghijk", "three"))
        val payload = Json.parseToJsonElement("""
            {"contents": {"singleColumnBrowseResultsRenderer": {
              "musicPlaylistShelfRenderer": {"contents": [${rows.joinToString()}]}
            }}}
        """.trimIndent())
        val shelf = assertNotNull(InnertubeParser.parsePlaylistShelf(payload))
        assertEquals(listOf("abcdefghijk", "mnopqrstuvw", "abcdefghijk"), shelf.songs.map { it.videoId })
        assertEquals(listOf("one", "two", "three"), shelf.songs.map { it.setVideoId })
    }

    @Test
    fun explicitEmptyShelfIsDifferentFromAMalformedTrackOrSuggestionsOnly() {
        fun shelf(content: String) = InnertubeParser.parsePlaylistShelf(Json.parseToJsonElement(
            """{"contents":{"singleColumnBrowseResultsRenderer":{$content}}}""",
        ))
        val empty = assertNotNull(shelf(""""musicPlaylistShelfRenderer":{"contents":[]}"""))
        val unparsed = assertNotNull(shelf(""""musicPlaylistShelfRenderer":{"contents":[{"musicResponsiveListItemRenderer":{}}]}"""))
        val suggestions = assertNotNull(shelf(""""musicShelfRenderer":{"title":{"runs":[{"text":"Suggestions"}]}}"""))
        assertTrue(empty.hasTrackShelf)
        assertFalse(empty.hasUnparsedTracks)
        assertTrue(unparsed.hasUnparsedTracks)
        assertFalse(suggestions.hasTrackShelf)
        assertEquals(emptyList(), empty.songs)
    }

    @Test
    fun readsAppendedRowsOnlyForAKnownPlaylistContinuation() {
        val response = Json.parseToJsonElement("""
            {"onResponseReceivedActions":[{"appendContinuationItemsAction":{"continuationItems":[
              {"musicResponsiveListItemRenderer":{
                "playlistItemData":{"videoId":"abcdefghijk","playlistSetVideoId":"row"},
                "flexColumns":[{"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Song"}]}}}]
              }},
              {"continuationItemRenderer":{"continuationEndpoint":{"commandExecutorCommand":{"commands":[
                {"continuationCommand":{"request":"CONTINUATION_REQUEST_TYPE_BROWSE","token":"next"}}
              ]}}}}
            ]}}]}
        """.trimIndent())
        assertEquals(null, InnertubeParser.parsePlaylistShelf(response))
        val page = assertNotNull(InnertubeParser.parsePlaylistShelf(response, playlistContinuation = true))
        assertEquals("row", page.trackOccurrences.single().setVideoId)
        assertEquals("next", page.continuation)
        assertFalse(page.hasUnparsedTracks)
    }
}
