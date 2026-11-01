package com.nuvio.domain.model

enum class TaskStatus {
    CREATED,
    QUEUED,
    INSPECTING,
    DOWNLOADING,
    PAUSED,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED;

    val isActive: Boolean
        get() = this == QUEUED || this == INSPECTING || this == DOWNLOADING || this == PROCESSING

    val isPaused: Boolean
        get() = this == PAUSED

    val isTerminal: Boolean
        get() = this == COMPLETED || this == FAILED || this == CANCELLED
}

enum class SegmentStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED
}
