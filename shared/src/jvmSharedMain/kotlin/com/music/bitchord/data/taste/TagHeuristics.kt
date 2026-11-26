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
package com.music.bitchord.data.taste

import java.util.Locale

/**
 * Tier-2 instant on-device tag inference (0 ms, offline).
 *
 * Combines Unicode script detection with curated multi-language, label/channel, genre,
 * and theme keyword matchers, plus conservative AI-generated release detection.
 */
object TagHeuristics {

    private val BHOJPURI_KEYWORDS = listOf(
        "bhojpuri", "wave music", "team films", "aarya digital", "worldwide records bhojpuri",
        "saregamahumbhojpuri", "saregama hum bhojpuri", "khesari lal", "pawan singh", "shilpi raj",
        "neelkamal singh", "akanksha dubey", "pramod premi", "ritesh pandey", "arvind akela",
        "kallu", "gunjan singh", "chaita", "bidesiya", "bol bam bhojpuri",
    )

    private val HARYANVI_KEYWORDS = listOf(
        "haryanvi", "white hill dhaakad", "desire music", "sonotek", "mor music", "ndj film",
        "sapna choudhary", "masoom sharma", "renuka panwar", "gulzaar chhaniwala", "khasa aala chahar",
        "sumit goswami", "diler kharkiya", "pranjal dahiya", "raju punjabi", "kd desi rock",
    )

    private val PUNJABI_KEYWORDS = listOf(
        "punjabi", "speed records", "white hill music", "t-series apna punjab", "geet mp3",
        "jass records", "desi junction", "desi crew", "sidhu moose wala", "diljit dosanjh",
        "karan aujla", "ap dhillon", "shubh", "gurinder gill", "ammy virk", "babbu maan",
        "satinder sartaaj", "amrinder gill", "parmish verma", "jatt", "gabhru", "sardarni",
        "bhangra", "moosetape", "brown munde",
    )

    private val HINDI_KEYWORDS = listOf(
        "hindi", "bollywood", "t-series", "zee music", "sony music india", "saregama music",
        "tips official", "yrf", "yash raj", "desi melodies", "kalamkaar", "azadi records",
        "gully gang", "arijit singh", "pritam", "shreya ghoshal", "atif aslam", "jubin nautiyal",
        "sonu nigam", "vishal-shekhar", "mithoon", "sachin-jigar", "a.r. rahman", "ar rahman",
        "mohit chauhan", "kk", "raftaar", "kr\$na", "krsna", "seedhe maut", "emiway", "divine",
        "badshah", "yo yo honey singh", "honey singh", "mc stan", "talha anjum", "king",
        "anuv jain", "prateek kuhad", "ritviz", "ishq", "pyaar", "dil", "mohabbat", "zindagi",
    )

    private val TAMIL_KEYWORDS = listOf(
        "tamil", "kollywood", "think music india", "sony music south", "anirudh ravichander",
        "ilayaraja", "harris jayaraj", "yuvan shankar raja", "g.v. prakash", "santhosh narayanan",
    )

    private val TELUGU_KEYWORDS = listOf(
        "telugu", "tollywood", "aditya music", "lahari music", "devi sri prasad",
        "thaman s", "s.s. thaman", "keeravani", "sid sriram",
    )

    private val MALAYALAM_KEYWORDS = listOf(
        "malayalam", "mollywood", "muzik247", "satyam audios", "sushin shyam", "thaikkudam bridge",
    )

    private val KANNADA_KEYWORDS = listOf(
        "kannada", "sandalwood", "anand audio", "arjun janya", "ravi basrur", "charan raj",
    )

    private val MARATHI_KEYWORDS = listOf(
        "marathi", "everest marathi", "ajay-atul", "sairat", "lavani",
    )

    private val GUJARATI_KEYWORDS = listOf(
        "gujarati", "garba", "dandiya", "kirtidan gadhvi", "kinjal dave", "jignesh kaviraj",
    )

    private val BENGALI_KEYWORDS = listOf(
        "bengali", "bangla", "svf music", "rabindra sangeet", "anupam roy", "fossils",
    )

