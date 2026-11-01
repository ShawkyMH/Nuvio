package com.nuvio.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskStateMachineTest {

    @Test
    fun testTaskStatus_activeStates() {
        assertTrue(TaskStatus.QUEUED.isActive)
        assertTrue(TaskStatus.INSPECTING.isActive)
        assertTrue(TaskStatus.DOWNLOADING.isActive)
        assertTrue(TaskStatus.FINALIZING.isActive)
        assertTrue(TaskStatus.VALIDATING.isActive)
        assertTrue(TaskStatus.PROCESSING.isActive)

        assertFalse(TaskStatus.CREATED.isActive)
        assertFalse(TaskStatus.PAUSED.isActive)
        assertFalse(TaskStatus.COMPLETED.isActive)
        assertFalse(TaskStatus.FAILED.isActive)
        assertFalse(TaskStatus.CANCELLED.isActive)
    }

    @Test
    fun testTaskStatus_terminalStates() {
        assertTrue(TaskStatus.COMPLETED.isTerminal)
        assertTrue(TaskStatus.FAILED.isTerminal)
        assertTrue(TaskStatus.CANCELLED.isTerminal)

        assertFalse(TaskStatus.QUEUED.isTerminal)
        assertFalse(TaskStatus.DOWNLOADING.isTerminal)
        assertFalse(TaskStatus.PAUSED.isTerminal)
    }

    @Test
    fun testDownloadTaskProgressCalculation() {
        val task = DownloadTask(
            id = "test",
            url = "https://example.com/file.zip",
            title = "file.zip",
            filePath = "/storage/file.zip",
            totalBytes = 1000L,
            downloadedBytes = 450L
        )

        assertTrue(task.progress > 0.44f && task.progress < 0.46f)
        assertTrue(task.progressPercent == 45)
    }
}
