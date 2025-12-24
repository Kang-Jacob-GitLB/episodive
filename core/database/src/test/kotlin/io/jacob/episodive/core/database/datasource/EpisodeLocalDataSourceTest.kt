package io.jacob.episodive.core.database.datasource

import io.jacob.episodive.core.database.dao.EpisodeDao
import io.jacob.episodive.core.database.mapper.toEpisodeEntities
import io.jacob.episodive.core.database.mapper.toEpisodeEntity
import io.jacob.episodive.core.database.model.PlayedEpisodeEntity
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class EpisodeLocalDataSourceTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dao = mockk<EpisodeDao>(relaxed = true)

    private val dataSource: EpisodeLocalDataSource = EpisodeLocalDataSourceImpl(
        episodeDao = dao,
    )

    private val episodeEntity = episodeTestData.toEpisodeEntity()
    private val episodeEntities = episodeTestDataList.toEpisodeEntities()

    @After
    fun teardown() {
        confirmVerified(dao)
    }

    @Test
    fun `Given id and duration, When updateEpisodeDuration is called, Then dao updateEpisodeDuration is called`() =
        runTest {
            // Given
            val id = episodeEntity.id
            val duration = 1.minutes

            // When
            dataSource.updateEpisodeDuration(id, duration)

            // Then
            coVerify {
                dao.updateEpisodeDuration(id, duration)
            }
        }

    @Test
    fun `Given id, When getEpisodeById is called, Then dao getEpisodeById is called`() =
        runTest {
            // Given
            val id = episodeEntity.id

            // When
            dataSource.getEpisodeById(id)

            // Then
            coVerify {
                dao.getEpisodeById(id)
            }
        }

    @Test
    fun `Given ids, When getEpisodesByIds is called, Then dao getEpisodesByIds is called`() =
        runTest {
            // Given
            val ids = listOf(1L, 2L, 3L)

            // When
            dataSource.getEpisodesByIds(ids)

            // Then
            coVerify {
                dao.getEpisodesByIds(ids)
            }
        }

    @Test
    fun `Given query and limit, When getEpisodes is called, Then dao getEpisodes is called`() =
        runTest {
            // Given
            val query = "test"
            val limit = 10

            // When
            dataSource.getEpisodes(query, limit)

            // Then
            coVerify {
                dao.getEpisodes("*$query*", limit)
            }
        }

    @Test
    fun `Given blank query and limit, When getEpisodes is called, Then dao getEpisodes is called with null query`() =
        runTest {
            // Given
            val query = "  "
            val limit = 10

            // When
            dataSource.getEpisodes(query, limit)

            // Then
            coVerify {
                dao.getEpisodes(null, limit)
            }
        }

    @Test
    fun `Given null query and limit, When getEpisodes is called, Then dao getEpisodes is called with null query`() =
        runTest {
            // Given
            val limit = 10

            // When
            dataSource.getEpisodes(null, limit)

            // Then
            coVerify {
                dao.getEpisodes(null, limit)
            }
        }

    @Test
    fun `Given query, When getEpisodesPaging is called, Then dao getEpisodesPaging is called`() =
        runTest {
            // Given
            val query = "test"

            // When
            dataSource.getEpisodesPaging(query)

            // Then
            coVerify {
                dao.getEpisodesPaging("*$query*")
            }
        }

    @Test
    fun `Given blank query, When getEpisodesPaging is called, Then dao getEpisodesPaging is called with null query`() =
        runTest {
            // Given
            val query = "  "

            // When
            dataSource.getEpisodesPaging(query)

            // Then
            coVerify {
                dao.getEpisodesPaging(null)
            }
        }

    @Test
    fun `Given groupKey and limit, When getEpisodesByGroupKey is called, Then dao getEpisodesByGroupKey is called`() =
        runTest {
            // Given
            val groupKey = "testGroup"
            val limit = 10

            // When
            dataSource.getEpisodesByGroupKey(groupKey, limit)

            // Then
            coVerify {
                dao.getEpisodesByGroupKey(groupKey, limit)
            }
        }

    @Test
    fun `Given groupKey, When getEpisodesByGroupKeyPaging is called, Then dao getEpisodesByGroupKeyPaging is called`() =
        runTest {
            // Given
            val groupKey = "testGroup"

            // When
            dataSource.getEpisodesByGroupKeyPaging(groupKey)

            // Then
            coVerify {
                dao.getEpisodesByGroupKeyPaging(groupKey)
            }
        }

    @Test
    fun `Given groupKey, When getOldestCreatedAtByGroupKey is called, Then dao getOldestCreatedAtByGroupKey is called`() =
        runTest {
            // Given
            val groupKey = "testGroup"

            // When
            dataSource.getOldestCreatedAtByGroupKey(groupKey)

            // Then
            coVerify {
                dao.getOldestCreatedAtByGroupKey(groupKey)
            }
        }

    @Test
    fun `Given episodes and groupKey, When replaceEpisodes is called, Then dao replaceEpisodes is called`() =
        runTest {
            // Given
            val groupKey = "testGroup"

            // When
            dataSource.replaceEpisodes(episodeEntities, groupKey)

            // Then
            coVerify {
                dao.replaceEpisodes(episodeEntities, groupKey)
            }
        }

    @Test
    fun `Given id, When isLikedEpisode is called, Then dao isLikedEpisode is called`() =
        runTest {
            // Given
            val id = episodeEntity.id

            // When
            dataSource.isLikedEpisode(episodeEntity)

            // Then
            coVerify {
                dao.isLikedEpisode(id)
            }
        }

    @Test
    fun `Given id, When toggleLikedEpisode is called, Then dao toggleLikedEpisode is called`() =
        runTest {
            // When
            dataSource.toggleLikedEpisode(episodeEntity)

            // Then
            coVerify {
                dao.toggleLikedEpisode(episodeEntity)
            }
        }

    @Test
    fun `Given query and limit, When getLikedEpisodes is called, Then dao getLikedEpisodes is called`() =
        runTest {
            // Given
            val query = "test"
            val limit = 10

            // When
            dataSource.getLikedEpisodes(query, limit)

            // Then
            coVerify {
                dao.getLikedEpisodes("*$query*", limit)
            }
        }

    @Test
    fun `Given blank query and limit, When getLikedEpisodes is called, Then dao getLikedEpisodes is called with null query`() =
        runTest {
            // Given
            val query = "  "
            val limit = 10

            // When
            dataSource.getLikedEpisodes(query, limit)

            // Then
            coVerify {
                dao.getLikedEpisodes(null, limit)
            }
        }

    @Test
    fun `Given query, When getLikedEpisodesPaging is called, Then dao getLikedEpisodesPaging is called`() =
        runTest {
            // Given
            val query = "test"

            // When
            dataSource.getLikedEpisodesPaging(query)

            // Then
            coVerify {
                dao.getLikedEpisodesPaging("*$query*")
            }
        }

    @Test
    fun `Given blank query, When getLikedEpisodesPaging is called, Then dao getLikedEpisodesPaging is called with null query`() =
        runTest {
            // Given
            val query = "  "

            // When
            dataSource.getLikedEpisodesPaging(query)

            // Then
            coVerify {
                dao.getLikedEpisodesPaging(null)
            }
        }

    @Test
    fun `Given playedEpisode, When updatePlayedEpisode is called, Then dao upsertPlayedEpisode is called`() =
        runTest {
            // Given
            val playedEpisode = PlayedEpisodeEntity(
                id = episodeEntity.id,
                playedAt = Clock.System.now(),
                position = 1.minutes,
            )

            // When
            dataSource.updatePlayedEpisode(playedEpisode)

            // Then
            coVerify {
                dao.upsertPlayedEpisode(playedEpisode)
            }
        }

    @Test
    fun `Given id, When removePlayedEpisode is called, Then dao removePlayedEpisode is called`() =
        runTest {
            // Given
            val id = episodeEntity.id

            // When
            dataSource.removePlayedEpisode(id)

            // Then
            coVerify {
                dao.removePlayedEpisode(id)
            }
        }

    @Test
    fun `Given isCompleted, query and limit, When getPlayedEpisodes is called, Then dao getPlayedEpisodes is called`() =
        runTest {
            // Given
            val isCompleted = true
            val query = "test"
            val limit = 10

            // When
            dataSource.getPlayedEpisodes(isCompleted, query, limit)

            // Then
            coVerify {
                dao.getPlayedEpisodes(isCompleted, "*$query*", limit)
            }
        }

    @Test
    fun `Given isCompleted and blank query and limit, When getPlayedEpisodes is called, Then dao getPlayedEpisodes is called with null query`() =
        runTest {
            // Given
            val isCompleted = false
            val query = "  "
            val limit = 10

            // When
            dataSource.getPlayedEpisodes(isCompleted, query, limit)

            // Then
            coVerify {
                dao.getPlayedEpisodes(isCompleted, null, limit)
            }
        }

    @Test
    fun `Given isCompleted and query, When getPlayedEpisodesPaging is called, Then dao getPlayedEpisodesPaging is called`() =
        runTest {
            // Given
            val isCompleted = true
            val query = "test"

            // When
            dataSource.getPlayedEpisodesPaging(isCompleted, query)

            // Then
            coVerify {
                dao.getPlayedEpisodesPaging(isCompleted, "*$query*")
            }
        }

    @Test
    fun `Given isCompleted and blank query, When getPlayedEpisodesPaging is called, Then dao getPlayedEpisodesPaging is called with null query`() =
        runTest {
            // Given
            val isCompleted = false
            val query = "  "

            // When
            dataSource.getPlayedEpisodesPaging(isCompleted, query)

            // Then
            coVerify {
                dao.getPlayedEpisodesPaging(isCompleted, null)
            }
        }
}