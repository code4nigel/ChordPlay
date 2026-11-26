package com.music.bitchord.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale

class TrackLogSessionTest {

    @After
    fun tearDown() {
        TrackLog.stopSession()
    }

    @Test
    fun `a session keeps every line from start to stop, and nothing after`() {
        val file = File.createTempFile("tracklog-session", ".txt").apply { deleteOnExit() }
        TrackLog.d("Before", "not recorded")
        assertFalse(TrackLog.recording.value)
        TrackLog.startSession(file, header = listOf("ChordPlay log", "device: Pixel"))
        assertTrue(TrackLog.recording.value)
        TrackLog.d("Player", "first", about = "abc")
        TrackLog.toSession('W', "Lyrics", "from the data layer")
        assertEquals(file, TrackLog.stopSession())
        assertFalse(TrackLog.recording.value)
        TrackLog.d("After", "not recorded either")

        val text = file.readText()
        assertTrue(text.startsWith("ChordPlay log\n"))
        assertTrue(text.contains("device: Pixel"))
        assertTrue(text.contains("D/Player {abc}: first"))
        assertTrue(text.contains("W/Lyrics: from the data layer"))
        assertFalse(text.contains("not recorded"))
        assertTrue(text.contains("(2 lines)"))
    }

    @Test
    fun `carrying on with an existing file appends without erasing previous lines`() {
        val file = File.createTempFile("tracklog-carried", ".txt").apply { deleteOnExit() }
        TrackLog.startSession(file, header = listOf("ChordPlay log"))
        TrackLog.toSession('I', "Log", "before restart")
        TrackLog.startSession(file, header = listOf("ChordPlay log"), append = true)
        TrackLog.toSession('I', "Log", "after restart")
        TrackLog.stopSession()

        val text = file.readText()
        assertTrue(text.contains("I/Log: before restart"))
        assertTrue(text.contains("carried on: "))
        assertTrue(text.contains("I/Log: after restart"))
        assertTrue(text.indexOf("before restart") < text.indexOf("after restart"))
    }

    @Test
    fun `stopping with nothing running returns null`() {
        assertNull(TrackLog.stopSession())
    }

    @Test
    fun `content country normalizes valid iso codes and rejects blank or invalid`() {
        ContentCountry.chosen = { " in " }
        assertEquals("IN", ContentCountry.chosenOrNull())

        ContentCountry.chosen = { "" }
        assertNull(ContentCountry.chosenOrNull())

        ContentCountry.chosen = { "USA" }
        assertNull(ContentCountry.chosenOrNull())

        ContentCountry.chosen = { null }
        assertNull(ContentCountry.chosenOrNull())

        assertTrue("IN" in ContentCountry.CODES)
        assertTrue(ContentCountry.displayName("IN", Locale.ENGLISH).isNotBlank())
    }
}
