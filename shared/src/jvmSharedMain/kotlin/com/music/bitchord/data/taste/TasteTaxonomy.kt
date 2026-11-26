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

import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
enum class TasteCategory {
    LANGUAGE,
    GENRE,
    THEME,
    ARTIST,
}

/**
 * Fixed canonical tag inside ChordPlay's taste taxonomy.
 *
 * [bitIndex] (0..30) allows compact 32-bit bitmask storage per category in `media_tag_cache`
 * and sub-microsecond intersection checks in RAM.
 */
data class TasteTag(
    val id: Int,
    val bitIndex: Int,
    val key: String,
    val displayName: String,
    val category: TasteCategory,
    val queryToken: String,
    val ytmCategoryHints: List<String> = emptyList(),
) {
    val bitMask: Int get() = 1 shl bitIndex
}

/**
 * Compact bitwise operations over canonical [TasteTag.bitIndex] masks.
 */
object TagMask {
    fun of(tags: Iterable<TasteTag>): Int {
        var mask = 0
        for (tag in tags) mask = mask or tag.bitMask
        return mask
    }

    fun ofTag(tag: TasteTag?): Int = tag?.bitMask ?: 0

    fun contains(mask: Int, tag: TasteTag): Boolean = (mask and tag.bitMask) != 0

    fun intersects(a: Int, b: Int): Boolean = (a and b) != 0

    fun toTags(mask: Int, pool: List<TasteTag>): List<TasteTag> {
        if (mask == 0) return emptyList()
        return pool.filter { (mask and it.bitMask) != 0 }
    }
}

/**
 * Fixed, finite canonical taxonomy (~20 Languages, ~25 Genres, ~14 Themes) + synonym normalizer.
 * Raw noisy external tags ("seen live", "albums i own") are discarded so only clean musical
 * concepts enter user-facing ranking UIs and filters.
 */
object TasteTaxonomy {

    // ── Languages (20, IDs 1..20, bits 0..19) ────────────────────────────────
    val LANG_HINDI = TasteTag(1, 0, "hindi", "Hindi", TasteCategory.LANGUAGE, "Hindi", listOf("indian", "hindi", "desi"))
    val LANG_ENGLISH = TasteTag(2, 1, "english", "English", TasteCategory.LANGUAGE, "English", listOf("pop", "rock", "hip-hop"))
    val LANG_PUNJABI = TasteTag(3, 2, "punjabi", "Punjabi", TasteCategory.LANGUAGE, "Punjabi", listOf("punjabi"))
    val LANG_TAMIL = TasteTag(4, 3, "tamil", "Tamil", TasteCategory.LANGUAGE, "Tamil", listOf("tamil"))
    val LANG_TELUGU = TasteTag(5, 4, "telugu", "Telugu", TasteCategory.LANGUAGE, "Telugu", listOf("telugu"))
    val LANG_MALAYALAM = TasteTag(6, 5, "malayalam", "Malayalam", TasteCategory.LANGUAGE, "Malayalam", listOf("malayalam"))
    val LANG_KANNADA = TasteTag(7, 6, "kannada", "Kannada", TasteCategory.LANGUAGE, "Kannada", listOf("kannada"))
    val LANG_MARATHI = TasteTag(8, 7, "marathi", "Marathi", TasteCategory.LANGUAGE, "Marathi", listOf("marathi"))
    val LANG_GUJARATI = TasteTag(9, 8, "gujarati", "Gujarati", TasteCategory.LANGUAGE, "Gujarati", listOf("gujarati"))
    val LANG_BENGALI = TasteTag(10, 9, "bengali", "Bengali", TasteCategory.LANGUAGE, "Bengali", listOf("bengali"))
    val LANG_BHOJPURI = TasteTag(11, 10, "bhojpuri", "Bhojpuri", TasteCategory.LANGUAGE, "Bhojpuri", listOf("bhojpuri"))
    val LANG_HARYANVI = TasteTag(12, 11, "haryanvi", "Haryanvi", TasteCategory.LANGUAGE, "Haryanvi", listOf("haryanvi"))
    val LANG_URDU = TasteTag(13, 12, "urdu", "Urdu", TasteCategory.LANGUAGE, "Urdu", listOf("urdu", "pakistani"))
    val LANG_KOREAN = TasteTag(14, 13, "korean", "Korean", TasteCategory.LANGUAGE, "Korean", listOf("k-pop", "korean"))
    val LANG_JAPANESE = TasteTag(15, 14, "japanese", "Japanese", TasteCategory.LANGUAGE, "Japanese", listOf("j-pop", "japanese", "anime"))
    val LANG_SPANISH = TasteTag(16, 15, "spanish", "Spanish", TasteCategory.LANGUAGE, "Spanish", listOf("latin", "spanish"))
    val LANG_ARABIC = TasteTag(17, 16, "arabic", "Arabic", TasteCategory.LANGUAGE, "Arabic", listOf("arabic"))
    val LANG_FRENCH = TasteTag(18, 17, "french", "French", TasteCategory.LANGUAGE, "French", listOf("french"))
    val LANG_PORTUGUESE = TasteTag(19, 18, "portuguese", "Portuguese", TasteCategory.LANGUAGE, "Portuguese", listOf("brazilian", "sertanejo", "funk"))
    val LANG_INSTRUMENTAL = TasteTag(20, 19, "instrumental", "Instrumental", TasteCategory.LANGUAGE, "Instrumental", listOf("instrumental", "classical"))

