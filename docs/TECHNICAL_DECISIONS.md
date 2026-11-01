# Technical Decisions Record (TDR)

## TDR-001: Platform Versions & Target SDK
- **Decision:** Target SDK 34 (Android 14) with compatibility forward to Android 15/16; Minimum SDK 26 (Android 8.0 Oreo).
- **Justification:**
  - Min SDK 26 provides modern Java 8+ / Kotlin language features natively, notification channels, background execution constraints, and covers ~95%+ of active Android devices globally.
  - Target SDK 34 enforces strict Android 14 requirements (foreground service types, User-Initiated Data Transfer jobs, granular photo/video permissions, notification permissions).

## TDR-002: Build Tools & Dependency Versions
- **Gradle:** 8.5
- **Android Gradle Plugin (AGP):** 8.3.2 / 8.4.0
- **Kotlin:** 1.9.23 / 2.0.0 compatible
- **Jetpack Compose Compiler / BOM:** `androidx.compose:compose-bom:2024.04.01`
- **Material 3:** `androidx.compose.material3:material3:1.2.1`
- **Room:** 2.6.1 (KSP supported)
- **DataStore:** `androidx.datastore:datastore-preferences:1.1.0`
- **Hilt:** 2.51.1
- **OkHttp:** 4.12.0
- **Media3 / ExoPlayer:** `androidx.media3:media3-exoplayer:1.3.1`, `androidx.media3:media3-ui:1.3.1`, `androidx.media3:media3-session:1.3.1`
- **Coroutines:** 1.8.0
- **Navigation Compose:** 2.7.7

## TDR-003: Background Execution Strategy — Hybrid UIDT + FGS
- **Options Evaluated:**
  1. *WorkManager ForegroundInfo:* Good for periodic or opportunistic work, but introduces execution jitter and overhead for active, high-priority user-initiated downloads.
  2. *Classic Foreground Service (DataSync):* Subject to new 6-hour execution caps and kill policies on Android 14/15 if misused.
  3. *User-Initiated Data Transfer (UIDT) Jobs:* Official Android 14+ mechanism specifically engineered for large user-requested transfers.
- **Decision:** Implement a dual-mode `DownloadExecutionCoordinator`:
  - On API 34+ (Android 14+), execute via `JobScheduler` with `setUserInitiated(true)` and `RUN_USER_INITIATED_JOBS`.
  - On API 26–33, execute via `ForegroundService` with notification channel and `FOREGROUND_SERVICE_TYPE_DATA_SYNC`.

## TDR-004: HTTP Download Engine Architecture
- **Decision:** Custom OkHttp-based multi-connection parallel range engine.
- **Justification:**
  - Avoids bundling massive external C++ download libraries.
  - Zero external native binary dependencies for standard HTTP/HTTPS transfers.
  - Full control over byte-range requests, ETag verification, socket timeouts, backpressure, and pause/resume logic.
  - Safe direct disk writes via `RandomAccessFile` / `FileChannel` into pre-allocated sparse files.

## TDR-005: Media Extraction & FFmpeg Protocol
- **Decision:** Protocol-driven process isolation with strict argument arrays (`ProcessBuilder(listOf(...))`).
- **Safety Policy:**
  - Never invoke `Runtime.getRuntime().exec(rawString)` or pass unsanitized shell strings through `/bin/sh` or `cmd.exe`.
  - All command arguments are passed as discrete tokenized arrays.
  - Input URLs are verified through strict URI schema validators before invocation.
  - FFmpeg operations prioritize stream copying (`-c copy`) over transcoding to minimize CPU and battery usage.

## TDR-006: In-App Browser Strategy
- **Decision:** Android WebView with pluggable `BrowserEngine` interface for future GeckoView extension.
- **Security Protections:**
  - Disable dangerous file access (`setAllowFileAccess(false)`, `setAllowContentAccess(false)`).
  - Enforce HTTPS redirect validation.
  - Passive media interception via `WebViewClient.shouldInterceptRequest` — inspect response MIME types (`video/*`, `audio/*`, `application/x-mpegURL`, `application/dash+xml`) without breaking page operation.
  - Private browsing mode utilizes isolated in-memory cookie stores (`CookieManager.getInstance().removeAllCookies(...)`).
