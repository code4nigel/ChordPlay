# Third-Party Notices & Attributions

ChordPlay (`Copyright (C) 2026 Shivanshu (code4nigel)`) is licensed under the **GNU General Public License v3.0 (GPLv3)**. See [LICENSE](LICENSE) for the full GPLv3 text and [LICENSES/](LICENSES/) for third-party license texts included with this repository:

- [LICENSES/Apache-2.0.txt](LICENSES/Apache-2.0.txt) — Apache License, Version 2.0
- [LICENSES/MIT.txt](LICENSES/MIT.txt) — MIT License
- [LICENSES/OFL-1.1.txt](LICENSES/OFL-1.1.txt) — SIL Open Font License, Version 1.1
- [LICENSES/MPL-2.0.txt](LICENSES/MPL-2.0.txt) — Mozilla Public License, Version 2.0
- [LICENSES/BSD-2-Clause.txt](LICENSES/BSD-2-Clause.txt) — BSD 2-Clause License
- [app/src/main/cpp/dsd/dst/COPYING.LGPL-2.1](app/src/main/cpp/dsd/dst/COPYING.LGPL-2.1) — GNU Lesser General Public License, Version 2.1

---

## 1. Upstream Baseline

- **BitChord** (`Copyright (C) 2024-2026 Kushagra Singh and BitChord contributors`)  
  Repository: https://github.com/kushagrasinghx/BitChord  
  License: GNU General Public License v3.0 (GPLv3)  
  Role: Upstream project foundation for audio service, YouTube Music streaming, downloads, and shared multiplatform baseline.

---

## 2. Vendored & Ported Third-Party Code

| Component | Author / Copyright | Source | License | Location in Repo |
| :--- | :--- | :--- | :--- | :--- |
| **backdrop** (`v2.0.0`) | Copyright 2025 Kyant0 | https://github.com/Kyant0/backdrop | Apache-2.0 | `app/src/main/java/com/music/bitchord/ui/components/backdrop/` |
| **compose-floating-tab-bar** (`v1.0.1`) | Elyes Mansour | https://github.com/elyesmansour/compose-floating-tab-bar | Apache-2.0 | `app/src/main/java/com/music/bitchord/ui/components/floatingtabbar/FloatingTabBar.kt` |
| **AndroidX Media3 FFmpeg Decoder** (`1.11.0`) | Copyright (C) 2016 The Android Open Source Project | https://github.com/androidx/media (`libraries/decoder_ffmpeg`) | Apache-2.0 | `app/src/main/java/androidx/media3/decoder/ffmpeg/`, `native/ffmpeg/ffmpeg_jni.cc` |
| **librempeg** (`libffmpegJNI.so`) | librempeg / FFmpeg contributors | https://github.com/librempeg/librempeg (commit `8e8b221e207f685119fcfe276ae28c94ff6f5aa0`, built via `native/ffmpeg/build.sh` with `--enable-gpl --enable-version3`, without `--enable-agpl`) | GPL-3.0 | `app/src/main/jniLibs/{arm64-v8a,armeabi-v7a,x86_64}/libffmpegJNI.so`, `native/ffmpeg/build.sh` |
| **DSD-Nexus / libdst** | Peter Ross (FFmpeg DST decoder) / DSD-Nexus | https://github.com/wichers/dsd-nexus/tree/b3f443d4c42ccf52806eb57def3b3a34805714bd/libs/libdst (commit `b3f443d4c42ccf52806eb57def3b3a34805714bd`) | LGPL-2.1-or-later | `app/src/main/cpp/dsd/dst/` (`NOTICE.md`, `COPYING.LGPL-2.1`) |
| **Zenith** | Zenith contributors | https://github.com/rezapranata089-bit/Zenith | GPL-3.0 | `PlayerPanel.kt`, `MiniPlayer.kt`, `SnapshotBackdrop.kt`, `PlayerDock.kt` |
| **Echo-Music** | EchoMusicApp contributors | EchoMusicApp/Echo-Music (`AppFloatingNavBar`, `FloatingMiniPlayer`, `GlassEffectConfig`) | GPL-3.0 | `LiquidGlass.kt`, `GlassNavBar.kt`, `FloatingTabBar.kt` |
| **liquid_glass_easy** (`4.3.4`) | Copyright (c) 2025 Ahmed Gamil | https://pub.dev/packages/liquid_glass_easy | MIT | `app/src/main/java/com/music/bitchord/ui/components/floatingtabbar/GlassSelectionPill.kt` |
| **SpotiFLAC** (`MornyeGlassWarmup`) | SpotiFLAC / Mornye contributors | Startup AGSL shader warmup technique | MIT | `app/src/main/java/com/music/bitchord/ui/components/backdrop/GlassShaderWarmup.kt` |
| **decent-player** | Copyright (c) 2026 Ma145 | https://github.com/Ma145/decent-player | MIT | `app/src/main/java/com/music/bitchord/playback/audio/usb/{UsbDirectManager.kt,DirectUsbCapability.kt}` |
| **Dial** | steeben | https://github.com/steeben/Dial | GPL-3.0 | 10 adapted files: `HideAiMusic.kt`, `HideAiMusicSettings.kt`, `GlassAlert.kt`, `IosSwitch.kt`, `ContentCountryDialog.kt`, `SegmentedControl.kt`, `SettingsDockingSearch.kt`, `ContentCountry.kt`, `AiMusicFilter.kt`, `AiMusicLists.kt` |
| **Convx** | cosmictaserdev-creator | https://github.com/cosmictaserdev-creator/Convx | GPL-3.0 | `IosOverscroll.kt` (adapted); JS module loader patterns in `data/sources/module/{ModuleManager.kt,QuickJsExecutor.kt,ModuleIndex.kt,SpineModule.kt}` |
| **Kizzy** / **Zion Huang** (`InnerTune`) | Copyright (C) 2022 yzziK (Vaibhav); modifications by Zion Huang | Kizzy / InnerTune Discord RPC gateway | Original Kizzy header notice (see below) | `shared/src/jvmSharedMain/kotlin/com/my/kizzy/` |