    // ── Genres (25, IDs 101..125, bits 0..24) ────────────────────────────────
    val GENRE_POP = TasteTag(101, 0, "pop", "Pop", TasteCategory.GENRE, "Pop", listOf("pop", "indian pop"))
    val GENRE_RAP = TasteTag(102, 1, "rap", "Rap / Hip-Hop", TasteCategory.GENRE, "Rap", listOf("hip-hop", "rap", "desi hip-hop", "indian hip-hop"))
    val GENRE_INDIE = TasteTag(103, 2, "indie", "Indie", TasteCategory.GENRE, "Indie", listOf("indie", "indie & alternative", "indian indie"))
    val GENRE_LOFI = TasteTag(104, 3, "lofi", "Lo-Fi", TasteCategory.GENRE, "Lo-Fi", listOf("lo-fi", "chill"))
    val GENRE_ROCK = TasteTag(105, 4, "rock", "Rock", TasteCategory.GENRE, "Rock", listOf("rock"))
    val GENRE_EDM = TasteTag(106, 5, "edm", "EDM / Electronic", TasteCategory.GENRE, "EDM", listOf("dance & electronic", "electronic", "edm"))
    val GENRE_RNB = TasteTag(107, 6, "rnb", "R&B", TasteCategory.GENRE, "R&B", listOf("r&b", "r&b & soul"))
    val GENRE_BOLLYWOOD = TasteTag(108, 7, "bollywood", "Bollywood / Filmi", TasteCategory.GENRE, "Bollywood", listOf("bollywood", "indian", "soundtracks"))
    val GENRE_CLASSICAL = TasteTag(109, 8, "classical", "Classical", TasteCategory.GENRE, "Classical", listOf("classical", "indian classical"))
    val GENRE_SUFI = TasteTag(110, 9, "sufi", "Sufi / Ghazal", TasteCategory.GENRE, "Sufi Ghazal", listOf("ghazal & sufi", "sufi", "ghazal"))
    val GENRE_DEVOTIONAL = TasteTag(111, 10, "devotional", "Devotional", TasteCategory.GENRE, "Devotional", listOf("devotional", "bhajan", "gospel"))
    val GENRE_FOLK = TasteTag(112, 11, "folk", "Folk", TasteCategory.GENRE, "Folk", listOf("folk", "folk & acoustic"))
    val GENRE_PHONK = TasteTag(113, 12, "phonk", "Phonk", TasteCategory.GENRE, "Phonk", listOf("phonk"))
    val GENRE_ACOUSTIC = TasteTag(114, 13, "acoustic", "Acoustic", TasteCategory.GENRE, "Acoustic", listOf("acoustic", "folk & acoustic"))
    val GENRE_JAZZ = TasteTag(115, 14, "jazz", "Jazz", TasteCategory.GENRE, "Jazz", listOf("jazz"))
    val GENRE_METAL = TasteTag(116, 15, "metal", "Metal", TasteCategory.GENRE, "Metal", listOf("metal", "hard rock & metal"))
    val GENRE_PUNK = TasteTag(117, 16, "punk", "Punk", TasteCategory.GENRE, "Punk", listOf("punk"))
    val GENRE_SOUL = TasteTag(118, 17, "soul", "Soul / Funk", TasteCategory.GENRE, "Soul Funk", listOf("soul", "funk", "r&b & soul"))
    val GENRE_REGGAE = TasteTag(119, 18, "reggae", "Reggae / Reggaeton", TasteCategory.GENRE, "Reggaeton", listOf("reggae", "reggaeton", "latin"))
    val GENRE_AFROBEATS = TasteTag(120, 19, "afrobeats", "Afrobeats", TasteCategory.GENRE, "Afrobeats", listOf("afrobeats", "african"))
    val GENRE_BLUES = TasteTag(121, 20, "blues", "Blues", TasteCategory.GENRE, "Blues", listOf("blues"))
    val GENRE_COUNTRY = TasteTag(122, 21, "country", "Country", TasteCategory.GENRE, "Country", listOf("country"))
    val GENRE_SOUNDTRACK = TasteTag(123, 22, "soundtrack", "Soundtrack / OST", TasteCategory.GENRE, "Soundtrack", listOf("soundtracks", "anime", "gaming"))
    val GENRE_DRILL = TasteTag(124, 23, "drill", "Drill / Trap", TasteCategory.GENRE, "Trap Drill", listOf("trap", "drill", "hip-hop"))
    val GENRE_SYNTHWAVE = TasteTag(125, 24, "synthwave", "Synthwave / Retro", TasteCategory.GENRE, "Synthwave", listOf("synthwave", "retro", "decades"))

