package com.music.bitchord.data.aimusic

import com.music.bitchord.data.innertube.InnertubeParser
import com.music.bitchord.data.model.ArtistRef
import com.music.bitchord.data.model.BrowseItem
import com.music.bitchord.data.model.BrowseType
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Base64

class AiMusicFilterTest {

    private val ai = "UCaiaiaiaiaiaiaiaiaiaiai"
    private val human = "UChumanhumanhumanhumanh"
    private val aiSpotify = "4PbKVheWA7ToOxOrqqzjol"

    private fun song(id: String, artistId: String? = null, artists: List<String> = emptyList(), local: Boolean = false) =
        Song(
            videoId = id, title = id, artist = "a", thumbnailUrl = null, artistId = artistId,
            artists = artists.map { ArtistRef("n", it) }, localUri = if (local) "content://x" else null,
        )

    private fun card(title: String, browseId: String? = null, artistIds: List<String> = emptyList()) =
        ShelfItem(title, "", null, if (browseId == null) title else null, browseId, artistIds = artistIds)

    @Before
    fun setUp() {
        AiMusicFilter.setLists(setOf(ai), setOf(aiSpotify))
        AiMusicFilter.enabled = true
    }

    @After
    fun tearDown() {
        AiMusicFilter.enabled = false
        AiMusicFilter.setLists(emptySet(), emptySet())
    }

    @Test
    fun aSongGoesWhenAnyCreditedArtistIsListed() {
        assertTrue(AiMusicFilter.isAi(song("a", artistId = ai)))
        assertTrue(AiMusicFilter.isAi(song("b", artistId = human, artists = listOf(human, ai))))
        assertFalse(AiMusicFilter.isAi(song("c", artistId = human)))
        assertFalse(AiMusicFilter.isAi(song("d")))
    }

    @Test
    fun aDeviceFileIsNeverFiltered() {
        assertFalse(AiMusicFilter.isAi(song("a", artistId = ai, local = true)))
    }

    @Test
    fun theTappedSongStaysInItsRadio() {
        val queue = listOf(song("seed", artistId = ai), song("x", artistId = ai), song("y", artistId = human))
        assertEquals(listOf("seed", "y"), AiMusicFilter.songs(queue, keepVideoId = "seed").map { it.videoId })
    }

    @Test
    fun shelvesLoseAiCardsAndAShelfOfOnlyAiCardsGoes() {
        val shelves = listOf(
            HomeShelf("Mixed", listOf(card("a", artistIds = listOf(ai)), card("b", artistIds = listOf(human)))),
            HomeShelf("Only AI", listOf(card("c", browseId = ai))),
            HomeShelf("Empty", emptyList()),
        )
        val out = AiMusicFilter.shelves(shelves)
        assertEquals(listOf("Mixed", "Empty"), out.map { it.title })
        assertEquals(listOf("b"), out.first().items.map { it.title })
    }

    @Test
    fun searchLosesAiSongsAndAiArtists() {
        val rows = listOf(
            SearchResult.TopTrack(song("top", artistId = ai)),
            SearchResult.Track(song("t", artistId = human)),
            SearchResult.Browse(BrowseItem(ai, "AI act", "", null, BrowseType.ARTIST)),
            SearchResult.Browse(BrowseItem("MPREb_x", "Album", "", null, BrowseType.ALBUM)),
        )
        val out = AiMusicFilter.searchRows(rows)
        assertEquals(2, out.size)
        assertTrue(out[0] is SearchResult.Track)
    }

    @Test
    fun offOrWithoutListsNothingIsFiltered() {
        val queue = listOf(song("x", artistId = ai))
        AiMusicFilter.enabled = false
        assertEquals(queue, AiMusicFilter.songs(queue))
        AiMusicFilter.enabled = true
        AiMusicFilter.setLists(emptySet(), emptySet())
        assertFalse(AiMusicFilter.active)
        assertEquals(queue, AiMusicFilter.songs(queue))
    }

