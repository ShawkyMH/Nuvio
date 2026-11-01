# Room Database Schema Specification

## 1. Overview
The database engine is built on **AndroidX Room 2.6.1**. It provides an ACID-compliant, persistent relational store for task lifecycles, download segments, playback history, media library cache, and browser records.

Database Name: `nuvio.db`  
Version: `1`

---

## 2. Entities & Schema Definitions

### 2.1 `DownloadTaskEntity` (`download_tasks`)
Stores the primary state of every active, completed, paused, or failed download.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `TEXT` | PRIMARY KEY | UUID identifier for the task |
| `url` | `TEXT` | NOT NULL | Remote resource or extraction URL |
| `title` | `TEXT` | NOT NULL | Display title of the file/media |
| `filePath` | `TEXT` | NOT NULL | Destination local file path (or temp staging path) |
| `mimeType` | `TEXT` | NOT NULL | MIME type (e.g., `video/mp4`, `application/zip`) |
| `totalBytes` | `INTEGER` | NOT NULL | Total expected size in bytes (-1 if unknown) |
| `downloadedBytes` | `INTEGER` | NOT NULL | Bytes downloaded and committed to disk |
| `status` | `TEXT` | NOT NULL | Enum: `CREATED`, `QUEUED`, `INSPECTING`, `DOWNLOADING`, `PAUSED`, `PROCESSING`, `COMPLETED`, `FAILED`, `CANCELLED` |
| `supportsRanges` | `INTEGER` | NOT NULL | Boolean (1 = ranges supported, 0 = single connection) |
| `etag` | `TEXT` | NULLABLE | Remote HTTP ETag for resume validation |
| `lastModified` | `TEXT` | NULLABLE | Remote HTTP Last-Modified header |
| `connectionCount` | `INTEGER` | NOT NULL | Number of parallel connection segments (1..8) |
| `downloadSpeed` | `INTEGER` | NOT NULL | Current speed in bytes/sec |
| `isMediaExtraction` | `INTEGER` | NOT NULL | Boolean (1 = yt-dlp/ffmpeg extraction, 0 = direct HTTP) |
| `formatId` | `TEXT` | NULLABLE | Selected media format/resolution tag (e.g., `1080p`, `mp3`) |
| `thumbnailUri` | `TEXT` | NULLABLE | Cached local or remote thumbnail image URI |
| `errorMessage` | `TEXT` | NULLABLE | Failure description or error code |
| `createdAt` | `INTEGER` | NOT NULL | Epoch timestamp of task creation |
| `completedAt` | `INTEGER` | NULLABLE | Epoch timestamp of task completion |

**Indexes:**
- `index_download_tasks_status` on `status` (for rapid queue polling)
- `index_download_tasks_createdAt` on `createdAt` (for reverse chronological sorting)

---

### 2.2 `DownloadSegmentEntity` (`download_segments`)
Stores the state of individual parallel byte ranges for multi-connection downloads.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `TEXT` | PRIMARY KEY | Segment UUID (`{taskId}_{segmentIndex}`) |
| `taskId` | `TEXT` | NOT NULL, FK -> `download_tasks(id)` ON DELETE CASCADE | Parent task ID |
| `segmentIndex` | `INTEGER` | NOT NULL | Zero-based index of the segment |
| `startByte` | `INTEGER` | NOT NULL | Starting byte offset (inclusive) |
| `endByte` | `INTEGER` | NOT NULL | Ending byte offset (inclusive) |
| `downloadedBytes` | `INTEGER` | NOT NULL | Bytes written within this specific segment |
| `status` | `TEXT` | NOT NULL | Enum: `PENDING`, `DOWNLOADING`, `COMPLETED`, `FAILED` |

**Indexes:**
- `index_download_segments_taskId` on `taskId`

---

### 2.3 `PlaybackHistoryEntity` (`playback_history`)
Tracks media playback state, position, and duration for resume playback.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `TEXT` | PRIMARY KEY | UUID or file URI hash |
| `mediaUri` | `TEXT` | NOT NULL, UNIQUE | File path or content URI of the media item |
| `title` | `TEXT` | NOT NULL | Track or video title |
| `artist` | `TEXT` | NULLABLE | Artist or channel name |
| `durationMs` | `INTEGER` | NOT NULL | Total track duration in milliseconds |
| `playbackPositionMs` | `INTEGER` | NOT NULL | Last playback position in milliseconds |
| `isVideo` | `INTEGER` | NOT NULL | Boolean (1 = video, 0 = audio) |
| `lastPlayedAt` | `INTEGER` | NOT NULL | Epoch timestamp |

---

### 2.4 `BrowserBookmarkEntity` (`browser_bookmarks`)
Stores user saved bookmarks.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `TEXT` | PRIMARY KEY | UUID |
| `title` | `TEXT` | NOT NULL | Page title |
| `url` | `TEXT` | NOT NULL | Target URL |
| `faviconUri` | `TEXT` | NULLABLE | Favicon local cached URI |
| `createdAt` | `INTEGER` | NOT NULL | Timestamp |

---

### 2.5 `BrowserHistoryEntity` (`browser_history`)
Stores browsed web pages.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `TEXT` | PRIMARY KEY | UUID |
| `title` | `TEXT` | NOT NULL | Page title |
| `url` | `TEXT` | NOT NULL | Visited URL |
| `visitedAt` | `INTEGER` | NOT NULL | Timestamp |

---

## 3. Migration Strategy
1. **Destructive Migrations Prohibited:** `fallbackToDestructiveMigration()` is strictly disabled for production releases.
2. **Explicit Migrations:** Any change to table schemas will be represented as a discrete `Migration(from, to)` class accompanied by an automated Room migration test.
