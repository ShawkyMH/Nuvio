# Security Architecture & Privacy Policy

## 1. Core Principles
Nuvio operates on a zero-trust, local-only architecture. No private analytics, crash trackers, or telemetry beacons are present.

---

## 2. Threat Modeling & Defenses

### 2.1 Path Traversal Exploits (`../`)
- **Risk:** Malicious servers returning `Content-Disposition: attachment; filename="../../evil.sh"`.
- **Defense:** All file names extracted from URLs or headers are strictly sanitized through `FilenameUtils.sanitizeFileName()`:
  - Strips all path separators (`/`, `\`), null bytes (`\0`), control characters, and relative specifiers (`..`).
  - Limits file name lengths to 255 characters.
  - Replaces illegal characters with underscores `_`.
  - Ensures destination paths always resolve within the verified target download directory.

### 2.2 Unsafe Shell Command Injection
- **Risk:** Shell metacharacters (`|`, `&`, `;`, `$()`) in user-provided URLs passed into `/bin/sh`.
- **Defense:** Nuvio NEVER invokes a shell. All process executions (`ProcessBuilder`) use array-based parameter tokens `[binary, arg1, arg2, ...]`, guaranteeing that URLs cannot break out of argument boundaries.

### 2.3 SSRF & Host Validation
- Prior to launching HTTP requests or media extraction:
  - Validate that the URI scheme is strictly `http` or `https`.
  - Block loopback and link-local addresses (`127.0.0.1`, `localhost`, `169.254.x.x`, `10.x.x.x`, `192.168.x.x`) for untrusted external media downloads to prevent local network scanning.

### 2.4 Android Permissions Minimization
- No `MANAGE_EXTERNAL_STORAGE` permission.
- Private files are kept in app-specific storage.
- Public exports use `MediaStore` downloads API or user-granted SAF URIs.
- Android 13+ `POST_NOTIFICATIONS` is requested gracefully at runtime.
- Android 14+ `RUN_USER_INITIATED_JOBS` is declared explicitly for User-Initiated Data Transfer jobs.
