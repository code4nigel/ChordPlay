# ChordPlay Evolution & Design Revolution Master Plan

> **Core Philosophy**: *Never Copy, Always Elevate.*  
> Transform ChordPlay into an atmospheric, high-end music streaming experience with its own visual soul, tactile audio player, and dynamic discovery hub, while guaranteeing **100% backward compatibility** to the original BitChord design via a single setting toggle.

---

## Architecture Principles & Backward Compatibility Guarantee

### The "Classic BitChord" Fallback Requirement
ChordPlay will introduce rich visual identity enhancements, but users must always retain the option to revert to the exact, original BitChord / Apple Music monochrome aesthetic at any time.

This is governed by a core setting in `AppSettings`:

```kotlin
enum class AppDesignStyle(val label: String, val detail: String) {
    CHORDPLAY("ChordPlay Modern", "Atmospheric glowing glass, vibrant palettes, tactile controls, and dynamic discovery"),
    CLASSIC_BITCHORD("Classic BitChord", "Original Apple Music-inspired monochrome dark UI, standard player, and default shelves")
}
```

When `AppDesignStyle.CLASSIC_BITCHORD` is selected:
1. **Theme**: Falls back to pure black (`#000000`), `#0D0D0F` dark surfaces, and `#FA2D48` Apple red accent.
2. **Typography**: Falls back to SF Pro Display with tight negative tracking.
3. **Player**: Renders the original BitChord square cover art and standard transport row.
4. **Home Screen**: Renders standard horizontal shelves without the mood chips or hero banner.
5. **MiniPlayer**: Renders original static pill without swipe-to-skip or perimeter glow.

---

# Phase 1: Theme Engine & Design System

### 🎯 Objective
Replace the hard-coded monochrome Apple palette with a dynamic, curated atmospheric theme engine, while allowing 1-tap switching between ChordPlay themes and Classic BitChord.

### 📂 Key Files to Modify & Create
- `app/src/main/java/com/music/bitchord/data/settings/AppSettings.kt`
- `app/src/main/java/com/music/bitchord/ui/theme/Theme.kt`
- `app/src/main/java/com/music/bitchord/ui/theme/ChordPlayTheme.kt` (New)
- `app/src/main/java/com/music/bitchord/ui/screens/settings/sections/AppearanceSection.kt`

### 🛠️ Detailed Specifications

#### 1. Settings State in `AppSettings.kt`
Add the following persistent settings:
```kotlin
val appDesignStyle = MutableStateFlow(AppDesignStyle.CHORDPLAY)
val colorPalette = MutableStateFlow(ChordPlayPalette.ELECTRIC_VIOLET)
val appFontFamilyChoice = MutableStateFlow(AppFontChoice.SYSTEM_DEFAULT)
```

Where `ChordPlayPalette` defines curated atmospheric themes:
- **`ELECTRIC_VIOLET` (Signature Default)**:
  - Background: `#0B0C10` (Deep obsidian night)
  - Surface: `#141622` (Translucent navy-violet glass)
  - Surface Variant: `#1E2235`
  - Primary Accent: `#7C4DFF` / `#6C5CE7` (Luminous violet)
  - Secondary Glow: `#00D2D3` (Neon cyan touch)
- **`SUNSET_AMBER`**:
  - Background: `#0E0B09` (Warm roasted chocolate)
  - Surface: `#1A1410`
  - Primary Accent: `#FF9F43` (Molten honey/amber)
- **`EMERALD_NOIR`**:
  - Background: `#08100C` (Midnight forest)
  - Surface: `#0F1D16`
  - Primary Accent: `#00E676` (Mint/emerald neon)
- **`DYNAMIC_ARTWORK`**:
  - Dynamically samples vibrant and dominant colors from the currently playing track's artwork palette (`ArtworkPalette.kt`).

