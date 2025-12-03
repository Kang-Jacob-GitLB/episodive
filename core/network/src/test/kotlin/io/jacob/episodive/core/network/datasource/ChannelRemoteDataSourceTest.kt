package io.jacob.episodive.core.network.datasource

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChannelRemoteDataSourceTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var dataSource: ChannelRemoteDataSource

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataSource = ChannelRemoteDataSourceImpl(
            context = context,
            ioDispatcher = mainDispatcherRule.testDispatcher
        )
    }

    @Test
    fun getChannels_returnsChannelListFromAsset() = runTest {
        // When
        val channels = dataSource.getChannels()

        // Then
        assertTrue(channels.isNotEmpty())
        assertEquals(14, channels.size)
    }

    @Test
    fun getChannels_firstChannelHasCorrectData() = runTest {
        // When
        val channels = dataSource.getChannels()

        // Then
        val firstChannel = channels.first()
        assertEquals(1, firstChannel.id)
        assertEquals("CNN Podcasts", firstChannel.title)
        assertEquals("Exclusive stories and the latest headlines.", firstChannel.description)
        assertEquals(
            "https://edition.cnn.com/audio/static/images/live/CNN-m.4945681d.png",
            firstChannel.image
        )
        assertEquals("https://edition.cnn.com/audio", firstChannel.link)
        assertEquals(12, firstChannel.count)
        assertEquals(12, firstChannel.podcastGuids.size)
        assertEquals("ed51d808-f6a0-5586-92f1-0cfa4dc6609a", firstChannel.podcastGuids.first())
    }

    @Test
    fun getChannels_allChannelsHaveRequiredFields() = runTest {
        // When
        val channels = dataSource.getChannels()

        // Then
        channels.forEach { channel ->
            assertTrue(channel.id > 0)
            assertTrue(channel.title.isNotEmpty())
            assertTrue(channel.description.isNotEmpty())
            assertTrue(channel.image.isNotEmpty())
            assertTrue(channel.link.isNotEmpty())
            assertTrue(channel.count > 0)
            assertTrue(channel.podcastGuids.isNotEmpty())
            assertEquals(channel.count, channel.podcastGuids.size)
        }
    }

    @Test
    fun getChannels_containsExpectedChannels() = runTest {
        // When
        val channels = dataSource.getChannels()

        // Then
        val channelTitles = channels.map { it.title }
        assertTrue(channelTitles.contains("CNN Podcasts"))
        assertTrue(channelTitles.contains("뇌부자들"))
        assertTrue(channelTitles.contains("Netflix Podcasts"))
        assertTrue(channelTitles.contains("National Geographic"))
        assertTrue(channelTitles.contains("BBC"))
        assertTrue(channelTitles.contains("SBS"))
        assertTrue(channelTitles.contains("KBS"))
        assertTrue(channelTitles.contains("MBC"))
        assertTrue(channelTitles.contains("JTBC"))
        assertTrue(channelTitles.contains("YTN"))
        assertTrue(channelTitles.contains("CNBC"))
        assertTrue(channelTitles.contains("The New York Times"))
        assertTrue(channelTitles.contains("FOX News Podcasts"))
    }

    @Test
    fun getChannels_podcastGuidsAreValidUuids() = runTest {
        // When
        val channels = dataSource.getChannels()

        // Then
        val uuidRegex = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
        channels.forEach { channel ->
            channel.podcastGuids.forEach { guid ->
                assertTrue(guid.matches(uuidRegex))
            }
        }
    }
}