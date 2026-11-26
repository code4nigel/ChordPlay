/*
 * Adapted from Dial (https://github.com/steeben/Dial) by steeben.
 * Copyright (C) 2026 Shivanshu (code4nigel) <https://github.com/code4nigel/ChordPlay>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.music.bitchord.data

import java.util.Locale

/**
 * The country the listener chose for recommendations (Settings, Language &
 * location, Country).
 *
 * YouTube Music sends Home, Explore, charts, new releases and radios for the
 * country in the `gl` field of each request context. When unset, [chosenOrNull]
 * returns null and requests fall back to [com.music.bitchord.data.innertube.Innertube.automaticRegion].
 *
 * Stream resolution and play reports do not use the chosen country (see
 * [com.music.bitchord.data.innertube.Innertube.automaticRegion]): what plays
 * is decided by the listener's real location, and a different `gl` there can
 * only make a stream fail.
 */
object ContentCountry {
    /** The stored choice: an ISO 3166 country code, or blank for automatic. */
    @Volatile
    var chosen: () -> String? = { null }

    /** The chosen country as an upper case two letter code, or null for automatic. */
    fun chosenOrNull(): String? =
        chosen()?.trim()?.uppercase(Locale.ROOT)?.takeIf { it.length == 2 && it.all(Char::isLetter) }

    /**
     * The countries the picker offers: the locations YouTube lets a listener
     * choose. Names come from [Locale], so they show in the app language.
     */
    val CODES: List<String> = listOf(
        "AE", "AR", "AT", "AU", "AZ", "BA", "BD", "BE", "BG", "BH", "BO", "BR", "BY",
        "CA", "CH", "CL", "CO", "CR", "CY", "CZ", "DE", "DK", "DO", "DZ", "EC", "EE",
        "EG", "ES", "FI", "FR", "GB", "GE", "GH", "GR", "GT", "HK", "HN", "HR", "HU",
        "ID", "IE", "IL", "IN", "IQ", "IS", "IT", "JM", "JO", "JP", "KE", "KH", "KR",
        "KW", "KZ", "LA", "LB", "LI", "LK", "LT", "LU", "LV", "LY", "MA", "ME", "MK",
        "MT", "MX", "MY", "NG", "NI", "NL", "NO", "NP", "NZ", "OM", "PA", "PE", "PG",
        "PH", "PK", "PL", "PR", "PT", "PY", "QA", "RO", "RS", "RU", "SA", "SE", "SG",
        "SI", "SK", "SN", "SV", "TH", "TN", "TR", "TW", "TZ", "UA", "UG", "US", "UY",
        "VE", "VN", "YE", "ZA", "ZW",
    )

    /** The name of [code] in [displayLocale], for example "Germany" or "Deutschland". */
    fun displayName(code: String, displayLocale: Locale): String =
        runCatching { Locale.Builder().setRegion(code).build().getDisplayCountry(displayLocale) }
            .getOrNull()?.ifBlank { null } ?: code
}