    private val URDU_KEYWORDS = listOf(
        "urdu", "coke studio", "nusrat fateh ali khan", "rahat fateh ali khan", "kaifi khalil",
        "young stunners", "talha yunus", "hasan raheem", "ali zafar",
    )

    private val KOREAN_KEYWORDS = listOf(
        "k-pop", "kpop", "hybe", "smtown", "jyp entertainment", "yg entertainment", "blackpink",
        "bts", "stray kids", "newjeans", "seventeen", "twice", "aespa", "exo",
    )

    private val JAPANESE_KEYWORDS = listOf(
        "j-pop", "jpop", "j-rock", "jrock", "vocaloid", "yoasobi", "ado", "kenshi yonezu",
        "fujii kaze", "lisa", "radwimps", "anime opening", "anime ending",
    )

    private val SPANISH_KEYWORDS = listOf(
        "reggaeton", "musica", "en espanol", "urbano latino", "bad bunny", "j balvin",
        "karol g", "feid", "peso pluma", "shakira", "maluma", "rauw alejandro",
    )

    private val GENRE_RULES: List<Pair<TasteTag, List<String>>> = listOf(
        TasteTaxonomy.GENRE_LOFI to listOf("lo-fi", "lofi", "lo fi", "chillhop", "slowed + reverb", "slowed & reverb", "slowed reverb"),
        TasteTaxonomy.GENRE_PHONK to listOf("phonk", "brazilian phonk", "drift phonk", "funk montagem"),
        TasteTaxonomy.GENRE_RAP to listOf("rap", "hip-hop", "hip hop", "freestyle", "cypher", "bars", "dhh", "desi hip hop"),
        TasteTaxonomy.GENRE_DRILL to listOf("drill", "trap", "808", "type beat"),
        TasteTaxonomy.GENRE_BOLLYWOOD to listOf("bollywood", "filmi", "from \"", "from '", "motion picture", "lyrical video"),
        TasteTaxonomy.GENRE_SUFI to listOf("sufi", "ghazal", "qawwali", "qawali", "kalam"),
        TasteTaxonomy.GENRE_DEVOTIONAL to listOf("bhajan", "aarti", "chalisa", "mantra", "kirtan", "devotional", "stotra", "naat", "hamd", "gurbani", "shabad"),
        TasteTaxonomy.GENRE_CLASSICAL to listOf("classical", "raga", "raaga", "symphony", "concerto", "sonata", "nocturne", "sitar", "tabla"),
        TasteTaxonomy.GENRE_EDM to listOf("edm", "remix", "dj mix", "club mix", "mashup", "electronic", "house mix", "techno", "dubstep", "dnb"),
        TasteTaxonomy.GENRE_ACOUSTIC to listOf("acoustic", "unplugged", "mtv unplugged", "stripped"),
        TasteTaxonomy.GENRE_INDIE to listOf("indie", "bedroom pop", "indie folk"),
        TasteTaxonomy.GENRE_RNB to listOf("r&b", "rnb", "neo soul", "neo-soul"),
        TasteTaxonomy.GENRE_JAZZ to listOf("jazz", "bossa nova", "saxophone", "jazz cafe"),
        TasteTaxonomy.GENRE_METAL to listOf("metal", "metalcore", "heavy metal", "death metal"),
        TasteTaxonomy.GENRE_ROCK to listOf("rock", "grunge", "shoegaze", "punk rock"),
        TasteTaxonomy.GENRE_SYNTHWAVE to listOf("synthwave", "retrowave", "vaporwave", "80s synth"),
        TasteTaxonomy.GENRE_SOUNDTRACK to listOf("soundtrack", "original motion picture", "ost", "theme song", "bgm"),
    )

