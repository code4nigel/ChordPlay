package com.music.bitchord.data.settings

/**
 * Stable persisted ordering for a Library "Show all" grid — playlists or
 * albums. A card there only ever carries a title, so unlike `LocalMusicSort`
 * there is nothing date-based to offer.
 */
enum class LibrarySort(val label: String) {
    /** Whatever order the shelf itself arrived in — YouTube Music's own. */
    DEFAULT("Date Added"),
    TITLE_ASC("Title A-Z"),
    TITLE_DESC("Title Z-A"),
}

/** Display mode for music lists: compact rows or grid cards. */
enum class LibraryViewType {
    LIST,
    GRID,
}

/** Display mode for Library "Show all" page: grid cards or full-width list rows. */
enum class LibraryViewMode {
    GRID,
    LIST,
}

/** Layout mode for Home Speed Dial section. */
enum class SpeedDialLayoutMode(val label: String) {
    TWO_BY_THREE("2×3 Grid"),
    THREE_BY_THREE("3×3 Grid"),
    ONE_BY_FOUR_LIST("1×4 List");

    fun next(): SpeedDialLayoutMode = when (this) {
        TWO_BY_THREE -> THREE_BY_THREE
        THREE_BY_THREE -> ONE_BY_FOUR_LIST
        ONE_BY_FOUR_LIST -> TWO_BY_THREE
    }
}


enum class HomeShelfKey(val id: String, val label: String, val subtitle: String) {
    SPEED_DIAL("speed_dial", "Your Space", "Pinned favorites, mixes & high-frecency grid"),
    QUICK_PICKS("quick_picks", "Quick Picks", "4-row instant radio starter tracks"),
    TOP_PICKS("top_picks", "Top Picks for You", "Artist stations, mixes & spotlight cards"),
    SIMILAR_TO("similar_to", "Similar to Recent Listens", "Recommendations & radios based on recent tracks"),
    COMMUNITY_PLAYLISTS("community_playlists", "Community Playlists", "Trending playlists from the YouTube Music community"),
    FEATURED_PLAYLISTS("featured_playlists", "Featured Playlists", "Editorial spotlights & curated playlist carousels"),
    CHARTS("charts", "Charts & Trending", "Top tracks, global/regional charts & trending hits"),
    REMIXES("remixes", "Remixes & Edits", "Club mixes, VIP edits & reimagined tracks"),
    MUSIC_VIDEOS("music_videos", "Music Videos", "16:9 widescreen music videos & live sessions"),
    MIXED_FOR_YOU("mixed_for_you", "Mixed for You", "YouTube Music personalized mix carousels"),
    LISTEN_AGAIN("listen_again", "Listen Again & Favorites", "Forgotten favorites & heavy rotation shelves"),
    NEW_RELEASES("new_releases", "New Releases", "Latest albums, singles & EPs"),
    OTHER("other", "More Recommendations", "Genre spotlights, moods & other discoveries");

