# HTTP Download Engine Specification

## 1. Engine Core Principles
The Nuvio HTTP Download Engine is designed to achieve maximum throughput on reliable connections while remaining resilient against network drops, server restarts, and non-compliant servers.

---

## 2. Server Capability Probing Workflow
Before launching multiple concurrent connections, the engine executes a safe probe:

1. **Head / Initial Range GET:**
   Send an HTTP request with `Range: bytes=0-0` or `HEAD`.
2. **Examine Headers:**
   - Check `Accept-Ranges`: If `bytes` is reported, ranges are supported.
   - Check `Content-Range`: If response status is `206 Partial Content` and returns `bytes 0-0/totalBytes`, the server accurately handles byte slicing.
   - Check `Content-Length`: Extract total file size. If length is missing or chunked transfer encoding is active, disable parallel connections and fall back to single stream mode.
   - Record `ETag` and `Last-Modified` for resume validation.
3. **Fallback Determination:**
   - If server returns `200 OK` ignoring the `Range` header, the engine marks `supportsRanges = false` and streams sequentially into a single connection.

---

## 3. Parallel Range Algorithm & Segment Math

Given:
- `totalBytes`: Total file size in bytes
- `K`: Number of connections (default: 4, maximum: 8)

```kotlin
val segmentSize = totalBytes / K
for (i in 0 until K) {
    val start = i * segmentSize
    val end = if (i == K - 1) totalBytes - 1 else (start + segmentSize - 1)
    createSegment(segmentIndex = i, startByte = start, endByte = end)
}
```

### Direct Sparse Pre-Allocation:
Instead of writing segments to separate files (`file.part0`, `file.part1`) and stitching them together (which doubles disk IO and risks out-of-disk errors on large files):
- Nuvio pre-allocates the destination file `.tmp` using `RandomAccessFile(file, "rw").setLength(totalBytes)`.
- Each segment worker opens its own `RandomAccessFile` in write mode and invokes `seek(segment.startByte + segment.downloadedBytes)`.
- As bytes arrive over OkHttp stream, they are written directly to the target byte offsets.
- Progress is committed to Room periodically (e.g. every 500ms or 256KB) to avoid database thrashing.

---

## 4. Resume & ETag Invalidation Protocol
When a paused or interrupted download is resumed:
1. Re-issue `HEAD` / probe request to remote server.
2. If `ETag` differs from recorded task `ETag` or `Last-Modified` is newer than recorded timestamp:
   - Server resource has mutated.
   - Existing partial bytes are invalid.
   - The engine automatically resets `downloadedBytes = 0`, deletes the intermediate file, and restarts cleanly to prevent corrupt data corruption.
3. If `ETag` matches:
   - Remaining ranges (`startByte + downloadedBytes .. endByte`) are scheduled immediately.

---

## 5. Speed Calculation & Smoothing
To prevent wildly fluctuating speed meters, Nuvio uses an Exponential Moving Average (EMA) or sliding window of downloaded bytes over the last 1.5 seconds:

$$\text{Speed}_{\text{current}} = \alpha \cdot \text{Speed}_{\text{sample}} + (1 - \alpha) \cdot \text{Speed}_{\text{previous}}$$

Where $\alpha = 0.25$ provides smooth and accurate feedback in KB/s and MB/s.
