package com.nuvio.engine.http

data class RangeProbeResult(
    val supportsRanges: Boolean,
    val totalBytes: Long,
    val etag: String?,
    val lastModified: String?,
    val mimeType: String,
    val suggestedFileName: String?,
    val resolvedUrl: String = ""
)
