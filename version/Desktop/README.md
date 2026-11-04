# ChordPlay Desktop (Windows 10/11)

**Aesthetic Lossless Music Client for Windows**

This directory contains the production-ready Windows binaries for ChordPlay Desktop, built according to `CHORDPLAY_DESKTOP_MASTER_PLAN.md`.

---

## 1. Release Binaries & Checksums

| File | Type | Size | SHA-256 Checksum |
| :--- | :--- | :--- | :--- |
| **`ChordPlay-1.8.29.exe`** | Standalone Setup Executable | 305.86 MB | `A7E4BA73A6D2809059F6693DA572BB805810278558FB53A87FFC24591A5520B6` |
| **`ChordPlay-1.8.29.msi`** | Windows Installer Package | 305.21 MB | `F4C430CF5DAEC4AA5C34F666163072824E05751911F41B65762BCCA8263BACB4` |
| **`ChordPlay-1.8-beta1-windows-portable.zip`** | Portable Distribution | 267.97 MB | `3F2ACA0E321C4FA776DEB84729C2F4BCEA019036B1980BEF11E279ED67875A84` |
| **`ChordPlay-portable-preview.zip`** | Portable Preview Archive | 267.97 MB | `3F2ACA0E321C4FA776DEB84729C2F4BCEA019036B1980BEF11E279ED67875A84` |

---

## 2. Key Architecture & Desktop-First Enhancements

### 1. Complete Upstream Detachment
- **Severed BitChord Links:** Removed all queries to `api.github.com/repos/kushagrasinghx/BitChord`. Safely disabled external update polling until the ChordPlay backend releases endpoint is online.
- **ChordPlay Branding:** Rebranded settings footer, window caption, Discord RPC, and launcher scripts (`ChordPlay.bat`).

### 2. Elimination of Mobile Drag Sheet & Introduction of Panoramic Cinema Mode
- **No Mobile Artifacts:** Completely eliminated the mobile drag handle (`—`) and modal sheet hijacking.
- **Dedicated Opt-In Cinema Stage:**
  - **Left Column (45%):** High-resolution album artwork with ChordPlay dynamic ambient aura glow, track title, artist, favorite heart toggle, real-time Lossless Audio Specs Badge (`FLAC 24-bit / 96kHz` or `AAC 320kbps Lossless Engine`), transport controls, precision scrubber, and volume slider.
  - **Right Column (55%):** Synchronized auto-scrolling karaoke lyrics with glowing active lines, alongside a tabbed Up Next queue manager.
  - **Desktop Collapse:** Dismiss via top-right desktop close button (`✕`) or `Esc` / `F11`.

### 3. Desktop-First 3-Pane + Fixed Bottom Dock Layout
- **Clean 48dp Top Bar:** ChordPlay logo mark + wordmark, history navigation (`< >`), desktop search bar with `Ctrl+F` badge, settings shortcut, account switcher, and native Windows caption controls (`_`, `□`, `✕` with signature red close hover).
- **Fixed 82dp Bottom Player Dock:** Anchored at the bottom of the viewport:
  - Left: 54dp artwork, title, artist, favorite heart button, quality badge (`LOSSLESS` / `320K`).
  - Center: Shuffle, Previous, Play/Pause circular button (`palette.primaryGlow`), Next, Repeat, precision scrubber with elapsed and total duration.
  - Right: Volume slider, output device switcher, synchronized lyrics toggle, queue toggle, and cinema mode toggle.
- **240dp Left Navigation Rail:** Liquid Glass styling, direct navigation (Home, Explore, Library, Playlists), and quick playlist creation.
- **Speed Dial Canvas Grid:** 2x3 quick-launch card grid on the Home feed featuring hover-to-play circular accent buttons.

### 4. Zero-Stutter Audio Pipeline & Dynamic Palette
- **750ms Ring Buffer:** Upgraded audio sink ring buffer in `DesktopAudioSink.kt` from 200ms to 750ms to eliminate pops and stutter during heavy UI transitions and resizing.
- **Dynamic Swatch Clustering:** `DesktopArtworkPalette.kt` extracts vibrant accents (`primaryGlow`, `ambientAura`, `surfaceGlass`) filtering out muddy grayscale and extreme luminance values.
- **Lossless Stream Racing:** `DesktopJioSaavn.kt` quality floor threshold (`> 160 kbps`) enforcing bit-perfect 320kbps AAC and FLAC stream racing.

---

## 3. Installation & Run Instructions

### Option A: Standard Windows Setup (.exe)
1. Double-click `ChordPlay-1.8.29.exe`.
2. Follow the standard Windows setup wizard.
3. Launch **ChordPlay** from the Start Menu or Desktop shortcut.

### Option B: Windows MSI Installer (.msi)
1. Double-click or run from administrative terminal:
   ```pwsh
   msiexec /i ChordPlay-1.8.29.msi
   ```

### Option C: Portable Run (No installation required)
1. Extract `ChordPlay-1.8-beta1-windows-portable.zip` into any directory.
2. Ensure Java 21+ is available on `PATH` or configured in `JAVA_HOME`.
3. Double-click `ChordPlay.bat` inside the extracted folder.
