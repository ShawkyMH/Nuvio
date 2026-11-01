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
