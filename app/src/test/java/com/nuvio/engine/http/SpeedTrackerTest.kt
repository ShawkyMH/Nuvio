package com.nuvio.engine.http

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedTrackerTest {

    @Test
    fun testInitialSpeedIsZero() {
        val tracker = SpeedTracker()
        tracker.reset(0L)
        assertEquals(0L, tracker.getCurrentSpeed())
    }

    @Test
    fun testSpeedSmoothingUnderUpdates() {
        val tracker = SpeedTracker(alpha = 0.5f)
        tracker.reset(0L)

        // Simulated updates
        val speed1 = tracker.update(0L)
        assertEquals(0L, speed1)
    }
}
