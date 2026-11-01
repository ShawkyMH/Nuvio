package com.nuvio.engine.http

import com.nuvio.domain.model.DownloadSegment
import com.nuvio.domain.model.SegmentStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SegmentMathTest {

    @Test
    fun testSegmentCalculation_evenDivision() {
        val totalBytes = 1000L
        val connections = 4
        val segmentSize = totalBytes / connections

        val segments = mutableListOf<DownloadSegment>()
        for (i in 0 until connections) {
            val start = i * segmentSize
            val end = if (i == connections - 1) totalBytes - 1 else (start + segmentSize - 1)
            segments.add(
                DownloadSegment(
                    id = "test_$i",
                    taskId = "task1",
                    segmentIndex = i,
                    startByte = start,
                    endByte = end,
                    status = SegmentStatus.PENDING
                )
            )
        }

        assertEquals(4, segments.size)
        assertEquals(0L, segments[0].startByte)
        assertEquals(249L, segments[0].endByte)
        assertEquals(250L, segments[0].totalBytes)

        assertEquals(250L, segments[1].startByte)
        assertEquals(499L, segments[1].endByte)

        assertEquals(500L, segments[2].startByte)
        assertEquals(749L, segments[2].endByte)

        assertEquals(750L, segments[3].startByte)
        assertEquals(999L, segments[3].endByte)

        val totalCalculated = segments.sumOf { it.totalBytes }
        assertEquals(totalBytes, totalCalculated)
    }

    @Test
    fun testSegmentCalculation_unevenDivision() {
        val totalBytes = 103L
        val connections = 4
        val segmentSize = totalBytes / connections // 25

        val segments = mutableListOf<DownloadSegment>()
        for (i in 0 until connections) {
            val start = i * segmentSize
            val end = if (i == connections - 1) totalBytes - 1 else (start + segmentSize - 1)
            segments.add(
                DownloadSegment(
                    id = "test_$i",
                    taskId = "task2",
                    segmentIndex = i,
                    startByte = start,
                    endByte = end,
                    status = SegmentStatus.PENDING
                )
            )
        }

        assertEquals(0L, segments[0].startByte)
        assertEquals(24L, segments[0].endByte) // 25 bytes
        assertEquals(25L, segments[1].startByte)
        assertEquals(49L, segments[1].endByte) // 25 bytes
        assertEquals(50L, segments[2].startByte)
        assertEquals(74L, segments[2].endByte) // 25 bytes
        assertEquals(75L, segments[3].startByte)
        assertEquals(102L, segments[3].endByte) // 28 bytes (includes remainder)

        val totalCalculated = segments.sumOf { it.totalBytes }
        assertEquals(totalBytes, totalCalculated)
    }

    @Test
    fun testSingleConnection_spansEntireFile() {
        val totalBytes = 5000000L
        val segment = DownloadSegment(
            id = "single_0",
            taskId = "task3",
            segmentIndex = 0,
            startByte = 0L,
            endByte = totalBytes - 1,
            status = SegmentStatus.PENDING
        )

        assertEquals(0L, segment.startByte)
        assertEquals(4999999L, segment.endByte)
        assertEquals(totalBytes, segment.totalBytes)
    }
}
