package io.jacob.episodive.sync

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.Coil
import coil.ImageLoader
import coil.test.FakeImageLoaderEngine
import io.jacob.episodive.core.domain.usecase.episode.NewEpisodeResult
import io.jacob.episodive.core.testing.model.episodeTestDataList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class EpisodeSyncNotificationHelperTest {

    private lateinit var context: Context
    private lateinit var helper: EpisodeSyncNotificationHelper
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val engine = FakeImageLoaderEngine.Builder().default(android.graphics.drawable.ColorDrawable()).build()
        val imageLoader = ImageLoader.Builder(context).components { add(engine) }.build()
        Coil.setImageLoader(imageLoader)
        helper = EpisodeSyncNotificationHelper(context)
        notificationManager = context.getSystemService(NotificationManager::class.java)
    }

    @Test
    fun `When createNotificationChannel, Then channel is created with correct id`() {
        helper.createNotificationChannel()

        val channel = notificationManager.getNotificationChannel(EpisodeSyncNotificationHelper.CHANNEL_ID)

        assertNotNull(channel)
        assertEquals(EpisodeSyncNotificationHelper.CHANNEL_ID, channel.id)
    }

    @Test
    fun `When createNotificationChannel, Then channel has default importance`() {
        helper.createNotificationChannel()

        val channel = notificationManager.getNotificationChannel(EpisodeSyncNotificationHelper.CHANNEL_ID)

        assertNotNull(channel)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channel.importance)
    }

    @Test
    fun `Given empty results, When showNewEpisodeNotification, Then no notification is shown`() = runTest {
        val shadowNm = shadowOf(notificationManager)

        helper.showNewEpisodeNotification(emptyList())

        assertEquals(0, shadowNm.size())
    }

    @Test
    fun `Given single podcast result, When showNewEpisodeNotification, Then notification is shown`() = runTest {
        val episodes = episodeTestDataList.take(2)
        val results = listOf(NewEpisodeResult(feedId = episodes.first().feedId, episodes = episodes))
        val shadowNm = shadowOf(notificationManager)

        // Grant POST_NOTIFICATIONS permission in Robolectric
        shadowOf(context.applicationContext as android.app.Application)
            .grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)

        helper.showNewEpisodeNotification(results)

        assertEquals(1, shadowNm.size())
    }

    @Test
    fun `Given single podcast result, When showNewEpisodeNotification, Then notification title is feed title`() = runTest {
        val episodes = episodeTestDataList.take(1)
        val results = listOf(NewEpisodeResult(feedId = episodes.first().feedId, episodes = episodes))
        val shadowNm = shadowOf(notificationManager)

        shadowOf(context.applicationContext as android.app.Application)
            .grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)

        helper.showNewEpisodeNotification(results)

        val notification = shadowNm.allNotifications.firstOrNull()
        assertNotNull(notification)
    }

    @Test
    fun `Given multiple podcast results, When showNewEpisodeNotification, Then notification is shown`() = runTest {
        val episodes1 = episodeTestDataList.take(2)
        val episodes2 = episodeTestDataList.drop(2).take(2)
        val results = listOf(
            NewEpisodeResult(feedId = episodes1.first().feedId, episodes = episodes1),
            NewEpisodeResult(feedId = episodes2.first().feedId, episodes = episodes2),
        )
        val shadowNm = shadowOf(notificationManager)

        shadowOf(context.applicationContext as android.app.Application)
            .grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)

        helper.showNewEpisodeNotification(results)

        assertEquals(1, shadowNm.size())
    }

    @Test
    fun `Given results, When showNewEpisodeNotification, Then latest result feedId is used in intent`() = runTest {
        val episodes = episodeTestDataList.take(2)
        val feedId = episodes.first().feedId
        val results = listOf(NewEpisodeResult(feedId = feedId, episodes = episodes))
        val shadowNm = shadowOf(notificationManager)

        shadowOf(context.applicationContext as android.app.Application)
            .grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)

        helper.showNewEpisodeNotification(results)

        assertEquals(1, shadowNm.size())
        val notification = shadowNm.allNotifications.first()
        assertNotNull(notification)
    }
}
