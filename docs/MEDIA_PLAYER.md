# Built-in Media Player Architecture (AndroidX Media3)

## 1. Engine Foundation
Nuvio utilizes **AndroidX Media3 (ExoPlayer 1.3.1)** for all video and audio rendering. Media3 is the official Android media framework, replacing the legacy standalone ExoPlayer 2.x and providing tight integration with system `MediaSession`.

---

## 2. Video Playback Experience

### 2.1 UI Layer
- Built using Jetpack Compose with an embedded `AndroidView(factory = { PlayerView(it) })`.
- Custom overlay controls styled after Google Photos / YouTube:
  - Minimalistic pill-shaped control bar.
  - Large center play/pause/loading indicator with smooth alpha transitions.
  - Bottom timeline scrubber with current time and duration labels.
  - Top action bar with back navigation, aspect ratio toggle (Fit, Crop, Stretch, 16:9), and overflow menu for playback speed (0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 2.0x) and subtitle selection.

### 2.2 Touch Gesture Controls
- **Left Vertical Swipe:** Adjusts display brightness via `Activity.window.attributes.screenBrightness`.
- **Right Vertical Swipe:** Adjusts stream audio volume via `AudioManager.setStreamVolume`.
- **Horizontal Swipe:** Fast scrub / seek through the media timeline.
- **Double Tap Left / Right:** Quick 10-second skip backward / forward.

### 2.3 Picture-in-Picture (PiP)
- Triggers automatically when the user leaves the app (`Activity.onUserLeaveHint()`) if video playback is active.
- Configured using `PictureInPictureParams.Builder` with source rect hints and remote media actions (Play, Pause, Skip).

---

## 3. Audio Playback Experience

### 3.1 MediaSession & Background Playback
- Managed via a dedicated `MediaSessionService` (`NuvioPlaybackService`).
- Publishes system media notifications (`MediaStyleNotification`) showing track title, artist, album art, play/pause, seek, and close controls.
- Integrates with Android lock screen media controls, Bluetooth headsets, and Android Auto.
- Manages audio focus transitions (ducking on notification sounds, pausing on incoming phone calls).

### 3.2 Queue & State Persistence
- Maintains current playing queue in memory with next/previous track navigation.
- Persists last played track and position in Room `playback_history` to allow seamless "Continue Listening" on app launch.
