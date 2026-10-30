# ChordPlay Android APK Builds

This directory contains versioned Android APK builds organized into two distinct environments:

### 1. [Production Builds (`prod/`)](file:///D:/Projects/A_Applicatons/ChordPlay/version/Android/prod)
- **Package Name (`applicationId`)**: `com.chordplay.music`
- **Target Audience**: Official GitHub Releases and general public users.
- **Collision Protection**: Fully isolated namespace and authority; installs safely alongside BitChord with zero conflicts.

### 2. [Development Builds (`dev/`)](file:///D:/Projects/A_Applicatons/ChordPlay/version/Android/dev)
- **Package Name (`applicationId`)**: `com.dev.chordplay`
- **Target Audience**: Developer testing and preview builds.
- **Purpose**: Can be installed side-by-side on the exact same phone alongside the Production build without overwriting your daily music player.

