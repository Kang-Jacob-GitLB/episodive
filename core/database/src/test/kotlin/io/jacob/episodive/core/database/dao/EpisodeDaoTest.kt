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
            dao.getEpisode(episodeTestData.id).test {
                val episode = awaitItem()
                // Then
                assertEquals(episodeEntity.id, episode?.id)
                assertEquals(
                    now.plus(2.minutes), episode?.cachedAt
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
            dao.getEpisodes().test {
                val episodes = awaitItem()
                // Then
                val episodeIds = episodeEntities.map { it.id }
                val entityIds = episodes.map { it.id }
                assertTrue(episodeIds.containsAll(entityIds))
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
            dao.getEpisodesByCacheKey("test_key1").test {
                val episodes = awaitItem()
                // Then
                assertEquals(entities[0].size, episodes.size)
                val episodeIds = entities[0].map { it.id }
                val entityIds = episodes.map { it.id }
                assertTrue(episodeIds.containsAll(entityIds))
                cancel()
            }
        }

    @Test
    fun `Given some episode entities, When deleteEpisode is called, Then the episode is deleted`() =
        runTest {
            // Given
            dao.upsertEpisodes(episodeEntities)

            // When
            dao.deleteEpisode(episodeEntity.id)
            dao.getEpisodes().test {
                val episodes = awaitItem()
                // Then
                assertFalse(episodes.contains(episodeEntity))
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
            dao.getEpisodes().test {
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
            dao.getEpisodesByCacheKey("test_key2").test {
                val episodes = awaitItem()
                // Then
                assertTrue(episodes.isEmpty())
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
            val likedEpisodes = dao.getLikedEpisodes().first()

            // Then
            assertEquals(3, likedEpisodes.size)
            assertEquals(episodeEntities[2].id, likedEpisodes[0].id)
            assertEquals(episodeEntities[1].id, likedEpisodes[1].id)
            assertEquals(episodeEntities[0].id, likedEpisodes[2].id)
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
            dao.getLikedEpisodes().test {
                val likedEpisodes = awaitItem()
                // Then
                assertEquals(3, likedEpisodes.size)
                assertEquals(episodeEntities[2].id, likedEpisodes[0].id)
                assertEquals(episodeEntities[1].id, likedEpisodes[1].id)
                assertEquals(episodeEntities[0].id, likedEpisodes[2].id)
                cancel()
            }
        }

    @Test
    fun `Given some episode entity liked, When isLiked is called, Then true is returned`() =
        runTest {
            // Given
            val likedAt = Clock.System.now()
            dao.addLiked(LikedEpisodeEntity(episodeEntity.id, likedAt))
            dao.upsertEpisode(episodeEntity)

            // When
            val isLiked = dao.isLiked(episodeEntity.id)

            // Then
            assertTrue(isLiked)
        }

    @Test
    fun `Given some episode entities liked, When toggleLiked is called, Then getLikedEpisodes returns correct`() =
        runTest {
            // Given
            val likedAt = Clock.System.now()
            dao.addLiked(LikedEpisodeEntity(episodeEntities[0].id, likedAt))
            dao.addLiked(LikedEpisodeEntity(episodeEntities[1].id, likedAt.plus(1.minutes)))

            // When
            dao.toggleLiked(episodeEntities[0].id)

            dao.getLikedEpisodes().test {
                val likedEpisodes = awaitItem()
                // Then
                assertEquals(1, likedEpisodes.size)
                assertEquals(episodeEntities[1].id, likedEpisodes[0].id)
                cancel()
            }

            // When
            dao.toggleLiked(episodeEntities[0].id)

            dao.getLikedEpisodes().test {
                val likedEpisodes = awaitItem()
                // Then
                assertEquals(2, likedEpisodes.size)
                assertEquals(episodeEntities[1].id, likedEpisodes[0].id)
                assertEquals(episodeEntities[0].id, likedEpisodes[1].id)
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
            dao.getPlayedEpisodes().test {
                val playedEpisodes = awaitItem()
                // Then
                assertEquals(3, playedEpisodes.size)
                assertEquals(episodeEntities[2].id, playedEpisodes[0].id)
                assertEquals(episodeEntities[1].id, playedEpisodes[1].id)
                assertEquals(episodeEntities[0].id, playedEpisodes[2].id)
            }
        }
}