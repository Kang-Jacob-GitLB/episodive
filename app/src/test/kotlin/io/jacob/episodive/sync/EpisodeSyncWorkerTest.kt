package io.jacob.episodive.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import io.jacob.episodive.core.data.widget.WidgetUpdater
import io.jacob.episodive.core.domain.usecase.episode.NewEpisodeResult
import io.jacob.episodive.core.domain.usecase.episode.SyncNewEpisodesUseCase
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EpisodeSyncWorkerTest {

    private lateinit var context: Context
    private val syncNewEpisodesUseCase = mockk<SyncNewEpisodesUseCase>()
    private val notificationHelper = mockk<EpisodeSyncNotificationHelper>(relaxed = true)
    private val widgetUpdater = mockk<WidgetUpdater>(relaxed = true)

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun createWorker(): EpisodeSyncWorker {
        return TestListenableWorkerBuilder<EpisodeSyncWorker>(context)
            .setWorkerFactory(
                TestWorkerFactory(syncNewEpisodesUseCase, notificationHelper, widgetUpdater),
            )
            .build() as EpisodeSyncWorker
    }

    @Test
    fun `Given new episodes, when doWork, then returns success and notifies widget`() = runTest {
        val results = listOf(NewEpisodeResult(5778530L, episodeTestDataList.take(2)))
        coEvery { syncNewEpisodesUseCase() } returns results

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(Result.success(), result)
        coVerify(exactly = 1) { notificationHelper.showNewEpisodeNotification(results) }
        verify(exactly = 1) { widgetUpdater.notifyRecentEpisodesChanged() }
    }

    @Test
    fun `Given no new episodes, when doWork, then returns success without notification or widget update`() = runTest {
        coEvery { syncNewEpisodesUseCase() } returns emptyList()

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(Result.success(), result)
        coVerify(exactly = 0) { notificationHelper.showNewEpisodeNotification(any()) }
        verify(exactly = 0) { widgetUpdater.notifyRecentEpisodesChanged() }
    }

    @Test
    fun `Given sync failure, when doWork, then returns retry and does not notify widget`() = runTest {
        coEvery { syncNewEpisodesUseCase() } throws RuntimeException("Network error")

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(Result.retry(), result)
        verify(exactly = 0) { widgetUpdater.notifyRecentEpisodesChanged() }
    }
}

private class TestWorkerFactory(
    private val syncNewEpisodesUseCase: SyncNewEpisodesUseCase,
    private val notificationHelper: EpisodeSyncNotificationHelper,
    private val widgetUpdater: WidgetUpdater,
) : androidx.work.WorkerFactory() {
    override fun createWorker(
        appContext: android.content.Context,
        workerClassName: String,
        workerParameters: androidx.work.WorkerParameters,
    ): androidx.work.ListenableWorker {
        return EpisodeSyncWorker(
            context = appContext,
            workerParams = workerParameters,
            syncNewEpisodesUseCase = syncNewEpisodesUseCase,
            notificationHelper = notificationHelper,
            widgetUpdater = widgetUpdater,
        )
    }
}
