# ChordPlay v1.9.0

Welcome to ChordPlay v1.9.0! This release introduces branding and attribution refinements, listening experience / recap layout restructuring, and builds upon the complete YouTube Music redesign:

### What's New in v1.9.0:
- **Branding & Upstream Attribution**:
  - Cleaned developer attribution in Settings: removed redundant hyperlink on author name, leaving direct links for GitHub, Developer Profile, and Releases.
  - Added full attribution links for upstream **BitChord by Kushagra Singh** (GitHub, Developer, Releases) mirroring the ChordPlay section.
  - In Miscellaneous settings, reordered **Links & Community** so the upstream BitChord repository is placed cleanly at the bottom.
- **Library Recap Card Placement**:
  - Relocated the personal **Listening Experience / Recap card** to the very top of the Library screen, positioned prominently above the search bar and category filter tabs.
- **YouTube Music-Style Vertical Library**:
  - Full-width vertical rows, live search, sort dropdown (A-Z, Z-A, Date Added), and Grid/List toggle.
- **Speed Dial 1×4 List Mode & 3-Way Layout Switcher**:
  - Switch seamlessly between 2×3 Squircle Grid, 3×3 Grid, and 1×4 Track Column List mode matching Recents.
- **Default Dynamic Song Art Palette**:
  - Adaptive real-time album aura theming active by default.
- **Roll a Dice**:
  - Dedicated dice icon (Casino) and contrast-adaptive theming.

---

# ChordPlay v1.8.9

Welcome to ChordPlay v1.8.9! This update introduces a full YouTube Music-style Library experience with real-time search, sorting, and view switching, adaptive Speed Dial layout modes with a new 1×4 track column list, default Dynamic Song Art theming, high-contrast chip rendering, and Liquid Glass refinements:

### What's New in v1.8.9:
- **Dynamic Song Art Enabled by Default**:
  - The real-time adaptive **Dynamic Song Art** theme is now the default appearance palette, dynamically extracting and matching atmospheric colors and aura lighting from whatever song is currently playing.
- **YouTube Music-Style Vertical Library with Search & Sorting**:
  - **Live Search Bar**: Search across all your saved playlists, albums, artists, and downloads directly from the top of the Library screen.
  - **Sorting Options**: Sort your library by Title (A-Z), Title (Z-A), or Date Added via the dedicated sort menu.
  - **Grid & List View Switcher**: Seamlessly toggle between full-width vertical list rows and clean multi-column card grids.
  - **Category Filter Tabs**: Quick scrollable selector tabs for `All`, `Playlists`, `Albums`, `Artists`, and `Downloads`.
- **Speed Dial Section Header & 3-Way Layout Switcher**:
  - Added a dedicated **Speed Dial** section header with an instantaneous layout toggle button cycling through 3 distinct presentations:
    1. **2×3 Grid**: Elegant squircle cards with page dots and quick-return navigation.
    2. **3×3 Grid**: Compact YouTube Music speed dial grid (9 items per page).
    3. **1×4 List**: Horizontal swipeable columns of 4 tracks per column, matching the Recents layout.
- **"Roll a Dice" Discovery Tile & Theme Contrast**:
  - Transformed the discovery card into a dedicated **Roll a Dice** tile with a dice icon and high-contrast styling that dynamically adapts when white or light theme highlights are selected.
- **Enhanced Speed Dial Artwork Cutouts**:
  - Removed cluttering text badges in favor of distinct tactile artwork cutouts: circular avatars for Artists, stacked folder sleeves with micro-badges for Playlists, and vinyl jackets for Albums.
- **Context Menu "Pin to Library"**:
  - Clarified library pinning action to **Pin to Library** / **Unpin from Library** and elevated it immediately beneath "Pin to Speed Dial".
- **Liquid Glass on Account Switcher**:
  - Extended the tactile Liquid Glass blur and specular highlights to the Account Switcher popup dialog when Liquid Glass is enabled.
- **High-Contrast Mood Filter Chips**:
  - Dynamically flips selected chip text to high-contrast dark obsidian when using light or white highlights (such as Monochrome mode).

---

# ChordPlay v1.8.8

