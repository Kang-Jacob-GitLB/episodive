package io.jacob.episodive.core.database.datasource

import io.jacob.episodive.core.database.dao.PodcastDao
import io.jacob.episodive.core.database.mapper.toPodcastEntities
import io.jacob.episodive.core.database.mapper.toPodcastEntity
import io.jacob.episodive.core.database.model.FollowedPodcastEntity
import io.jacob.episodive.core.testing.model.podcastTestData
import io.jacob.episodive.core.testing.model.podcastTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import kotlin.time.Clock

class PodcastLocalDataSourceTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val podcastDao = mockk<PodcastDao>(relaxed = true)

    private val dataSource: PodcastLocalDataSource =
        PodcastLocalDataSourceImpl(
            podcastDao = podcastDao,
        )

    private val cacheKey = "test_cache"
    private val podcastEntity = podcastTestData.toPodcastEntity(cacheKey = cacheKey)
    private val podcastEntities = podcastTestDataList.toPodcastEntities(cacheKey = cacheKey)

    @After
    fun teardown() {
        confirmVerified(podcastDao)
    }

    @Test
    fun `Given dependencies, When upsertPodcast is called, Then upsertPodcast of dao is called`() =
        runTest {
            // Given
            coEvery { podcastDao.upsertPodcast(any()) } just Runs

            // When
            dataSource.upsertPodcast(podcastEntity)

            // Then
            coVerify { podcastDao.upsertPodcast(podcastEntity) }
            confirmVerified(
                podcastDao,
            )
        }

    @Test
    fun `Given dependencies, When upsertPodcasts is called, Then upsertPodcasts of dao is called`() =
        runTest {
            // Given
            coEvery { podcastDao.upsertPodcasts(any()) } just Runs

            // When
            dataSource.upsertPodcasts(podcastEntities)

            // Then
            coVerify { podcastDao.upsertPodcasts(podcastEntities) }
        }

    @Test
    fun `Given dependencies, When deletePodcast is called, Then deletePodcast of dao is called`() =
        runTest {
            // Given
            coEvery { podcastDao.deletePodcast(any()) } just Runs

            // When
            dataSource.deletePodcast(podcastEntity.id)

            // Then
            coVerify { podcastDao.deletePodcast(podcastEntity.id) }
        }

    @Test
    fun `Given dependencies, When deletePodcasts is called, Then deletePodcasts of dao is called`() =
        runTest {
            // Given
            coEvery { podcastDao.deletePodcasts() } just Runs

            // When
            dataSource.deletePodcasts()

            // Then
            coVerify { podcastDao.deletePodcasts() }
        }

    @Test
    fun `Given dependencies, When deletePodcastsByCacheKey is called, Then deletePodcastsByCacheKey of dao is called`() =
        runTest {
            // Given
            coEvery { podcastDao.deletePodcastsByCacheKey(any()) } just Runs

            // When
            dataSource.deletePodcastsByCacheKey(cacheKey)

            // Then
            coVerify { podcastDao.deletePodcastsByCacheKey(cacheKey) }
        }

    @Test
    fun `Given dependencies, When addFollowed is called, Then addFollowed of dao is called`() =
        runTest {
            // Given
            coEvery { podcastDao.addFollowed(any()) } just Runs

            // When
            dataSource.addFollowed(
                FollowedPodcastEntity(
                    id = podcastEntity.id,
                    followedAt = Clock.System.now(),
                    isNotificationEnabled = true,
                )
            )

            // Then
            coVerify { podcastDao.addFollowed(any()) }
        }

    @Test
    fun `Given dependencies, When removeFollowed is called, Then removeFollowed of dao is called`() =
        runTest {
            // Given
            coEvery { podcastDao.removeFollowed(any()) } just Runs

            // When
            dataSource.removeFollowed(podcastEntity.id)

            // Then
            coVerify { podcastDao.removeFollowed(podcastEntity.id) }
        }

    @Test
    fun `Given dependencies, When getPodcast is called, Then getPodcast of dao is called`() =
        runTest {
            // Given
            coEvery { podcastDao.getPodcast(any()) } returns mockk()

            // When
            dataSource.getPodcast(podcastEntity.id)

            // Then
            coVerify { podcastDao.getPodcast(podcastEntity.id) }
        }

    @Test
    fun `Given dependencies, When getPodcasts is called, Then getPodcasts of dao is called`() =
        runTest {
            // Given
            coEvery { podcastDao.getPodcasts() } returns mockk()

            // When
            dataSource.getPodcasts()

            // Then
            coVerify { podcastDao.getPodcasts() }
        }

    @Test
    fun `Given dependencies, When getPodcastsByCacheKey is called, Then getPodcastsByCacheKey of dao is called`() =
        runTest {
            // Given
            coEvery { podcastDao.getPodcastsByCacheKey(any()) } returns mockk()

            // When
            dataSource.getPodcastsByCacheKey(cacheKey)

            // Then
            coVerify { podcastDao.getPodcastsByCacheKey(cacheKey) }
        }

    @Test
    fun `Given dependencies, When getFollowedPodcasts is called, Then getFollowedPodcasts of dao is called`() =
        runTest {
            // Given
            coEvery { podcastDao.getFollowedPodcasts() } returns mockk()

            // When
            dataSource.getFollowedPodcasts()

            // Then
            coVerify { podcastDao.getFollowedPodcasts() }
        }

    @Test
    fun `Given dependencies, When isFollowed is called, Then isFollowed of dao is called`() =
        runTest {
            // Given
            coEvery { podcastDao.isFollowed(any()) } returns mockk()

            // When
            dataSource.isFollowed(podcastEntity.id)

            // Then
            coVerify { podcastDao.isFollowed(podcastEntity.id) }
        }

    @Test
    fun `Given dependencies, When getPodcastCount is called, Then getPodcastCount of dao is called`() =
        runTest {
            // Given
            coEvery { podcastDao.getPodcastCount() } returns mockk()

            // When
            dataSource.getPodcastCount()

            // Then
            coVerify { podcastDao.getPodcastCount() }
        }

    @Test
    fun `Given dependencies, When getFollowedPodcastCount is called, Then getFollowedPodcastCount of dao is called`() =
        runTest {
            // Given
            coEvery { podcastDao.getFollowedPodcastCount() } returns mockk()

            // When
            dataSource.getFollowedPodcastCount()

            // Then
            coVerify { podcastDao.getFollowedPodcastCount() }
        }

    @Test
    fun `Given dependencies, When addFolloweds is called, Then addFolloweds of dao is called`() =
        runTest {
            // Given
            coEvery { podcastDao.addFolloweds(any()) } just Runs

            // When
            dataSource.addFolloweds(
                listOf(
                    FollowedPodcastEntity(
                        id = podcastEntity.id,
                        followedAt = Clock.System.now(),
                        isNotificationEnabled = true,
                    )
                )
            )

            // Then
            coVerify { podcastDao.addFolloweds(any()) }
        }
}