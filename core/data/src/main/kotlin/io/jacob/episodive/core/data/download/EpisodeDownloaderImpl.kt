package io.jacob.episodive.core.data.download

import android.app.DownloadManager
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import io.jacob.episodive.core.domain.download.EpisodeDownloader
import io.jacob.episodive.core.model.Episode
import java.io.File

class EpisodeDownloaderImpl(
    private val context: Context,
) : EpisodeDownloader {

    private val downloadManager: DownloadManager
        get() = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private val prefs: SharedPreferences
        get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun downloadEpisode(episode: Episode, filePath: String): Long {
        val uri = Uri.parse(episode.enclosureUrl)
        val request = DownloadManager.Request(uri).apply {
            setTitle(episode.title)
            setDescription(episode.feedTitle ?: "")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(false)

            val dir = getDownloadDirectory()
            val file = File(dir, filePath)
            file.parentFile?.mkdirs()
            setDestinationUri(Uri.fromFile(file))
        }

        val downloadId = downloadManager.enqueue(request)
        prefs.edit().putLong(downloadId.toString(), episode.id).apply()
        return downloadId
    }

    override fun cancelDownload(downloadId: Long) {
        downloadManager.remove(downloadId)
        prefs.edit().remove(downloadId.toString()).apply()
    }

    override fun deleteDownloadedFile(filePath: String) {
        val dir = getDownloadDirectory()
        val file = File(dir, filePath)
        if (file.exists()) {
            file.delete()
        }
    }

    override fun getDownloadDirectory(): String {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_PODCASTS)
            ?: context.filesDir.resolve("Podcasts")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir.absolutePath
    }

    fun getEpisodeIdForDownload(downloadId: Long): Long? {
        val episodeId = prefs.getLong(downloadId.toString(), -1L)
        return if (episodeId == -1L) null else episodeId
    }

    fun removeDownloadMapping(downloadId: Long) {
        prefs.edit().remove(downloadId.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "episodive_download_mappings"
    }
}