Welcome to ChordPlay v1.8.8! This update introduces a unified single-layer Account & Integrations hub, a revamped Miscellaneous section with live in-app update checking and upstream BitChord credit, a prominent golden accent for Dynamic Song Art, flexible playlist swipe actions, dedicated lyrics offset tuning in the music player, and taste-aware lucky discovery:

### What's New in v1.8.8:
- **Unified Single-Layer Account & Integrations Hub**:
  - Eliminated confusing nested sub-pages: Clicking either the top account profile banner or the "Account & integrations" category row in Settings directly opens one unified integrations screen.
  - Manage your YouTube Music account, switch profiles ("Listen as"), connect/disconnect Spotify, link Discord Rich Presence, monitor Listen Together party sessions, and configure ListenBrainz & Last.fm scrobbler settings—all in one place with zero nested layers.
- **Renamed to "Miscellaneous" & Live In-App Update Checker**:
  - Renamed the bottom settings section from "Software Update" to **Miscellaneous**.
  - Added an interactive **Check for updates** button inside the top ChordPlay card in Miscellaneous, displaying real-time checking status, live download progress percentage, and one-tap install actions.
  - Added an upstream project credit card linking to **BitChord** (`github.com/kushagrasinghx/BitChord`) under "Links & Community" recognizing the project's upstream foundations.
- **Featured Dynamic Song Art Theme (Golden Circle)**:
  - Positioned **Dynamic Song Art** directly beside our default **ChordPlay Crimson** in the Appearance color palette carousel.
  - Adorned with an eye-catching animated golden ring and gold accent typography so users can immediately discover the real-time adaptive artwork colorway.
  - Eliminated transient theme flashbangs: smoothly retains the previous song's color scheme during track transitions while new cover art decodes, strictly respecting light and dark mode settings.
- **Enhanced Swipe Gestures (Remove from Playlist & Open Menu)**:
  - Added **"Remove from Playlist"** as a swipe action for instantaneous playlist cleanup.
  - Added **"Open Menu"** as a swipe action to effortlessly pop up the 3-dot song options sheet with a single swipe gesture.
  - Configurable individually under **Settings → Appearance → Swipe Gestures** for Left and Right swipes.
- **Dedicated Player Lyrics Offset Control**:
  - Moved lyrics offset out of the congested 3-dot overflow menu into dedicated player controls.
  - **Long-Press on Player Lyrics Button**: Holding the lyrics button in the player control deck directly opens the Lyrics Offset tuning sheet.
  - **Quick Tune Button in Lyrics View**: When synced lyrics are open, a quick tuning button (`Tune`) appears right beside the font style and translation toggles.
- **Taste-Aware Lucky Draw Discovery Dice**:
  - Refactored the Speed Dial lucky draw engine to immediately harvest personalized seeds from your listening history, home shelves (Quick picks, Listen again, etc.), and library favorites with 0s latency.
- **Monochrome Theme**:
  - Minimalist pure obsidian black and crisp stark white palette added to predefined color schemes.
- **Fullscreen Lyrics & FastScroller Polish**:
  - Lifted fullscreen artwork mini lyrics comfortably above system gesture navigation bars.
  - Adjusted 3-dot menu padding and scroller touch boundaries in library lists to eliminate accidental gestures.

---

# ChordPlay v1.8.7

Welcome to ChordPlay v1.8.7! This release introduces flexible lyrics presentation styles, a 3-way search division, quick Speed Dial return navigation, and a true YouTube Music style Lucky Draw discovery dice engine:

### What's New in v1.8.7:
- **Fullscreen Artwork Lyrics Presentation Modes**:
  - Choose how lyrics display in fullscreen artwork/Canvas mode under **Settings → Synced Lyrics → Fullscreen Artwork Lyrics Style**:
    - **Floating Capsule**: Sleek, centered frosted glass pill above the song title with fluid width spring morphing.
    - **Main Player Strip**: Matches the main player deck layout, positioned below the song title and action icons with left alignment and a right chevron arrow (`>`).
    - **Hidden**: Option to hide lyrics completely for an uninterrupted visual stage.