    @Test
    fun homeCardsCarryTheChannelsTheirSubtitleLinks() {
        val home = Json.parseToJsonElement(
            """
            {"contents":{"singleColumnBrowseResultsRenderer":{"tabs":[{"tabRenderer":{"content":{"sectionListRenderer":{"contents":[
              {"musicCarouselShelfRenderer":{
                "header":{"musicCarouselShelfBasicHeaderRenderer":{"title":{"runs":[{"text":"Quick picks"}]}}},
                "contents":[{"musicTwoRowItemRenderer":{
                  "title":{"runs":[{"text":"Dust on the Wind"}]},
                  "subtitle":{"runs":[{"text":"Song"},{"text":" • "},
                    {"text":"The Velvet Sundown","navigationEndpoint":{"browseEndpoint":{"browseId":"UCLeR-eGh2OFy9ydX7IUcOTw"}}}]},
                  "navigationEndpoint":{"watchEndpoint":{"videoId":"eQJ9IWoclhk"}}
                }}]
              }}
            ]}}}}]}}}
            """.trimIndent(),
        ).jsonObject
        val item = InnertubeParser.parseHome(home).single().items.single()
        assertEquals(listOf("UCLeR-eGh2OFy9ydX7IUcOTw"), item.artistIds)
    }
}

class AiMusicListsTest {

    @Test
    fun readsTheFlowManifestAndSnapshot() {
        val payload = """{"schema":1,"list":"aimusic","version":9,"snapshot":{"path":"snapshot/9.json","sha256":"ab"}}"""
        val manifest = """{"key_id":"k","payload":"${Base64.getEncoder().encodeToString(payload.toByteArray())}","sig":"s"}"""
        assertEquals(AiMusicLists.SnapshotRef("snapshot/9.json", "ab"), AiMusicLists.flowSnapshotRef(manifest))
        val snapshot = """{"schema":1,"list":"aimusic","version":9,"block":{"handles":[],"ids":["UC--ppxarUtjMpI6lZfApKzg","bad"]},"warn":{"handles":[],"ids":[]}}"""
        assertEquals(setOf("UC--ppxarUtjMpI6lZfApKzg"), AiMusicLists.flowChannels(snapshot))
    }

    @Test
    fun refusesAManifestForAnotherListOrAPathOutOfItsFolder() {
        fun manifest(payload: String) =
            """{"payload":"${Base64.getEncoder().encodeToString(payload.toByteArray())}"}"""
        assertNull(AiMusicLists.flowSnapshotRef(manifest("""{"list":"aislist","snapshot":{"path":"snapshot/1.json"}}""")))
        assertNull(AiMusicLists.flowSnapshotRef(manifest("""{"list":"aimusic","snapshot":{"path":"../x.json"}}""")))
        assertNull(AiMusicLists.flowSnapshotRef("<html>"))
    }

    @Test
    fun readsTheCrawlerList() {
        val body = """{"version":1,"count":2,"artists":[{"name":"A","channelId":"UC-0Fh6_E6q8uzyFv6j4hPQg"},{"name":"B","channelId":"nope"}]}"""
        assertEquals(setOf("UC-0Fh6_E6q8uzyFv6j4hPQg"), AiMusicLists.crawlerChannels(body))
        assertTrue(AiMusicLists.crawlerChannels("[]").isEmpty())
    }

    @Test
    fun readsTheSpotifyCsv() {
        val csv = "artist,id\nAtomship,4PbKVheWA7ToOxOrqqzjol\n\"Comma, Band\",32PcXsAFr3ArYaHSYr8qGh\nbroken line\n"
        assertEquals(setOf("4PbKVheWA7ToOxOrqqzjol", "32PcXsAFr3ArYaHSYr8qGh"), AiMusicLists.spotifyArtists(csv))
    }
}
