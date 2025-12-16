package io.jacob.episodive.core.database.dao

import app.cash.turbine.test
import io.jacob.episodive.core.database.RoomDatabaseRule
import io.jacob.episodive.core.database.mapper.toEpisodeEntities
import io.jacob.episodive.core.database.mapper.toEpisodeEntity
import io.jacob.episodive.core.database.model.LikedEpisodeEntity
import io.jacob.episodive.core.database.model.PlayedEpisodeEntity
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.episodeTestDataList
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
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
class EpisodeDaoTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val dbRule = RoomDatabaseRule()

    private lateinit var dao: EpisodeDao

    @Before
    fun setup() {
        dao = dbRule.db.episodeDao()
    }

    private val cacheKey = "test_cache"
    private val episodeEntity = episodeTestData.toEpisodeEntity(cacheKey = cacheKey)
    private val episodeEntities = episodeTestDataList.toEpisodeEntities(cacheKey = cacheKey)

    @Test
    fun `Given a episode entity, When upsertEpisode is called, Then the episode is inserted or updated`() =
        runTest {
            // Given
            val now = Instant.fromEpochSeconds(Clock.System.now().epochSeconds)
            dao.upsertEpisode(episodeEntity.copy(cachedAt = now))
            dao.upsertEpisode(episodeEntity.copy(cachedAt = now.plus(1.minutes)))
            dao.upsertEpisode(episodeEntity.copy(cachedAt = now.plus(2.minutes)))

            // When
            dao.getEpisodeById(episodeTestData.id).test {
                val episode = awaitItem()
                // Then
                assertEquals(episodeEntity.id, episode?.episode?.id)
                assertEquals(
                    now.plus(2.minutes), episode?.episode?.cachedAt
                )
                cancel()
            }
        }

    @Test
    fun `Given list episode entities, When upsertEpisodes is called, Then the episodes are inserted or updated`() =
        runTest {
            // Given
            dao.upsertEpisodes(episodeEntities)

            // When
            dao.getEpisodes(10).test {
                val episodes = awaitItem()
                // Then
                val episodeIds = episodeEntities.map { it.id }
                val entityIds = episodes.map { it.episode.id }
                assertTrue(episodeIds.containsAll(entityIds))
                cancel()
            }
        }

    @Test
    fun `Given some episode entities, When getEpisodesPaging is called, Then episodes are returned`() =
        runTest {
            // Given
            dao.upsertEpisodes(episodeEntities)

            // When
            val pagingSource = dao.getEpisodesPaging()
            val loadResult = pagingSource.load(
                androidx.paging.PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 10,
                    placeholdersEnabled = false
                )
            )

            // Then
            assertTrue(loadResult is androidx.paging.PagingSource.LoadResult.Page)
            val page = loadResult as androidx.paging.PagingSource.LoadResult.Page
            assertEquals(episodeEntities.size, page.data.size)
        }

    @Test
    fun `Given episodes with duplicate IDs, When getEpisodes is called, Then only most recent cachedAt per ID is returned`() =
        runTest {
            // Given - Insert multiple episodes with same ID but different cachedAt times
            val now = Instant.fromEpochSeconds(Clock.System.now().epochSeconds)
            val duplicateId = 12345L

            dao.upsertEpisode(
                episodeEntity.copy(
                    id = duplicateId,
                    title = "Version 1",
                    cachedAt = now
                )
            )
            dao.upsertEpisode(
                episodeEntity.copy(
                    id = duplicateId,
                    title = "Version 2",
                    cachedAt = now.plus(1.minutes)
                )
            )
            dao.upsertEpisode(
                episodeEntity.copy(
                    id = duplicateId,
                    title = "Version 3 (Most Recent)",
                    cachedAt = now.plus(2.minutes)
                )
            )

            // Insert other episodes to ensure filtering works correctly
            dao.upsertEpisodes(episodeEntities.take(3))

            // When
            dao.getEpisodes(10).test {
                val episodes = awaitItem()

                // Then - Should only have one episode with duplicateId
                val episodesWithDuplicateId = episodes.filter { it.episode.id == duplicateId }
                assertEquals(1, episodesWithDuplicateId.size)

                // Then - Should be the most recent one (Version 3)
                val mostRecentEpisode = episodesWithDuplicateId.first()
                assertEquals("Version 3 (Most Recent)", mostRecentEpisode.episode.title)
                assertEquals(now.plus(2.minutes), mostRecentEpisode.episode.cachedAt)

                // Then - Total count should be duplicateId + other episodes (3)
                assertEquals(4, episodes.size)

                cancel()
            }
        }

    @Test
    fun `Given some episode entities, When getEpisodesByCacheKey is called, Then the episodes with the cache key are returned`() =
        runTest {
            // Given
            val entities = episodeEntities.chunked(3)
            dao.upsertEpisodes(entities[0].map { it.copy(cacheKey = "test_key1") })
            dao.upsertEpisodes(entities[1].map { it.copy(cacheKey = "test_key2") })
            dao.upsertEpisodes(entities[2].map { it.copy(cacheKey = "test_key3") })
            dao.upsertEpisodes(entities[3])

            // When
            dao.getEpisodesByCacheKey("test_key1", 10).test {
                val episodes = awaitItem()
                // Then
                assertEquals(entities[0].size, episodes.size)
                val episodeIds = entities[0].map { it.id }
                val entityIds = episodes.map { it.episode.id }
                assertTrue(episodeIds.containsAll(entityIds))
                cancel()
            }
        }

    @Test
    fun `Given some episode entities, When getEpisodesByCacheKeyPaging is called, Then episodes with cache key are returned`() =
        runTest {
            // Given
            val entities = episodeEntities.chunked(3)
            dao.upsertEpisodes(entities[0].map { it.copy(cacheKey = "test_key1") })
            dao.upsertEpisodes(entities[1].map { it.copy(cacheKey = "test_key2") })
            dao.upsertEpisodes(entities[2].map { it.copy(cacheKey = "test_key3") })

            // When
            val pagingSource = dao.getEpisodesByCacheKeyPaging("test_key1")
            val loadResult = pagingSource.load(
                androidx.paging.PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 10,
                    placeholdersEnabled = false
                )
            )

            // Then
            assertTrue(loadResult is androidx.paging.PagingSource.LoadResult.Page)
            val page = loadResult as androidx.paging.PagingSource.LoadResult.Page
            assertEquals(entities[0].size, page.data.size)
            assertTrue(page.data.all { it.episode.cacheKey == "test_key1" })
        }

    @Test
    fun `Given some episode entities, When deleteEpisode is called, Then the episode is deleted`() =
        runTest {
            // Given
            dao.upsertEpisodes(episodeEntities)

            // When
            dao.deleteEpisode(episodeEntity.id)
            dao.getEpisodes(10).test {
                val episodes = awaitItem()
                // Then
                assertFalse(episodes.map { it.episode }.contains(episodeEntity))
                cancel()
            }
        }

    @Test
    fun `Given some episode entities, When deleteEpisodes is called, Then all episodes are deleted`() =
        runTest {
            // Given
            dao.upsertEpisodes(episodeEntities)

            // When
            dao.deleteEpisodes()
            dao.getEpisodes(10).test {
                val episodes = awaitItem()
                // Then
                assertTrue(episodes.isEmpty())
                cancel()
            }
        }

    @Test
    fun `Given some episode entities, When deleteEpisodesByCacheKey is called, Then episodes with the cache key are deleted`() =
        runTest {
            // Given
            val entities = episodeEntities.chunked(2)
            dao.upsertEpisodes(entities[0].map { it.copy(cacheKey = "test_key1") })
            dao.upsertEpisodes(entities[1].map { it.copy(cacheKey = "test_key2") })
            dao.upsertEpisodes(entities[2].map { it.copy(cacheKey = "test_key3") })
            dao.upsertEpisodes(entities[3])

            // When
            dao.deleteEpisodesByCacheKey("test_key2")
            dao.getEpisodesByCacheKey("test_key2", 10).test {
                val episodes = awaitItem()
                // Then
                assertTrue(episodes.isEmpty())
                cancel()
            }
        }

    @Test
    fun `Given existing episodes with different cache keys, When replaceEpisodes is called, Then episodes are replaced by cache key`() =
        runTest {
            // Given - Insert initial episodes with different cache keys
            val initialEntities = episodeEntities.chunked(2)
            dao.upsertEpisodes(initialEntities[0].map { it.copy(cacheKey = "key1") })
            dao.upsertEpisodes(initialEntities[1].map { it.copy(cacheKey = "key2") })
            dao.upsertEpisodes(initialEntities[2].map { it.copy(cacheKey = "key3") })

            // Verify initial state
            dao.getEpisodesByCacheKey("key1", 10).test {
                assertEquals(2, awaitItem().size)
                cancel()
            }

            // When - Replace episodes with mixed cache keys
            val newEntities = listOf(
                episodeEntity.copy(id = 999L, cacheKey = "key1"),
                episodeEntity.copy(id = 998L, cacheKey = "key1"),
                episodeEntity.copy(id = 997L, cacheKey = "key2")
            )
            dao.replaceEpisodes(newEntities)

            // Then - Verify key1 was replaced
            dao.getEpisodesByCacheKey("key1", 10).test {
                val key1Episodes = awaitItem()
                assertEquals(2, key1Episodes.size)
                assertTrue(key1Episodes.any { it.episode.id == 999L })
                assertTrue(key1Episodes.any { it.episode.id == 998L })
                assertFalse(key1Episodes.any { it.episode.id == initialEntities[0][0].id })
                cancel()
            }

            // Then - Verify key2 was replaced
            dao.getEpisodesByCacheKey("key2", 10).test {
                val key2Episodes = awaitItem()
                assertEquals(1, key2Episodes.size)
                assertTrue(key2Episodes.any { it.episode.id == 997L })
                assertFalse(key2Episodes.any { it.episode.id == initialEntities[1][0].id })
                cancel()
            }

            // Then - Verify key3 was not affected
            dao.getEpisodesByCacheKey("key3", 10).test {
                val key3Episodes = awaitItem()
                assertEquals(2, key3Episodes.size)
                assertTrue(key3Episodes.any { it.episode.id == initialEntities[2][0].id })
                assertTrue(key3Episodes.any { it.episode.id == initialEntities[2][1].id })
                cancel()
            }
        }

    @Test
    fun `Given som episode entities, When updateDurationOfEpisodes is called, Then duration is updated`() =
        runTest {
            // Given
            dao.upsertEpisodes(episodeEntities)

            // When
            dao.updateDurationOfEpisodes(episodeEntities[0].id, 1000.seconds)

            // Then
            dao.getEpisodes(10).test {
                val episodes = awaitItem()
                assertEquals(1000L, episodes[0].episode.duration?.inWholeSeconds)
                cancel()
            }
        }

    @Test
    fun `Given episodes with same cache key, When replaceEpisodes is called, Then old episodes are deleted and new episodes are inserted`() =
        runTest {
            // Given - Insert initial episodes
            val initialEpisodes = episodeEntities.take(3).map { it.copy(cacheKey = "trending") }
            dao.upsertEpisodes(initialEpisodes)

            // When - Replace with new episodes
            val newEpisodes = listOf(
                episodeEntity.copy(id = 100L, cacheKey = "trending"),
                episodeEntity.copy(id = 101L, cacheKey = "trending")
            )
            dao.replaceEpisodes(newEpisodes)

            // Then - Verify old episodes are gone and new episodes exist
            dao.getEpisodesByCacheKey("trending", 10).test {
                val episodes = awaitItem()
                assertEquals(2, episodes.size)
                assertTrue(episodes.any { it.episode.id == 100L })
                assertTrue(episodes.any { it.episode.id == 101L })
                assertFalse(episodes.any { it.episode.id in initialEpisodes.map { e -> e.id } })
                cancel()
            }
        }

    @Test
    fun `Given some episode entity liked and some episode entities, When getLikedEpisodes is called, Then liked episodes are returned`() =
        runTest {
            // Given
            val likedAt = Clock.System.now()
            dao.addLiked(LikedEpisodeEntity(episodeEntities[0].id, likedAt))
            dao.addLiked(LikedEpisodeEntity(episodeEntities[1].id, likedAt.plus(1.minutes)))
            dao.addLiked(LikedEpisodeEntity(episodeEntities[2].id, likedAt.plus(2.minutes)))
            dao.upsertEpisodes(episodeEntities)

            // When
            val likedEpisodes = dao.getLikedEpisodes(10).first()

            // Then
            assertEquals(3, likedEpisodes.size)
            assertEquals(episodeEntities[2].id, likedEpisodes[0].episode.id)
            assertEquals(episodeEntities[1].id, likedEpisodes[1].episode.id)
            assertEquals(episodeEntities[0].id, likedEpisodes[2].episode.id)
        }

    @Test
    fun `Given some episode entities, When getLikedEpisodes is called with query, Then liked episodes matching the query are returned`() =
        runTest {
            // Given
            val likedAt = Clock.System.now()
            dao.addLiked(LikedEpisodeEntity(episodeEntities[0].id, likedAt))
            dao.addLiked(LikedEpisodeEntity(episodeEntities[1].id, likedAt.plus(1.minutes)))
            dao.addLiked(LikedEpisodeEntity(episodeEntities[2].id, likedAt.plus(2.minutes)))
            dao.upsertEpisodes(episodeEntities)

            // When
            dao.getLikedEpisodes(10).test {
                val likedEpisodes = awaitItem()
                // Then
                assertEquals(3, likedEpisodes.size)
                assertEquals(episodeEntities[2].id, likedEpisodes[0].episode.id)
                assertEquals(episodeEntities[1].id, likedEpisodes[1].episode.id)
                assertEquals(episodeEntities[0].id, likedEpisodes[2].episode.id)
                cancel()
            }
        }

    @Test
    fun `Given some liked episode entities, When getLikedEpisodesPaging is called, Then liked episodes are returned`() =
        runTest {
            // Given
            val likedAt = Clock.System.now()
            dao.addLiked(LikedEpisodeEntity(episodeEntities[0].id, likedAt))
            dao.addLiked(LikedEpisodeEntity(episodeEntities[1].id, likedAt.plus(1.minutes)))
            dao.addLiked(LikedEpisodeEntity(episodeEntities[2].id, likedAt.plus(2.minutes)))
            dao.upsertEpisodes(episodeEntities)

            // When
            val pagingSource = dao.getLikedEpisodesPaging()
            val loadResult = pagingSource.load(
                androidx.paging.PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 10,
                    placeholdersEnabled = false
                )
            )

            // Then
            assertTrue(loadResult is androidx.paging.PagingSource.LoadResult.Page)
            val page = loadResult as androidx.paging.PagingSource.LoadResult.Page
            assertEquals(3, page.data.size)
            assertEquals(episodeEntities[2].id, page.data[0].episode.id)
            assertEquals(episodeEntities[1].id, page.data[1].episode.id)
            assertEquals(episodeEntities[0].id, page.data[2].episode.id)
        }

    @Test
    fun `Given some episode entity liked, When isLiked is called, Then true is returned`() =
        runTest {
            // Given
            val likedAt = Clock.System.now()
            dao.addLiked(LikedEpisodeEntity(episodeEntity.id, likedAt))
            dao.upsertEpisode(episodeEntity)

            // When
            dao.isLiked(episodeEntity.id).test {
                val isLiked = awaitItem()

                // Then
                assertTrue(isLiked)
            }
        }

    @Test
    fun `Given some episode entities liked, When toggleLiked is called, Then getLikedEpisodes returns correct`() =
        runTest {
            // Given
            val likedAt = Clock.System.now()
            dao.upsertEpisodes(episodeEntities)
            dao.addLiked(LikedEpisodeEntity(episodeEntities[0].id, likedAt))
            dao.addLiked(LikedEpisodeEntity(episodeEntities[1].id, likedAt.plus(1.minutes)))

            // When
            dao.toggleLiked(episodeEntities[0].id)

            dao.getLikedEpisodes(10).test {
                val likedEpisodes = awaitItem()
                // Then
                assertEquals(1, likedEpisodes.size)
                assertEquals(episodeEntities[1].id, likedEpisodes[0].episode.id)
                cancel()
            }

            // When
            dao.toggleLiked(episodeEntities[0].id)

            dao.getLikedEpisodes(10).test {
                val likedEpisodes = awaitItem()
                // Then
                assertEquals(2, likedEpisodes.size)
                assertEquals(episodeEntities[1].id, likedEpisodes[0].episode.id)
                assertEquals(episodeEntities[0].id, likedEpisodes[1].episode.id)
                cancel()
            }
        }

    @Test
    fun `Given some episode entities, When getPlayedEpisodes is called, Then played episodes are returned`() =
        runTest {
            // Given
            val now = Clock.System.now()
            dao.upsertPlayed(
                PlayedEpisodeEntity(
                    id = episodeEntities[0].id,
                    playedAt = now,
                    position = 1000.seconds,
                    isCompleted = false
                )
            )
            dao.upsertPlayed(
                PlayedEpisodeEntity(
                    id = episodeEntities[1].id,
                    playedAt = now.plus(1.minutes),
                    position = 2000.seconds,
                    isCompleted = false
                )
            )
            dao.upsertPlayed(
                PlayedEpisodeEntity(
                    id = episodeEntities[2].id,
                    playedAt = now.plus(2.minutes),
                    position = 3000.seconds,
                    isCompleted = true
                )
            )
            dao.upsertEpisodes(episodeEntities)

            // When
            dao.getPlayedEpisodes(10).test {
                val playedEpisodes = awaitItem()
                // Then
                assertEquals(3, playedEpisodes.size)
                assertEquals(episodeEntities[2].id, playedEpisodes[0].episode.id)
                assertEquals(episodeEntities[1].id, playedEpisodes[1].episode.id)
                assertEquals(episodeEntities[0].id, playedEpisodes[2].episode.id)
            }
        }

    @Test
    fun `Given some played episode entities, When getPlayedEpisodesPaging is called, Then played episodes are returned`() =
        runTest {
            // Given
            val now = Clock.System.now()
            dao.upsertPlayed(
                PlayedEpisodeEntity(
                    id = episodeEntities[0].id,
                    playedAt = now,
                    position = 1000.seconds,
                    isCompleted = false
                )
            )
            dao.upsertPlayed(
                PlayedEpisodeEntity(
                    id = episodeEntities[1].id,
                    playedAt = now.plus(1.minutes),
                    position = 2000.seconds,
                    isCompleted = false
                )
            )
            dao.upsertPlayed(
                PlayedEpisodeEntity(
                    id = episodeEntities[2].id,
                    playedAt = now.plus(2.minutes),
                    position = 3000.seconds,
                    isCompleted = true
                )
            )
            dao.upsertEpisodes(episodeEntities)

            // When
            val pagingSource = dao.getPlayedEpisodesPaging()
            val loadResult = pagingSource.load(
                androidx.paging.PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 10,
                    placeholdersEnabled = false
                )
            )

            // Then
            assertTrue(loadResult is androidx.paging.PagingSource.LoadResult.Page)
            val page = loadResult as androidx.paging.PagingSource.LoadResult.Page
            assertEquals(3, page.data.size)
            assertEquals(episodeEntities[2].id, page.data[0].episode.id)
            assertEquals(episodeEntities[1].id, page.data[1].episode.id)
            assertEquals(episodeEntities[0].id, page.data[2].episode.id)
        }

    @Test
    fun `Given no episodes with cache key, When getEpisodesOldestCachedAtByCacheKey is called, Then null is returned`() =
        runTest {
            // Given - No episodes inserted

            // When
            val oldestCachedAt = dao.getEpisodesOldestCachedAtByCacheKey("non_existent_key")

            // Then
            assertEquals(null, oldestCachedAt)
        }

    @Test
    fun `Given one episode with cache key, When getEpisodesOldestCachedAtByCacheKey is called, Then that cachedAt is returned`() =
        runTest {
            // Given
            val now = Instant.fromEpochSeconds(Clock.System.now().epochSeconds)
            dao.upsertEpisode(episodeEntity.copy(cacheKey = "test_key", cachedAt = now))

            // When
            val oldestCachedAt = dao.getEpisodesOldestCachedAtByCacheKey("test_key")

            // Then
            assertEquals(now, oldestCachedAt)
        }

    @Test
    fun `Given multiple episodes with same cache key, When getEpisodesOldestCachedAtByCacheKey is called, Then oldest cachedAt is returned`() =
        runTest {
            // Given
            val now = Instant.fromEpochSeconds(Clock.System.now().epochSeconds)
            val oldestTime = now
            val middleTime = now.plus(1.minutes)
            val newestTime = now.plus(2.minutes)

            dao.upsertEpisode(episodeEntities[0].copy(cacheKey = "test_key", cachedAt = middleTime))
            dao.upsertEpisode(episodeEntities[1].copy(cacheKey = "test_key", cachedAt = oldestTime))
            dao.upsertEpisode(episodeEntities[2].copy(cacheKey = "test_key", cachedAt = newestTime))

            // When
            val oldestCachedAt = dao.getEpisodesOldestCachedAtByCacheKey("test_key")

            // Then
            assertEquals(oldestTime, oldestCachedAt)
        }

    @Test
    fun `Given episodes with different cache keys, When getEpisodesOldestCachedAtByCacheKey is called, Then only matching cache key episodes are considered`() =
        runTest {
            // Given
            val now = Instant.fromEpochSeconds(Clock.System.now().epochSeconds)
            val key1OldestTime = now.plus(5.minutes)
            val key2OldestTime = now

            dao.upsertEpisode(episodeEntities[0].copy(cacheKey = "key1", cachedAt = key1OldestTime))
            dao.upsertEpisode(
                episodeEntities[1].copy(
                    cacheKey = "key1",
                    cachedAt = now.plus(10.minutes)
                )
            )
            dao.upsertEpisode(episodeEntities[2].copy(cacheKey = "key2", cachedAt = key2OldestTime))
            dao.upsertEpisode(
                episodeEntities[3].copy(
                    cacheKey = "key2",
                    cachedAt = now.plus(3.minutes)
                )
            )

            // When
            val key1OldestCachedAt = dao.getEpisodesOldestCachedAtByCacheKey("key1")
            val key2OldestCachedAt = dao.getEpisodesOldestCachedAtByCacheKey("key2")

            // Then
            assertEquals(key1OldestTime, key1OldestCachedAt)
            assertEquals(key2OldestTime, key2OldestCachedAt)
        }
}