    // ── Themes / Moods (14, IDs 201..214, bits 0..13) ────────────────────────
    val THEME_CHILL = TasteTag(201, 0, "chill", "Chill", TasteCategory.THEME, "Chill", listOf("chill", "relax"))
    val THEME_ROMANTIC = TasteTag(202, 1, "romantic", "Romantic", TasteCategory.THEME, "Romantic", listOf("romance", "romantic"))
    val THEME_SAD = TasteTag(203, 2, "sad", "Sad / Heartbreak", TasteCategory.THEME, "Sad", listOf("sad"))
    val THEME_PARTY = TasteTag(204, 3, "party", "Party / Club", TasteCategory.THEME, "Party", listOf("party", "dance"))
    val THEME_HYPE = TasteTag(205, 4, "hype", "Hype / Workout", TasteCategory.THEME, "Workout", listOf("workout", "energy"))
    val THEME_FOCUS = TasteTag(206, 5, "focus", "Focus / Study", TasteCategory.THEME, "Focus", listOf("focus", "study"))
    val THEME_LATE_NIGHT = TasteTag(207, 6, "late_night", "Late Night", TasteCategory.THEME, "Late Night", listOf("chill", "night", "sleep"))
    val THEME_FEEL_GOOD = TasteTag(208, 7, "feel_good", "Feel Good", TasteCategory.THEME, "Feel Good", listOf("feel good", "happy"))
    val THEME_NOSTALGIC = TasteTag(209, 8, "nostalgic", "Nostalgic", TasteCategory.THEME, "90s 2000s Retro", listOf("nostalgic", "decades", "90s", "2000s"))
    val THEME_COMMUTE = TasteTag(210, 9, "commute", "Commute / Drive", TasteCategory.THEME, "Drive", listOf("commute", "drive"))
    val THEME_SLEEP = TasteTag(211, 10, "sleep", "Sleep", TasteCategory.THEME, "Sleep", listOf("sleep"))
    val THEME_AGGRESSIVE = TasteTag(212, 11, "aggressive", "Aggressive", TasteCategory.THEME, "Hard", listOf("workout", "rock", "metal"))
    val THEME_SPIRITUAL = TasteTag(213, 12, "spiritual", "Spiritual", TasteCategory.THEME, "Spiritual", listOf("devotional", "spiritual"))
    val THEME_ENERGETIC = TasteTag(214, 13, "energetic", "Energetic", TasteCategory.THEME, "Energetic", listOf("energy", "feel good"))

    val languages: List<TasteTag> = listOf(
        LANG_HINDI, LANG_ENGLISH, LANG_PUNJABI, LANG_TAMIL, LANG_TELUGU,
        LANG_MALAYALAM, LANG_KANNADA, LANG_MARATHI, LANG_GUJARATI, LANG_BENGALI,
        LANG_BHOJPURI, LANG_HARYANVI, LANG_URDU, LANG_KOREAN, LANG_JAPANESE,
        LANG_SPANISH, LANG_ARABIC, LANG_FRENCH, LANG_PORTUGUESE, LANG_INSTRUMENTAL,
    )

    val genres: List<TasteTag> = listOf(
        GENRE_POP, GENRE_RAP, GENRE_INDIE, GENRE_LOFI, GENRE_ROCK,
        GENRE_EDM, GENRE_RNB, GENRE_BOLLYWOOD, GENRE_CLASSICAL, GENRE_SUFI,
        GENRE_DEVOTIONAL, GENRE_FOLK, GENRE_PHONK, GENRE_ACOUSTIC, GENRE_JAZZ,
        GENRE_METAL, GENRE_PUNK, GENRE_SOUL, GENRE_REGGAE, GENRE_AFROBEATS,
        GENRE_BLUES, GENRE_COUNTRY, GENRE_SOUNDTRACK, GENRE_DRILL, GENRE_SYNTHWAVE,
    )

    val themes: List<TasteTag> = listOf(
        THEME_CHILL, THEME_ROMANTIC, THEME_SAD, THEME_PARTY, THEME_HYPE,
        THEME_FOCUS, THEME_LATE_NIGHT, THEME_FEEL_GOOD, THEME_NOSTALGIC,
        THEME_COMMUTE, THEME_SLEEP, THEME_AGGRESSIVE, THEME_SPIRITUAL, THEME_ENERGETIC,
    )

    val allCoreTags: List<TasteTag> = languages + genres + themes

    private val byIdMap: Map<Int, TasteTag> = allCoreTags.associateBy { it.id }
    private val byKeyMap: Map<String, TasteTag> = allCoreTags.associateBy { it.key }

    fun poolFor(category: TasteCategory): List<TasteTag> = when (category) {
        TasteCategory.LANGUAGE -> languages
        TasteCategory.GENRE -> genres
        TasteCategory.THEME -> themes
        TasteCategory.ARTIST -> emptyList()
    }

    fun byId(id: Int): TasteTag? = byIdMap[id]
    fun byKey(key: String): TasteTag? = byKeyMap[key.trim().lowercase(Locale.ROOT)]

    fun nodeId(category: TasteCategory, key: String): String =
        "${category.name}:${key.trim().lowercase(Locale.ROOT)}"

    fun nodeId(tag: TasteTag): String = nodeId(tag.category, tag.key)

    /** Builds a clean YTM search query for a preferred (language, genre/theme) combination. */
    fun buildQuery(language: TasteTag?, genre: TasteTag?, theme: TasteTag? = null): String =
        listOfNotNull(
            language?.takeIf { it != LANG_ENGLISH || (genre == null && theme == null) }?.queryToken,
            genre?.queryToken,
            theme?.queryToken,
        ).distinct().joinToString(" ").ifBlank { "Top Hits" }
}

/**
 * Normalizes messy external tags, YTM mood/genre labels, iTunes `primaryGenreName`s,
 * and MusicBrainz `area` / `genre` strings into canonical [TasteTaxonomy] tags.
 */
object TagNormalizer {

    data class NormalizedMasks(
        val langMask: Int = 0,
        val genreMask: Int = 0,
        val themeMask: Int = 0,
    ) {
        val isEmpty: Boolean get() = langMask == 0 && genreMask == 0 && themeMask == 0

        operator fun plus(other: NormalizedMasks): NormalizedMasks = NormalizedMasks(
            langMask = langMask or other.langMask,
            genreMask = genreMask or other.genreMask,
            themeMask = themeMask or other.themeMask,
        )
    }