> [!NOTE]
> **Kizzy Notice**: Files under `shared/src/jvmSharedMain/kotlin/com/my/kizzy/` retain their original upstream header (`Copyright (C) 2022 ... KizzyRPC.kt is part of Kizzy and can not be copied and/or distributed without the express permission of yzziK(Vaibhav)`, modified by Zion Huang) with no separate open-source license header in those files.

### AI Music Filter Community Blocklists (used via Dial's filter)
- **Flow AI music list** (`https://ailist.flow-tube.org/v1/aimusic/`) — Soul Over AI (CC BY 4.0)
- **Spotify AI Blocker** (`https://github.com/CennoxX/spotify-ai-blocker`) — MIT
- **aimusic-blocklist** (`https://github.com/mankidev/aimusic-blocklist`) — MIT

---

## 3. Bundled Fonts

- **Poppins** — `Copyright 2020 The Poppins Project Authors (https://github.com/itfoundry/Poppins)` — Indian Type Foundry (Ninad Kale for Devanagari, Jonny Pinhorn for Latin).  
  License: SIL Open Font License, Version 1.1 ([LICENSES/OFL-1.1.txt](LICENSES/OFL-1.1.txt)).
- **Parkinsans** — `Copyright 2024 The Parkinsans Project Authors (https://github.com/redstonedesign/parkinsans)` — Red Stone & Indian Type Foundry.  
  License: SIL Open Font License, Version 1.1 ([LICENSES/OFL-1.1.txt](LICENSES/OFL-1.1.txt)).

---

## 4. Machine Learning Models

- **Beat This!** (`models/beat_this_int8.onnx`) — CPJKU (ISMIR 2024), beat and downbeat tracking model (code and trained weights).  
  License: MIT License ([LICENSES/MIT.txt](LICENSES/MIT.txt)).
- **Open-Unmix** (`models/vocals_umxhq_int8.onnx`) — Fabian-Robert Stöter & Antoine Liutkus / Inria / SigSep (`vocals` target model and Zenodo weights).  
  License: MIT License ([LICENSES/MIT.txt](LICENSES/MIT.txt)).

---

## 5. Android, Shared & Backend Library Dependencies

### Copyleft & Weak-Copyleft Dependencies
- **InnerTubeX** (`com.github.MetrolistGroup.innertubex:innertubex-android:v0.11.7` / `innertubex:v0.11.7`) — MetrolistGroup — **GPL-3.0**
- **NewPipeExtractor** (`com.github.TeamNewPipe:NewPipeExtractor:v0.26.3`) — Team NewPipe — **GPL-3.0-or-later**
- **Mozilla Rhino** (`org.mozilla:rhino:1.8.1`, `org.mozilla:rhino-engine:1.8.1`) — Mozilla Foundation & Rhino contributors — **MPL-2.0** ([LICENSES/MPL-2.0.txt](LICENSES/MPL-2.0.txt))

