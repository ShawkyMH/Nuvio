# Engineering Guidelines & Agent Team Charter

This project is built and maintained by a senior multi-disciplinary Android engineering team:

| Role | Core Mandate |
|---|---|
| **Principal Android Architect** | Module boundaries, Clean Architecture enforcement, API contracts, lifecycle hygiene |
| **Senior Kotlin Engineer** | Coroutines structure, Flow backpressure, immutability, thread dispatching |
| **Senior Jetpack Compose Engineer** | Material 3 implementation, Google App aesthetics, UI performance, state hoisting |
| **Download Engine Engineer** | OkHttp range transfers, segment merging, retry backoff, socket timeout handling |
| **Media Processing Engineer** | yt-dlp IPC protocol, FFmpeg stream remuxing/extraction, metadata extraction |
| **Android Browser Engineer** | WebView sandbox security, passive media detection, URL sanitization |
| **UI/UX Product Designer** | Minimalist Google Design Language, typography scales, accessibility, touch targets |
| **QA and Automation Engineer** | Unit test coverage, integration tests, mock server tests, state transition tests |
| **Android Security Engineer** | Path traversal guards, intent filter validation, secure storage, zero telemetry |

---

## Strict Engineering Rules

1. **No Mock/Placeholder Code Presented as Real:** Never leave fake progress bars, fake hardcoded download lists, or empty stub functions pretending to be functional engines.
2. **Clean-Room Independent Implementation:** VRKA Android is licensed under GPL-3.0. Do NOT copy, paste, or transpile VRKA source code. All architectures and components in Nuvio are clean-room designed based on documented public Android and networking APIs.
3. **Structured Concurrency:** Always use appropriate coroutine scopes and dispatchers (`Dispatchers.IO` for disk/network, `Dispatchers.Default` for CPU computations, `Dispatchers.Main.immediate` for UI updates). Never use `GlobalScope`.
4. **Android 14+ / 15+ Compliance:** Never launch raw background services that will be killed by modern Android OS power managers. Use `JobScheduler.setUserInitiated(true)` on API 34+ and Foreground Services with appropriate notification bindings on API 26-33.
5. **Safe File IO:** Always sanitize file names using `FilenameUtils.sanitizeFileName` to prevent directory traversal (`../` exploits). Always perform atomic renames (`.tmp` -> target file).
6. **Exception Handling:** Never swallow exceptions silently. Log structured errors and map them to domain `DownloadError` or `AppResult.Failure` types.
7. **No God Classes:** Keep classes focused on a single responsibility. DAOs, repositories, use cases, and view models must adhere strictly to SOLID principles.

---

## Continuous GitHub Publishing Workflow & Release Discipline

8. **Permanent Continuous Publishing:**
   - Every completed change—whether a bug fix, new feature, architectural refactor, UI adjustment, or dependency update—MUST be published to GitHub.
   - Never leave completed work uncommitted, unpushed, or unreleased.
9. **Semantic Versioning & Version Code Bumping:**
   - Every release requires bumping version information in `app/build.gradle.kts` and `UpdateState.kt`:
     - **PATCH** (`x.y.Z`): For backwards-compatible bug fixes, minor reliability/performance improvements, and internal refactors. Increment `versionCode` by 1.
     - **MINOR** (`x.Y.0`): For new backwards-compatible user-facing features, new screens, or major capabilities. Increment `versionCode` by 1.
     - **MAJOR** (`X.0.0`): For breaking architectural changes, major database schema migrations, or fundamental UI redesigns. Increment `versionCode` by 1.
   - `versionCode` MUST strictly monotonically increase with each release.
10. **Git Commit & Push Hygiene:**
    - Always inspect `git status` and `git diff` before committing.
    - Use Conventional Commit format: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`.
    - Never commit secrets, keys, local environment configs, or unverified work.
    - Push directly to `origin main` and push semantic tags (`vX.Y.Z`).
11. **Release Asset Packaging & Verification:**
    - Every GitHub release MUST attach:
      - Signed/assembled release APK: `nuvio-vX.Y.Z.apk`.
      - Cryptographic checksum: `nuvio-vX.Y.Z.apk.sha256`.
      - Detailed release notes outlining changes, fixes, and verification steps.
    - The in-app update check must be verified against GitHub Releases API to guarantee users receive instant update notifications.
