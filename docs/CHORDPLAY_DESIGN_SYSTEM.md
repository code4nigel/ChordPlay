# ChordPlay Design System & UI Specification

> **Version**: 1.0  
> **Target Platforms**: Android (Compose) & Desktop (Compose Multiplatform)  
> **Soul**: *Fluid Glassmorphism, Dynamic Artwork Aura, and Modular Tactility.*

ChordPlay blends the fluid, frosted-glass elegance of **BitChord** with the structured, customizable, and preview-driven hierarchy of **Prism**. Every screen, dialog, and component built for ChordPlay must adhere to the principles in this document.

---

## 1. Core Design Philosophy

1. **Depth Through Translucency (Frosted Glass)**  
   - Avoid flat solid panels. Use multi-layer translucent surfaces with `Haze` blur and subtle 1dp border strokes (`Color.White.copy(alpha = 0.08f)`).
   - The UI should feel like layers of frosted glass resting above a living, breathing artwork canvas.

2. **Artwork-Driven Ambient Aura**  
   - The currently playing song's album art dictates the atmosphere.
   - Backgrounds blend artwork colors using mesh gradients or blurred backdrops, tinted to ensure text legibility (WCAG AA contrast).

3. **Physics-Based Fluid Motion**  
   - Every transition, sheet drag, and expand/collapse action must use spring physics rather than linear ease-in/out.
   - Typical spring token:
     ```kotlin
     spring(
         dampingRatio = Spring.DampingRatioMediumBouncy, // 0.75f
         stiffness = Spring.StiffnessLow                // 300f
     )
     ```

4. **Modular & Preview-Driven Customization** *(Inspired by Prism)*  
   - Settings and customization must never be hidden in confusing, 2,000-line monolithic lists.
   - Every customizable component (Lyrics Font, Player Layout, Colors, Mini-Player) should feature:
     - A dedicated, clean sub-page.
     - A **Live Interactive Preview** pinned at the top showing changes in real time before committing.

---

## 2. Color System & Dynamic Palettes

### Base Palette Tokens
- **Background Deep**: `#0D0E12` (OLED Pure Black fallback: `#000000`)
- **Surface Translucent**: `Color(0xFF16181F).copy(alpha = 0.72f)`
- **Surface Frosted**: `MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)`
- **Glass Border Stroke**: `Color.White.copy(alpha = 0.08f)`
- **Accent Primary**: Dynamic artwork vibrant hue, fallback to Electric Purple/Cyan (`#7C4DFF` / `#00E5FF`).

### Surface Hierarchy
| Level | Purpose | Styling / Modifier |
| :--- | :--- | :--- |
| **L0: Canvas** | Wallpaper / Ambient Artwork Mesh | Fullscreen blurred artwork + subtle noise texture |
| **L1: Base Scrim** | Page content background | `Modifier.background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))` |
| **L2: Frosted Cards** | List items, cards, replay items | `Modifier.hazeChild(...).background(surfaceColor).border(1.dp, glassBorder)` |
| **L3: Floating Bar** | Mini-player, Bottom navigation | Floating pill with 28dp rounded corners, haze blur, elevated shadow |
| **L4: Modal Sheets** | Now Playing, Settings, Quick Queue | Full sheet with smooth gesture-dismiss springs and top drag handle |

---

## 3. Typography System

ChordPlay prioritizes expressive typography over default system fonts:

### Font Families
- **Display / Headings**: **Poppins** (SemiBold / Bold) for modern, clean, geometric musical headers.
- **Body & Metadata**: **Parkinsans** or System Sans for warm, readable artist and track metadata.
- **Lyrics Engine**: Multi-font switcher supporting:
  - System Default
  - Poppins
  - Parkinsans
  - Serif
  - Monospace
  - Cursive
  - **Custom User Font** (`.ttf` / `.otf` uploaded by the user from local storage).