- **Three-Way Search Separation (YouTube, Library & Local)**:
  - The search bar now features three dedicated tabs:
    - **YouTube**: Explore the complete online YouTube Music catalogue (tracks, artists, albums, community playlists).
    - **Library**: Search through your personal saved playlists, followed artists, saved albums, and library songs.
    - **Local**: Search exclusively through audio files stored on device storage in your Local Music folder.
- **Speed Dial Quick Return Navigation**:
  - When paging horizontally past Page 0 of the home Speed Dial, a sleek floating frosted glass back button appears on the left edge.
  - Tapping it smoothly springs back to Page 0 with a single tap, granting instant access to the Lucky Draw discovery button.
- **True YouTube Music "Lucky Draw / Dice" Engine**:
  - Completely re-engineered the Lucky Draw dice button into a true genre exploration engine.
  - Randomly selects from a curated matrix of 25+ diverse musical genres and styles (Synthwave, Neo-Soul, City Pop, Afrobeats, 90s Alternative Rock, Deep House, K-Pop, Lo-Fi, Nu-Metal, Jazz, Latin, and more) with infinite dynamic radio generation.
- **Android Target Polish**:
  - Cleaned up cross-platform host bindings and verified APK signing against the permanent repository keystore.

### Download Assets:
- `chordplay-v1.8.7-build33-devDebug-universal.apk` (and alias `chordplay-v1.8.7-build33-devDebug.apk`): Universal build compatible with all Android devices (`arm64-v8a`, `armeabi-v7a`, `x86_64`).
- `chordplay-v1.8.7-build33-devDebug-arm64.apk`: Optimized, lightweight build for modern 64-bit ARM devices.

---

# ChordPlay v1.8.6

Welcome to ChordPlay v1.8.6! This release focuses on stability, fluid micro-animations, fast library navigation, and seamless in-app updating:

### What's New in v1.8.6:
- **Startup Crash Resolution**:
  - Eliminated fatal `NullPointerException` on cold launch caused by coroutine flow initialization timing in `MainViewModel`.
- **Smooth Fullscreen Canvas Lyric Transitions**:
  - Replaced abrupt text swaps with fluid `AnimatedContent` featuring vertical slide (`slideInVertically` / `slideOutVertically`) and alpha cross-fading.
  - Added spring-physics `animateContentSize` to the floating capsule pill so its width morphs dynamically between short and long lines without jarring dimension snaps.
  - Keyed transitions by line index so repeating chorus lyrics always transition cleanly.
- **FastScroller Performance & Touch Precision**:
  - Re-engineered fast scroll thumb touch detection: touches outside the thumb pass through freely, eliminating touch-blocking on playlist 3-dots menus.
  - Throttled 60FPS scroll loop preventing reverse jumping or frame pacing stutter.
- **Library Search & Categorization**:
  - Search within the Library tab across songs, playlists, albums, and artists with real-time debounced queries and offline local media matching.
- **Library View Layout Toggle**:
  - Instant toggle between Grid and List views in the Library tab for playlists and albums.
- **Reliable Playlist Membership Detection**:
  - Integrated `PlaylistMembershipCache` so "Already in Playlist" indicators update instantaneously without duplicate entries or redundant network requests.
  - Playlists already containing the active song are automatically prioritized to the top of the "Add to Playlist" sheet.
- **Lyrics Typography & Frosted Glass Polish**:
  - Frosted glass design applied to lyrics styling controls.
  - Added full typography weight options (Medium, SemiBold, Bold, Heavy) with text alignment options.
- **Permanent Repository Keystore**:
  - Integrated permanent ChordPlay repository signing certificate ensuring all future APK releases update in-place with zero signature mismatches.

### Download Assets:
- `chordplay-v1.8.6-build32-devDebug-universal.apk` (and alias `chordplay-v1.8.6-build32-devDebug.apk`): Universal build compatible with all Android devices (`arm64-v8a`, `armeabi-v7a`, `x86_64`).
- `chordplay-v1.8.6-build32-devDebug-arm64.apk`: Optimized, lightweight build for modern 64-bit ARM devices.

---

# ChordPlay v1.8.5

Welcome to ChordPlay v1.8.5! This major release delivers the complete ChordPlay Evolution experience: content-aware Speed Dial visual differentiation, true Dynamic Song Art theming, iconic ChordPlay Crimson palette, tactile Vinyl Turntable player layout, true-to-life interactive settings previews, and rock-solid playlist sheet persistence.