    private fun clean(raw: String): String = raw
        .trim()
        .lowercase(Locale.ROOT)
        .replace('&', ' ')
        .replace('/', ' ')
        .replace('-', ' ')
        .replace('_', ' ')
        .replace(Regex("[^\\p{L}\\p{N}\\s+]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private val LANGUAGE_SYNONYMS: Map<String, TasteTag> = mapOf(
        "hindi" to TasteTaxonomy.LANG_HINDI,
        "bollywood" to TasteTaxonomy.LANG_HINDI,
        "filmi" to TasteTaxonomy.LANG_HINDI,
        "desi" to TasteTaxonomy.LANG_HINDI,
        "hindi pop" to TasteTaxonomy.LANG_HINDI,
        "hindi rap" to TasteTaxonomy.LANG_HINDI,
        "desi hip hop" to TasteTaxonomy.LANG_HINDI,
        "indian pop" to TasteTaxonomy.LANG_HINDI,
        "indian indie" to TasteTaxonomy.LANG_HINDI,
        "indian hip hop" to TasteTaxonomy.LANG_HINDI,
        "english" to TasteTaxonomy.LANG_ENGLISH,
        "punjabi" to TasteTaxonomy.LANG_PUNJABI,
        "punjabi pop" to TasteTaxonomy.LANG_PUNJABI,
        "bhangra" to TasteTaxonomy.LANG_PUNJABI,
        "tamil" to TasteTaxonomy.LANG_TAMIL,
        "kollywood" to TasteTaxonomy.LANG_TAMIL,
        "telugu" to TasteTaxonomy.LANG_TELUGU,
        "tollywood" to TasteTaxonomy.LANG_TELUGU,
        "malayalam" to TasteTaxonomy.LANG_MALAYALAM,
        "mollywood" to TasteTaxonomy.LANG_MALAYALAM,
        "kannada" to TasteTaxonomy.LANG_KANNADA,
        "sandalwood" to TasteTaxonomy.LANG_KANNADA,
        "marathi" to TasteTaxonomy.LANG_MARATHI,
        "gujarati" to TasteTaxonomy.LANG_GUJARATI,
        "bengali" to TasteTaxonomy.LANG_BENGALI,
        "bangla" to TasteTaxonomy.LANG_BENGALI,
        "bhojpuri" to TasteTaxonomy.LANG_BHOJPURI,
        "haryanvi" to TasteTaxonomy.LANG_HARYANVI,
        "urdu" to TasteTaxonomy.LANG_URDU,
        "pakistani" to TasteTaxonomy.LANG_URDU,
        "coke studio pakistan" to TasteTaxonomy.LANG_URDU,
        "korean" to TasteTaxonomy.LANG_KOREAN,
        "k pop" to TasteTaxonomy.LANG_KOREAN,
        "kpop" to TasteTaxonomy.LANG_KOREAN,
        "k hip hop" to TasteTaxonomy.LANG_KOREAN,
        "k rnb" to TasteTaxonomy.LANG_KOREAN,
        "japanese" to TasteTaxonomy.LANG_JAPANESE,
        "j pop" to TasteTaxonomy.LANG_JAPANESE,
        "jpop" to TasteTaxonomy.LANG_JAPANESE,
        "j rock" to TasteTaxonomy.LANG_JAPANESE,
        "jrock" to TasteTaxonomy.LANG_JAPANESE,
        "anime" to TasteTaxonomy.LANG_JAPANESE,
        "vocaloid" to TasteTaxonomy.LANG_JAPANESE,
        "spanish" to TasteTaxonomy.LANG_SPANISH,
        "latin" to TasteTaxonomy.LANG_SPANISH,
        "latin pop" to TasteTaxonomy.LANG_SPANISH,
        "urbano latino" to TasteTaxonomy.LANG_SPANISH,
        "musica mexicana" to TasteTaxonomy.LANG_SPANISH,
        "reggaeton" to TasteTaxonomy.LANG_SPANISH,
        "arabic" to TasteTaxonomy.LANG_ARABIC,
        "khaleeji" to TasteTaxonomy.LANG_ARABIC,
        "french" to TasteTaxonomy.LANG_FRENCH,
        "chanson" to TasteTaxonomy.LANG_FRENCH,
        "french pop" to TasteTaxonomy.LANG_FRENCH,
        "portuguese" to TasteTaxonomy.LANG_PORTUGUESE,
        "brazilian" to TasteTaxonomy.LANG_PORTUGUESE,
        "mpb" to TasteTaxonomy.LANG_PORTUGUESE,
        "sertanejo" to TasteTaxonomy.LANG_PORTUGUESE,
        "baile funk" to TasteTaxonomy.LANG_PORTUGUESE,
        "instrumental" to TasteTaxonomy.LANG_INSTRUMENTAL,
    )

    private val GENRE_SYNONYMS: Map<String, TasteTag> = mapOf(
        "pop" to TasteTaxonomy.GENRE_POP,
        "synthpop" to TasteTaxonomy.GENRE_POP,
        "synth pop" to TasteTaxonomy.GENRE_POP,
        "electropop" to TasteTaxonomy.GENRE_POP,
        "dream pop" to TasteTaxonomy.GENRE_POP,
        "art pop" to TasteTaxonomy.GENRE_POP,
        "k pop" to TasteTaxonomy.GENRE_POP,
        "kpop" to TasteTaxonomy.GENRE_POP,
        "j pop" to TasteTaxonomy.GENRE_POP,
        "jpop" to TasteTaxonomy.GENRE_POP,
        "indian pop" to TasteTaxonomy.GENRE_POP,
        "hindi pop" to TasteTaxonomy.GENRE_POP,
        "punjabi pop" to TasteTaxonomy.GENRE_POP,
        "rap" to TasteTaxonomy.GENRE_RAP,
        "hip hop" to TasteTaxonomy.GENRE_RAP,
        "hiphop" to TasteTaxonomy.GENRE_RAP,
        "hip hop rap" to TasteTaxonomy.GENRE_RAP,
        "desi hip hop" to TasteTaxonomy.GENRE_RAP,
        "indian hip hop" to TasteTaxonomy.GENRE_RAP,
        "gangsta rap" to TasteTaxonomy.GENRE_RAP,
        "conscious hip hop" to TasteTaxonomy.GENRE_RAP,
        "boom bap" to TasteTaxonomy.GENRE_RAP,
        "cloud rap" to TasteTaxonomy.GENRE_RAP,
        "underground hip hop" to TasteTaxonomy.GENRE_RAP,
        "freestyle" to TasteTaxonomy.GENRE_RAP,
        "indie" to TasteTaxonomy.GENRE_INDIE,
        "indie pop" to TasteTaxonomy.GENRE_INDIE,
        "indie rock" to TasteTaxonomy.GENRE_INDIE,
        "indie alternative" to TasteTaxonomy.GENRE_INDIE,
        "alternative" to TasteTaxonomy.GENRE_INDIE,
        "indian indie" to TasteTaxonomy.GENRE_INDIE,
        "bedroom pop" to TasteTaxonomy.GENRE_INDIE,
        "singer songwriter" to TasteTaxonomy.GENRE_INDIE,
        "lo fi" to TasteTaxonomy.GENRE_LOFI,
        "lofi" to TasteTaxonomy.GENRE_LOFI,
        "lo fi hip hop" to TasteTaxonomy.GENRE_LOFI,
        "chillhop" to TasteTaxonomy.GENRE_LOFI,
        "slowed reverb" to TasteTaxonomy.GENRE_LOFI,
        "rock" to TasteTaxonomy.GENRE_ROCK,
        "alternative rock" to TasteTaxonomy.GENRE_ROCK,
        "classic rock" to TasteTaxonomy.GENRE_ROCK,
        "hard rock" to TasteTaxonomy.GENRE_ROCK,
        "soft rock" to TasteTaxonomy.GENRE_ROCK,
        "pop rock" to TasteTaxonomy.GENRE_ROCK,
        "post rock" to TasteTaxonomy.GENRE_ROCK,
        "j rock" to TasteTaxonomy.GENRE_ROCK,
        "grunge" to TasteTaxonomy.GENRE_ROCK,
        "shoegaze" to TasteTaxonomy.GENRE_ROCK,
        "britpop" to TasteTaxonomy.GENRE_ROCK,
        "edm" to TasteTaxonomy.GENRE_EDM,
        "electronic" to TasteTaxonomy.GENRE_EDM,
        "dance" to TasteTaxonomy.GENRE_EDM,
        "dance electronic" to TasteTaxonomy.GENRE_EDM,
        "house" to TasteTaxonomy.GENRE_EDM,
        "deep house" to TasteTaxonomy.GENRE_EDM,
        "techno" to TasteTaxonomy.GENRE_EDM,
        "trance" to TasteTaxonomy.GENRE_EDM,
        "dubstep" to TasteTaxonomy.GENRE_EDM,
        "drum and bass" to TasteTaxonomy.GENRE_EDM,
        "dnb" to TasteTaxonomy.GENRE_EDM,
        "electro" to TasteTaxonomy.GENRE_EDM,
        "electronica" to TasteTaxonomy.GENRE_EDM,
        "r b" to TasteTaxonomy.GENRE_RNB,
        "rnb" to TasteTaxonomy.GENRE_RNB,
        "r and b" to TasteTaxonomy.GENRE_RNB,
        "randb" to TasteTaxonomy.GENRE_RNB,
        "rhythm and blues" to TasteTaxonomy.GENRE_RNB,
        "contemporary r b" to TasteTaxonomy.GENRE_RNB,
        "r b soul" to TasteTaxonomy.GENRE_RNB,
        "bollywood" to TasteTaxonomy.GENRE_BOLLYWOOD,
        "filmi" to TasteTaxonomy.GENRE_BOLLYWOOD,
        "classical" to TasteTaxonomy.GENRE_CLASSICAL,
        "indian classical" to TasteTaxonomy.GENRE_CLASSICAL,
        "hindustani classical" to TasteTaxonomy.GENRE_CLASSICAL,
        "carnatic" to TasteTaxonomy.GENRE_CLASSICAL,
        "orchestral" to TasteTaxonomy.GENRE_CLASSICAL,
        "opera" to TasteTaxonomy.GENRE_CLASSICAL,
        "sufi" to TasteTaxonomy.GENRE_SUFI,
        "ghazal" to TasteTaxonomy.GENRE_SUFI,
        "ghazal sufi" to TasteTaxonomy.GENRE_SUFI,
        "qawwali" to TasteTaxonomy.GENRE_SUFI,
        "devotional" to TasteTaxonomy.GENRE_DEVOTIONAL,
        "devotional spiritual" to TasteTaxonomy.GENRE_DEVOTIONAL,
        "bhajan" to TasteTaxonomy.GENRE_DEVOTIONAL,
        "kirtan" to TasteTaxonomy.GENRE_DEVOTIONAL,
        "gospel" to TasteTaxonomy.GENRE_DEVOTIONAL,
        "christian" to TasteTaxonomy.GENRE_DEVOTIONAL,
        "islamic" to TasteTaxonomy.GENRE_DEVOTIONAL,
        "folk" to TasteTaxonomy.GENRE_FOLK,
        "folk acoustic" to TasteTaxonomy.GENRE_FOLK,
        "americana" to TasteTaxonomy.GENRE_FOLK,
        "bhangra" to TasteTaxonomy.GENRE_FOLK,
        "phonk" to TasteTaxonomy.GENRE_PHONK,
        "brazilian phonk" to TasteTaxonomy.GENRE_PHONK,
        "drift phonk" to TasteTaxonomy.GENRE_PHONK,
        "acoustic" to TasteTaxonomy.GENRE_ACOUSTIC,
        "unplugged" to TasteTaxonomy.GENRE_ACOUSTIC,
        "jazz" to TasteTaxonomy.GENRE_JAZZ,
        "bossa nova" to TasteTaxonomy.GENRE_JAZZ,
        "swing" to TasteTaxonomy.GENRE_JAZZ,
        "metal" to TasteTaxonomy.GENRE_METAL,
        "heavy metal" to TasteTaxonomy.GENRE_METAL,
        "metalcore" to TasteTaxonomy.GENRE_METAL,
        "death metal" to TasteTaxonomy.GENRE_METAL,
        "black metal" to TasteTaxonomy.GENRE_METAL,
        "nu metal" to TasteTaxonomy.GENRE_METAL,
        "hard rock metal" to TasteTaxonomy.GENRE_METAL,
        "punk" to TasteTaxonomy.GENRE_PUNK,
        "punk rock" to TasteTaxonomy.GENRE_PUNK,
        "pop punk" to TasteTaxonomy.GENRE_PUNK,
        "post punk" to TasteTaxonomy.GENRE_PUNK,
        "hardcore" to TasteTaxonomy.GENRE_PUNK,
        "emo" to TasteTaxonomy.GENRE_PUNK,
        "soul" to TasteTaxonomy.GENRE_SOUL,
        "funk" to TasteTaxonomy.GENRE_SOUL,
        "motown" to TasteTaxonomy.GENRE_SOUL,
        "neo soul" to TasteTaxonomy.GENRE_SOUL,
        "reggae" to TasteTaxonomy.GENRE_REGGAE,
        "reggaeton" to TasteTaxonomy.GENRE_REGGAE,
        "dancehall" to TasteTaxonomy.GENRE_REGGAE,
        "ska" to TasteTaxonomy.GENRE_REGGAE,
        "afrobeats" to TasteTaxonomy.GENRE_AFROBEATS,
        "afrobeat" to TasteTaxonomy.GENRE_AFROBEATS,
        "afropop" to TasteTaxonomy.GENRE_AFROBEATS,
        "amapiano" to TasteTaxonomy.GENRE_AFROBEATS,
        "blues" to TasteTaxonomy.GENRE_BLUES,
        "country" to TasteTaxonomy.GENRE_COUNTRY,
        "bluegrass" to TasteTaxonomy.GENRE_COUNTRY,
        "soundtrack" to TasteTaxonomy.GENRE_SOUNDTRACK,
        "soundtracks" to TasteTaxonomy.GENRE_SOUNDTRACK,
        "ost" to TasteTaxonomy.GENRE_SOUNDTRACK,
        "film score" to TasteTaxonomy.GENRE_SOUNDTRACK,
        "anime" to TasteTaxonomy.GENRE_SOUNDTRACK,
        "video game" to TasteTaxonomy.GENRE_SOUNDTRACK,
        "drill" to TasteTaxonomy.GENRE_DRILL,
        "trap" to TasteTaxonomy.GENRE_DRILL,
        "uk drill" to TasteTaxonomy.GENRE_DRILL,
        "synthwave" to TasteTaxonomy.GENRE_SYNTHWAVE,
        "retrowave" to TasteTaxonomy.GENRE_SYNTHWAVE,
        "vaporwave" to TasteTaxonomy.GENRE_SYNTHWAVE,
        "new wave" to TasteTaxonomy.GENRE_SYNTHWAVE,
        "disco" to TasteTaxonomy.GENRE_SYNTHWAVE,
    )

    private val THEME_SYNONYMS: Map<String, TasteTag> = mapOf(
        "chill" to TasteTaxonomy.THEME_CHILL,
        "chillout" to TasteTaxonomy.THEME_CHILL,
        "chillwave" to TasteTaxonomy.THEME_CHILL,
        "relax" to TasteTaxonomy.THEME_CHILL,
        "relaxing" to TasteTaxonomy.THEME_CHILL,
        "downtempo" to TasteTaxonomy.THEME_CHILL,
        "ambient" to TasteTaxonomy.THEME_CHILL,
        "romantic" to TasteTaxonomy.THEME_ROMANTIC,
        "romance" to TasteTaxonomy.THEME_ROMANTIC,
        "love" to TasteTaxonomy.THEME_ROMANTIC,
        "love songs" to TasteTaxonomy.THEME_ROMANTIC,
        "sad" to TasteTaxonomy.THEME_SAD,
        "heartbreak" to TasteTaxonomy.THEME_SAD,
        "melancholy" to TasteTaxonomy.THEME_SAD,
        "party" to TasteTaxonomy.THEME_PARTY,
        "club" to TasteTaxonomy.THEME_PARTY,
        "party club" to TasteTaxonomy.THEME_PARTY,
        "hype" to TasteTaxonomy.THEME_HYPE,
        "workout" to TasteTaxonomy.THEME_HYPE,
        "gym" to TasteTaxonomy.THEME_HYPE,
        "fitness" to TasteTaxonomy.THEME_HYPE,
        "focus" to TasteTaxonomy.THEME_FOCUS,
        "study" to TasteTaxonomy.THEME_FOCUS,
        "concentration" to TasteTaxonomy.THEME_FOCUS,
        "late night" to TasteTaxonomy.THEME_LATE_NIGHT,
        "midnight" to TasteTaxonomy.THEME_LATE_NIGHT,
        "night" to TasteTaxonomy.THEME_LATE_NIGHT,
        "feel good" to TasteTaxonomy.THEME_FEEL_GOOD,
        "happy" to TasteTaxonomy.THEME_FEEL_GOOD,
        "upbeat" to TasteTaxonomy.THEME_FEEL_GOOD,
        "nostalgic" to TasteTaxonomy.THEME_NOSTALGIC,
        "nostalgia" to TasteTaxonomy.THEME_NOSTALGIC,
        "retro" to TasteTaxonomy.THEME_NOSTALGIC,
        "90s" to TasteTaxonomy.THEME_NOSTALGIC,
        "2000s" to TasteTaxonomy.THEME_NOSTALGIC,
        "80s" to TasteTaxonomy.THEME_NOSTALGIC,
        "decades" to TasteTaxonomy.THEME_NOSTALGIC,
        "commute" to TasteTaxonomy.THEME_COMMUTE,
        "drive" to TasteTaxonomy.THEME_COMMUTE,
        "driving" to TasteTaxonomy.THEME_COMMUTE,
        "road trip" to TasteTaxonomy.THEME_COMMUTE,
        "sleep" to TasteTaxonomy.THEME_SLEEP,
        "sleeping" to TasteTaxonomy.THEME_SLEEP,
        "bedtime" to TasteTaxonomy.THEME_SLEEP,
        "aggressive" to TasteTaxonomy.THEME_AGGRESSIVE,
        "rage" to TasteTaxonomy.THEME_AGGRESSIVE,
        "spiritual" to TasteTaxonomy.THEME_SPIRITUAL,
        "devotional" to TasteTaxonomy.THEME_SPIRITUAL,
        "meditation" to TasteTaxonomy.THEME_SPIRITUAL,
        "energetic" to TasteTaxonomy.THEME_ENERGETIC,
        "energy" to TasteTaxonomy.THEME_ENERGETIC,
    )

    private val AREA_TO_LANGUAGE: Map<String, TasteTag> = mapOf(
        "punjab" to TasteTaxonomy.LANG_PUNJABI,
        "chandigarh" to TasteTaxonomy.LANG_PUNJABI,
        "ludhiana" to TasteTaxonomy.LANG_PUNJABI,
        "jalandhar" to TasteTaxonomy.LANG_PUNJABI,
        "amritsar" to TasteTaxonomy.LANG_PUNJABI,
        "patiala" to TasteTaxonomy.LANG_PUNJABI,
        "bihar" to TasteTaxonomy.LANG_BHOJPURI,
        "patna" to TasteTaxonomy.LANG_BHOJPURI,
        "varanasi" to TasteTaxonomy.LANG_BHOJPURI,
        "haryana" to TasteTaxonomy.LANG_HARYANVI,
        "rohtak" to TasteTaxonomy.LANG_HARYANVI,
        "hisar" to TasteTaxonomy.LANG_HARYANVI,
        "tamil nadu" to TasteTaxonomy.LANG_TAMIL,
        "chennai" to TasteTaxonomy.LANG_TAMIL,
        "telangana" to TasteTaxonomy.LANG_TELUGU,
        "andhra pradesh" to TasteTaxonomy.LANG_TELUGU,
        "hyderabad" to TasteTaxonomy.LANG_TELUGU,
        "kerala" to TasteTaxonomy.LANG_MALAYALAM,
        "kochi" to TasteTaxonomy.LANG_MALAYALAM,
        "thiruvananthapuram" to TasteTaxonomy.LANG_MALAYALAM,
        "karnataka" to TasteTaxonomy.LANG_KANNADA,
        "bengaluru" to TasteTaxonomy.LANG_KANNADA,
        "bangalore" to TasteTaxonomy.LANG_KANNADA,
        "maharashtra" to TasteTaxonomy.LANG_MARATHI,
        "pune" to TasteTaxonomy.LANG_MARATHI,
        "mumbai" to TasteTaxonomy.LANG_HINDI,
        "new delhi" to TasteTaxonomy.LANG_HINDI,
        "delhi" to TasteTaxonomy.LANG_HINDI,
        "india" to TasteTaxonomy.LANG_HINDI,
        "gujarat" to TasteTaxonomy.LANG_GUJARATI,
        "ahmedabad" to TasteTaxonomy.LANG_GUJARATI,
        "surat" to TasteTaxonomy.LANG_GUJARATI,
        "west bengal" to TasteTaxonomy.LANG_BENGALI,
        "kolkata" to TasteTaxonomy.LANG_BENGALI,
        "bangladesh" to TasteTaxonomy.LANG_BENGALI,
        "dhaka" to TasteTaxonomy.LANG_BENGALI,
        "pakistan" to TasteTaxonomy.LANG_URDU,
        "lahore" to TasteTaxonomy.LANG_URDU,
        "karachi" to TasteTaxonomy.LANG_URDU,
        "islamabad" to TasteTaxonomy.LANG_URDU,
        "south korea" to TasteTaxonomy.LANG_KOREAN,
        "korea" to TasteTaxonomy.LANG_KOREAN,
        "seoul" to TasteTaxonomy.LANG_KOREAN,
        "japan" to TasteTaxonomy.LANG_JAPANESE,
        "tokyo" to TasteTaxonomy.LANG_JAPANESE,
        "osaka" to TasteTaxonomy.LANG_JAPANESE,
        "spain" to TasteTaxonomy.LANG_SPANISH,
        "mexico" to TasteTaxonomy.LANG_SPANISH,
        "colombia" to TasteTaxonomy.LANG_SPANISH,
        "argentina" to TasteTaxonomy.LANG_SPANISH,
        "puerto rico" to TasteTaxonomy.LANG_SPANISH,
        "france" to TasteTaxonomy.LANG_FRENCH,
        "paris" to TasteTaxonomy.LANG_FRENCH,
        "brazil" to TasteTaxonomy.LANG_PORTUGUESE,
        "portugal" to TasteTaxonomy.LANG_PORTUGUESE,
        "united states" to TasteTaxonomy.LANG_ENGLISH,
        "united kingdom" to TasteTaxonomy.LANG_ENGLISH,
        "canada" to TasteTaxonomy.LANG_ENGLISH,
        "australia" to TasteTaxonomy.LANG_ENGLISH,
    )

    fun normalizeTag(raw: String): NormalizedMasks {
        val cleaned = clean(raw)
        if (cleaned.isEmpty()) return NormalizedMasks()

        var langMask = 0
        var genreMask = 0
        var themeMask = 0

        LANGUAGE_SYNONYMS[cleaned]?.let { langMask = langMask or it.bitMask }
        GENRE_SYNONYMS[cleaned]?.let { genreMask = genreMask or it.bitMask }
        THEME_SYNONYMS[cleaned]?.let { themeMask = themeMask or it.bitMask }

        // Handle compound labels like "Hindi Pop", "Punjabi Hip-Hop", "Late Night Chill"
        if (langMask == 0 || genreMask == 0 || themeMask == 0) {
            val words = cleaned.split(' ')
            if (words.size >= 2) {
                for (w in words) {
                    if (w.length < 2) continue
                    LANGUAGE_SYNONYMS[w]?.let { langMask = langMask or it.bitMask }
                    GENRE_SYNONYMS[w]?.let { genreMask = genreMask or it.bitMask }
                    THEME_SYNONYMS[w]?.let { themeMask = themeMask or it.bitMask }
                }
                for (i in 0 until words.size - 1) {
                    val bigram = "${words[i]} ${words[i + 1]}"
                    LANGUAGE_SYNONYMS[bigram]?.let { langMask = langMask or it.bitMask }
                    GENRE_SYNONYMS[bigram]?.let { genreMask = genreMask or it.bitMask }
                    THEME_SYNONYMS[bigram]?.let { themeMask = themeMask or it.bitMask }
                }
            }
        }
        return NormalizedMasks(langMask = langMask, genreMask = genreMask, themeMask = themeMask)
    }

    fun normalizeAll(rawTags: Iterable<String>): NormalizedMasks {
        var acc = NormalizedMasks()
        for (tag in rawTags) {
            acc = acc + normalizeTag(tag)
        }
        return acc
    }

    fun normalizeArea(area: String?): Int {
        val cleaned = area?.let(::clean)?.takeIf { it.isNotEmpty() } ?: return 0
        AREA_TO_LANGUAGE[cleaned]?.let { return it.bitMask }
        for ((needle, lang) in AREA_TO_LANGUAGE) {
            if (cleaned.contains(needle)) return lang.bitMask
        }
        return 0
    }

    /** Maps MusicBrainz ISO-3166 country codes when `area.name` is missing. */
    fun normalizeCountryCode(code: String?): Int = when (code?.trim()?.uppercase(Locale.ROOT)) {
        "IN" -> TasteTaxonomy.LANG_HINDI.bitMask
        "PK" -> TasteTaxonomy.LANG_URDU.bitMask
        "BD" -> TasteTaxonomy.LANG_BENGALI.bitMask
        "KR" -> TasteTaxonomy.LANG_KOREAN.bitMask
        "JP" -> TasteTaxonomy.LANG_JAPANESE.bitMask
        "ES", "MX", "CO", "AR", "CL", "PE", "PR" -> TasteTaxonomy.LANG_SPANISH.bitMask
        "FR" -> TasteTaxonomy.LANG_FRENCH.bitMask
        "BR", "PT" -> TasteTaxonomy.LANG_PORTUGUESE.bitMask
        "SA", "AE", "EG", "LB", "MA" -> TasteTaxonomy.LANG_ARABIC.bitMask
        "US", "GB", "CA", "AU", "NZ", "IE" -> TasteTaxonomy.LANG_ENGLISH.bitMask
        else -> 0
    }
}
