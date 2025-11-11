package io.jacob.episodive.core.database.dao

import app.cash.turbine.test
import io.jacob.episodive.core.database.RoomDatabaseRule
import io.jacob.episodive.core.database.mapper.toPodcastEntities
import io.jacob.episodive.core.database.mapper.toPodcastEntity
import io.jacob.episodive.core.database.model.FollowedPodcastEntity
import io.jacob.episodive.core.testing.model.podcastTestData
import io.jacob.episodive.core.testing.model.podcastTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
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
                assertEquals(podcastEntity.id, podcast?.podcast?.id)
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
                val podcastIds = podcasts.map { it.podcast.id }
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
                assertFalse(podcasts.map { it.podcast }.contains(podcastEntity))
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
    fun `Given some podcast entities, When deletePodcastsByCacheKey is called, Then the correct podcasts are deleted`() =
        runTest {
            // Given
            val entities = podcastEntities.chunked(2)
            dao.upsertPodcasts(entities[0].map { it.copy(cacheKey = "test_key1") })
            dao.upsertPodcasts(entities[1].map { it.copy(cacheKey = "test_key2") })
            dao.upsertPodcasts(entities[2])

            // When
            dao.deletePodcastsByCacheKey("test_key1")

            dao.getPodcasts().test {
                val podcasts = awaitItem()
                // Then
                val remainingSize = entities[1].size + entities[2].size
                assertEquals(remainingSize, podcasts.size)
                val deletedIds = entities[0].map { it.id }
                val entityIds = podcasts.map { it.podcast.id }
                deletedIds.forEach { id ->
                    assertFalse(entityIds.contains(id))
                }
                cancel()
            }
        }

    @Test
    fun `Given existing podcasts with different cache keys, When replacePodcasts is called, Then podcasts are replaced by cache key`() =
        runTest {
            // Given - Insert initial podcasts with different cache keys
            val initialEntities = podcastEntities.chunked(2)
            dao.upsertPodcasts(initialEntities[0].map { it.copy(cacheKey = "key1") })
            dao.upsertPodcasts(initialEntities[1].map { it.copy(cacheKey = "key2") })
            dao.upsertPodcasts(initialEntities[2].map { it.copy(cacheKey = "key3") })

            // Verify initial state
            dao.getPodcastsByCacheKey("key1").test {
                assertEquals(2, awaitItem().size)
                cancel()
            }

            // When - Replace podcasts with mixed cache keys
            val newEntities = listOf(
                podcastEntity.copy(id = 999L, cacheKey = "key1"),
                podcastEntity.copy(id = 998L, cacheKey = "key1"),
                podcastEntity.copy(id = 997L, cacheKey = "key2")
            )
            dao.replacePodcasts(newEntities)

            // Then - Verify key1 was replaced
            dao.getPodcastsByCacheKey("key1").test {
                val key1Podcasts = awaitItem()
                assertEquals(2, key1Podcasts.size)
                assertTrue(key1Podcasts.any { it.podcast.id == 999L })
                assertTrue(key1Podcasts.any { it.podcast.id == 998L })
                assertFalse(key1Podcasts.any { it.podcast.id == initialEntities[0][0].id })
                cancel()
            }

            // Then - Verify key2 was replaced
            dao.getPodcastsByCacheKey("key2").test {
                val key2Podcasts = awaitItem()
                assertEquals(1, key2Podcasts.size)
                assertTrue(key2Podcasts.any { it.podcast.id == 997L })
                assertFalse(key2Podcasts.any { it.podcast.id == initialEntities[1][0].id })
                cancel()
            }

            // Then - Verify key3 was not affected
            dao.getPodcastsByCacheKey("key3").test {
                val key3Podcasts = awaitItem()
                assertEquals(2, key3Podcasts.size)
                assertTrue(key3Podcasts.any { it.podcast.id == initialEntities[2][0].id })
                assertTrue(key3Podcasts.any { it.podcast.id == initialEntities[2][1].id })
                cancel()
            }
        }

    @Test
    fun `Given podcasts with same cache key, When replacePodcasts is called, Then old podcasts are deleted and new podcasts are inserted`() =
        runTest {
            // Given - Insert initial podcasts
            val initialPodcasts = podcastEntities.take(3).map { it.copy(cacheKey = "trending") }
            dao.upsertPodcasts(initialPodcasts)

            // When - Replace with new podcasts
            val newPodcasts = listOf(
                podcastEntity.copy(id = 100L, cacheKey = "trending"),
                podcastEntity.copy(id = 101L, cacheKey = "trending")
            )
            dao.replacePodcasts(newPodcasts)

            // Then - Verify old podcasts are gone and new podcasts exist
            dao.getPodcastsByCacheKey("trending").test {
                val podcasts = awaitItem()
                assertEquals(2, podcasts.size)
                assertTrue(podcasts.any { it.podcast.id == 100L })
                assertTrue(podcasts.any { it.podcast.id == 101L })
                assertFalse(podcasts.any { it.podcast.id in initialPodcasts.map { p -> p.id } })
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
                val entityIds = podcasts.map { it.podcast.id }
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
                val entityIds = podcasts.map { it.podcast.id }
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
            dao.getFollowedPodcasts().test {
                val podcasts = awaitItem()
                // Then
                assertEquals(10, podcasts.size)
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
                val entityIds = podcasts.map { it.podcast.id }
                assertFalse(entityIds.contains(podcastEntities[3].id))
                cancel()
            }
        }

    @Test
    fun `Given a followed podcast entity, When isFollowed is called, Then the correct result is returned`() =
        runTest {
            // Given
            val followed = listOf(
                podcastEntities[1].id,
                podcastEntities[3].id,
                podcastEntities[5].id,
            )

            // When
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

            // Then
            assertTrue(dao.isFollowed(podcastEntities[3].id))
            assertFalse(dao.isFollowed(podcastEntities[4].id))
        }

    @Test
    fun `Given a followed podcast entity, When toggleFollowed is called, Then the podcast followed or unfollowed`() =
        runTest {
            // When
            dao.toggleFollowed(podcastEntities[3].id)

            // Then
            assertTrue(dao.isFollowed(podcastEntities[3].id))

            // When
            dao.toggleFollowed(podcastEntities[3].id)

            // Then
            assertFalse(dao.isFollowed(podcastEntities[3].id))
        }
}