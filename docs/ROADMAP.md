# Engineering Roadmap & Acceptance Criteria

This roadmap defines the sequential, verifiable delivery milestones for Nuvio.

---

## Phase 0: Workspace, Research, Architecture & Licensing
- [x] Environment audit (Windows 11, JDK 17, Android SDK platforms 26/33/34, Gradle 8.5).
- [x] Git repository initialization and `.gitignore` setup.
- [x] Comprehensive research into Snaptube, VRKA, ADM, and Google Design System (`docs/RESEARCH.md`).
- [x] System architecture, module boundaries, and state machines (`docs/ARCHITECTURE.md`).
- [x] Technical decisions record (`docs/TECHNICAL_DECISIONS.md`).
- [x] GPL-3.0 compliance & license audit (`docs/LICENSE_AUDIT.md`).
- [x] Database schema specifications (`docs/DATABASE.md`).
- [x] HTTP multi-connection download algorithm documentation (`docs/DOWNLOAD_ENGINE.md`).
- [x] Media player specification (`docs/MEDIA_PLAYER.md`).
- [x] In-app browser specification (`docs/BROWSER.md`).
- [x] Security and privacy guardrails (`docs/SECURITY.md`).
- [x] Acceptance Criteria: All core documentation created, clean-room boundary enforced, target SDK 34 verified.

---

## Phase 1: Foundation, Gradle Setup & Material 3 Design System
- [ ] Multi-module or unified modular Gradle build with Kotlin DSL.
- [ ] Hilt dependency injection setup.
- [ ] Material 3 color system (Light, Dark, Dynamic Color support).
- [ ] Google-inspired design components (AppSearchBar, MediaCard, DownloadCard, NavigationBar).
- [ ] Navigation structure (NavigationSuiteScaffold with Home, Downloads, Browser, Library, Settings).
- [ ] Acceptance Criteria: Clean build compiles, app boots to Google-style Home screen with responsive bottom navigation and theme switching.

---

## Phase 2: HTTP Download Engine
- [ ] Multi-connection segmented range downloader (`OkHttp`, `Range: bytes=X-Y`).
- [ ] Server range capability inspection (`Accept-Ranges: bytes`, `Content-Length`).
- [ ] Segment math calculation & pre-allocation via `RandomAccessFile`.
- [ ] Persistent download state machine in Room (`DownloadTask`, `DownloadSegment`).
- [ ] Pause, resume, retry with exponential backoff, and cancellation.
- [ ] Download queue manager with configurable concurrency and speed calculation.
- [ ] Unit & integration tests for range transfers, segment calculations, and resume validation.
- [ ] Acceptance Criteria: Direct file URLs can be downloaded over multiple segments, paused, resumed, and verified with exact SHA-256 integrity.

---

## Phase 3: Media Extraction Engine
- [ ] Process execution abstraction (`ProcessBuilder` with safe tokenized arguments).
- [ ] Media URL validator and format probe.
- [ ] Metadata parser (Title, Duration, Thumbnails, Stream resolutions, Audio formats).
- [ ] Format selection UI (bottom sheet with 1080p, 720p, 480p, MP3, M4A).
- [ ] FFmpeg stream remuxing and audio extraction pipeline.
- [ ] Output verification via file length and container sanity checks.
- [ ] Acceptance Criteria: Media URLs present interactive format choices, trigger proper downloads and post-processing remuxing, producing playable MP4/MP3 files.

---

## Phase 4: Studio-Grade Media Player
- [ ] AndroidX Media3 / ExoPlayer integration.
- [ ] Video playback with custom Google-inspired controls overlay.
- [ ] Touch gestures (vertical swipe for brightness on left, volume on right, horizontal swipe for seeking).
- [ ] Picture-in-Picture (PiP) support with lifecycle management.
- [ ] Audio playback with notification controls via `MediaSessionService`.
- [ ] Playback history tracking in Room with resume position memory.
- [ ] Acceptance Criteria: Both local video and audio files play smoothly with responsive gesture controls, background audio playback, and PiP.

---

## Phase 5: Smart In-App Browser
- [ ] Android WebView integration with clean modern address bar.
- [ ] Multiple tab management and history/bookmarks.
- [ ] Passive media sniffing via `shouldInterceptRequest` for video/audio/HLS streams.
- [ ] Download link capture dialog handoff to downloader engine.
- [ ] Private browsing mode with isolated session state.
- [ ] Security hardening (disable file/content URL access, strict HTTPS redirects).
- [ ] Acceptance Criteria: User can browse web pages, streamable media links are detected automatically, and users can send media directly to the download queue.

---

## Phase 6: Media Library & Settings
- [ ] Categorized offline file manager (Videos, Audio, Downloads, Documents, APKs).
- [ ] File operations: Open in player, Rename, Share, Delete, Details.
- [ ] Storage breakdown visualizer (Used space, Available space).
- [ ] Settings sections: Download limits, Network preferences (Wi-Fi only), Player defaults, Storage paths, Theme.
- [ ] Acceptance Criteria: Downloaded files are neatly indexed, searchable, playable, and settings persist reliably via DataStore.

---

## Phase 7 & 8: Verification, Release Preparation & Polish
- [ ] Complete unit test suite pass.
- [ ] Memory leak audit and coroutine lifecycle review.
- [ ] ProGuard / R8 rules configuration.
- [ ] Final build verification (Debug & Release APK generation).
