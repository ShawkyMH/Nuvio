# Changelog

All notable changes to the Nuvio project are documented here.

## [1.0.0] - Initial Production Implementation

### Added
- **Phase 0: Environment & Research Audit**
  - Git repository setup and comprehensive `.gitignore`.
  - Research document (`docs/RESEARCH.md`) comparing Snaptube, VRKA, ADM, and Google Design Systems.
  - License audit (`docs/LICENSE_AUDIT.md`) establishing clean-room independence from GPL-3.0.
  - Architecture specifications (`docs/ARCHITECTURE.md`) with Mermaid diagrams for data flows and state machines.
  - Technical decisions record (`docs/TECHNICAL_DECISIONS.md`).
  - Database schema (`docs/DATABASE.md`) with Room/SQLite table definitions, foreign keys, and indexes.
  - Multi-connection HTTP download algorithm document (`docs/DOWNLOAD_ENGINE.md`).
  - Built-in media player architecture (`docs/MEDIA_PLAYER.md`) and browser architecture (`docs/BROWSER.md`).
  - Security architecture (`docs/SECURITY.md`) and testing strategy (`docs/TESTING.md`).

- **Phase 1: Foundation & Material 3 Design System**
  - Gradle Kotlin DSL multi-package structure with AGP 8.2.2 and Kotlin 1.9.22.
  - Google Material 3 Theme (`Color.kt`, `Shape.kt`, `Type.kt`, `Theme.kt`) with Dynamic Color (Material You) and Light/Dark palettes.
  - Google-style reusable components: `AppSearchBar` (centered pill), `DownloadCard`, `EmptyState`, `SectionHeader`, `NuvioBottomNavigation`.
  - `NuvioApp` application container, notification channels, and manual DI container (`AppContainer`).
  - Full bottom navigation across 5 core destinations: Home, Downloads, Browser, Library, Settings.

- **Phase 2: High-Performance Segmented HTTP Download Engine**
  - Multi-connection byte-range downloader using OkHttp (`Range: bytes=start-end`).
  - `HttpRangeProber` for server capability detection (`Accept-Ranges`, `Content-Range`, `Content-Length`, `ETag`).
  - Direct sparse pre-allocation via `RandomAccessFile.setLength()` and parallel stream seek writes.
  - `SpeedTracker` with Exponential Moving Average (EMA) smoothing.
  - Automatic single-connection fallback when ranges are unsupported.
  - Resume validation with ETag mutation invalidation guards.
  - Persistent SQLite database (`NuvioDatabaseHelper`, `DownloadTaskDao`, `DownloadSegmentDao`, `PlaybackHistoryDao`) with reactive Flow updates.

- **Phase 3: Media Extraction & Processing Engine**
  - `YtDlpEngine` tokenized process executor (`ProcessBuilder`) with zero raw shell interpolation.
  - Metadata parsing (title, duration, source name, format resolutions).
  - Format selection bottom sheet (`FormatSelectionBottomSheet`) offering 1080p, 720p, 480p video and MP3 (320 kbps), M4A (128 kbps) audio.
  - `FfmpegProcessor` stream copy remuxer (`-c copy`) and audio extractor.

- **Phase 4: Studio-Grade Media Player**
  - AndroidX Media3 (ExoPlayer 1.2.1) integration with Compose `AndroidView`.
  - Gesture overlay: vertical swipe on left for brightness, right for volume, double-tap for 10-sec skip.
  - Google Photos / YouTube style overlay controls (Play/Pause, Timeline scrubber, Aspect ratio toggle, Playback speed selector).
  - Picture-in-Picture (PiP) support with `PictureInPictureParams`.
  - Background audio playback service (`MediaPlaybackService`).
  - Playback position persistence in Room/SQLite for resume playback.

- **Phase 5: Smart In-App Browser & Passive Media Sniffer**
  - Android WebView with sandbox security (`allowFileAccess = false`, `allowContentAccess = false`).
  - Passive media discovery via `shouldInterceptRequest` matching video/audio MIME types, `.m3u8` playlists, and `.mpd` manifests.
  - Floating bottom banner for one-tap download handoff to the downloader engine.
  - Private browsing toggle with in-memory isolation.

- **Phase 6: Media Library & Settings**
  - Local file manager with storage gauge (Used vs Free GB).
  - Categorized media filters: Videos, Music, Documents, Archives, Other.
  - Direct play now, delete, and file details.
  - Settings screen: Concurrent downloads slider, connections per download slider, Wi-Fi only toggle, Material You toggle, component versions, and history clearance.

- **Verification & QA**
  - Unit test suite: 17 unit tests verifying path traversal sanitization, URL SSRF blocking, segment math, speed tracking, and state machines. All 17 passed (`BUILD SUCCESSFUL in 35s`).
  - Full APK compilation & packaging verified (`app-debug.apk` 19.3 MB, `BUILD SUCCESSFUL in 2m 20s`).