#### 2. Composable Theme Wrapper in `Theme.kt`
```kotlin
@Composable
fun ChordPlayAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val designStyle by AppSettings.appDesignStyle.collectAsStateWithLifecycle()
    val palette by AppSettings.colorPalette.collectAsStateWithLifecycle()
    
    if (designStyle == AppDesignStyle.CLASSIC_BITCHORD) {
        // Untouched legacy BitChord theme
        BitChordTheme(darkTheme = darkTheme, content = content)
    } else {
        // ChordPlay Modern Theme with luminous surfaces and selected palette
        ChordPlayMaterialTheme(darkTheme = darkTheme, palette = palette, content = content)
    }
}
```

#### 3. Appearance Settings UI in `AppearanceSection.kt`
- **Style Selector Card**: Segmented control or preview cards choosing between **ChordPlay Modern** and **Classic BitChord**.
- **Palette Picker**: A carousel of vibrant color circles with live checkmarks (Violet, Amber, Emerald, Cyber, Dynamic Monet).
- **Live Preview Card**: Pinned at the top of the screen showing an animated mini-card that reflects the chosen theme, surface borders, and colors in real-time.

---

# Phase 2: The Player Screen Elevation

### 🎯 Objective
Turn the Now Playing screen from a plain Apple Music replica into an iconic, atmospheric audio stage with breathing ambient auras, a frosted pill transport deck, tactile scrubbing, and quick actions.

### 📂 Key Files to Modify & Create
- `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/NowPlayingScreen.kt`
- `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/PlayerControls.kt`
- `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/ChordPlayAmbientCover.kt` (New)
- `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/ChordPlayTransportDeck.kt` (New)
- `app/src/main/java/com/music/bitchord/data/settings/AppSettings.kt` (`playerLayoutMode`)

### 🛠️ Detailed Specifications

#### 1. Settings State in `AppSettings.kt`
```kotlin
enum class PlayerLayoutMode(val label: String, val detail: String) {
    MODERN_AMBIENT("Modern Ambient", "Floating cover with breathing aura, frosted capsule deck, and quick actions"),
    VINYL_RECORD("Vinyl Turntable", "Spinning vinyl record sliding out from sleeve with moving tonearm"),
    CLASSIC_CLEAN("Classic BitChord", "Original Apple Music-style player layout")
}

val playerLayoutMode = MutableStateFlow(PlayerLayoutMode.MODERN_AMBIENT)
val enableBreathingAura = MutableStateFlow(true)
```

#### 2. Breathing Artwork Aura (`ChordPlayAmbientCover.kt`)
- An elevated squircle album cover (`RoundedCornerShape(24.dp)`).
- Behind the artwork, render a softly blurred (`blur(40.dp)`), scaled (`1.08f`) duplicate of the artwork or canvas.
- When playing, gently animate scale (`1.05f` to `1.10f`) and opacity with an infinite pulse tween (`3000ms`, `FastOutSlowInEasing`) to simulate a "breathing" audio aura. When paused, smoothly settle to `1.0f`.

#### 3. Pill-Shaped Frosted Control Deck (`ChordPlayTransportDeck.kt`)
- Instead of bare buttons floating on the page:
  - Encase Previous, Play/Pause, and Next in a floating frosted glass capsule (`Modifier.hazeChild(hazeState, shape = CircleShape)`).
  - Give the central Play/Pause button a luminous circular background (`Primary` color with soft drop shadow `shadow(12.dp, spotColor = primaryColor)`).
  - Add smooth scale feedback on touch (`animateFloatAsState` pressing down to `0.92f`).

#### 4. Quick Action Bar on Player
Add a discreet, elegant row right below the scrub bar:
- **Audio Fidelity Badge**: e.g., `[ 256k OPUS ]` or `[ LOSSLESS FLAC ]` with subtle outline.
- **Sleep Timer Quick Toggle**: Tap to set 15m / 30m / 1h / End of Track.
- **Direct Add to Playlist Button**: Direct `Icons.Rounded.PlaylistAdd` icon button that triggers `PlaylistPickerSheet` in one tap.

#### 5. Backward Compatibility Guard
Inside `NowPlayingScreen.kt`:
```kotlin
val designStyle by AppSettings.appDesignStyle.collectAsStateWithLifecycle()
val playerMode by AppSettings.playerLayoutMode.collectAsStateWithLifecycle()

if (designStyle == AppDesignStyle.CLASSIC_BITCHORD || playerMode == PlayerLayoutMode.CLASSIC_CLEAN) {
    // Untouched legacy BitChord player layout
    LegacyBitChordPlayerContent(...)
} else {
    // ChordPlay Modern Elevated Player
    ChordPlayModernPlayerContent(...)
}
```