### Permissive Dependencies (Apache-2.0 / MIT / BSD / Unlicense)
- **Haze** (`dev.chrisbanes.haze:haze:1.3.1`, `haze-materials:1.3.1`) — Chris Banes — **Apache-2.0**
- **AndroidX & Jetpack** (`Media3 / ExoPlayer 1.11.0`, `Compose`, `Activity`, `Navigation`, `Lifecycle`, `WorkManager`, `Core`, `AppCompat`, `Palette`, `MediaRouter`, `ProfileInstaller`, `SplashScreen`, `Security Crypto`) — Google / Android Open Source Project — **Apache-2.0**
- **Kotlin, Kotlinx & Compose Multiplatform** (`kotlinx-coroutines`, `kotlinx-serialization-json`, `Compose Multiplatform`) — JetBrains — **Apache-2.0**
- **Ktor** (`io.ktor:ktor-client-{core,okhttp,content-negotiation,websockets}:3.5.2`, `ktor-serialization-kotlinx-json:3.5.2`) — JetBrains — **Apache-2.0**
- **Coil 3** (`io.coil-kt.coil3:coil-compose:3.0.4`, `coil-network-okhttp:3.0.4`) — Coil Contributors — **Apache-2.0**
- **OkHttp** (`com.squareup.okhttp3`, `5.3.2`) — Square, Inc. — **Apache-2.0**
- **ZXing** (`com.google.zxing:core:3.5.3`) — ZXing Authors — **Apache-2.0**
- **compose-richtext** (`com.halilibo.compose-richtext:richtext-{ui-material3,commonmark}:0.20.0`) — Halilibo — **Apache-2.0**
- **QuickJS-kt** (`io.github.dokar3:quickjs-kt-android:1.0.14`) — dokar3 — **Apache-2.0**
- **SMBJ** (`com.hierynomus:smbj:0.15.0`) — Jeroen van Erp — **Apache-2.0** (uses **BouncyCastle**, MIT-style Bouncy Castle License)
- **JSR-305** (`com.google.code.findbugs:jsr305:3.0.2`) — **Apache-2.0**
- **ONNX Runtime** (`com.microsoft.onnxruntime:onnxruntime-android:1.28.0`, `onnxruntime:1.20.0`) — Microsoft Corporation — **MIT**
- **Jsoup** (`org.jsoup:jsoup:1.22.2`) — Jonathan Hedley — **MIT**
- **gorilla/websocket** (`github.com/gorilla/websocket v1.5.3`, in `backend/go.mod`) — Gorilla WebSocket Authors — **BSD-2-Clause** ([LICENSES/BSD-2-Clause.txt](LICENSES/BSD-2-Clause.txt))
- **Protobuf JavaLite** (`com.google.protobuf:protobuf-javalite:4.35.0`) — Google — **BSD-3-Clause**
- **nanojson** (`com.github.TeamNewPipe:nanojson`) — **The Unlicense**
- **Google Play Services Cast & Auth** (`play-services-cast-framework`, `play-services-auth`) — Google APIs Terms of Service

---

## 6. Lyrics & Canvas Providers

ChordPlay queries third-party lyrics and community canvas providers directly from the user's device when enabled:

- **Active Lyrics Providers** (`shared/src/jvmSharedMain/kotlin/com/music/bitchord/data/lyrics/LyricsSource.kt`):
  - **lrc.red** (`https://lrc.red`)
  - **BiniLyrics** (`https://github.com/binimum`)
  - **BetterLyrics** & **BetterLyrics Portato** (`https://github.com/better-lyrics/better-lyrics`)
  - **PaxSenix** (`https://lyrics.paxsenix.org`, including Spotify & Musixmatch fallback endpoints when an API key is configured)
  - **LyricsPlus** (`https://github.com/ibratabian17/YouLyPlus`)
  - **Unison** (`https://unison.boidu.dev`)
  - **KuGou** (`https://www.kugou.com`)
  - **LRCLIB** (`https://lrclib.net`)
  - **Musixmatch** (`https://www.musixmatch.com`)
  - **Genius** (`https://genius.com`)
  - **YouTube Music / YouTube Captions**
- **Integrated / Currently Disabled (`hidden = true`) Lyrics Sources**:
  - **SimpMusic** (`https://github.com/maxrave-dev/SimpMusic`)
  - **Megalobiz** (`https://www.megalobiz.com`)
- **Canvas Provider**:
  - **vivimusicanvas** (`CommunityCanvas.kt`)

---

## 7. Trademarks & Brand Assets

- **Dolby** and **Dolby Atmos** (and the double-D emblem adapted in `ic_dolby_atmos.xml`) are registered trademarks of Dolby Laboratories Licensing Corporation.
- **Spotify** and the Spotify logo (`spotify_logo.webp`) are trademarks of Spotify AB.
- **YouTube**, **YouTube Music**, and **Google Drive** are trademarks of Google LLC.
- **Discord**, **Last.fm**, and **ListenBrainz** are trademarks of their respective owners.
- ChordPlay is an independent open-source project and is not affiliated with, endorsed by, or sponsored by any of the above companies.