### What's New in v1.8.5:
- **Intelligent Speed Dial Differentiation**:
  - **Content-Aware Visual Hierarchy**: Content types on the home Speed Dial are now instantly recognizable at a glance:
    - **Artists**: Circular avatar clipping with gold badge icon.
    - **Albums**: Dual-layered vinyl jacket presentation with vinyl disc peek and album badge.
    - **Playlists**: Multi-layered card stack shadow effect with playlist badge and track count.
    - **Songs**: Rounded squircle cover art with track badge.
  - **Clean Subtitles**: Redundant prefixes ("Artist • ", "Album • ", "Playlist • ") are automatically stripped for a clean, modern aesthetic.
  - **Instant Reactive Pin/Unpin**: Toggling "Pin to Speed Dial" / "Unpin from Speed Dial" in track and browse action sheets updates the menu instantaneously with zero lag.
- **Dynamic Theming Suite**:
  - **Dynamic Song Art (True Dynamic Theme)**: Dynamically derives vibrant dominant and glowing accent tones directly from the active song's album artwork, morphing the entire app's backdrop, buttons, and navigation bar in real time as tracks change.
  - **ChordPlay Crimson**: Iconic signature dark theme matching the ChordPlay brand icon (deep obsidian, vivid crimson ruby, and neon accent highlights).
- **Vinyl Turntable Player Layout**:
  - Authentic spinning vinyl record with micro-textured groove rings, spinning center label, and an animated tone-arm pickup that smoothly moves into place on play and lifts on pause.
- **True-to-Life Settings Live Previews**:
  - Appearance and Player settings previews completely rewritten to mirror actual in-app screens (blurred aura backdrop, lyrics teaser, capsule deck, 140K OPUS badge, and home dashboard).
- **Dynamic Context-Aware Home Greetings**:
  - Rotating greeting messages cycling smoothly on launch with user profile name injection.
- **Rock-Solid Playlist Sheet Persistence**:
  - Bottom sheet maintains full user-set height during keyboard interactions, preventing sheet collapse when filtering or naming playlists.
- **Audio Fidelity & Interaction Polish**:
  - Real-time 140K/160K OPUS stream indicators, micro-haptic ticks on transport controls, and perimeter progress glow rings.

### Download Assets:
- `chordplay-v1.8.5-build31-devDebug-universal.apk` (and alias `chordplay-v1.8.5-build31-devDebug.apk`): Universal build compatible with all Android ABIs (`arm64-v8a`, `armeabi-v7a`, `x86_64`).
- `chordplay-v1.8.5-build31-devDebug-arm64.apk`: Optimized, lightweight build for modern 64-bit ARM devices.

---

# ChordPlay v1.8.4

Welcome to ChordPlay v1.8.4! This release implements **Phase 4: MiniPlayer & Interaction Superpowers** of the ChordPlay Evolution Master Plan, introducing horizontal swipe gestures, perimeter progress ring animations, and rich haptic interaction feedback across transport controls and navigation.

### What's New in v1.8.4:
- **MiniPlayer Horizontal Swipe-to-Skip**: Smooth swipe-to-skip gestures directly on the floating MiniPlayer and Glass Nav Bar (drag left for next track, drag right for previous track) with responsive spring physics.
- **Dynamic Artwork Translation**: Album art translations dynamically track horizontal gesture offset during swipes with natural resistance and bounce-back physics.
- **Perimeter Progress Glow Ring**: Clockwise glowing perimeter path tracing the capsule boundary proportional to audio playback progress, themed dynamically with artwork palette accents and fallback to primary theme color.
- **Micro-Haptic Feedback**: Subtle, tactile `CLOCK_TICK` haptics on playback transport controls (Play/Pause, Skip), bottom navigation tab switching, and scrubber dragging.
- **Strict Backward Compatibility**: Selecting **Classic BitChord** style cleanly disables gesture offsets and glowing border rings, reverting immediately to the lightweight classic pill.

