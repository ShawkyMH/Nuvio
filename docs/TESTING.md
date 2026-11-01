# Quality Assurance & Testing Strategy

## 1. Testing Pyramid

Nuvio employs a comprehensive multi-layered testing strategy:
- **Unit Tests (JUnit 4/5 + MockK):** Test pure business logic, range calculations, state transitions, URL sanitization, and speed smoothing algorithms.
- **Integration Tests (Robolectric / AndroidX Test):** Verify Room DAOs, migrations, and OkHttp MockWebServer responses.
- **Compose UI Tests:** Verify screen rendering, search input responsiveness, bottom sheet format selection, and navigation transitions.

---

## 2. Critical Test Matrix

| Component | Target Scenarios | Verification Method |
|---|---|---|
| **URL Validator** | HTTP/HTTPS schemes, invalid schemes, local IP blocking | Unit test |
| **Filename Sanitizer** | Path traversal strings (`../../test.mp4`), null characters, colon/pipe characters | Unit test |
| **Segment Calculator** | Single connection, 4 connections, 8 connections, uneven byte division, 1-byte files | Unit test |
| **State Machine** | Valid transitions (QUEUED -> DOWNLOADING -> COMPLETED), invalid transitions (COMPLETED -> DOWNLOADING) | Unit test |
| **HTTP Engine** | Accept-Ranges support, 206 Partial Content, 200 fallback, ETag mismatch on resume | MockWebServer |
| **Room DAOs** | Insert task, update progress, cascade delete segments on task deletion, query active tasks | In-memory Room test |
| **Speed Smoother** | Speed calculations under fluctuating byte inputs, zero division protection | Unit test |
