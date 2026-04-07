package io.jacob.episodive.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.jacob.episodive.core.domain.usecase.episode.SyncNewEpisodesUseCase
import timber.log.Timber

@HiltWorker
class EpisodeSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncNewEpisodesUseCase: SyncNewEpisodesUseCase,
    private val notificationHelper: EpisodeSyncNotificationHelper,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val results = syncNewEpisodesUseCase()
            if (results.isNotEmpty()) {
                notificationHelper.showNewEpisodeNotification(results)
            }
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Episode sync worker failed")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
