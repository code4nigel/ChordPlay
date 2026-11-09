# ChordPlay iOS Builds

This directory contains versioned iOS IPA builds, metadata, and AltStore/SideStore distribution manifests organized into two distinct environments:

### 1. [Production Builds (`prod/`)](file:///D:/Projects/A_Applicatons/ChordPlay/version/iOS/prod)
- **Bundle Identifier**: `com.chordplay.music`
- **Target Audience**: Official GitHub Releases, AltStore, SideStore, and Sideloadly users.
- **Artifacts**: Signed/Unsigned `.ipa` release packages, `altstore-source.json`, and SHA-256 checksums.

### 2. [Development Builds (`dev/`)](file:///D:/Projects/A_Applicatons/ChordPlay/version/iOS/dev)
- **Bundle Identifier**: `com.dev.chordplay`
- **Target Audience**: Developer testing, Xcode simulator builds, and rapid iterations.
- **Purpose**: Can be installed side-by-side with production builds on testing devices without overwriting the production music library.

