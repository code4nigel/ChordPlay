# ChordPlay for iOS — Development Guide

Welcome to the iOS port of ChordPlay! This project targets iOS devices (iOS 16.0+) using **Compose Multiplatform (CMP)** and **Kotlin Multiplatform (KMP)**.

---

## 1. Branch Strategy & Feature Work

To keep the stable Android and Desktop versions safe on `main`, all iOS porting and experimentation happens in the `feature/ios-port` branch.

### Git Branch Cheat Sheet

| Action | Command | What it does |
| :--- | :--- | :--- |
| **Check where you are** | `git branch` | Lists local branches; the active one has an asterisk `*`. |
| **Check changed files** | `git status` | Shows staged, unstaged, and untracked files. |
| **Switch to main** | `git checkout main` | Switches back to the clean, release-ready `main` branch. |
| **Switch back to iOS** | `git checkout feature/ios-port` | Resumes work on the iOS branch. |
| **Create a new branch** | `git checkout -b <name>` | Creates a new branch from your current point and switches to it. |
| **Save your work** | `git add .` <br> `git commit -m "Description"` | Records a snapshot in the current branch's history. |
| **Merge into main** | `git checkout main` <br> `git merge feature/ios-port` | Brings iOS work into `main` once ready. |
| **Discard branch** | `git checkout main` <br> `git branch -D feature/ios-port` | Deletes the branch if you ever decide to abandon it. `main` is untouched. |

---

## 2. iOS Project Layout

```
ChordPlay/
├── iosApp/                # Xcode / SwiftUI host application
├── shared/                # Data models, networking, repositories (KMP)
├── sharedUi/              # Shared Compose UI screens and components (CMP)
├── version/
│   └── iOS/               # Versioned iOS releases & AltStore manifests
│       ├── dev/           # Development & Simulator test builds
│       └── prod/          # Production releases and altstore-source.json
└── docs/                  # Documentation & guides
```

---

## 3. Technology Architecture

1. **User Interface**: Rendered via Skia/Metal using `ComposeUIViewController` from Jetpack Compose Multiplatform.
2. **Audio Playback**: Uses Apple's native `AVPlayer` / `AVQueuePlayer` from `AVFoundation`, exposed through ChordPlay's `PlayerHost` interface.
3. **Lock Screen Controls**: Integrated with `MPNowPlayingInfoCenter` and `MPRemoteCommandCenter`.
4. **Distribution**: Built as an unsigned `.ipa` on GitHub Actions and distributed via **AltStore / SideStore** and direct sideloading.
