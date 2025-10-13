package io.jacob.episodive.core.database.dao

import app.cash.turbine.test
import io.jacob.episodive.core.database.RoomDatabaseRule
import io.jacob.episodive.core.database.mapper.toPodcastEntities
import io.jacob.episodive.core.database.mapper.toPodcastEntity
import io.jacob.episodive.core.database.model.FollowedPodcastEntity
import io.jacob.episodive.core.testing.model.podcastTestData
import io.jacob.episodive.core.testing.model.podcastTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Clock

@RunWith(RobolectricTestRunner::class)
class PodcastDaoTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val dbRule = RoomDatabaseRule()

    private lateinit var dao: PodcastDao

    @Before
    fun setup() {
        dao = dbRule.db.podcastDao()
    }

    private val cacheKey = "test_cache"
    private val podcastEntity = podcastTestData.toPodcastEntity(cacheKey = cacheKey)
    private val podcastEntities = podcastTestDataList.toPodcastEntities(cacheKey = cacheKey)

    @Test
    fun `Given a podcast entity, When upsertPodcast is called, Then the podcast is inserted or updated`() =
        runTest {
            // Given
            dao.upsertPodcast(podcastEntity)

            // When
            dao.getPodcast(podcastEntity.id).test {
                val podcast = awaitItem()
                // Then
                assertEquals(podcastEntity.id, podcast?.id)
                cancel()
            }
        }

    @Test
    fun `Given list podcast entities, When upsertPodcasts is called, Then the podcasts are inserted or updated`() =
        runTest {
            // Given
            dao.upsertPodcasts(podcastEntities)

            // When
            dao.getPodcasts().test {
                val podcasts = awaitItem()
                // Then
                assertEquals(10, podcasts.size)
                assertEquals(10, dao.getPodcastCount().first())
                val podcastIds = podcasts.map { it.id }
                val entityIds = podcastEntities.map { it.id }
                assertTrue(podcastIds.containsAll(entityIds))
                cancel()
            }
        }

    @Test
    fun `Given a podcast entity, When deletePodcast is called, Then the podcast is deleted`() =
        runTest {
            // Given
            dao.upsertPodcasts(podcastEntities)

            // When
            dao.deletePodcast(podcastTestData.id)

            dao.getPodcasts().test {
                val podcasts = awaitItem()
                // Then
                assertEquals(9, podcasts.size)
                assertFalse(podcasts.contains(podcastEntity))
                cancel()
            }
        }

    @Test
    fun `Given some podcast entities, When deletePodcasts is called, Then all podcasts are deleted`() =
        runTest {
            // Given
            dao.upsertPodcasts(podcastEntities)

            // When
            dao.deletePodcasts()

            dao.getPodcasts().test {
                val podcasts = awaitItem()
                // Then
                assertTrue(podcasts.isEmpty())
                cancel()
            }
        }

    @Test
    fun `Given some podcast entities, When getPodcastsByCacheKey is called, Then the correct podcasts are returned`() =
        runTest {
            // Given
            val entities = podcastEntities.chunked(3)
            dao.upsertPodcasts(entities[0].map { it.copy(cacheKey = "test_key1") })
            dao.upsertPodcasts(entities[1].map { it.copy(cacheKey = "test_key2") })
            dao.upsertPodcasts(entities[2].map { it.copy(cacheKey = "test_key3") })
            dao.upsertPodcasts(entities[3])

            // When
            dao.getPodcastsByCacheKey("test_key1").test {
                val podcasts = awaitItem()
                // Then
                assertEquals(entities[0].size, podcasts.size)
                val podcastIds = entities[0].map { it.id }
                val entityIds = podcasts.map { it.id }
                assertTrue(podcastIds.containsAll(entityIds))
                cancel()
            }
        }

    @Test
    fun `Given some podcast entity followed and some podcast entities, When getFollowedPodcasts is called, Then the correct followed podcasts are returned`() =
        runTest {
            // Given
            dao.upsertPodcasts(podcastEntities)
            val followed = listOf(
                podcastEntities[1].id,
                podcastEntities[3].id,
                podcastEntities[5].id,
            )
            val followedAt = Clock.System.now()
            followed.forEach {
                dao.addFollowed(
                    FollowedPodcastEntity(
                        id = it,
                        followedAt = followedAt,
                        isNotificationEnabled = true
                    )
                )
            }

            // When
            dao.getFollowedPodcasts().test {
                val podcasts = awaitItem()
                // Then
                assertEquals(followed.size, podcasts.size)
                assertEquals(followed.size, dao.getFollowedPodcastCount().first())
                val entityIds = podcasts.map { it.podcast?.id }
                assertTrue(followed.containsAll(entityIds))
            }
        }

    @Test
    fun `Given some podcast entities, When getFollowedPodcasts is called with query, Then the correct followed podcasts are returned`() =
        runTest {
            // Given
            dao.upsertPodcasts(podcastEntities)
            val followedAt = Clock.System.now()
            podcastEntities.forEach {
                dao.addFollowed(
                    FollowedPodcastEntity(
                        id = it.id,
                        followedAt = followedAt,
                        isNotificationEnabled = true
                    )
                )
            }

            // When
            dao.getFollowedPodcasts(query = "jtbc").test {
                val podcasts = awaitItem()
                // Then
                assertEquals(5, podcasts.size)
                cancel()
            }
        }

    @Test
    fun `Given a followed podcast entity, When removeFollowed is called, Then the podcast is unfollowed`() =
        runTest {
            // Given
            dao.upsertPodcasts(podcastEntities)
            val followed = listOf(
                podcastEntities[1].id,
                podcastEntities[3].id,
                podcastEntities[5].id,
            )
            val followedAt = Clock.System.now()
            followed.forEach {
                dao.addFollowed(
                    FollowedPodcastEntity(
                        id = it,
                        followedAt = followedAt,
                        isNotificationEnabled = true
                    )
                )
            }

            // When
            dao.removeFollowed(podcastEntities[3].id)

            dao.getFollowedPodcasts().test {
                val podcasts = awaitItem()
                // Then
                assertEquals(followed.size - 1, podcasts.size)
                assertEquals(followed.size - 1, dao.getFollowedPodcastCount().first())
                val entityIds = podcasts.map { it.podcast?.id }
                assertFalse(entityIds.contains(podcastEntities[3].id))
                cancel()
            }
        }

    @Test
    fun `Given a followed podcast entity, When isFollowed is called, Then the correct result is returned`() =
        runTest {
            // Given
            dao.upsertPodcasts(podcastEntities)
            val followed = listOf(
                podcastEntities[1].id,
                podcastEntities[3].id,
                podcastEntities[5].id,
            )
            val followedAt = Clock.System.now()
            followed.forEach {
                dao.addFollowed(
                    FollowedPodcastEntity(
                        id = it,
                        followedAt = followedAt,
                        isNotificationEnabled = true
                    )
                )
            }

            // When
            dao.isFollowed(podcastEntities[3].id).test {
                val isFollowed = awaitItem()
                // Then
                assertTrue(isFollowed)
                cancel()
            }
            dao.isFollowed(podcastEntities[4].id).test {
                val isFollowed = awaitItem()
                // Then
                assertFalse(isFollowed)
                cancel()
            }
        }

    @Test
    fun `Given some followed podcast entities, When getFollowedPodcastCount is called, Then the correct count is returned`() =
        runTest {
            // Given
            dao.upsertPodcasts(podcastEntities)
            val followed = listOf(
                podcastEntities[1].id,
                podcastEntities[3].id,
                podcastEntities[5].id,
            )
            val followedAt = Clock.System.now()
            val entities = mutableListOf<FollowedPodcastEntity>()
            followed.forEach {
                entities.add(
                    FollowedPodcastEntity(
                        id = it,
                        followedAt = followedAt,
                        isNotificationEnabled = true
                    )
                )
            }
            dao.addFolloweds(entities)

            // When
            val count = dao.getFollowedPodcastCount().first()

            // Then
            assertEquals(followed.size, count)
        }
}