    companion object {
        val DEFAULT_ORDER: List<HomeShelfKey> = entries.toList()

        private val cache = java.util.concurrent.ConcurrentHashMap<String, HomeShelfKey>()

        fun clearCache() {
            cache.clear()
        }

        fun fromTitle(title: String): HomeShelfKey = cache.getOrPut(title) {
            val t = title.lowercase()
            when {
                // QUICK_PICKS / RECENTS
                t.contains("quick pick") || t.contains("quick_pick") ||
                    t.contains("schnelle auswahl") || t.contains("sélection rapide") || t.contains("selection rapide") ||
                    t.contains("szybki wybór") || t.contains("szybki wybor") ||
                    t.contains("selección rápida") || t.contains("seleccion rapida") ||
                    t.contains("escolhas rápidas") || t.contains("escolhas rapidas") ||
                    t.contains("pilihan instan") || t.contains("pilihan cepat") ||
                    t.contains("क्विक पिक्स") || t.contains("त्वरित चयन") ||
                    t.contains("クイック ミックス") || t.contains("クイックミックス") || t.contains("クイックピック") ||
                    t.contains("быстрый выбор") || t.contains("快速选择") || t.contains("快速選擇") ||
                    t.contains("בחירות מהירות") || t.contains("בחירה מהירה") ||
                    t.contains("scelte rapide") || t.contains("scelta rapida") ||
                    t.contains("hızlı seçimler") || t.contains("hizli secimler") ||
                    t.contains("เลือกด่วน") || t.contains("ตัวเลือกด่วน") ||
                    t.contains("lựa chọn nhanh") || t.contains("chọn nhanh đài phát") || t.contains("lua chon nhanh") ||
                    t.contains("recents") || t.contains("recently played") || t.contains("gần đây") || t.contains("最近") -> QUICK_PICKS

                // SIMILAR_TO
                t.contains("similar to") || t.contains("similar a") || t.contains("similaire à") || t.contains("similaire a") ||
                    t.contains("ähnlich wie") || t.contains("aehnlich wie") || t.contains("simile a") || t.contains("podobne do") ||
                    t.contains("похоже на") || t.contains("похожие на") || t.contains("semelhante a") || t.contains("mirip dengan") ||
                    t.contains("คล้ายกับ") || t.contains("דומה ל") || t.contains("בדומה ל") ||
                    t.contains("şuna benzer") || t.contains("benzeri") || t.contains("tương tự") ||
                    t.contains("に似ている") || t.contains("के समान") || t.contains("से मिलते-जुलते") ||
                    (t.contains("与") && t.contains("类似")) || (t.contains("與") && t.contains("類似")) ||
                    t.contains("类似") || t.contains("類似") || t.contains("相似") -> SIMILAR_TO

                // COMMUNITY_PLAYLISTS
                t.contains("community") || t.contains("cộng đồng") || t.contains("cong dong") ||
                    t.contains("コミュニティ") || t.contains("comunidad") || t.contains("communauté") || t.contains("communaute") ||
                    t.contains("społeczności") || t.contains("spolecznosci") || t.contains("comunidade") ||
                    t.contains("komunitas") || t.contains("समुदाय") || t.contains("сообществ") ||
                    t.contains("社群") || t.contains("社区") || t.contains("קהילה") ||
                    t.contains("topluluk") || t.contains("ชุมชน") -> COMMUNITY_PLAYLISTS

                // MUSIC_VIDEOS / SHORTS
                t.contains("video") || t.contains("vídeo") || t.contains("teledysk") || t.contains("клип") ||
                    t.contains("ミュージック ビデオ") || t.contains("ミュージックビデオ") ||
                    t.contains("音乐视频") || t.contains("音樂影片") || t.contains("音乐短片") || t.contains("mv") ||
                    t.contains("קליפ") || t.contains("videoclip") || t.contains("วิดีโอ") ||
                    t.contains("shorts") || t.contains("short") || t.contains("ショート") || t.contains("krótkie filmy") -> MUSIC_VIDEOS

                // TOP_PICKS
                t.contains("top pick") || t.contains("top-auswahl") || t.contains("top auswahl") ||
                    t.contains("meilleures sélections") || t.contains("meilleure selection") ||
                    t.contains("najlepszy wybór") || t.contains("najlepszy wybor") ||
                    t.contains("principais escolhas") || t.contains("pilihan teratas") ||
                    t.contains("टॉप पिक्स") || t.contains("главный выбор") || t.contains("лучший выбор") ||
                    t.contains("为你精选") || t.contains("為你精選") || t.contains("特别推荐") || t.contains("特別推薦") ||
                    t.contains("בחירות המובילות") || t.contains("scelte migliori") || t.contains("en iyi seçimler") ||
                    t.contains("ตัวเลือกยอดนิยม") || t.contains("lựa chọn hàng đầu") || t.contains("spotlight") -> TOP_PICKS

                // FEATURED_PLAYLISTS
                t.contains("featured playlist") || t.contains("listas destacadas") || t.contains("playlist destacadas") ||
                    t.contains("playlists à la une") || t.contains("empfohlene playlists") ||
                    t.contains("polecane playlisty") || t.contains("playlists em destaque") ||
                    t.contains("playlist unggulan") || t.contains("विशेष प्लेलिस्ट") ||
                    t.contains("注目の再生リスト") || t.contains("рекомендуемые плейлисты") ||
                    t.contains("精选播放列表") || t.contains("精選播放清單") ||
                    t.contains("פלייליסטים נבחרים") || t.contains("playlist in primo piano") ||
                    t.contains("öne çıkan oynatma listeleri") || t.contains("เพลย์ลิสต์แนะนำ") ||
                    t.contains("danh sách phát nổi bật") -> FEATURED_PLAYLISTS

                // CHARTS / TRENDING / HITS
                t.contains("chart") || t.contains("trending") || t.contains("éxitos") || t.contains("classement") ||
                    t.contains("listy przebojów") || t.contains("listy przebojow") || t.contains("paradas") ||
                    t.contains("tangga lagu") || t.contains("चार्ट") || t.contains("ट्रेंडिंग") ||
                    t.contains("急上昇") || t.contains("чарт") || t.contains("в тренде") ||
                    t.contains("排行榜") || t.contains("熱門榜") || t.contains("榜单") || t.contains("發燒") || t.contains("发烧") ||
                    t.contains("מצעד") || t.contains("classifiche") || t.contains("classifica") ||
                    t.contains("listeler") || t.contains("trendler") || t.contains("ชาร์ต") ||
                    t.contains("bảng xếp hạng") || t.contains("thịnh hành") || t.contains("hits today") ||
                    t.contains("today's hits") || t.contains("top tunes") -> CHARTS

                // REMIXES
                t.contains("remix") || t.contains("remezclas") || t.contains("remiksy") || t.contains("reprises") ||
                    t.contains("bearbeitungen") || t.contains("रीमिक्स") || t.contains("リミックス") ||
                    t.contains("ремикс") || t.contains("混音") || t.contains("重混") ||
                    t.contains("רמיקס") || t.contains("düzenlemeler") || t.contains("รีมิกซ์") ||
                    t.contains("bản phối lại") || t.contains("edits") || t.contains("club mix") -> REMIXES

                // LISTEN_AGAIN / FAVORITES
                t.contains("listen again") || t.contains("play again") || t.contains("nochmal anhören") ||
                    t.contains("nochmal anhoeren") || t.contains("écouter à nouveau") || t.contains("ecouter a nouveau") ||
                    t.contains("volver a escuchar") || t.contains("posłuchaj jeszcze raz") || t.contains("posluchaj jeszcze raz") ||
                    t.contains("ouvir novamente") || t.contains("dengarkan lagi") || t.contains("फिर से सुनें") ||
                    t.contains("もう一度聴く") || t.contains("слушать снова") ||
                    t.contains("重新收听") || t.contains("再次收聽") || t.contains("再次聆聽") ||
                    t.contains("האזנה חוזרת") || t.contains("ascolta di nuovo") || t.contains("tekrar dinle") ||
                    t.contains("ฟังอีกครั้ง") || t.contains("nghe lại") || t.contains("quen thuộc") ||
                    t.contains("favorite") || t.contains("favourite") || t.contains("rotation") ||
                    t.contains("favoriten") || t.contains("favoritos") || t.contains("ulubione") -> LISTEN_AGAIN

                // NEW_RELEASES
                t.contains("new release") || t.contains("novedades") || t.contains("nouveautés") || t.contains("nouveautes") ||
                    t.contains("neuerscheinungen") || t.contains("nowe wydania") || t.contains("nowości") || t.contains("nowosci") ||
                    t.contains("lançamentos") || t.contains("lancamentos") || t.contains("rilis baru") ||
                    t.contains("नई रिलीज़") || t.contains("नई रिलीज") || t.contains("最新リリース") ||
                    t.contains("новинки") || t.contains("新作品") || t.contains("最新發行") || t.contains("最新发行") ||
                    t.contains("מהדורות חדשות") || t.contains("nuove uscite") || t.contains("yeni çıkanlar") ||
                    t.contains("เพลงออกใหม่") || t.contains("bản phát hành mới") || t.contains("mới") ||
                    t.contains("new album") || t.contains("new singles") -> NEW_RELEASES

                // MIXED_FOR_YOU
                t.contains("mixed for you") || t.contains("mixes para ti") || t.contains("mix pour vous") ||
                    t.contains("für dich gemixt") || t.contains("fuer dich gemixt") || t.contains("miks dla ciebie") ||
                    t.contains("músicas para você") || t.contains("dibuat khusus untuk anda") ||
                    t.contains("आपके लिए मिक्स") || t.contains("специально для вас") ||
                    t.contains("量身打造") || t.contains("为你打造") || t.contains("為你打造") ||
                    t.contains("מיקסים בשבילך") || t.contains("mix per te") || t.contains("sizin için karışık") ||
                    t.contains("มิกซ์สำหรับคุณ") || t.contains("dành riêng cho bạn") ||
                    t.contains("my mix") || t.contains("mixes") || t.contains("for you") -> MIXED_FOR_YOU

                else -> OTHER
            }
        }
    }
}
