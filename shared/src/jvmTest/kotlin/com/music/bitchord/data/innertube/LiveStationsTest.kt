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

import com.music.bitchord.data.model.LiveStationCategory
import com.music.bitchord.data.settings.VideoQuality
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LiveStationsTest {

    private fun parseJson(raw: String) = Json.parseToJsonElement(raw).jsonObject

    @Test
    fun parseHomeParsesListenTogetherStationShelfAndPreservesLiveMetadata() {
        val response = parseJson(
            """
            {
              "contents": {
                "singleColumnBrowseResultsRenderer": {
                  "tabs": [
                    {
                      "tabRenderer": {
                        "content": {
                          "sectionListRenderer": {
                            "contents": [
                              {
                                "musicCarouselShelfRenderer": {
                                  "header": {
                                    "musicCarouselShelfBasicHeaderRenderer": {
                                      "strapline": { "runs": [ { "text": "STATION" } ] },
                                      "title": { "runs": [ { "text": "Listen together" } ] }
                                    }
                                  },
                                  "contents": [
                                    {
                                      "musicTwoRowItemRenderer": {
                                        "title": { "runs": [ { "text": "Michael Jackson: The Station" } ] },
                                        "subtitle": { "runs": [ { "text": "Michael Jackson • 4.2K watching" } ] },
                                        "navigationEndpoint": {
                                          "watchEndpoint": { "videoId": "liveMJ123" }
                                        },
                                        "thumbnailRenderer": {
                                          "musicThumbnailRenderer": {
                                            "thumbnail": {
                                              "thumbnails": [
                                                { "url": "https://i.ytimg.com/vi/liveMJ123/hqdefault.jpg", "width": 480, "height": 270 }
                                              ]
                                            }
                                          }
                                        },
                                        "subtitleBadges": [
                                          {
                                            "metadataBadgeRenderer": {
                                              "style": "BADGE_STYLE_TYPE_LIVE_NOW",
                                              "label": "LIVE"
                                            }
                                          }
                                        ]
                                      }
                                    }
                                  ]
                                }
                              }
                            ]
                          }
                        }
                      }
                    }
                  ]
                }
              }
            }
            """.trimIndent(),
        )

        val shelves = InnertubeParser.parseHome(response)
        assertEquals(1, shelves.size)
        val shelf = shelves.single()
        assertEquals("Listen together", shelf.title)
        assertEquals(1, shelf.items.size)
        val item = shelf.items.single()
        assertEquals("liveMJ123", item.videoId)
        assertEquals("Michael Jackson: The Station", item.title)
        assertTrue(item.isLive)
        assertTrue(item.isVideo)
        assertEquals("4.2K watching", item.liveViewersText)
        assertEquals(LiveStationCategory.ARTIST_STATIONS, item.liveCategory)
        assertEquals("Michael Jackson", InnertubeParser.artistFromSubtitle(item.subtitle))
    }

    @Test
    fun parseYouTubeLiveSearchKeepsLiveAndSkipsVodAndUpcoming() {
        val response = parseJson(
            """
            {
              "contents": {
                "twoColumnSearchResultsRenderer": {
                  "primaryContents": {
                    "sectionListRenderer": {
                      "contents": [
                        {
                          "itemSectionRenderer": {
                            "contents": [
                              {
                                "videoRenderer": {
                                  "videoId": "liveLofi1",
                                  "title": { "runs": [ { "text": "lofi hip hop radio - beats to relax/study to" } ] },
                                  "ownerText": { "runs": [ { "text": "Lofi Girl" } ] },
                                  "viewCountText": { "runs": [ { "text": "24K watching" } ] },
                                  "badges": [
                                    { "metadataBadgeRenderer": { "style": "BADGE_STYLE_TYPE_LIVE_NOW", "label": "LIVE" } }
                                  ],
                                  "thumbnail": {
                                    "thumbnails": [
                                      { "url": "https://i.ytimg.com/vi/liveLofi1/hqdefault.jpg", "width": 480, "height": 270 }
                                    ]
                                  }
                                }
                              },
                              {
                                "videoRenderer": {
                                  "videoId": "vodNormal2",
                                  "title": { "runs": [ { "text": "Regular Chill Mix" } ] },
                                  "ownerText": { "runs": [ { "text": "Channel" } ] },
                                  "viewCountText": { "simpleText": "1.5M views" }
                                }
                              },
                              {
                                "videoRenderer": {
                                  "videoId": "upcoming3",
                                  "title": { "runs": [ { "text": "Upcoming Premiere" } ] },
                                  "ownerText": { "runs": [ { "text": "Premiere Channel" } ] },
                                  "viewCountText": { "runs": [ { "text": "500 waiting" } ] },
                                  "badges": [
                                    { "metadataBadgeRenderer": { "style": "BADGE_STYLE_TYPE_LIVE_NOW", "label": "LIVE" } }
                                  ],
                                  "thumbnailOverlays": [
                                    { "thumbnailOverlayTimeStatusRenderer": { "style": "UPCOMING" } }
                                  ]
                                }
                              }
                            ]
                          }
                        }
                      ]
                    }
                  }
                }
              }
            }
            """.trimIndent(),
        )

        val items = InnertubeParser.parseYouTubeLiveSearch(response, LiveStationCategory.LOFI_CHILL)
        assertEquals(1, items.size)
        val live = items.single()
        assertEquals("liveLofi1", live.videoId)
        assertEquals("Lofi Girl", live.subtitle)
        assertEquals("24K watching", live.liveViewersText)
        assertEquals(LiveStationCategory.LOFI_CHILL, live.liveCategory)
        assertTrue(live.isLive)
        assertTrue(live.isVideo)
    }

    @Test
    fun parseArtistPageRetainsLiveStationCardWhileDroppingUnbrowsableVod() {
        val response = parseJson(
            """
            {
              "header": {
                "musicImmersiveHeaderRenderer": {
                  "title": { "runs": [ { "text": "Michael Jackson" } ] }
                }
              },
              "contents": {
                "singleColumnBrowseResultsRenderer": {
                  "tabs": [
                    {
                      "tabRenderer": {
                        "content": {
                          "sectionListRenderer": {
                            "contents": [
                              {
                                "musicCarouselShelfRenderer": {
                                  "header": {
                                    "musicCarouselShelfBasicHeaderRenderer": {
                                      "title": { "runs": [ { "text": "Live stations" } ] }
                                    }
                                  },
                                  "contents": [
                                    {
                                      "musicTwoRowItemRenderer": {
                                        "title": { "runs": [ { "text": "Michael Jackson: The Station" } ] },
                                        "subtitle": { "runs": [ { "text": "2.1K watching" } ] },
                                        "navigationEndpoint": {
                                          "watchEndpoint": { "videoId": "artistLive1" }
                                        },
                                        "subtitleBadges": [
                                          { "metadataBadgeRenderer": { "style": "BADGE_STYLE_TYPE_LIVE_NOW" } }
                                        ]
                                      }
                                    },
                                    {
                                      "musicTwoRowItemRenderer": {
                                        "title": { "runs": [ { "text": "Random Live Clip" } ] },
                                        "subtitle": { "runs": [ { "text": "900K views" } ] },
                                        "navigationEndpoint": {
                                          "watchEndpoint": { "videoId": "vodDrop2" }
                                        }
                                      }
                                    }
                                  ]
                                }
                              }
                            ]
                          }
                        }
                      }
                    }
                  ]
                }
              }
            }
            """.trimIndent(),
        )

        val page = InnertubeParser.parseArtistPage(response)
        assertEquals(1, page.sections.size)
        val items = page.sections.single().items
        assertEquals(1, items.size)
        assertEquals("artistLive1", items.single().videoId)
        assertTrue(items.single().isLive)
    }

    @Test
    fun streamResolverLiveAndHlsDetectionAndKnownLiveCacheWork() {
        assertTrue(StreamResolver.isHlsManifestUrl("https://manifest.googlevideo.com/api/manifest/hls_variant/foo/playlist.m3u8"))
        assertTrue(StreamResolver.isHlsManifestUrl("https://manifest.googlevideo.com/api/manifest/hls_playlist/itag/95"))
        assertTrue(StreamResolver.isHlsManifestUrl("https://example.com/hls_playlist?id=1"))
        assertFalse(StreamResolver.isHlsManifestUrl("https://rr1---sn-4g5e6nzz.googlevideo.com/videoplayback?itag=251"))

        assertTrue(StreamResolver.isLiveUrl("https://rr1.googlevideo.com/videoplayback?itag=95&live=1"))
        assertTrue(StreamResolver.isLiveUrl("https://manifest.googlevideo.com/api/manifest/hls_variant/source/yt_live_broadcast/id/x"))
        assertTrue(StreamResolver.isLiveUrl("https://manifest.googlevideo.com/api/manifest/hls_variant/live/1/id/x"))
        assertFalse(StreamResolver.isLiveUrl("https://rr1.googlevideo.com/videoplayback?itag=251&clen=12345"))

        val uniqueId = "live_test_station_xyz"
        assertFalse(StreamResolver.isKnownLive(uniqueId))
        StreamResolver.markKnownLive(uniqueId)
        assertTrue(StreamResolver.isKnownLive(uniqueId))
        assertFalse(StreamResolver.isKnownLive(null))
        StreamResolver.invalidate(uniqueId)
    }

    @Test
    fun videoQualityAutoIsFirstAndSelectVideoCandidateAdaptsWithoutFailing() {
        assertEquals(VideoQuality.AUTO, VideoQuality.entries.first())
        assertTrue(VideoQuality.AUTO.isAuto)
        assertEquals("Auto", VideoQuality.AUTO.label)
        assertEquals("AUTO", VideoQuality.AUTO.badge)
        assertFalse(VideoQuality.Q_720P.isAuto)
        assertTrue(
            listOf(VideoQuality.Q_360P, VideoQuality.Q_480P, VideoQuality.Q_720P, VideoQuality.Q_1080P)
                .all { it in VideoQuality.entries && !it.isAuto },
        )

        val candidates = listOf(
            StreamResolver.CandidateVideoStream("https://example.com/240.mp4", 240, isMp4 = true, isMuxed = true, mimeType = "video/mp4"),
            StreamResolver.CandidateVideoStream("https://example.com/480.mp4", 480, isMp4 = true, isMuxed = false, mimeType = "video/mp4"),
            StreamResolver.CandidateVideoStream("https://example.com/1440.webm", 1440, isMp4 = false, isMuxed = false, mimeType = "video/webm"),
        )
        // Auto picks highest valid height
        val autoPicked = StreamResolver.selectVideoCandidate(candidates, VideoQuality.AUTO.maxHeight)
        assertNotNull(autoPicked)
        assertEquals(1440, autoPicked.height)

        // When no candidate is at or below 360p (e.g. only 480p and 720p exist), falls back safely to lowest valid height
        val highOnly = listOf(
            StreamResolver.CandidateVideoStream("https://example.com/480.mp4", 480, isMp4 = true, isMuxed = false, mimeType = "video/mp4"),
            StreamResolver.CandidateVideoStream("https://example.com/720.mp4", 720, isMp4 = true, isMuxed = false, mimeType = "video/mp4"),
        )
        val fallbackPicked = StreamResolver.selectVideoCandidate(highOnly, 360)
        assertNotNull(fallbackPicked)
        assertEquals(480, fallbackPicked.height)

        assertNull(StreamResolver.selectVideoCandidate(emptyList(), VideoQuality.AUTO.maxHeight))
    }

    @Test
    fun probeHlsVerifiesMasterVariantAndSegmentAndRejectsSegment403() {
        val okServer = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                    "/master.m3u8" -> MockResponse()
                        .setResponseCode(200)
                        .addHeader("Content-Type", "application/vnd.apple.mpegurl")
                        .setBody(
                            """
                            #EXTM3U
                            #EXT-X-STREAM-INF:BANDWIDTH=269034,RESOLUTION=256x144
                            /variant144.m3u8
                            #EXT-X-STREAM-INF:BANDWIDTH=507418,RESOLUTION=426x240
                            /variant240.m3u8
                            #EXT-X-STREAM-INF:BANDWIDTH=962461,RESOLUTION=640x360
                            /variant360.m3u8
                            #EXT-X-STREAM-INF:BANDWIDTH=1282517,RESOLUTION=854x480
                            /variant480.m3u8
                            #EXT-X-STREAM-INF:BANDWIDTH=2447970,RESOLUTION=1280x720
                            /variant720.m3u8
                            #EXT-X-STREAM-INF:BANDWIDTH=4561186,RESOLUTION=1920x1080
                            /variant1080.m3u8
                            """.trimIndent(),
                        )
                    "/variant144.m3u8" -> MockResponse()
                        .setResponseCode(200)
                        .addHeader("Content-Type", "application/vnd.apple.mpegurl")
                        .setBody(
                            """
                            #EXTM3U
                            #EXT-X-TARGETDURATION:2
                            #EXTINF:2.0,
                            /seg1.ts
                            """.trimIndent(),
                        )
                    "/seg1.ts" -> MockResponse()
                        .setResponseCode(200)
                        .addHeader("Content-Type", "video/mp2t")
                        .setBody("S".repeat(1024))
                    else -> MockResponse().setResponseCode(404)
                }
            }
            start()
        }
        try {
            val masterUrl = okServer.url("/master.m3u8").toString()
            val verdict = StreamResolver.probeHls(masterUrl, emptyMap())
            assertEquals(StreamResolver.Probe.OK, verdict)
            assertEquals(listOf(1080, 720, 480, 360, 240, 144), StreamResolver.liveHeightsFor(masterUrl))
        } finally {
            okServer.shutdown()
        }

        val refusedServer = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                    "/master.m3u8" -> MockResponse()
                        .setResponseCode(200)
                        .setBody(
                            """
                            #EXTM3U
                            #EXT-X-STREAM-INF:BANDWIDTH=500000,RESOLUTION=640x360
                            /variant.m3u8
                            """.trimIndent(),
                        )
                    "/variant.m3u8" -> MockResponse()
                        .setResponseCode(200)
                        .setBody(
                            """
                            #EXTM3U
                            #EXTINF:2.0,
                            /blocked.ts
                            """.trimIndent(),
                        )
                    "/blocked.ts" -> MockResponse().setResponseCode(403)
                    else -> MockResponse().setResponseCode(404)
                }
            }
            start()
        }
        try {
            val masterUrl = refusedServer.url("/master.m3u8").toString()
            assertEquals(StreamResolver.Probe.REFUSED, StreamResolver.probeHls(masterUrl, emptyMap()))
        } finally {
            refusedServer.shutdown()
        }

        val directServer = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                    "/media.m3u8" -> MockResponse()
                        .setResponseCode(200)
                        .setBody(
                            """
                            #EXTM3U
                            #EXT-X-TARGETDURATION:2
                            #EXTINF:2.0,
                            /chunk_0.ts
                            """.trimIndent(),
                        )
                    "/chunk_0.ts" -> MockResponse()
                        .setResponseCode(200)
                        .setBody("M".repeat(1024))
                    else -> MockResponse().setResponseCode(404)
                }
            }
            start()
        }
        try {
            val mediaUrl = directServer.url("/media.m3u8").toString()
            assertEquals(StreamResolver.Probe.OK, StreamResolver.probeHls(mediaUrl, emptyMap()))
        } finally {
            directServer.shutdown()
        }
    }

    @Test
    fun parseAndroidLiveHlsResponseExtractsPlayableManifestOnlyWhenOk() {
        val okJson = """
            {
              "playabilityStatus": { "status": "OK" },
              "streamingData": {
                "hlsManifestUrl": "https://manifest.googlevideo.com/api/manifest/hls_variant/id/test/file/index.m3u8"
              }
            }
        """.trimIndent()
        assertEquals(
            "https://manifest.googlevideo.com/api/manifest/hls_variant/id/test/file/index.m3u8",
            StreamResolver.parseAndroidLiveHlsResponse(okJson),
        )

        val loginRequiredJson = """
            {
              "playabilityStatus": { "status": "LOGIN_REQUIRED", "reason": "Sign in to confirm" },
              "streamingData": {
                "hlsManifestUrl": "https://manifest.googlevideo.com/should_ignore.m3u8"
              }
            }
        """.trimIndent()
        assertNull(StreamResolver.parseAndroidLiveHlsResponse(loginRequiredJson))

        val missingManifestJson = """
            {
              "playabilityStatus": { "status": "OK" },
              "streamingData": {}
            }
        """.trimIndent()
        assertNull(StreamResolver.parseAndroidLiveHlsResponse(missingManifestJson))
    }

    @Test
    fun isDedicatedArtistLiveStationAcceptsOwnedOrSingleArtistAndRejectsCompilations() {
        val dedicatedChannel = com.music.bitchord.data.model.ShelfItem(
            title = "Lover Live Radio 24/7",
            subtitle = "Taylor Swift • 1.2K watching",
            thumbnailUrl = null,
            videoId = "liveTS1",
            browseId = null,
            isVideo = true,
            isLive = true,
        )
        val titledStation = com.music.bitchord.data.model.ShelfItem(
            title = "Taylor Swift: The Station",
            subtitle = "YouTube Music",
            thumbnailUrl = null,
            videoId = "liveTS2",
            browseId = null,
            isVideo = true,
            isLive = true,
        )
        val multiArtistCompilation = com.music.bitchord.data.model.ShelfItem(
            title = "Billboard Hot 100 Pop Songs 2026 - Taylor Swift, Billie Eilish & Dua Lipa",
            subtitle = "Top Hits 24/7 • 4K watching",
            thumbnailUrl = null,
            videoId = "liveComp1",
            browseId = null,
            isVideo = true,
            isLive = true,
        )
        val unrelatedLofi = com.music.bitchord.data.model.ShelfItem(
            title = "lofi hip hop radio - beats to relax/study to",
            subtitle = "Lofi Girl • 18K watching",
            thumbnailUrl = null,
            videoId = "liveLofi1",
            browseId = null,
            isVideo = true,
            isLive = true,
        )

        assertTrue(com.music.bitchord.data.YtMusicRepository.isDedicatedArtistLiveStation(dedicatedChannel, "Taylor Swift"))
        assertTrue(com.music.bitchord.data.YtMusicRepository.isDedicatedArtistLiveStation(titledStation, "Taylor Swift"))
        assertFalse(com.music.bitchord.data.YtMusicRepository.isDedicatedArtistLiveStation(multiArtistCompilation, "Taylor Swift"))
        assertFalse(com.music.bitchord.data.YtMusicRepository.isDedicatedArtistLiveStation(unrelatedLofi, "Taylor Swift"))
    }
}
