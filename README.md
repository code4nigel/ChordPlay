<div align="center">

<img src="Logo.png" alt="ChordPlay Logo" width="160" />

# ChordPlay

**An aesthetic, tactile YouTube Music client for Android with living player stages, dynamic artwork aura theming, and zero-lag playlist curation.**

<br/>

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg?style=flat-square)](LICENSE)
[![Latest Release](https://img.shields.io/github/v/release/code4nigel/ChordPlay?style=flat-square&label=Release&color=7C4DFF)](https://github.com/code4nigel/ChordPlay/releases)
[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat-square)](https://github.com/code4nigel/ChordPlay/releases)
[![Kotlin Multiplatform](https://img.shields.io/badge/Architecture-Kotlin%20Compose-7F52FF.svg?style=flat-square)](https://kotlinlang.org/)

<br/>

[Overview](#overview) • [Key Features](#key-features) • [Design System](#design-system--aesthetics) • [Downloads](#downloads) • [Lineage & Attribution](#lineage--attribution) • [Building](#building-from-source) • [License](#license)

</div>

---

## Overview

ChordPlay is a modern, high-performance music client designed for listeners who care about both audio quality and visual craft. Engineered with Jetpack Compose and Kotlin Multiplatform, ChordPlay combines the extensive streaming catalog of YouTube Music with a fluid, tactile interface inspired by modern glassmorphic design systems.

Rather than offering a static playback screen or generic list views, ChordPlay introduces living audio stages, dynamic album-art aura lighting that breathes with the track, authentic vinyl turntable layouts, intelligent content-aware speed dials, and a robust playlist management system built to handle large libraries without UI freezes.

ChordPlay is built upon the open-source foundation of [BitChord](https://github.com/kushagrasinghx/BitChord) by Kushagra Singh, taking its proven streaming and audio architecture and transforming the user experience with custom-crafted UI layers, interactive live settings previews, dynamic theming engines, and refined motion ergonomics.

---

## Key Features

### 1. Living Audio Stages (Player Layouts)
Switch effortlessly between distinct listening environments tailored to your mood:
- **Modern Ambient**: A layered stage featuring an atmospheric breathing aura that pulses gently behind the album artwork in sync with playback. Floats a frosted-glass capsule transport deck above the canvas, complete with drop-shadow depth, real-time audio quality badges (`140K OPUS`, `256K AAC`), and an inline lyrics teaser.
- **Vinyl Turntable**: A tactile listening layout showcasing a realistic spinning vinyl record with micro-textured grooves, spinning center label artwork, and an animated tone-arm pickup that lowers onto the vinyl when playing and lifts when paused.
- **Classic Clean**: A streamlined, lightweight layout for minimalists, retaining the original BitChord playback view with a single toggle.

### 2. Intelligent Home Speed Dial
Quickly access your frequent listens with an ergonomic, 6-tile paginated grid that visually differentiates content types:
- **Artists**: Rendered in circular avatar frames with gold badge indicators and clean artist titles.
- **Albums**: Displayed in dual-layered vinyl sleeve presentations with disc peeks and album format badges.
- **Playlists**: Styled with multi-layered stacked card shadows, playlist badges, and track counts.
- **Songs**: Presented in rounded squircles with audio track badges.
- **Subtitle Sanitization**: Automatically strips redundant prefixes (e.g. "Artist • ", "Album • ", "Playlist • ") for a cleaner aesthetic.
- **Instant Reactive Pinning**: Direct reactive Flow binding ensures action sheet toggles between "Pin to Speed Dial" and "Unpin from Speed Dial" reflect in real time without lag.

### 3. Living Home Experience & Contextual Discovery
- **Context-Aware Greetings**: Home screen dynamically greets you with rotating witty messages ("Back in the mix", "Ears first, questions later", "Your soundtrack awaits", "Turn it up") personalized with your profile name.
- **Lucky Radio Card**: One-tap serendipitous music discovery that generates instant streaming mixes based on listening vibe.
- **Jump Back In**: Dedicated carousel resuming previous listening sessions right where you left off.

### 4. Dynamic Theming Suite
- **Dynamic Song Art (True Dynamic Theme)**: Extracts dominant colors and glowing accent tones directly from the current song's album art in real time, adapting the app's background, navigation bar, and controls as tracks change.
- **ChordPlay Crimson**: Iconic signature dark theme tailored to the ChordPlay brand identity (deep obsidian base, vivid crimson ruby accents, and neon highlights).
- **Dynamic Monet (Material You)**: System-synced Material 3 dynamic color scheme matching your Android device wallpaper.
- **OLED Pure Black**: True `#000000` AMOLED mode delivering infinite contrast and battery efficiency.
- **Liquid Glass Navigation**: Frosted glass bottom bar with smooth pill indicators and micro-haptic responses.

### 5. Tactile Micro-Interactions & Gestures
- **MiniPlayer Horizontal Swipe-to-Skip**: Swipe left on the MiniPlayer to skip to the next track, or swipe right to play the previous track, complete with translated album art and spring physics.
- **Perimeter Progress Glow Ring**: An optional glowing path tracing the capsule border in real-time proportional to playback progress, themed dynamically with artwork accents.
- **Micro-Haptic Feedback**: Subtle `CLOCK_TICK` haptic feedback on play/pause, skips, tab switches, and scrubber drags.

### 6. Zero-Lag, Persistent Playlist Curation
- **Height-Locked Bottom Sheet**: Engineered bottom sheet state that stays fixed at user-dragged or full height even when opening the on-screen keyboard or typing in search filters.
- **Two-Way Batch Toggle**: Tap to queue additions ("Will add"), tap again to mark for removal ("Will remove"), and commit safely only upon tapping "Done".
- **Zero Parallel Network Freezes**: Replaced brute-force network spam with fast local caching and optimistic ViewModel resolution, completely eliminating GC freezes and ANR crashes.
- **Source Differentiation**: Visual badges clearly distinguishing local ChordPlay playlists from synced YouTube Music cloud playlists.

### 7. Modular Settings with Live Previews
- **Domain-Focused Settings Architecture**: Organized into clean sub-sections (Appearance, Player, Lyrics, Audio Quality, Storage, Accounts, and About).
- **True-to-Life Interactive Previews**: Pinned live preview cards in Appearance and Player settings that faithfully reflect real in-app screens (blurred aura, capsule deck, mini-player, and lyrics snippet) before committing changes.
- **Custom Lyrics Typography**: Built-in fonts (Poppins, Parkinsans, Serif, Sans, Monospace, Cursive) plus support for importing custom `.ttf` and `.otf` font files from device storage.

### 8. Audiophile-Grade Playback & Offline Streaming
- **High-Bitrate Audio**: Direct streaming of high-resolution Opus (140k/160k) and AAC (256k) formats extracted via NewPipeExtractor.
- **Gapless Playback & Crossfade**: Smooth transitions between tracks with configurable crossfade durations.
- **Synchronized Lyrics**: Syllable-level and line-by-line animated lyrics across LRCLIB, Musixmatch, Genius, and KuGou, with support for exportable lyrics share cards.
- **Offline Caching & Downloads**: Save tracks locally with embedded high-resolution artwork and complete ID3 metadata.
- **Native Last.fm Scrobbling**: Integrated scrobbler tracking listens directly to your Last.fm account.

---

## Design System & Aesthetics

ChordPlay is built on the **ChordPlay Design System**, adhering to strict aesthetic tokens:

| Token / Layer | Implementation | Purpose |
| :--- | :--- | :--- |
| **Canvas (L0)** | Fullscreen blurred artwork aura + mesh gradient | Living ambient atmosphere that shifts with track art |
| **Base Scrim (L1)** | Deep background (`#0D0E12` or OLED `#000000`) | High-contrast foundation ensuring WCAG AA legibility |
| **Frosted Cards (L2)** | Translucent panels with Haze blur and 1dp border stroke | Structural separation without visual heaviness |
| **Floating Deck (L3)** | Floating pill capsule with haptic transport controls | Ergonomic one-handed thumb reachability |
| **Modal Sheets (L4)** | Full-height sheets with keyboard-resilient layout | Smooth drag dismiss and stable in-sheet search |

---

## Downloads

Download the latest pre-compiled APK from the [Releases](https://github.com/code4nigel/ChordPlay/releases) page:

| Package | Architecture | Description |
| :--- | :--- | :--- |
| **Universal APK** (Recommended) | All ABIs (`arm64-v8a`, `armeabi-v7a`, `x86_64`) | Compatible with all modern and legacy Android devices |
| **ARM64-v8a APK** | `arm64-v8a` | Optimized, lightweight package for modern 64-bit Android smartphones |

### Automatic In-App Updates
ChordPlay contains a built-in update detector. When a new release is published on GitHub, the app alerts you with the update changelog and provides one-tap in-app download and installation.

---

## Lineage & Attribution

ChordPlay is an open-source project built with deep gratitude toward the developers who make independent Android music software possible.

| Project | Author | Role in ChordPlay |
| :--- | :--- | :--- |
| [BitChord](https://github.com/kushagrasinghx/BitChord) | Kushagra Singh ([@kushagrasinghx](https://github.com/kushagrasinghx)) | Foundational audio service, YouTube Music stream extraction, download pipeline, and baseline architecture |
| [Prism](https://github.com/Yigder/Prism) | Yigder ([@Yigder](https://github.com/Yigder)) | Inspiration for modular settings and interactive live preview cards |
| [Metrolist](https://github.com/metrolistgroup/metrolist) | Metrolist Group | Inspiration for local playlist caching and indexing |
| [Haze](https://github.com/chrisbanes/haze) | Chris Banes | High-performance blur and frosted glass rendering for Compose |
| [am-lyrics](https://github.com/binimum/am-lyrics) | binimum | Synchronized lyrics animation concepts |
| [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) | Team NewPipe | YouTube stream extraction engine |

---

## Building from Source

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17 or higher
- Android SDK 35 (compileSdk 35, minSdk 26)

### Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/code4nigel/ChordPlay.git
   cd ChordPlay
   ```
2. Build the debug APK:
   ```bash
   ./gradlew assembleDevDebug
   ```
3. Locate the built APKs:
   - Universal: `app/build/outputs/apk/dev/debug/app-dev-universal-debug.apk`
   - ARM64: `app/build/outputs/apk/dev/debug/app-dev-arm64-v8a-debug.apk`

---

## License

ChordPlay is licensed under the **GNU General Public License v3.0 (GPLv3)**.

- **BitChord**: Copyright (C) 2024-2026 Kushagra Singh and BitChord contributors.
- **ChordPlay modifications & additions**: Copyright (C) 2026 Shivanshu (code4nigel).

See the [LICENSE](LICENSE) file for the full text of the GNU General Public License v3.0.
