package io.jacob.episodive.core.database.mapper

import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.podcastTestData
import io.jacob.episodive.core.testing.model.podcastTestDataList
import io.jacob.episodive.core.testing.model.recentFeedTestData
import io.jacob.episodive.core.testing.model.recentFeedTestDataList
import io.jacob.episodive.core.testing.model.recentNewFeedTestData
import io.jacob.episodive.core.testing.model.recentNewFeedTestDataList
import io.jacob.episodive.core.testing.model.soundbiteTestData
import io.jacob.episodive.core.testing.model.soundbiteTestDataList
import io.jacob.episodive.core.testing.model.trendingFeedTestData
import io.jacob.episodive.core.testing.model.trendingFeedTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.time.Clock

class DatabaseMapperTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val cacheKey = "test_cache"
    private val cachedAt = Clock.System.now()

    @Test
    fun `toPodcast converts PodcastDto to Podcast correctly`() {
        // Given
        val podcastDto = podcastTestData.toPodcastWithExtrasView()

        // When
        val podcast = podcastDto.toPodcast()

        // Then
        assertEquals(podcastTestData.id, podcast.id)
        assertEquals(podcastTestData.title, podcast.title)
        assertEquals(podcastTestData.description, podcast.description)
        assertEquals(podcastTestData.medium, podcast.medium)
        assertEquals(podcastTestData.categories, podcast.categories)
    }

    @Test
    fun `toPodcasts converts list of PodcastDto to list of Podcast correctly`() {
        // Given
        val podcastDtos = podcastTestDataList.toPodcastWithExtrasViews()

        // When
        val podcasts = podcastDtos.toPodcasts()

        // Then
        assertEquals(podcastTestDataList.size, podcasts.size)
        assertEquals(podcastTestDataList.first().id, podcasts.first().id)
        assertEquals(podcastTestDataList.last().id, podcasts.last().id)
    }

    @Test
    fun `toPodcastEntity converts Podcast to PodcastEntity correctly`() {
        // When
        val podcastEntity = podcastTestData.toPodcastEntity()

        // Then
        assertEquals(podcastTestData.id, podcastEntity.id)
        assertEquals(podcastTestData.title, podcastEntity.title)
        assertEquals(podcastTestData.description, podcastEntity.description)
        assertEquals(podcastTestData.medium, podcastEntity.medium)
        assertEquals(podcastTestData.categories, podcastEntity.categories)
    }

    @Test
    fun `toPodcastEntities converts list of Podcast to list of PodcastEntity correctly`() {
        // When
        val podcastEntities = podcastTestDataList.toPodcastEntities()

        // Then
        assertEquals(podcastTestDataList.size, podcastEntities.size)
    }

    @Test
    fun `toEpisode converts EpisodeEntity to Episode correctly`() {
        // Given
        val episodeDto = episodeTestData.toEpisodeWithExtrasView()

        // When
        val episode = episodeDto.toEpisode()

        // Then
        assertEquals(episodeTestData.id, episode.id)
        assertEquals(episodeTestData.title, episode.title)
        assertEquals(episodeTestData.description, episode.description)
        assertEquals(episodeTestData.duration, episode.duration)
        assertEquals(episodeTestData.categories, episode.categories)
    }

    @Test
    fun `toEpisodes converts list of EpisodeEntity to list of Episode correctly`() {
        // Given
        val episodeDtos = episodeTestDataList.toEpisodeWithExtrasViews()

        // When
        val episodes = episodeDtos.toEpisodes()

        // Then
        assertEquals(episodeTestDataList.size, episodes.size)
        assertEquals(episodeTestDataList.first().id, episodes.first().id)
        assertEquals(episodeTestDataList.last().id, episodes.last().id)
    }

    @Test
    fun `toEpisodeEntity converts Episode to EpisodeEntity correctly`() {
        // When
        val episodeEntity = episodeTestData.toEpisodeEntity()

        // Then
        assertEquals(episodeTestData.id, episodeEntity.id)
        assertEquals(episodeTestData.title, episodeEntity.title)
        assertEquals(episodeTestData.description, episodeEntity.description)
        assertEquals(episodeTestData.duration, episodeEntity.duration)
        assertEquals(episodeTestData.categories, episodeEntity.categories)
    }

    @Test
    fun `toEpisodeEntities converts list of Episode to list of EpisodeEntity correctly`() {
        // When
        val episodeEntities = episodeTestDataList.toEpisodeEntities()

        // Then
        assertEquals(episodeTestDataList.size, episodeEntities.size)
    }

    @Test
    fun `toTrendingFeedEntity converts TrendingFeed to TrendingFeedEntity correctly`() {
        // When
        val trendingFeedEntity = trendingFeedTestData.toTrendingFeedEntity(cacheKey, cachedAt)

        // Then
        assertEquals(trendingFeedTestData.id, trendingFeedEntity.id)
        assertEquals(trendingFeedTestData.title, trendingFeedEntity.title)
        assertEquals(trendingFeedTestData.description, trendingFeedEntity.description)
        assertEquals(trendingFeedTestData.categories, trendingFeedEntity.categories)
        assertEquals(cacheKey, trendingFeedEntity.cacheKey)
        assertEquals(cachedAt, trendingFeedEntity.cachedAt)
    }

    @Test
    fun `toTrendingFeedEntities converts list of TrendingFeed to list of TrendingFeedEntity correctly`() {
        // When
        val trendingFeedEntities = trendingFeedTestDataList.toTrendingFeedEntities(cacheKey, cachedAt)

        // Then
        assertEquals(trendingFeedTestDataList.size, trendingFeedEntities.size)
        trendingFeedEntities.forEach { entity ->
            assertEquals(cacheKey, entity.cacheKey)
            assertEquals(cachedAt, entity.cachedAt)
        }
    }

    @Test
    fun `toTrendingFeed converts TrendingFeedEntity to TrendingFeed correctly`() {
        // Given
        val trendingFeedEntity = trendingFeedTestData.toTrendingFeedEntity(cacheKey, cachedAt)

        // When
        val trendingFeed = trendingFeedEntity.toTrendingFeed()

        // Then
        assertEquals(trendingFeedTestData.id, trendingFeed.id)
        assertEquals(trendingFeedTestData.title, trendingFeed.title)
        assertEquals(trendingFeedTestData.description, trendingFeed.description)
        assertEquals(trendingFeedTestData.categories, trendingFeed.categories)
    }

    @Test
    fun `toTrendingFeeds converts list of TrendingFeedEntity to list of TrendingFeed correctly`() {
        // Given
        val trendingFeedEntities = trendingFeedTestDataList.toTrendingFeedEntities(cacheKey, cachedAt)

        // When
        val trendingFeeds = trendingFeedEntities.toTrendingFeeds()

        // Then
        assertEquals(trendingFeedTestDataList.size, trendingFeeds.size)
        assertEquals(trendingFeedTestDataList.first().id, trendingFeeds.first().id)
        assertEquals(trendingFeedTestDataList.last().id, trendingFeeds.last().id)
    }

    @Test
    fun `toRecentFeedEntity converts RecentFeed to RecentFeedEntity correctly`() {
        // When
        val recentFeedEntity = recentFeedTestData.toRecentFeedEntity(cacheKey, cachedAt)

        // Then
        assertEquals(recentFeedTestData.id, recentFeedEntity.id)
        assertEquals(recentFeedTestData.title, recentFeedEntity.title)
        assertEquals(recentFeedTestData.description, recentFeedEntity.description)
        assertEquals(recentFeedTestData.categories, recentFeedEntity.categories)
        assertEquals(cacheKey, recentFeedEntity.cacheKey)
        assertEquals(cachedAt, recentFeedEntity.cachedAt)
    }

    @Test
    fun `toRecentFeedEntities converts list of RecentFeed to list of RecentFeedEntity correctly`() {
        // When
        val recentFeedEntities = recentFeedTestDataList.toRecentFeedEntities(cacheKey, cachedAt)

        // Then
        assertEquals(recentFeedTestDataList.size, recentFeedEntities.size)
        recentFeedEntities.forEach { entity ->
            assertEquals(cacheKey, entity.cacheKey)
            assertEquals(cachedAt, entity.cachedAt)
        }
    }

    @Test
    fun `toRecentFeed converts RecentFeedEntity to RecentFeed correctly`() {
        // Given
        val recentFeedEntity = recentFeedTestData.toRecentFeedEntity(cacheKey, cachedAt)

        // When
        val recentFeed = recentFeedEntity.toRecentFeed()

        // Then
        assertEquals(recentFeedTestData.id, recentFeed.id)
        assertEquals(recentFeedTestData.title, recentFeed.title)
        assertEquals(recentFeedTestData.description, recentFeed.description)
        assertEquals(recentFeedTestData.categories, recentFeed.categories)
    }

    @Test
    fun `toRecentFeeds converts list of RecentFeedEntity to list of RecentFeed correctly`() {
        // Given
        val recentFeedEntities = recentFeedTestDataList.toRecentFeedEntities(cacheKey, cachedAt)

        // When
        val recentFeeds = recentFeedEntities.toRecentFeeds()

        // Then
        assertEquals(recentFeedTestDataList.size, recentFeeds.size)
        assertEquals(recentFeedTestDataList.first().id, recentFeeds.first().id)
        assertEquals(recentFeedTestDataList.last().id, recentFeeds.last().id)
    }

    @Test
    fun `toRecentNewFeedEntity converts RecentNewFeed to RecentNewFeedEntity correctly`() {
        // When
        val recentNewFeedEntity = recentNewFeedTestData.toRecentNewFeedEntity(cacheKey, cachedAt)

        // Then
        assertEquals(recentNewFeedTestData.id, recentNewFeedEntity.id)
        assertEquals(recentNewFeedTestData.url, recentNewFeedEntity.url)
        assertEquals(recentNewFeedTestData.status, recentNewFeedEntity.status)
        assertEquals(recentNewFeedTestData.language, recentNewFeedEntity.language)
        assertEquals(cacheKey, recentNewFeedEntity.cacheKey)
        assertEquals(cachedAt, recentNewFeedEntity.cachedAt)
    }

    @Test
    fun `toRecentNewFeedEntities converts list of RecentNewFeed to list of RecentNewFeedEntity correctly`() {
        // When
        val recentNewFeedEntities = recentNewFeedTestDataList.toRecentNewFeedEntities(cacheKey, cachedAt)

        // Then
        assertEquals(recentNewFeedTestDataList.size, recentNewFeedEntities.size)
        recentNewFeedEntities.forEach { entity ->
            assertEquals(cacheKey, entity.cacheKey)
            assertEquals(cachedAt, entity.cachedAt)
        }
    }

    @Test
    fun `toRecentNewFeed converts RecentNewFeedEntity to RecentNewFeed correctly`() {
        // Given
        val recentNewFeedEntity = recentNewFeedTestData.toRecentNewFeedEntity(cacheKey, cachedAt)

        // When
        val recentNewFeed = recentNewFeedEntity.toRecentNewFeed()

        // Then
        assertEquals(recentNewFeedTestData.id, recentNewFeed.id)
        assertEquals(recentNewFeedTestData.url, recentNewFeed.url)
        assertEquals(recentNewFeedTestData.status, recentNewFeed.status)
        assertEquals(recentNewFeedTestData.language, recentNewFeed.language)
    }

    @Test
    fun `toRecentNewFeeds converts list of RecentNewFeedEntity to list of RecentNewFeed correctly`() {
        // Given
        val recentNewFeedEntities = recentNewFeedTestDataList.toRecentNewFeedEntities(cacheKey, cachedAt)

        // When
        val recentNewFeeds = recentNewFeedEntities.toRecentNewFeeds()

        // Then
        assertEquals(recentNewFeedTestDataList.size, recentNewFeeds.size)
        assertEquals(recentNewFeedTestDataList.first().id, recentNewFeeds.first().id)
        assertEquals(recentNewFeedTestDataList.last().id, recentNewFeeds.last().id)
    }

    @Test
    fun `toSoundbiteEntity converts Soundbite to SoundbiteEntity correctly`() {
        // When
        val soundbiteEntity = soundbiteTestData.toSoundbiteEntity(cachedAt)

        // Then
        assertEquals(soundbiteTestData.enclosureUrl, soundbiteEntity.enclosureUrl)
        assertEquals(soundbiteTestData.title, soundbiteEntity.title)
        assertEquals(soundbiteTestData.startTime, soundbiteEntity.startTime)
        assertEquals(soundbiteTestData.duration, soundbiteEntity.duration)
        assertEquals(soundbiteTestData.episodeId, soundbiteEntity.episodeId)
        assertEquals(cachedAt, soundbiteEntity.cachedAt)
    }

    @Test
    fun `toSoundbiteEntities converts list of Soundbite to list of SoundbiteEntity correctly`() {
        // When
        val soundbiteEntities = soundbiteTestDataList.toSoundbiteEntities(cachedAt)

        // Then
        assertEquals(soundbiteTestDataList.size, soundbiteEntities.size)
        soundbiteEntities.forEach { entity ->
            assertEquals(cachedAt, entity.cachedAt)
        }
    }

    @Test
    fun `toSoundbite converts SoundbiteEntity to Soundbite correctly`() {
        // Given
        val soundbiteEntity = soundbiteTestData.toSoundbiteEntity(cachedAt)

        // When
        val soundbite = soundbiteEntity.toSoundbite()

        // Then
        assertEquals(soundbiteTestData.enclosureUrl, soundbite.enclosureUrl)
        assertEquals(soundbiteTestData.title, soundbite.title)
        assertEquals(soundbiteTestData.startTime, soundbite.startTime)
        assertEquals(soundbiteTestData.duration, soundbite.duration)
        assertEquals(soundbiteTestData.episodeId, soundbite.episodeId)
    }

    @Test
    fun `toSoundbites converts list of SoundbiteEntity to list of Soundbite correctly`() {
        // Given
        val soundbiteEntities = soundbiteTestDataList.toSoundbiteEntities(cachedAt)

        // When
        val soundbites = soundbiteEntities.toSoundbites()

        // Then
        assertEquals(soundbiteTestDataList.size, soundbites.size)
        assertEquals(soundbiteTestDataList.first().enclosureUrl, soundbites.first().enclosureUrl)
        assertEquals(soundbiteTestDataList.last().enclosureUrl, soundbites.last().enclosureUrl)
    }
}