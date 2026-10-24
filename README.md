<div align="center">

<br/>
<br/>

<img src="Logo.png" alt="ChordPlay app icon" width="180" />

# ChordPlay

### The Aesthetic, Modern YouTube Music & Lossless Audio Client

<br/>

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg?style=for-the-badge&labelColor=0d1117)](LICENSE)
[![Latest Release](https://img.shields.io/github/v/release/code4nigel/ChordPlay?style=for-the-badge&label=Latest%20Release&color=7C4DFF&labelColor=0d1117)](https://github.com/code4nigel/ChordPlay/releases)
[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white&labelColor=0d1117)](https://github.com/code4nigel/ChordPlay/releases)
[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white&labelColor=0d1117)](https://kotlinlang.org/)

<br/>

[**Download APK**](#download) · [**Key Features**](#features) · [**Building Locally**](#building-locally) · [**Credits & Upstream**](#credits--upstream) · [**License**](#license)

</div>

> [!NOTE]
> **Lineage & Upstream Gratitude**: ChordPlay is built upon the wonderful foundation of [**BitChord**](https://github.com/kushagrasinghx/BitChord) created by **Kushagra Singh** ([@kushagrasinghx](https://github.com/kushagrasinghx)). We are deeply grateful to Kushagra and the BitChord open-source community for their exceptional work licensed under GNU GPLv3, which made this evolution, design refinement, and ongoing learning possible.

> [!IMPORTANT]
> **ChordPlay** is an independent, non-commercial open-source music player developed for personal research, educational study, and audio enjoyment. It is not affiliated with, endorsed by, or sponsored by Google, YouTube, or Spotify.

---

<div align="center">

<h1><a id="features"></a>Key Features</h1>

<table>
  <tr>
    <td width="50%" valign="top">

#### 🎵 Playback & High-Res Audio
- **Ad-Free Streaming** — Stream and search millions of tracks from YouTube Music with zero advertisements.
- **Hi-Res Lossless Audio** — Pluggable FLAC/ALAC playback with seamless YouTube Music fallback.
- **True Gapless & Crossfade** — Smooth transitions between tracks (configurable 0–12s).
- **Automix [Beta]** — Intelligent DJ-style tempo-matching transitions between songs.
- **Offline Downloads** — Save tracks with embedded high-resolution artwork and metadata.
- **Local Music Library** — Unified playback for local audio files stored on your device.

#### ✨ Standout ChordPlay Innovations
- **Modular Settings & Live Previews** — Inspired by Prism, visual settings (Lyrics typography, Theme, Player layout) feature **live interactive preview cards pinned at the top** that update in real time as you adjust controls.
- **Custom Lyrics Typography** — Switch between bundled typefaces (**Default**, **Poppins**, **Parkinsans**, Serif, Sans, Monospace, Cursive) or **Upload your own custom font file** (`.ttf` / `.otf`) from storage.
- **Fast Playlist Search & Sync** — Search your playlists instantly from the "Add to Playlist" sheet with parallel lookup and optimistic caching.
- **ChordPlay Identity Badges** — Clear visual badges distinguishing ChordPlay-created playlists from standard cloud playlists.

    </td>
    <td width="50%" valign="top">

#### 📜 Visuals & Aesthetic Depth
- **Apple-Style Word-Synced Lyrics** — Silky smooth line animations, syllable-level highlighting, and Romanization support.
- **Dynamic Frosted-Glass UI** — Translucent backgrounds powered by `Haze`, physics-based spring animations, and Material 3 artwork-adaptive palettes.
- **Animated Motion Artwork** — Fullscreen video canvas for supported songs.
- **Broad Lyrics Coverage** — Multi-source fallback across LRCLIB, Musixmatch, Genius, KuGou, Megalobiz, SimpMusic, and more.

#### 🔗 Accounts & Integrations
- **Google Account Sync** — Access your personalized YouTube Music library, mixes, and subscriptions.
- **Spotify Library Import** — Connect and stream your Spotify playlists and liked tracks.
- **Last.fm & ListenBrainz** — Full scrobbling integration out-of-the-box with custom API credentials support.
- **Discord Rich Presence** — Real-time song title, artist, and playback progress status on Discord.

    </td>
  </tr>
</table>

</div>

---

<div align="center">

<h1><a id="download"></a>Download</h1>

</div>

### Grab the Latest APK Directly:
Pre-compiled APKs are published with every release on GitHub:

1. Visit [**ChordPlay Releases**](https://github.com/code4nigel/ChordPlay/releases).
2. Download the latest APK for your device architecture:
   - **`universal`**: Compatible with all modern Android devices (`arm64-v8a`, `armeabi-v7a`, `x86_64`).
   - **`arm64-v8a`**: Optimized, lightweight build for modern 64-bit phones.
3. Open the downloaded file and install (ensure *"Install unknown apps"* permission is granted).

> [!TIP]
> ChordPlay includes an **in-app update checker**. Once installed, the app will automatically notify you whenever a newer version is published so you can update directly in one tap.

---

<div align="center">

<h1><a id="building-locally"></a>Building Locally</h1>

</div>

To compile ChordPlay from source on your own machine:

### Prerequisites
- **JDK 17 or 21** (Eclipse Adoptium Temurin or Microsoft OpenJDK):
  ```powershell
  winget install EclipseAdoptium.Temurin.21.JDK
  ```
- **CMake** (required for native audio analysis library):
  ```powershell
  winget install Kitware.CMake
  ```
- **Android SDK & NDK** (API 35/37, NDK 27.x).

### Build Commands
Run from the repository root:
```powershell
# Build Development Debug APK
.\gradlew.bat :app:assembleDevDebug

# Or use the automated build script (with version stamping and multi-ABI outputs):
.\build-apk.ps1 -Variant devDebug
```
The output APKs will be generated in `app/build/outputs/apk/dev/debug/` and archived in `version/Android/`.

---

<div align="center">

<h1><a id="credits--upstream"></a>Credits & Upstream</h1>

</div>

ChordPlay is an educational, community-driven project created to explore modern mobile design, audio engineering, and music client architecture.

We extend our sincere gratitude and respect to:

- [**BitChord**](https://github.com/kushagrasinghx/BitChord) created by **Kushagra Singh** ([@kushagrasinghx](https://github.com/kushagrasinghx)) and its open-source contributors. BitChord provided the foundational architecture, fluid UI concepts, and core playback framework upon which ChordPlay is built and evolved.
- [**Prism**](https://github.com/Yigder/Prism) by **Yigder** for inspiring our modular settings hierarchy, live preview cards, and audio customization philosophy.
- [**Metrolist**](https://github.com/metrolistgroup/metrolist) for ideas around fast local search caching and clean queue ergonomics.
- [**Haze**](https://github.com/chrisbanes/haze) by Chris Banes for frosted-glass blur rendering in Jetpack Compose.
- [**am-lyrics**](https://github.com/binimum/am-lyrics) for fluid word-synced lyrics animation inspiration.
- [**NewPipeExtractor**](https://github.com/TeamNewPipe/NewPipeExtractor) for YouTube stream parsing.

---

<div align="center">

<h1><a id="license"></a>License & Copyright</h1>

</div>

**ChordPlay** is authored and maintained by **Shivanshu** ([@code4nigel](https://github.com/code4nigel)).

```text
ChordPlay
Copyright (C) 2026 Shivanshu (code4nigel) <https://github.com/code4nigel>
Portions Copyright (C) 2024-2026 Kushagra Singh and BitChord contributors <https://github.com/kushagrasinghx/BitChord>
```

This program is free software: you can redistribute it and/or modify it under the terms of the **GNU General Public License v3.0 (GPLv3)** as published by the Free Software Foundation. See the [LICENSE](LICENSE) file for complete details.

<div align="center">
<sub>Crafted with passion by Shivanshu · Made for music lovers.</sub>
</div>
