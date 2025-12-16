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
    fun `Given dependencies, When replacePodcasts is called, Then replacePodcasts of dao is called`() =
        runTest {
            // Given
            coEvery { podcastDao.replacePodcasts(any()) } just Runs

            // When
            dataSource.replacePodcasts(podcastEntities)

            // Then
            coVerify { podcastDao.replacePodcasts(podcastEntities) }
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
            coEvery { podcastDao.getPodcasts(10) } returns mockk()

            // When
            dataSource.getPodcasts(10)

            // Then
            coVerify { podcastDao.getPodcasts(10) }
        }

    @Test
    fun `Given dependencies, When getPodcastsPaging is called, Then getPodcastsPaging of dao is called`() =
        runTest {
            // Given
            coEvery { podcastDao.getPodcastsPaging() } returns mockk()

            // When
            dataSource.getPodcastsPaging()

            // Then
            coVerify { podcastDao.getPodcastsPaging() }
        }

    @Test
    fun `Given dependencies, When getPodcastsByCacheKey is called, Then getPodcastsByCacheKey of dao is called`() =
        runTest {
            // Given
            coEvery { podcastDao.getPodcastsByCacheKey(any(), 10) } returns mockk()

            // When
            dataSource.getPodcastsByCacheKey(cacheKey, 10)

            // Then
            coVerify { podcastDao.getPodcastsByCacheKey(cacheKey, 10) }
        }

    @Test
    fun `Given dependencies, When getPodcastsByCacheKeyPaging is called, Then getPodcastsByCacheKeyPaging of dao is called`() =
        runTest {
            // Given
            coEvery { podcastDao.getPodcastsByCacheKeyPaging(any()) } returns mockk()

            // When
            dataSource.getPodcastsByCacheKeyPaging(cacheKey)

            // Then
            coVerify { podcastDao.getPodcastsByCacheKeyPaging(cacheKey) }
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
    fun `Given dependencies, When isFollowed is called, Then isFollowed of dao is called`() =
        runTest {
            // Given
            coEvery { podcastDao.isFollowed(any()) } returns false

            // When
            dataSource.isFollowed(podcastEntity.id)

            // Then
            coVerify { podcastDao.isFollowed(podcastEntity.id) }
        }

    @Test
    fun `Given dependencies, When toggleFollowed is called, Then toggleFollowed of dao is called`() =
        runTest {
            // Given
            coEvery { podcastDao.toggleFollowed(any()) } returns false

            // When
            dataSource.toggleFollowed(podcastEntity.id)

            // Then
            coVerify { podcastDao.toggleFollowed(podcastEntity.id) }
        }

    @Test
    fun `Given dependencies, When getFollowedPodcasts is called, Then getFollowedPodcasts of dao is called`() =
        runTest {
            // Given
            coEvery { podcastDao.getFollowedPodcasts(10) } returns mockk()

            // When
            dataSource.getFollowedPodcasts(10)

            // Then
            coVerify { podcastDao.getFollowedPodcasts(10) }
        }

    @Test
    fun `Given dependencies, When getFollowedPodcastsPaging is called, Then getFollowedPodcastsPaging of dao is called`() =
        runTest {
            // Given
            coEvery { podcastDao.getFollowedPodcastsPaging() } returns mockk()

            // When
            dataSource.getFollowedPodcastsPaging()

            // Then
            coVerify { podcastDao.getFollowedPodcastsPaging() }
        }

    @Test
    fun `Given dependencies, When getPodcastsOldestCachedAtByCacheKey is called, Then getPodcastsOldestCachedAtByCacheKey of dao is called`() =
        runTest {
            // Given
            coEvery { podcastDao.getPodcastsOldestCachedAtByCacheKey(any()) } returns mockk()

            // When
            dataSource.getPodcastsOldestCachedAtByCacheKey(cacheKey)

            // Then
            coVerify { podcastDao.getPodcastsOldestCachedAtByCacheKey(cacheKey) }
        }
}