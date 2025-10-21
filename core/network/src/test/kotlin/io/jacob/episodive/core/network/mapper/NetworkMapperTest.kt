package io.jacob.episodive.core.network.mapper

import android.os.Build
import io.jacob.episodive.core.model.Transcript
import io.jacob.episodive.core.network.model.EpisodeResponse
import io.jacob.episodive.core.network.model.PodcastResponse
import io.jacob.episodive.core.network.model.TranscriptResponse
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.podcastTestData
import io.jacob.episodive.core.testing.model.podcastTestDataList
import io.jacob.episodive.core.testing.model.recentFeedTestData
import io.jacob.episodive.core.testing.model.recentFeedTestDataList
import io.jacob.episodive.core.testing.model.recentNewFeedTestData
import io.jacob.episodive.core.testing.model.recentNewFeedTestDataList
import io.jacob.episodive.core.testing.model.recentNewValueFeedTestData
import io.jacob.episodive.core.testing.model.recentNewValueFeedTestDataList
import io.jacob.episodive.core.testing.model.soundbiteTestData
import io.jacob.episodive.core.testing.model.soundbiteTestDataList
import io.jacob.episodive.core.testing.model.trendingFeedTestData
import io.jacob.episodive.core.testing.model.trendingFeedTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class NetworkMapperTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `toPodcast converts PodcastResponse to Podcast correctly`() {
        // Given
        val podcastResponse = podcastTestData.toPodcastResponse()

        // When
        val podcast = podcastResponse.toPodcast()

        // Then
        assertEquals(podcastTestData.id, podcast.id)
        assertEquals(podcastTestData.title, podcast.title)
        assertEquals(podcastTestData.url, podcast.url)
        assertEquals(podcastTestData.author, podcast.author)
        assertEquals(podcastTestData.medium, podcast.medium)
        assertEquals(podcastTestData.categories, podcast.categories)
        assertNotNull(podcast.description)
    }

    @Test
    fun `toPodcasts converts list of PodcastResponse to list of Podcast correctly`() {
        // Given
        val podcastResponses = podcastTestDataList.toPodcastResponses()

        // When
        val podcasts = podcastResponses.toPodcasts()

        // Then
        assertEquals(podcastTestDataList.size, podcasts.size)
        assertEquals(podcastTestDataList.first().id, podcasts.first().id)
        assertEquals(podcastTestDataList.last().id, podcasts.last().id)
    }

    @Test
    fun `toPodcastResponse converts Podcast to PodcastResponse correctly`() {
        // When
        val podcastResponse = podcastTestData.toPodcastResponse()

        // Then
        assertEquals(podcastTestData.id, podcastResponse.id)
        assertEquals(podcastTestData.title, podcastResponse.title)
        assertEquals(podcastTestData.url, podcastResponse.url)
        assertEquals(podcastTestData.author, podcastResponse.author)
        assertEquals(podcastTestData.medium?.value, podcastResponse.medium)
        assertEquals(podcastTestData.lastUpdateTime.epochSeconds, podcastResponse.lastUpdateTime)
    }

    @Test
    fun `toPodcastResponses converts list of Podcast to list of PodcastResponse correctly`() {
        // When
        val podcastResponses = podcastTestDataList.toPodcastResponses()

        // Then
        assertEquals(podcastTestDataList.size, podcastResponses.size)
        assertEquals(podcastTestDataList.first().id, podcastResponses.first().id)
        assertEquals(podcastTestDataList.last().id, podcastResponses.last().id)
    }

    @Test
    fun `toEpisode converts EpisodeResponse to Episode correctly`() {
        // Given
        val episodeResponse = episodeTestData.toEpisodeResponse()

        // When
        val episode = episodeResponse.toEpisode()

        // Then
        assertEquals(episodeTestData.id, episode.id)
        assertEquals(episodeTestData.title, episode.title)
        assertEquals(episodeTestData.link, episode.link)
        assertEquals(episodeTestData.guid, episode.guid)
        assertEquals(episodeTestData.datePublished, episode.datePublished)
        assertEquals(episodeTestData.duration, episode.duration)
        assertEquals(episodeTestData.explicit, episode.explicit)
        assertEquals(episodeTestData.episodeType, episode.episodeType)
        assertNotNull(episode.description)
    }

    @Test
    fun `toEpisodes converts list of EpisodeResponse to list of Episode correctly`() {
        // Given
        val episodeResponses = episodeTestDataList.toEpisodeResponses()

        // When
        val episodes = episodeResponses.toEpisodes()

        // Then
        assertEquals(episodeTestDataList.size, episodes.size)
        assertEquals(episodeTestDataList.first().id, episodes.first().id)
        assertEquals(episodeTestDataList.last().id, episodes.last().id)
    }

    @Test
    fun `toEpisodeResponse converts Episode to EpisodeResponse correctly`() {
        // When
        val episodeResponse = episodeTestData.toEpisodeResponse()

        // Then
        assertEquals(episodeTestData.id, episodeResponse.id)
        assertEquals(episodeTestData.title, episodeResponse.title)
        assertEquals(episodeTestData.link, episodeResponse.link)
        assertEquals(episodeTestData.guid, episodeResponse.guid)
        assertEquals(episodeTestData.datePublished.epochSeconds, episodeResponse.datePublished)
        assertEquals(episodeTestData.duration?.inWholeSeconds?.toInt(), episodeResponse.duration)
        assertEquals(if (episodeTestData.explicit) 1 else 0, episodeResponse.explicit)
        assertEquals(episodeTestData.episodeType?.value, episodeResponse.episodeType)
    }

    @Test
    fun `toEpisodeResponses converts list of Episode to list of EpisodeResponse correctly`() {
        // When
        val episodeResponses = episodeTestDataList.toEpisodeResponses()

        // Then
        assertEquals(episodeTestDataList.size, episodeResponses.size)
        assertEquals(episodeTestDataList.first().id, episodeResponses.first().id)
        assertEquals(episodeTestDataList.last().id, episodeResponses.last().id)
    }

    @Test
    fun `toTrendingFeed converts TrendingFeedResponse to TrendingFeed correctly`() {
        // Given
        val trendingFeedResponse = trendingFeedTestData.toTrendingFeedResponse()

        // When
        val trendingFeed = trendingFeedResponse.toTrendingFeed()

        // Then
        assertEquals(trendingFeedTestData.id, trendingFeed.id)
        assertEquals(trendingFeedTestData.url, trendingFeed.url)
        assertEquals(trendingFeedTestData.title, trendingFeed.title)
        assertEquals(trendingFeedTestData.author, trendingFeed.author)
        assertEquals(trendingFeedTestData.image, trendingFeed.image)
        assertEquals(trendingFeedTestData.artwork, trendingFeed.artwork)
        assertEquals(trendingFeedTestData.trendScore, trendingFeed.trendScore)
        assertEquals(trendingFeedTestData.language, trendingFeed.language)
        assertEquals(trendingFeedTestData.categories, trendingFeed.categories)
        assertNotNull(trendingFeed.description)
    }

    @Test
    fun `toTrendingFeeds converts list of TrendingFeedResponse to list of TrendingFeed correctly`() {
        // Given
        val trendingFeedResponses = trendingFeedTestDataList.toTrendingFeedResponses()

        // When
        val trendingFeeds = trendingFeedResponses.toTrendingFeeds()

        // Then
        assertEquals(trendingFeedTestDataList.size, trendingFeeds.size)
        assertEquals(trendingFeedTestDataList.first().id, trendingFeeds.first().id)
        assertEquals(trendingFeedTestDataList.last().id, trendingFeeds.last().id)
    }

    @Test
    fun `toTrendingFeedResponse converts TrendingFeed to TrendingFeedResponse correctly`() {
        // When
        val trendingFeedResponse = trendingFeedTestData.toTrendingFeedResponse()

        // Then
        assertEquals(trendingFeedTestData.id, trendingFeedResponse.id)
        assertEquals(trendingFeedTestData.url, trendingFeedResponse.url)
        assertEquals(trendingFeedTestData.title, trendingFeedResponse.title)
        assertEquals(trendingFeedTestData.description, trendingFeedResponse.description)
        assertEquals(trendingFeedTestData.author, trendingFeedResponse.author)
        assertEquals(trendingFeedTestData.image, trendingFeedResponse.image)
        assertEquals(trendingFeedTestData.artwork, trendingFeedResponse.artwork)
        assertEquals(trendingFeedTestData.newestItemPublishTime.epochSeconds, trendingFeedResponse.newestItemPublishTime)
        assertEquals(trendingFeedTestData.trendScore, trendingFeedResponse.trendScore)
        assertEquals(trendingFeedTestData.language, trendingFeedResponse.language)
    }

    @Test
    fun `toTrendingFeedResponses converts list of TrendingFeed to list of TrendingFeedResponse correctly`() {
        // When
        val trendingFeedResponses = trendingFeedTestDataList.toTrendingFeedResponses()

        // Then
        assertEquals(trendingFeedTestDataList.size, trendingFeedResponses.size)
        assertEquals(trendingFeedTestDataList.first().id, trendingFeedResponses.first().id)
        assertEquals(trendingFeedTestDataList.last().id, trendingFeedResponses.last().id)
    }

    @Test
    fun `toRecentFeed converts RecentFeedResponse to RecentFeed correctly`() {
        // Given
        val recentFeedResponse = recentFeedTestData.toRecentFeedResponse()

        // When
        val recentFeed = recentFeedResponse.toRecentFeed()

        // Then
        assertEquals(recentFeedTestData.id, recentFeed.id)
        assertEquals(recentFeedTestData.url, recentFeed.url)
        assertEquals(recentFeedTestData.title, recentFeed.title)
        assertEquals(recentFeedTestData.author, recentFeed.author)
        assertEquals(recentFeedTestData.newestItemPublishTime, recentFeed.newestItemPublishTime)
        assertEquals(recentFeedTestData.oldestItemPublishTime, recentFeed.oldestItemPublishTime)
        assertEquals(recentFeedTestData.itunesId, recentFeed.itunesId)
        assertEquals(recentFeedTestData.trendScore, recentFeed.trendScore)
        assertEquals(recentFeedTestData.language, recentFeed.language)
        assertEquals(recentFeedTestData.categories, recentFeed.categories)
        assertNotNull(recentFeed.description)
    }

    @Test
    fun `toRecentFeeds converts list of RecentFeedResponse to list of RecentFeed correctly`() {
        // Given
        val recentFeedResponses = recentFeedTestDataList.toRecentFeedResponses()

        // When
        val recentFeeds = recentFeedResponses.toRecentFeeds()

        // Then
        assertEquals(recentFeedTestDataList.size, recentFeeds.size)
        assertEquals(recentFeedTestDataList.first().id, recentFeeds.first().id)
        assertEquals(recentFeedTestDataList.last().id, recentFeeds.last().id)
    }

    @Test
    fun `toRecentFeedResponse converts RecentFeed to RecentFeedResponse correctly`() {
        // When
        val recentFeedResponse = recentFeedTestData.toRecentFeedResponse()

        // Then
        assertEquals(recentFeedTestData.id, recentFeedResponse.id)
        assertEquals(recentFeedTestData.url, recentFeedResponse.url)
        assertEquals(recentFeedTestData.title, recentFeedResponse.title)
        assertEquals(recentFeedTestData.description, recentFeedResponse.description)
        assertEquals(recentFeedTestData.author, recentFeedResponse.author)
        assertEquals(recentFeedTestData.newestItemPublishTime.epochSeconds, recentFeedResponse.newestItemPublishTime)
        assertEquals(recentFeedTestData.oldestItemPublishTime?.epochSeconds, recentFeedResponse.oldestItemPublishTime)
        assertEquals(recentFeedTestData.itunesId, recentFeedResponse.itunesId)
        assertEquals(recentFeedTestData.trendScore, recentFeedResponse.trendScore)
        assertEquals(recentFeedTestData.language, recentFeedResponse.language)
    }

    @Test
    fun `toRecentFeedResponses converts list of RecentFeed to list of RecentFeedResponse correctly`() {
        // When
        val recentFeedResponses = recentFeedTestDataList.toRecentFeedResponses()

        // Then
        assertEquals(recentFeedTestDataList.size, recentFeedResponses.size)
        assertEquals(recentFeedTestDataList.first().id, recentFeedResponses.first().id)
        assertEquals(recentFeedTestDataList.last().id, recentFeedResponses.last().id)
    }

    @Test
    fun `toRecentNewFeed converts RecentNewFeedResponse to RecentNewFeed correctly`() {
        // Given
        val recentNewFeedResponse = recentNewFeedTestData.toRecentNewFeedResponse()

        // When
        val recentNewFeed = recentNewFeedResponse.toRecentNewFeed()

        // Then
        assertEquals(recentNewFeedTestData.id, recentNewFeed.id)
        assertEquals(recentNewFeedTestData.url, recentNewFeed.url)
        assertEquals(recentNewFeedTestData.timeAdded, recentNewFeed.timeAdded)
        assertEquals(recentNewFeedTestData.status, recentNewFeed.status)
        assertEquals(recentNewFeedTestData.contentHash, recentNewFeed.contentHash)
        assertEquals(recentNewFeedTestData.language, recentNewFeed.language)
    }

    @Test
    fun `toRecentNewFeeds converts list of RecentNewFeedResponse to list of RecentNewFeed correctly`() {
        // Given
        val recentNewFeedResponses = recentNewFeedTestDataList.toRecentNewFeedResponses()

        // When
        val recentNewFeeds = recentNewFeedResponses.toRecentNewFeeds()

        // Then
        assertEquals(recentNewFeedTestDataList.size, recentNewFeeds.size)
        assertEquals(recentNewFeedTestDataList.first().id, recentNewFeeds.first().id)
        assertEquals(recentNewFeedTestDataList.last().id, recentNewFeeds.last().id)
    }

    @Test
    fun `toRecentNewFeedResponse converts RecentNewFeed to RecentNewFeedResponse correctly`() {
        // When
        val recentNewFeedResponse = recentNewFeedTestData.toRecentNewFeedResponse()

        // Then
        assertEquals(recentNewFeedTestData.id, recentNewFeedResponse.id)
        assertEquals(recentNewFeedTestData.url, recentNewFeedResponse.url)
        assertEquals(recentNewFeedTestData.timeAdded.epochSeconds, recentNewFeedResponse.timeAdded)
        assertEquals(recentNewFeedTestData.status, recentNewFeedResponse.status)
        assertEquals(recentNewFeedTestData.contentHash, recentNewFeedResponse.contentHash)
        assertEquals(recentNewFeedTestData.language, recentNewFeedResponse.language)
    }

    @Test
    fun `toRecentNewFeedResponses converts list of RecentNewFeed to list of RecentNewFeedResponse correctly`() {
        // When
        val recentNewFeedResponses = recentNewFeedTestDataList.toRecentNewFeedResponses()

        // Then
        assertEquals(recentNewFeedTestDataList.size, recentNewFeedResponses.size)
        assertEquals(recentNewFeedTestDataList.first().id, recentNewFeedResponses.first().id)
        assertEquals(recentNewFeedTestDataList.last().id, recentNewFeedResponses.last().id)
    }

    @Test
    fun `toRecentNewValueFeed converts RecentNewValueFeedResponse to RecentNewValueFeed correctly`() {
        // Given
        val recentNewValueFeedResponse = recentNewValueFeedTestData.toRecentNewValueFeedResponse()

        // When
        val recentNewValueFeed = recentNewValueFeedResponse.toRecentNewValueFeed()

        // Then
        assertEquals(recentNewValueFeedTestData.id, recentNewValueFeed.id)
        assertEquals(recentNewValueFeedTestData.url, recentNewValueFeed.url)
        assertEquals(recentNewValueFeedTestData.title, recentNewValueFeed.title)
        assertEquals(recentNewValueFeedTestData.author, recentNewValueFeed.author)
        assertEquals(recentNewValueFeedTestData.image, recentNewValueFeed.image)
        assertEquals(recentNewValueFeedTestData.newestItemPublishTime, recentNewValueFeed.newestItemPublishTime)
        assertEquals(recentNewValueFeedTestData.itunesId, recentNewValueFeed.itunesId)
        assertEquals(recentNewValueFeedTestData.trendScore, recentNewValueFeed.trendScore)
        assertEquals(recentNewValueFeedTestData.language, recentNewValueFeed.language)
        assertEquals(recentNewValueFeedTestData.categories, recentNewValueFeed.categories)
    }

    @Test
    fun `toRecentNewValueFeeds converts list of RecentNewValueFeedResponse to list of RecentNewValueFeed correctly`() {
        // Given
        val recentNewValueFeedResponses = recentNewValueFeedTestDataList.toRecentNewValueFeedResponses()

        // When
        val recentNewValueFeeds = recentNewValueFeedResponses.toRecentNewValueFeeds()

        // Then
        assertEquals(recentNewValueFeedTestDataList.size, recentNewValueFeeds.size)
        assertEquals(recentNewValueFeedTestDataList.first().id, recentNewValueFeeds.first().id)
        assertEquals(recentNewValueFeedTestDataList.last().id, recentNewValueFeeds.last().id)
    }

    @Test
    fun `toSoundbite converts SoundbiteResponse to Soundbite correctly`() {
        // Given
        val soundbiteResponse = soundbiteTestData.toSoundbiteResponse()

        // When
        val soundbite = soundbiteResponse.toSoundbite()

        // Then
        assertEquals(soundbiteTestData.enclosureUrl, soundbite.enclosureUrl)
        assertEquals(soundbiteTestData.title, soundbite.title)
        assertEquals(soundbiteTestData.startTime, soundbite.startTime)
        assertEquals(soundbiteTestData.duration, soundbite.duration)
        assertEquals(soundbiteTestData.episodeId, soundbite.episodeId)
        assertEquals(soundbiteTestData.episodeTitle, soundbite.episodeTitle)
        assertEquals(soundbiteTestData.feedTitle, soundbite.feedTitle)
        assertEquals(soundbiteTestData.feedUrl, soundbite.feedUrl)
        assertEquals(soundbiteTestData.feedId, soundbite.feedId)
    }

    @Test
    fun `toSoundbites converts list of SoundbiteResponse to list of Soundbite correctly`() {
        // Given
        val soundbiteResponses = soundbiteTestDataList.map { it.toSoundbiteResponse() }

        // When
        val soundbites = soundbiteResponses.toSoundbites()

        // Then
        assertEquals(soundbiteTestDataList.size, soundbites.size)
        assertEquals(soundbiteTestDataList.first().enclosureUrl, soundbites.first().enclosureUrl)
        assertEquals(soundbiteTestDataList.last().enclosureUrl, soundbites.last().enclosureUrl)
    }

    @Test
    fun `toTranscript converts TranscriptResponse to Transcript correctly`() {
        // Given
        val transcriptResponse = TranscriptResponse(
            url = "https://example.com/transcript.vtt",
            type = "text/vtt"
        )

        // When
        val transcript = transcriptResponse.toTranscript()

        // Then
        assertEquals(transcriptResponse.url, transcript.url)
        assertEquals(transcriptResponse.type, transcript.type)
    }

    @Test
    fun `toTranscripts converts list of TranscriptResponse to list of Transcript correctly`() {
        // Given
        val transcriptResponses = listOf(
            TranscriptResponse(url = "https://example.com/transcript1.vtt", type = "text/vtt"),
            TranscriptResponse(url = "https://example.com/transcript2.srt", type = "text/srt")
        )

        // When
        val transcripts = transcriptResponses.toTranscripts()

        // Then
        assertEquals(2, transcripts.size)
        assertEquals("https://example.com/transcript1.vtt", transcripts[0].url)
        assertEquals("text/vtt", transcripts[0].type)
        assertEquals("https://example.com/transcript2.srt", transcripts[1].url)
        assertEquals("text/srt", transcripts[1].type)
    }

    @Test
    fun `toTranscriptResponse converts Transcript to TranscriptResponse correctly`() {
        // Given
        val transcript = Transcript(
            url = "https://example.com/transcript.vtt",
            type = "text/vtt"
        )

        // When
        val transcriptResponse = transcript.toTranscriptResponse()

        // Then
        assertEquals(transcript.url, transcriptResponse.url)
        assertEquals(transcript.type, transcriptResponse.type)
    }

    @Test
    fun `toTranscriptResponses converts list of Transcript to list of TranscriptResponse correctly`() {
        // Given
        val transcripts = listOf(
            Transcript(url = "https://example.com/transcript1.vtt", type = "text/vtt"),
            Transcript(url = "https://example.com/transcript2.srt", type = "text/srt")
        )

        // When
        val transcriptResponses = transcripts.toTranscriptResponses()

        // Then
        assertEquals(2, transcriptResponses.size)
        assertEquals("https://example.com/transcript1.vtt", transcriptResponses[0].url)
        assertEquals("text/vtt", transcriptResponses[0].type)
        assertEquals("https://example.com/transcript2.srt", transcriptResponses[1].url)
        assertEquals("text/srt", transcriptResponses[1].type)
    }

    @Test
    fun `round trip conversion for Podcast and PodcastResponse works correctly`() {
        // Given
        val originalPodcast = podcastTestData

        // When
        val podcastResponse = originalPodcast.toPodcastResponse()
        val backToPodcast = podcastResponse.toPodcast()

        // Then
        assertEquals(originalPodcast.id, backToPodcast.id)
        assertEquals(originalPodcast.title, backToPodcast.title)
        assertEquals(originalPodcast.url, backToPodcast.url)
        assertEquals(originalPodcast.author, backToPodcast.author)
        assertEquals(originalPodcast.medium, backToPodcast.medium)
        assertEquals(originalPodcast.lastUpdateTime, backToPodcast.lastUpdateTime)
    }

    @Test
    fun `round trip conversion for Episode and EpisodeResponse works correctly`() {
        // Given
        val originalEpisode = episodeTestData

        // When
        val episodeResponse = originalEpisode.toEpisodeResponse()
        val backToEpisode = episodeResponse.toEpisode()

        // Then
        assertEquals(originalEpisode.id, backToEpisode.id)
        assertEquals(originalEpisode.title, backToEpisode.title)
        assertEquals(originalEpisode.guid, backToEpisode.guid)
        assertEquals(originalEpisode.datePublished, backToEpisode.datePublished)
        assertEquals(originalEpisode.explicit, backToEpisode.explicit)
        assertEquals(originalEpisode.episodeType, backToEpisode.episodeType)
    }

    @Test
    fun `explicit field conversion works correctly for episodes`() {
        // Given
        val episodeResponseExplicit = EpisodeResponse(
            id = 123L,
            title = "Test Episode",
            link = "https://example.com/episode",
            description = "Test description",
            guid = "test-guid",
            datePublished = 1758000000L,
            dateCrawled = 1758000000L,
            enclosureUrl = "https://example.com/audio.mp3",
            enclosureType = "audio/mpeg",
            enclosureLength = 1234567L,
            explicit = 1, // Explicit
            image = "https://example.com/image.jpg",
            feedImage = "https://example.com/feed-image.jpg",
            feedId = 456L,
            feedLanguage = "en"
        )

        val episodeResponseNotExplicit = episodeResponseExplicit.copy(explicit = 0)

        // When
        val explicitEpisode = episodeResponseExplicit.toEpisode()
        val notExplicitEpisode = episodeResponseNotExplicit.toEpisode()

        // Then
        assertTrue(explicitEpisode.explicit)
        assertFalse(notExplicitEpisode.explicit)
    }

    @Test
    fun `null categories are handled correctly`() {
        // Given
        val podcastResponseWithoutCategories = PodcastResponse(
            id = 123L,
            podcastGuid = "test-guid",
            title = "Test Podcast",
            url = "https://example.com/feed",
            originalUrl = "https://example.com/feed",
            link = "https://example.com",
            description = "Test description",
            author = "Test Author",
            ownerName = "Test Owner",
            image = "https://example.com/image.jpg",
            artwork = "https://example.com/artwork.jpg",
            lastUpdateTime = 1758000000L,
            lastCrawlTime = 1758000000L,
            lastParseTime = 1758000000L,
            lastGoodHttpStatusTime = 1758000000L,
            lastHttpStatus = 200,
            contentType = "application/rss+xml",
            language = "en",
            type = 0,
            medium = "podcast",
            dead = 0,
            episodeCount = 100,
            crawlErrors = 0,
            parseErrors = 0,
            locked = 0,
            categories = null
        )

        // When
        val podcast = podcastResponseWithoutCategories.toPodcast()

        // Then
        assertTrue(podcast.categories.isEmpty())
    }
}