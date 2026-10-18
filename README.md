<div align="center">

<br/>
<br/>

<img src="Logo.png" alt="ChordPlay app icon" width="180" />

# ChordPlay

### The Aesthetic, Modern YouTube Music & Lossless Client

<br/>

[![License](https://img.shields.io/github/license/code4nigel/ChordPlay?style=for-the-badge&labelColor=0d1117)](https://github.com/code4nigel/ChordPlay/blob/main/LICENSE)
[![Repository](https://img.shields.io/badge/GitHub-ChordPlay-blue?style=for-the-badge&logo=github&labelColor=0d1117)](https://github.com/code4nigel/ChordPlay)

<br/>

[**Download**](#download) · [**Features**](#features) · [**Building Locally**](#building-locally) · [**Credits & Upstream**](#credits--upstream) · [**Disclaimer**](#disclaimer)

</div>

> [!IMPORTANT]
> ChordPlay is an independent open-source client. It is not affiliated with, endorsed by, or connected to YouTube, Google, or Spotify in any way.

---

<div align="center">

<h1><a id="features"></a>Key Features</h1>

<table>
  <tr>
    <td width="50%" valign="top">

#### 🎵 Playback & Audio
- **Search, browse & stream** everything on YouTube Music with zero ads.
- **Hi-Res Lossless Audio** — FLAC/ALAC playback via pluggable module sources with YouTube Music fallback.
- **True Gapless & Crossfade** — Smooth transition between tracks (0–12s).
- **Automix [Beta]** — Intelligent DJ-style tempo-matching transitions.
- **Offline Downloads** — Save songs with embedded cover art and metadata.
- **Local Music Library** — Unified playback for files stored on your device.

#### ✨ Enhanced ChordPlay Features
- **Custom Lyrics Typography** — Switch between fonts (Default, **Poppins**, **Parkinsans**, Serif, Sans, Monospace, Cursive) or **Upload your own custom font file** (`.ttf` / `.otf`) from storage.
- **Full Lyrics Scaling & Alignment** — Custom scale slider, font weight selection, and left/center alignment options.
- **Quick Playlist Manager** — Add tracks to playlists directly from the Now Playing screen with intuitive checkmark badges showing where the song already exists.
- **Redesigned Modern Identity** — Refined branding and icons crafted for a sleek, cohesive music experience.

    </td>
    <td width="50%" valign="top">

#### 📜 Visuals & Experience
- **Apple-Style Word-Synced Lyrics** — Silky smooth line animations, syllable-level highlighting, and Romanization support.
- **Dynamic Frosted-Glass UI** — Translucent backgrounds via Haze, fluid spring animations, and Material 3 artwork-driven theming.
- **Animated Motion Artwork** — Fullscreen video canvas for supported tracks.
- **Broad Lyrics Sources** — Integrated with LRCLIB, Musixmatch, Genius, KuGou, Megalobiz, SimpMusic, and more.

#### 🔗 Accounts & Integrations
- **Google Account Sync** — Access your personalized library, mixes, and subscriptions.
- **Spotify Library Import** — Connect your account to play your Spotify playlists and Liked Songs.
- **Discord Rich Presence** — Real-time song title, artist, and playback progress on Discord.
- **Scrobbling** — Native integration with Last.fm and ListenBrainz.

    </td>
  </tr>
</table>

</div>

---

<div align="center">

<h1><a id="download"></a>Download</h1>

Grab the pre-built APK directly from GitHub:

1. Go to the [**ChordPlay Actions Builds**](https://github.com/code4nigel/ChordPlay/actions).
2. Click on the latest workflow run.
3. Under **Artifacts**, download `chordplay-dev-debug-apk`.
4. Unzip the file and install `app-dev-debug.apk` on your Android device (ensure *"Install unknown apps"* permission is granted).

</div>

---

<div align="center">

<h1><a id="building-locally"></a>Building Locally</h1>

</div>

To build the Android APK directly on your own computer without using GitHub Actions:

### Prerequisites
- **JDK 17 or 21** (Adoptium Temurin or Microsoft OpenJDK). *Note: Android Gradle Plugin 8.8 does not yet support Java 25.*
  ```powershell
  winget install EclipseAdoptium.Temurin.21.JDK
  ```
- **CMake** (required for native audio analysis library):
  ```powershell
  winget install Kitware.CMake
  ```
- **Android SDK & NDK** (API 35, NDK 27.x).

### Build Command
Run the following in the repository root:
```powershell
.\gradlew.bat :app:assembleDevDebug
```
The compiled APK will be located at:
`app/build/outputs/apk/dev/debug/app-dev-debug.apk`

---

<div align="center">

<h1><a id="credits--upstream"></a>Credits & Upstream</h1>

ChordPlay is proudly built upon the open-source foundation of [**BitChord**](https://github.com/kushagrasinghx/BitChord) created by Kushagra Singh and its community contributors.

Special thanks to:
- [am-lyrics](https://github.com/binimum/am-lyrics) for the fluid lyrics animation concepts.
- [Haze](https://github.com/chrisbanes/haze) for frosted-glass blurs.
- All upstream contributors and open-source audio library developers.

</div>

---

<div align="center">

<h1><a id="disclaimer"></a>Disclaimer & License</h1>

This project is licensed under the **GNU General Public License v3.0 (GPLv3)**. See the [LICENSE](LICENSE) file for complete details.

ChordPlay is an independent, non-commercial software player for personal research and educational use. It does not host or distribute copyrighted media files.

</div>
