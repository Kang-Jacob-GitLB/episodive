package io.jacob.episodive.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EpisodeSyncSchedulerTest {

    private lateinit var context: Context
    private lateinit var scheduler: EpisodeSyncScheduler

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        scheduler = EpisodeSyncScheduler(context)
    }

    @Test
    fun `When schedule is called, Then periodic work is enqueued`() = runTest {
        scheduler.schedule()

        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(EpisodeSyncScheduler.WORK_NAME)
            .get()

        assertTrue(workInfos.isNotEmpty())
        assertEquals(WorkInfo.State.ENQUEUED, workInfos[0].state)
    }

    @Test
    fun `When schedule is called twice, Then only one work is enqueued`() = runTest {
        scheduler.schedule()
        scheduler.schedule()

        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(EpisodeSyncScheduler.WORK_NAME)
            .get()

        assertEquals(1, workInfos.size)
    }
}