    private val THEME_RULES: List<Pair<TasteTag, List<String>>> = listOf(
        TasteTaxonomy.THEME_CHILL to listOf("chill", "relax", "calm", "ambient", "breeze"),
        TasteTaxonomy.THEME_ROMANTIC to listOf("romantic", "love song", "ishq", "pyaar", "mohabbat", "valentine", "tera", "tum hi"),
        TasteTaxonomy.THEME_SAD to listOf("sad", "heartbreak", "bewafa", "dard", "broken", "judai", "tanha", "cry"),
        TasteTaxonomy.THEME_PARTY to listOf("party", "club", "dj", "dance floor", "banger"),
        TasteTaxonomy.THEME_HYPE to listOf("workout", "gym", "beast mode", "hype", "pump", "running", "motivation"),
        TasteTaxonomy.THEME_FOCUS to listOf("focus", "study", "coding", "deep work", "concentration", "reading"),
        TasteTaxonomy.THEME_LATE_NIGHT to listOf("late night", "midnight", "3am", "3 am", "night drive", "after hours"),
        TasteTaxonomy.THEME_COMMUTE to listOf("drive", "road trip", "highway", "cruising", "car"),
        TasteTaxonomy.THEME_SLEEP to listOf("sleep", "lullaby", "rain sounds", "deep sleep", "bedtime"),
        TasteTaxonomy.THEME_NOSTALGIC to listOf("nostalgia", "90s", "2000s", "retro", "throwback", "classic hits"),
        TasteTaxonomy.THEME_SPIRITUAL to listOf("spiritual", "meditation", "bhajan", "chalisa", "mantra", "prayer", "peaceful"),
    )

    private val AI_SLOP_PATTERNS = listOf(
        Regex("\\b(?:ai\\s*cover|ai\\s*generated|ai\\s*music|made\\s+with\\s+(?:suno|udio)|suno\\s*ai|udio\\s*ai)\\b", RegexOption.IGNORE_CASE),
        Regex("[\\[(]\\s*ai(?:\\s+(?:cover|version|remix|mix))?\\s*[\\])]", RegexOption.IGNORE_CASE),
    )

    /**
     * Conservative metadata marker check for AI-generated tracks (used only when Hide AI Music is ON).
     */
    fun isLikelyAiMetadata(title: String, artist: String = "", subtitle: String = ""): Boolean {
        val hay = "$title $artist $subtitle"
        return AI_SLOP_PATTERNS.any { it.containsMatchIn(hay) }
    }