---

# Phase 3: The Living Home Screen

### 🎯 Objective
Transform the Home screen from a static vertical list of horizontal shelves into an interactive, personalized discovery center.

### 📂 Key Files to Modify & Create
- `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/screens/HomeScreen.kt`
- `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/components/HomeHeroBanner.kt` (New)
- `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/components/MoodFilterChips.kt` (New)
- `app/src/main/java/com/music/bitchord/data/settings/AppSettings.kt` (`homeStyle`)

### 🛠️ Detailed Specifications

#### 1. Dynamic Greeting & Header
- Calculate time of day on device:
  - 05:00 – 11:59: *"Good morning, [User]"*
  - 12:00 – 16:59: *"Good afternoon, [User]"*
  - 17:00 – 21:59: *"Good evening, [User]"*
  - 22:00 – 04:59: *"Late Night Vibes"*
- Display account name if logged into YouTube Music, or personalized `"ChordPlay"` listener title.

#### 2. Quick Mood & Vibe Chips (`MoodFilterChips.kt`)
A horizontally scrollable pill row pinned under the header:
`[ ✨ All ]` `[ ☕ Chill ]` `[ ⚡ Energy ]` `[ 🎯 Focus ]` `[ 🏃 Workout ]` `[ 🎉 Party ]` `[ 🌙 Sleep ]`
- Tapping a mood chip filters the active home shelves or queries YouTube Music InnerTube mood endpoints, updating the feed with relevant recommendations.
- Active chip displays glowing primary accent with smooth spring transition.

#### 3. "Jump Back In" Hero Resume Card (`HomeHeroBanner.kt`)
- Displays the most recently played playlist, album, or mix in an elevated glass banner.
- Includes high-res artwork, title, subtitle, animated mini equalizer bars if currently playing, and a circular **1-tap Resume Play button**.

#### 4. Modern Squircle Card Styling
- Increase card corner radii from `8.dp`/`12.dp` to `20.dp`.
- Add subtle border highlight (`1.dp` `Color.White.copy(0.08f)`).
- Add soft tinted ambient shadow matching the album art thumbnail.

#### 5. Backward Compatibility Guard
In `HomeScreen.kt`:
```kotlin
val designStyle by AppSettings.appDesignStyle.collectAsStateWithLifecycle()

if (designStyle == AppDesignStyle.CLASSIC_BITCHORD) {
    // Untouched original BitChord shelves and header
    ClassicBitChordHomeContent(...)
} else {
    // ChordPlay Modern Home with Greeting, Mood Chips & Jump Back In
    ChordPlayLivingHomeContent(...)
}
```

---

# Phase 4: MiniPlayer & Interaction Superpowers

### 🎯 Objective
Make the MiniPlayer and bottom navigation bar tactile, responsive, and ergonomic with swipe-to-skip gestures and perimeter progress glows.

### 📂 Key Files to Modify
- `app/src/main/java/com/music/bitchord/ui/components/MiniPlayer.kt`
- `app/src/main/java/com/music/bitchord/ui/components/GlassNavBar.kt`
- `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/PlayerDock.kt`

### 🛠️ Detailed Specifications

#### 1. Swipe-to-Skip Gestures on MiniPlayer
- Attach a horizontal drag gesture detector (`detectHorizontalDragGestures`) to the MiniPlayer capsule:
  - Dragging left past threshold (`-60.dp`): Triggers haptic tick and skips to **Next Track** (`seekToNextMediaItem()`).
  - Dragging right past threshold (`+60.dp`): Triggers haptic tick and skips to **Previous Track** (`seekToPrevious()`).
  - While dragging, smoothly translate the artwork with spring physics (`graphicsLayer { translationX = dragOffset }`).

