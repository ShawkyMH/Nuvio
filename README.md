# Nuvio — Premium Android Media Downloader & Media Player

> Inspired by the precision of **Advanced Download Manager (ADM)**, the media extraction capabilities of **VRKA** & **Snaptube**, and the minimalist, intuitive aesthetics of **Google Material 3** apps (Google Search, Files, and Photos).

---

## 1. Overview

**Nuvio** is an offline-first, native Android application engineered to provide an all-in-one media downloading, streaming extraction, file acceleration, and media playback experience.

Unlike legacy download managers cluttered with ad banners, technical jargon, and complex dialogs, Nuvio treats media and file downloading as a fluid, unified experience:
1. **Google-Inspired Search Hub:** Paste any direct URL or media link directly into a clean, centered search experience.
2. **Advanced Multi-Connection HTTP Engine:** High-performance segmented file transfers with byte-range validation, resume verification, and connection pooling.
3. **Media Extraction & Format Discovery:** Safe, privacy-first media inspection powered by structured extractors (yt-dlp protocol compliant) and stream remuxing via FFmpeg.
4. **Studio-Grade Media Player:** Built on AndroidX Media3 (ExoPlayer) with swipe gestures for brightness/volume, Picture-in-Picture (PiP), background audio playback, and lock screen MediaSession support.
5. **Integrated Smart Browser:** Lightweight, privacy-focused browser with passive media link discovery, tab management, and zero tracking.
6. **Unified Media Library:** Categorized offline library with Storage Access Framework (SAF) integration and MediaStore indexing.

---

## 2. Architectural Pillars

- **Language:** 100% Kotlin with strict Coroutines & Flow concurrency.
- **UI Framework:** Jetpack Compose with Material 3 (Dynamic Color / Material You support).
- **Architecture:** Clean Architecture + Unidirectional Data Flow (MVI/MVVM).
- **Dependency Injection:** Hilt.
- **Persistence:** Room Database with versioned migrations + Jetpack DataStore for preferences.
- **Background Execution:** Hybrid architecture:
  - **Android 14+ (API 34+):** User-Initiated Data Transfer (UIDT) Jobs (`JobScheduler.setUserInitiated(true)`).
  - **Android 8–13 (API 26–33):** Controlled Foreground Service with `dataSync` type and notification channels.
- **Privacy & Security:** Zero analytics, zero ad SDKs, path traversal protection, strict SSL/TLS validation, sanitized filenames, and on-device execution.

---

## 3. Modular Architecture

```
nuvio/
├── app/                  # Application orchestrator, Hilt application, DI graph
├── core/
│   ├── common/           # Result monad, Dispatcher provider, extensions, logging
│   ├── designsystem/     # Material 3 theme, Google-inspired typography, icons, components
│   ├── database/         # Room DB, DAOs, entities, migrations, type converters
│   ├── network/          # OkHttp client, range verification, interceptors, user agents
│   └── storage/          # SAF file manager, storage volume calculation, URI resolution
├── domain/
│   ├── model/            # Immutable domain models (DownloadTask, MediaFormat, PlaybackState)
│   ├── repository/       # Repository interfaces (DownloadRepository, MediaRepository, BrowserRepository)
│   └── usecase/          # Focused business use cases (SubmitUrlUseCase, ResumeDownloadUseCase, etc.)
├── data/
│   └── repository/       # Repository implementations bridging Room, DataStore, and Engines
├── engine/
│   ├── http/             # Parallel segment HTTP download engine, byte-range parser
│   ├── ytdlp/            # Safe process execution wrapper, metadata parser, format selector
│   ├── ffmpeg/           # Remuxing, audio extraction, stream validation
│   └── browser/          # Passive media sniffer, URL validator, cookie isolator
├── feature/
│   ├── home/             # Search-centric landing page, paste shortcuts, active transfers
│   ├── downloads/        # Transfer queue, active progress, speed graphs, pause/resume/cancel
│   ├── browser/          # Tabbed browser, address bar, media detection bottom sheet
│   ├── player/           # Media3 video & audio playback, PiP, gesture overlay
│   ├── library/          # Categorized media viewer, search, batch delete, file details
│   └── settings/         # Engine settings, network limits, theme, storage, component info
└── service/
    └── download/         # User-Initiated JobService (API 34+) and ForegroundService (API 26-33)
```

---

## 4. Documentation Index

Detailed engineering documentation is available under [`docs/`](file:///docs):
- [`docs/RESEARCH.md`](file:///docs/RESEARCH.md) — Research into Snaptube, VRKA, ADM, Android APIs, and execution mechanisms.
- [`docs/ARCHITECTURE.md`](file:///docs/ARCHITECTURE.md) — System architecture, module boundaries, data flows, and state machines.
- [`docs/TECHNICAL_DECISIONS.md`](file:///docs/TECHNICAL_DECISIONS.md) — Justification of design, runtime, and dependency choices.
- [`docs/LICENSE_AUDIT.md`](file:///docs/LICENSE_AUDIT.md) — Licensing review (Clean-room implementation, GPL-3.0 compliance vs Apache 2.0).
- [`docs/ROADMAP.md`](file:///docs/ROADMAP.md) — Phased development milestones and acceptance criteria.
- [`docs/DATABASE.md`](file:///docs/DATABASE.md) — Complete Room entity schemas, indexes, and migration strategy.
- [`docs/DOWNLOAD_ENGINE.md`](file:///docs/DOWNLOAD_ENGINE.md) — Multi-connection HTTP download algorithm, byte-ranges, and resume validation.
- [`docs/MEDIA_PLAYER.md`](file:///docs/MEDIA_PLAYER.md) — Media3 / ExoPlayer setup, PiP, gestures, and audio MediaSession.
- [`docs/BROWSER.md`](file:///docs/BROWSER.md) — Browser engine architecture, passive media detection, and sandbox security.
- [`docs/SECURITY.md`](file:///docs/SECURITY.md) — Path traversal prevention, SSL pinning readiness, credential hygiene, and privacy.
- [`docs/TESTING.md`](file:///docs/TESTING.md) — Testing strategy across unit, integration, and UI verification.
- [`docs/CHANGELOG.md`](file:///docs/CHANGELOG.md) — Tracked changes by phase.

---

## 5. Building & Requirements

- **Android Studio / Android SDK:** Build-Tools 34.0.0+, Compile SDK 34, Target SDK 34, Min SDK 26 (Android 8.0 Oreo).
- **JDK:** OpenJDK 17 or higher.
- **Gradle:** 8.5+ with Gradle Kotlin DSL.