### Scale Tokens
```kotlin
// Display (HomeScreen Titles, Replay Highlights)
displayLarge: fontSize = 42.sp, lineHeight = 46.sp, fontWeight = Bold
displayMedium: fontSize = 34.sp, lineHeight = 38.sp, fontWeight = SemiBold

// Titles (Song Titles in Now Playing, Sheet Headers)
titleLarge: fontSize = 22.sp, lineHeight = 28.sp, fontWeight = Bold
titleMedium: fontSize = 17.sp, lineHeight = 22.sp, fontWeight = SemiBold
titleSmall: fontSize = 14.sp, lineHeight = 18.sp, fontWeight = Medium

// Metadata & Body (Artist Names, Descriptions)
bodyMedium: fontSize = 14.sp, lineHeight = 20.sp, fontWeight = Normal
labelSmall: fontSize = 11.sp, lineHeight = 14.sp, fontWeight = Medium, letterSpacing = 0.5.sp
```

---

## 4. Settings & Customization Hierarchy *(The Prism Model)*

Rather than one long continuous settings list, ChordPlay organizes settings into clean, modular categories with dedicated sub-sheets:

```
Settings Hub
 ├── 🎨 Appearance & Looks
 │    ├── Theme (Dark / Light / Pure Black OLED)
 │    ├── Accent & Dynamic Palette
 │    └── Glassmorphism & Blur Intensity (Haze slider)
 ├── 🎵 Player & Visuals
 │    ├── Player Layouts (Standard, Cover-focused, Minimal, Vinyl/Cassette)
 │    ├── Visualizer (Off, Spectrum Bars, Waveform, Pulsing Glow)
 │    └── Controls & Gestures (Double-tap, Swipe-to-skip)
 ├── 📜 Lyrics & Typography
 │    ├── [Live Preview Card Pinned at Top]
 │    ├── Font Family (Poppins, Parkinsans, Custom Font File Loader)
 │    ├── Size & Line-Height Scaling Slider
 │    ├── Alignment (Left / Center)
 │    └── Syllable Sync & Word-by-Word Animation Style
 ├── 🎧 Audio & Playback
 │    ├── Stream Quality & Lossless Sources
 │    ├── Gapless & Crossfade Duration (0-12s)
 │    └── DJ Automix & Tempo Blending
 ├── 📥 Library & Downloads
 │    ├── Offline Storage Path
 │    ├── Embedded Artwork & Metadata Quality
 │    └── Smart Cache Management
 └── ℹ️ About & ChordPlay
      ├── Version, Changelog & Upstream Credits
      └── GitHub, Releases & Contributor Links
```

### Rule for Customization Sub-pages:
> **Every visual sub-page (Lyrics, Player Layout, Theme) MUST include a live preview pinned at the top.** Users should immediately see what changes before closing the sheet.

---

## 5. Component Construction Rules

### A. Now Playing Screen
- Full-bleed artwork background with real-time blur.
- High-contrast controls that respond instantly with haptic feedback.
- Quick Playlist Manager badge on the cover or action row so songs can be added to playlists with a single tap.

### B. Mini-Player
- Floating capsule slightly elevated from the bottom navigation bar.
- Shows track title, artist, progress indicator bar along the bottom edge, and Play/Pause + Next buttons.
- Drag gesture up smoothly transitions into the full Now Playing screen.

### C. Cards & Tile Elements
- Always apply rounded corners: `16.dp` for standard cards, `24.dp` for hero/replay cards.
- Subtle inner shadow or 1dp translucent stroke to separate dark artwork from dark backgrounds.

---

## 6. Implementation Checklist for Agents & Contributors

Before submitting any UI code:
- [ ] Does this component use the design tokens (Poppins/Parkinsans typography, Haze frosted glass, translucent borders)?
- [ ] Are animations physics-driven springs, not linear transitions?
- [ ] If this adds a new user-facing setting, is it placed inside the proper category in the modular Settings hierarchy?
- [ ] Is contrast maintained against both dark and bright album artworks?
- [ ] Are clickable links, icons, and interactive elements sized with comfortable touch targets (minimum 48x48dp)?
