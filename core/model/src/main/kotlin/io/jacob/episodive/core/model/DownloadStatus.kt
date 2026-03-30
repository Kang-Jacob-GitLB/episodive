package io.jacob.episodive.core.model

enum class DownloadStatus(val value: String) {
    PENDING("pending"),
    DOWNLOADING("downloading"),
    COMPLETED("completed"),
    FAILED("failed");
}

fun String.toDownloadStatus(): DownloadStatus? =
    DownloadStatus.entries.find { it.value == this }
