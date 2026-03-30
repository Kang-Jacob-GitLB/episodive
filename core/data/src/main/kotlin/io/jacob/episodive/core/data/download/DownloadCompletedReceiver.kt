package io.jacob.episodive.core.data.download

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import dagger.hilt.android.AndroidEntryPoint
import io.jacob.episodive.core.database.datasource.EpisodeLocalDataSource
import io.jacob.episodive.core.model.DownloadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DownloadCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var episodeLocalDataSource: EpisodeLocalDataSource

    @Inject
    lateinit var episodeDownloaderImpl: EpisodeDownloaderImpl

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return

        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (downloadId == -1L) return

        val episodeId = episodeDownloaderImpl.getEpisodeIdForDownload(downloadId) ?: return
        episodeDownloaderImpl.removeDownloadMapping(downloadId)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor: Cursor? = downloadManager.query(query)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                cursor?.use {
                    if (it.moveToFirst()) {
                        val statusIndex = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val bytesIndex = it.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                        when (it.getInt(statusIndex)) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                val totalBytes = if (bytesIndex >= 0) it.getLong(bytesIndex) else 0L
                                episodeLocalDataSource.updateSavedEpisodeProgress(
                                    id = episodeId,
                                    downloadedSize = totalBytes,
                                    status = DownloadStatus.COMPLETED,
                                )
                            }

                            else -> {
                                episodeLocalDataSource.updateSavedEpisodeStatus(
                                    id = episodeId,
                                    status = DownloadStatus.FAILED,
                                )
                            }
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
