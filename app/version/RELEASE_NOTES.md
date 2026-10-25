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