    /**
     * Infers canonical language, genre, and theme masks offline in < 0.1 ms.
     */
    fun infer(
        title: String,
        artist: String = "",
        subtitle: String = "",
        albumName: String? = null,
    ): ResolvedTags {
        val combined = buildString {
            append(title).append(' ')
            append(artist).append(' ')
            if (subtitle.isNotBlank()) append(subtitle).append(' ')
            if (!albumName.isNullOrBlank()) append(albumName)
        }
        val lower = combined.lowercase(Locale.ROOT)

        var langMask = detectScriptLanguages(combined)
        if (containsAny(lower, BHOJPURI_KEYWORDS)) langMask = langMask or TasteTaxonomy.LANG_BHOJPURI.bitMask
        if (containsAny(lower, HARYANVI_KEYWORDS)) langMask = langMask or TasteTaxonomy.LANG_HARYANVI.bitMask
        if (containsAny(lower, PUNJABI_KEYWORDS)) langMask = langMask or TasteTaxonomy.LANG_PUNJABI.bitMask
        if (containsAny(lower, HINDI_KEYWORDS)) langMask = langMask or TasteTaxonomy.LANG_HINDI.bitMask
        if (containsAny(lower, TAMIL_KEYWORDS)) langMask = langMask or TasteTaxonomy.LANG_TAMIL.bitMask
        if (containsAny(lower, TELUGU_KEYWORDS)) langMask = langMask or TasteTaxonomy.LANG_TELUGU.bitMask
        if (containsAny(lower, MALAYALAM_KEYWORDS)) langMask = langMask or TasteTaxonomy.LANG_MALAYALAM.bitMask
        if (containsAny(lower, KANNADA_KEYWORDS)) langMask = langMask or TasteTaxonomy.LANG_KANNADA.bitMask
        if (containsAny(lower, MARATHI_KEYWORDS)) langMask = langMask or TasteTaxonomy.LANG_MARATHI.bitMask
        if (containsAny(lower, GUJARATI_KEYWORDS)) langMask = langMask or TasteTaxonomy.LANG_GUJARATI.bitMask
        if (containsAny(lower, BENGALI_KEYWORDS)) langMask = langMask or TasteTaxonomy.LANG_BENGALI.bitMask
        if (containsAny(lower, URDU_KEYWORDS)) langMask = langMask or TasteTaxonomy.LANG_URDU.bitMask
        if (containsAny(lower, KOREAN_KEYWORDS)) langMask = langMask or TasteTaxonomy.LANG_KOREAN.bitMask
        if (containsAny(lower, JAPANESE_KEYWORDS)) langMask = langMask or TasteTaxonomy.LANG_JAPANESE.bitMask
        if (containsAny(lower, SPANISH_KEYWORDS)) langMask = langMask or TasteTaxonomy.LANG_SPANISH.bitMask
        if (lower.contains("instrumental") && !lower.contains("non-instrumental")) {
            langMask = langMask or TasteTaxonomy.LANG_INSTRUMENTAL.bitMask
        }

        var genreMask = 0
        for ((tag, keywords) in GENRE_RULES) {
            if (containsAny(lower, keywords)) genreMask = genreMask or tag.bitMask
        }

        var themeMask = 0
        for ((tag, keywords) in THEME_RULES) {
            if (containsAny(lower, keywords)) themeMask = themeMask or tag.bitMask
        }

        return ResolvedTags(
            langMask = langMask,
            genreMask = genreMask,
            themeMask = themeMask,
            source = TagSource.HEURISTIC,
        )
    }

    private fun detectScriptLanguages(text: String): Int {
        var mask = 0
        for (ch in text) {
            when (ch.code) {
                in 0x0900..0x097F -> mask = mask or TasteTaxonomy.LANG_HINDI.bitMask
                in 0x0A00..0x0A7F -> mask = mask or TasteTaxonomy.LANG_PUNJABI.bitMask
                in 0x0B80..0x0BFF -> mask = mask or TasteTaxonomy.LANG_TAMIL.bitMask
                in 0x0C00..0x0C7F -> mask = mask or TasteTaxonomy.LANG_TELUGU.bitMask
                in 0x0C80..0x0CFF -> mask = mask or TasteTaxonomy.LANG_KANNADA.bitMask
                in 0x0D00..0x0D7F -> mask = mask or TasteTaxonomy.LANG_MALAYALAM.bitMask
                in 0x0A80..0x0AFF -> mask = mask or TasteTaxonomy.LANG_GUJARATI.bitMask
                in 0x0980..0x09FF -> mask = mask or TasteTaxonomy.LANG_BENGALI.bitMask
                in 0xAC00..0xD7AF, in 0x1100..0x11FF -> mask = mask or TasteTaxonomy.LANG_KOREAN.bitMask
                in 0x3040..0x30FF -> mask = mask or TasteTaxonomy.LANG_JAPANESE.bitMask
                in 0x0600..0x06FF -> mask = mask or TasteTaxonomy.LANG_ARABIC.bitMask
            }
        }
        return mask
    }

    private fun containsAny(hayLower: String, needles: List<String>): Boolean {
        for (needle in needles) {
            if (needle.length <= 3) {
                // Short tokens like "kk", "dj", "ost", "dil", "sad", "gym" require word boundaries.
                val idx = hayLower.indexOf(needle)
                if (idx >= 0) {
                    val beforeOk = idx == 0 || !hayLower[idx - 1].isLetterOrDigit()
                    val afterIdx = idx + needle.length
                    val afterOk = afterIdx == hayLower.length || !hayLower[afterIdx].isLetterOrDigit()
                    if (beforeOk && afterOk) return true
                }
            } else if (hayLower.contains(needle)) {
                return true
            }
        }
        return false
    }
}
