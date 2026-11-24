package com.music.bitchord.data.settings

enum class CanvasProvider {
    SPOTIFY,
    APPLE_MUSIC,
    TIDAL,
    COMMUNITY;

    companion object {
        val defaultOrder: List<CanvasProvider> = entries.toList()

        fun normalize(raw: List<CanvasProvider>): List<CanvasProvider> {
            val seen = LinkedHashSet<CanvasProvider>(entries.size)
            for (provider in raw) seen.add(provider)
            for (provider in entries) seen.add(provider)
            return seen.toList()
        }

        fun parse(stored: String?): List<CanvasProvider> {
            if (stored.isNullOrBlank()) return defaultOrder
            val parsed = stored.split(',').mapNotNull { token ->
                entries.firstOrNull { it.name == token.trim() }
            }
            return normalize(parsed)
        }

        fun serialize(order: List<CanvasProvider>): String =
            normalize(order).joinToString(",") { it.name }
    }
}
