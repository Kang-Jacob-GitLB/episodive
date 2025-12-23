package io.jacob.episodive.core.database.mapper

import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.podcastTestData
import io.jacob.episodive.core.testing.model.podcastTestDataList
import io.jacob.episodive.core.testing.model.soundbiteTestData
import io.jacob.episodive.core.testing.model.soundbiteTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.time.Clock

class DatabaseMapperTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

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