### Download Assets:
- `chordplay-v1.8.4-build30-devDebug-universal.apk` (and alias `chordplay-v1.8.4-build30-devDebug.apk`): Universal build for all Android ABIs (`arm64-v8a`, `armeabi-v7a`, `x86_64`).
- `chordplay-v1.8.4-build30-devDebug-arm64.apk`: Lightweight, optimized build for modern 64-bit ARM devices.

---

# ChordPlay v1.8.3

Welcome to ChordPlay v1.8.3! This release implements **Phase 2: The Player Screen Elevation** of the ChordPlay Evolution Master Plan, introducing an atmospheric audio stage with breathing ambient cover auras, frosted-glass capsule controls, and an instant quick action bar.

### What's New in v1.8.3:
- **Atmospheric Breathing Artwork Aura (`ChordPlayAmbientCover`)**: Gently blurred, rhythmic ambient pulse behind album artwork synchronized to playback cadence. Settles smoothly when paused.
- **Frosted Glass Capsule Transport Deck (`ChordPlayTransportDeck`)**: Tactile floating pill deck encasing Previous, elevated luminous Play/Pause (with drop shadow & press-scale physics), and Next controls.
- **Audio Fidelity Badge & Quick Action Row**: Live stream codec & resolution badge (e.g. `HI-RES LOSSLESS`, `LOSSLESS FLAC`, `256K OPUS`) with 1-tap deep link to audio pipeline specs, alongside 1-tap "Add to Playlist" and "More Options".
- **Player Layout & Aura Settings (`PlayerSection`)**: Choose between **Modern Ambient** and **Classic Clean** layouts, with an independent toggle for the breathing aura effect.
- **Live Interactive Player Preview**: Player settings card dynamically reflects layout mode, squircle corner curvature, and breathing aura pulses in real time.
- **100% Backward Compatibility Guard**: Selecting **Classic Clean** or **Classic BitChord** style immediately renders the original BitChord player layout untouched.

### Download Assets:
- `chordplay-v1.8.3-build29-devDebug-universal.apk` (and alias `chordplay-v1.8.3-build29-devDebug.apk`): Universal build for all Android ABIs (`arm64-v8a`, `armeabi-v7a`, `x86_64`).
- `chordplay-v1.8.3-build29-devDebug-arm64.apk`: Optimized for modern 64-bit devices.

---

# ChordPlay v1.8.2

Welcome to ChordPlay v1.8.2! This release introduces a comprehensive modular settings architecture, interactive live preview cards, user-customizable lyrics typography, out-of-the-box Last.fm scrobbling, and fast playlist search ergonomics.

### What's New:
- **Modular Settings Architecture**: Reorganized into clean sub-sections (Appearance & Look, Player & Artwork, Lyrics & Typography, Audio Quality & Engine, Storage & Data, Accounts & Integrations, About & Updates).
- **Live Interactive Preview Cards**: Pinned at the top of Appearance, Lyrics, and Player settings, updating in real time as you adjust themes, font styles, and layouts.
- **Custom Lyrics Typography**: Choose from built-in fonts (Poppins, Parkinsans, Serif, Sans, Monospace, Cursive) or upload custom TTF/OTF font files directly from device storage.
- **Full Last.fm Integration**: Native Last.fm scrobbling configured with direct authentication and scrobble tracking.
- **Instant Playlist Search & Caching**: Search through all your playlists with a dedicated search bar in the "Add to Playlist" sheet.
- **Two-Way Playlist Toggle**: Retap an existing playlist to uncheck and mark it for removal ("Will remove"), retap an unchecked playlist to mark for addition ("Will add"), with batch execution on clicking **Done**.
- **ChordPlay Playlist Badges**: Clear visual indicators distinguishing ChordPlay-created playlists from standard cloud playlists.
- **Smoother Navigation Transitions**: High-friction spring physics replaced with silky cubic-bezier easing curves.
- **Updated Branding**: Scaled vector logos across home header, status cards, and notification media styles.

### Download Assets:
- `chordplay-v1.8.2-build28-devDebug-universal.apk`: Universal APK compatible with all Android devices (`arm64-v8a`, `armeabi-v7a`, `x86_64`).
- `chordplay-v1.8.2-build28-devDebug-arm64.apk`: Lightweight, optimized build for modern 64-bit ARM smartphones.
