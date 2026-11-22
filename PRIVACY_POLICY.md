# ChordPlay Privacy Policy

**Last updated:** November 22, 2026

ChordPlay is an open-source, privacy-first music player. We do not operate tracking servers, collect analytics, serve ads, or sell your personal data.

## 1. Data Stored Locally on Your Device
By default, all of your ChordPlay data stays strictly on your device:
- **App Settings & Preferences** (theme, dock style, audio quality, playback preferences).
- **ChordPlay Identity** (your chosen display name and deterministic Wave Identity Pass).
- **Listening History & Replay Stats** (play counts, listening duration, and monthly/yearly summaries).
- **Local Backups** (exported `.chordplay.png` / `.chordplay.json` files saved when you choose to export them).

## 2. Google Drive Backup (Optional)
If you choose to connect **Google Drive Backup** in **Settings → Your Data → Google Drive backup**, ChordPlay requests only the narrow, non-sensitive scope:
- `https://www.googleapis.com/auth/drive.appdata`

### How Google Drive data is used
- **Hidden App Folder Only:** ChordPlay can only read and write its own private `appDataFolder` in your Google Drive. It **cannot** view, list, modify, or delete any of your other Google Drive files or folders.
- **What is backed up:** Your ChordPlay Identity (name and code), app settings, and monthly Replay listening statistics so you can restore or sync them across your own devices.
- **No Third-Party Sharing:** Your Google Drive data is transmitted directly between your device and Google's servers. ChordPlay does not send your Google account token or backup data to any third-party server.
- **Revoking Access & Deleting Backups:** You can disconnect Google Drive or delete any device's cloud backup at any time from **Settings → Your Data → Google Drive backup**, or revoke ChordPlay's access from your Google Account permissions page.

ChordPlay's use and transfer of information received from Google APIs adheres to the [Google API Services User Data Policy](https://developers.google.com/terms/api-services-user-data-policy), including the Limited Use requirements.

## 3. Optional Third-Party Services
If you explicitly sign in or enable optional integrations, ChordPlay communicates directly with those services on your behalf:
- **YouTube Music** (to stream music and access your playlists/library).
- **Last.fm / ListenBrainz / Discord Rich Presence** (only if enabled by you in Settings).
- **Lyrics Providers** (song title/artist metadata is queried only to fetch synced lyrics).

## 4. Children's Privacy
ChordPlay does not knowingly collect personal information from children under 13.

## 5. Contact
For privacy questions or data inquiries, please open an issue on GitHub:
https://github.com/code4nigel/ChordPlay/issues
