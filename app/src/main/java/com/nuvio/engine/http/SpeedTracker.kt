package com.nuvio.engine.http

class SpeedTracker(private val alpha: Float = 0.25f) {

    private var lastTimestamp: Long = 0L
    private var lastBytes: Long = 0L
    private var smoothedSpeed: Long = 0L

    @Synchronized
    fun reset(initialBytes: Long) {
        lastTimestamp = System.currentTimeMillis()
        lastBytes = initialBytes
        smoothedSpeed = 0L
    }

    @Synchronized
    fun update(currentTotalBytes: Long): Long {
        val now = System.currentTimeMillis()
        if (lastTimestamp == 0L) {
            lastTimestamp = now
            lastBytes = currentTotalBytes
            return 0L
        }

        val timeDeltaMs = now - lastTimestamp
        if (timeDeltaMs >= 500) {
            val bytesDelta = (currentTotalBytes - lastBytes).coerceAtLeast(0)
            val instantaneousSpeed = (bytesDelta * 1000) / timeDeltaMs

            smoothedSpeed = if (smoothedSpeed == 0L) {
                instantaneousSpeed
            } else {
                ((alpha * instantaneousSpeed) + ((1f - alpha) * smoothedSpeed)).toLong()
            }

            lastTimestamp = now
            lastBytes = currentTotalBytes
        }
        return smoothedSpeed
    }

    @Synchronized
    fun getCurrentSpeed(): Long = smoothedSpeed
}
