package io.jacob.episodive.core.domain.download

import io.jacob.episodive.core.model.Episode

interface EpisodeDownloader {
    fun downloadEpisode(episode: Episode, filePath: String): Long
    fun cancelDownload(downloadId: Long)
    fun deleteDownloadedFile(filePath: String)
    fun getDownloadDirectory(): String
}