#### 2. Perimeter Progress Glow Ring
- Instead of a thin horizontal line at the bottom of the MiniPlayer:
  - Draw a subtle glowing path along the outer pill boundary of the capsule proportional to playback progress (`position / durationMs`).
  - Uses the primary accent color or artwork dominant color.

#### 3. Haptic Tactile Engine
- Add subtle haptic clicks via `LocalView.current.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)` when:
  - Scrubbing past seekbar seconds.
  - Tapping play/pause.
  - Switching bottom navigation tabs.

---

# 🤖 Agent Deployment Prompts

You can hand the following prompts directly to subagents to execute each phase independently:

### Prompt for Agent (Phase 1: Theme Engine)
```text
Task: Implement Phase 1 of the ChordPlay Evolution Plan (Theme Engine & Design System).
Reference Specification: docs/CHORDPLAY_EVOLUTION_MASTER_PLAN.md (Phase 1).
Instructions:
1. In AppSettings.kt, add AppDesignStyle (CHORDPLAY vs CLASSIC_BITCHORD) and ChordPlayPalette enums.
2. Create ChordPlayTheme.kt defining curated palettes: Electric Violet (default), Sunset Amber, Emerald Noir, and Dynamic Monet.
3. Update Theme.kt to wrap BitChordTheme when CLASSIC_BITCHORD is selected, and ChordPlayTheme when CHORDPLAY is selected.
4. Update AppearanceSection.kt to add a Style Switcher card and Palette Carousel with real-time live preview.
5. Ensure zero compilation errors and test with gradle :app:assembleDevDebug.
```

### Prompt for Agent (Phase 2: Player Screen Elevation)
```text
Task: Implement Phase 2 of the ChordPlay Evolution Plan (Player Screen Elevation).
Reference Specification: docs/CHORDPLAY_EVOLUTION_MASTER_PLAN.md (Phase 2).
Instructions:
1. In AppSettings.kt, add PlayerLayoutMode (MODERN_AMBIENT vs CLASSIC_CLEAN).
2. Create ChordPlayAmbientCover.kt implementing the breathing artwork aura with smooth pulse animations during playback.
3. Create ChordPlayTransportDeck.kt implementing the frosted-glass capsule control deck with elevated play/pause button.
4. Add quick action row on player (audio fidelity badge and 1-tap Add to Playlist button).
5. Ensure that if AppDesignStyle == CLASSIC_BITCHORD or PlayerLayoutMode == CLASSIC_CLEAN, the original BitChord player is rendered untouched.
6. Verify build with :app:assembleDevDebug.
```

### Prompt for Agent (Phase 3: The Living Home Screen)
```text
Task: Implement Phase 3 of the ChordPlay Evolution Plan (The Living Home Screen).
Reference Specification: docs/CHORDPLAY_EVOLUTION_MASTER_PLAN.md (Phase 3).
Instructions:
1. In HomeScreen.kt, implement dynamic time-of-day greeting ("Good morning / afternoon / evening / Late Night Vibes").
2. Create MoodFilterChips.kt with quick mood filter pills (Chill, Focus, Workout, Energy, Party) that update home recommendations.
3. Create HomeHeroBanner.kt ("Jump Back In") featuring the last played collection with 1-tap resume.
4. Modernize card geometry to 20.dp rounded squircle cards with subtle ambient glow.
5. Ensure full backward compatibility: if AppDesignStyle == CLASSIC_BITCHORD, render original BitChord shelves.
6. Verify build with :app:assembleDevDebug.
```

### Prompt for Agent (Phase 4: MiniPlayer & Interaction Polish)
```text
Task: Implement Phase 4 of the ChordPlay Evolution Plan (MiniPlayer & Interaction Superpowers).
Reference Specification: docs/CHORDPLAY_EVOLUTION_MASTER_PLAN.md (Phase 4).
Instructions:
1. In MiniPlayer.kt, implement horizontal swipe-to-skip gestures (drag left for next, drag right for previous) with spring animations.
2. Add perimeter progress glow ring tracing the capsule border.
3. Add subtle haptic ticks to playback controls and tab switching.
4. Respect AppDesignStyle toggle for classic behavior fallback.
5. Verify build with :app:assembleDevDebug.
```
