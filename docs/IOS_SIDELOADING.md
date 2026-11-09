# Installing ChordPlay on iOS (iPhone / iPad)

ChordPlay for iOS targets **iOS 16.0 or later**. Because ChordPlay is open-source and free, it is distributed as a community sideload release via **AltStore Classic**, **SideStore**, and **Sideloadly** — no jailbreak or paid Apple Developer account required!

---

## Method 1: AltStore Classic (Recommended)

1. **Install AltServer & AltStore**:
   - Follow the official guide at [altstore.io](https://faq.altstore.io/altstore-classic/how-to-install-altstore-windows) to install AltServer on your Windows PC or Mac.
   - Install the AltStore app onto your iPhone using AltServer.
2. **Add the ChordPlay Source**:
   - Open **AltStore** on your iPhone.
   - Tap **Sources** tab, then tap **+** in the top right.
   - Enter the permanent ChordPlay source URL:
     ```text
     https://github.com/chordplay-music/ChordPlay/releases/download/ios/altstore-source.json
     ```
3. **Install & Update**:
   - Tap **FREE / INSTALL** next to ChordPlay.
   - AltStore signs ChordPlay with your Apple ID and installs it.
   - When new updates release, simply refresh your sources in AltStore and tap **UPDATE**!

---

## Method 2: SideStore (On-Device, No PC Needed after setup)

1. Install SideStore by following the [SideStore installation guide](https://docs.sidestore.io/docs/installation/prerequisites).
2. Go to **Sources**, tap **+**, and add:
   ```text
   https://github.com/chordplay-music/ChordPlay/releases/download/ios/altstore-source.json
   ```
3. Tap **Install**. SideStore handles background auto-refreshes directly over Wi-Fi without needing a PC plugged in!

---

## Method 3: Sideloadly (Direct USB Sideload from PC)

1. Download [Sideloadly for Windows](https://sideloadly.io/).
2. Connect your iPhone via USB and trust your computer.
3. Download the latest `ChordPlay-unsigned.ipa` from [ChordPlay GitHub Releases](https://github.com/chordplay-music/ChordPlay/releases/tag/ios).
4. Drag `ChordPlay-unsigned.ipa` into Sideloadly.
5. Enter your Apple ID and click **Start**.
6. On your iPhone, go to **Settings → General → VPN & Device Management**, tap your developer certificate, and tap **Trust**.
7. If on iOS 16+, enable **Developer Mode** under **Settings → Privacy & Security**.
8. Launch ChordPlay!

---

## Background Playback & Signing Expiry Note
- **Background Audio**: ChordPlay includes native `audio` background capabilities, so your music continues seamlessly when the screen locks or when switching between apps.
- **7-Day Refresh**: Free Apple accounts require apps to be re-signed once every 7 days. AltStore and SideStore handle this automatically in the background as long as their refresh service is active.
