# Nuvio In-App Update System Architecture

Nuvio features a secure, privacy-first, zero-telemetry in-app update engine that directly tracks official releases published on GitHub.

---

## 1. Architectural Overview

The update system is modularized inside `com.nuvio.core.update`:

```mermaid
flowchart TD
    A["Settings UI / User Click 'Check for Updates'"] --> B["UpdateViewModel"]
    B --> C["UpdateRepositoryImpl"]
    C --> D["GitHubReleaseDataSource"]
    D -->|GET /repos/ShawkyMH/Nuvio/releases/latest| E["GitHub REST API"]
    E -->|JSON Response| D
    D -->|Pure Kotlin SimpleJsonParser| F["UpdateInfo (Version, APK URL, SHA256 URL)"]
    F --> C
    C -->|Version Comparison: Remote > Installed?| G{"New Version?"}
    G -- No --> H["UpdateStatus.UpToDate"]
    G -- Yes --> I["UpdateStatus.UpdateAvailable (UpdateDialog Shown)"]
    I -->|User Clicks 'Update Now'| J["UpdateDownloader"]
    J -->|Chunked HTTPS Stream with Progress Flow| K["Local APK in cacheDir/updates/"]
    K --> L["ApkIntegrityVerifier"]
    L -->|Fetch & Compare SHA-256 Digest| M{"SHA-256 Match?"}
    M -- Mismatch --> N["UpdateStatus.Failed (Corrupt Payload Aborted)"]
    M -- Match --> O["ApkInstaller"]
    O -->|Check canRequestPackageInstalls()| P{"Permission Granted?"}
    P -- No --> Q["Launch ACTION_MANAGE_UNKNOWN_APP_SOURCES Settings"]
    P -- Yes --> R["Generate FileProvider Content URI & Launch ACTION_VIEW Installer"]
```

---

## 2. Component Reference

### A. `GitHubReleaseDataSource`
- Fetches the latest release from `https://api.github.com/repos/ShawkyMH/Nuvio/releases/latest`.
- Uses `SimpleJsonParser.kt` to extract release metadata without relying on Android framework `org.json` stubs, ensuring identical behavior across Android runtime and JVM unit tests.
- Identifies the `.apk` asset download URL and companion `.apk.sha256` asset URL.

### B. `VersionComparison`
- Compares SemVer strings (`major.minor.patch`) as integer tuples.
- Fallback support for integer `versionCode` when specified in release tags or descriptions.
- Ensures a user on `v1.0.0` is notified only when a strictly higher version (e.g. `v1.0.1`) is published.

### C. `UpdateDownloader`
- Streams APK bytes over HTTPS with a 32 KB chunk buffer.
- Emits download progress via Kotlin `StateFlow<UpdateState>` to drive the Material 3 progress bar in real time.
- Handles atomic write to `context.cacheDir/updates/nuvio-update.apk`.

### D. `ApkIntegrityVerifier`
- Computes cryptographic SHA-256 message digest of the downloaded APK.
- Fetches and parses the published checksum string.
- If the checksums do not match, the file is immediately purged and the process aborts to prevent malicious tampering or partial transfers.

### E. `ApkInstaller`
- Checks `context.packageManager.canRequestPackageInstalls()` (Android 8.0+ / API 26+).
- If permission is missing, opens `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES` scoped specifically to `package:com.nuvio`.
- Generates a secure content URI using Android `FileProvider`:
  ```
  content://com.nuvio.fileprovider/update_cache/nuvio-update.apk
  ```
- Launches the native system package installer with `Intent.FLAG_GRANT_READ_URI_PERMISSION`.

---

## 3. UI Integration & Settings

The update UI is integrated into `SettingsScreen.kt` under **About Nuvio > App Updates**:
- **Check for Updates Button**: Manually triggers check with spinner animation.
- **Current Version Badge**: Displays current `versionName` (`1.0.0`) and build variant.
- **Material 3 Update Dialog**: Displays:
  - Version comparison (`1.0.0 → 1.1.0`)
  - Changelog / Release notes
  - Progress bar with percentage and downloaded bytes during transfer
  - Install / Cancel actions
