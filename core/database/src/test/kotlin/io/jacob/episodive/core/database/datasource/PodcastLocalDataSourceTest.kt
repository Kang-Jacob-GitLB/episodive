package io.jacob.episodive.core.database.datasource

import io.jacob.episodive.core.database.dao.PodcastDao
import io.jacob.episodive.core.database.mapper.toPodcastEntities
import io.jacob.episodive.core.database.mapper.toPodcastEntity
import io.jacob.episodive.core.testing.model.podcastTestData
import io.jacob.episodive.core.testing.model.podcastTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import androidx.room.RoomDatabase
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class PodcastLocalDataSourceTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dao = mockk<PodcastDao>(relaxed = true)
    private val database = mockk<RoomDatabase>(relaxed = true)

    private val dataSource: PodcastLocalDataSource = PodcastLocalDataSourceImpl(
        database = database,
        podcastDao = dao,
    )

    private val podcastEntity = podcastTestData.toPodcastEntity()
    private val podcastEntities = podcastTestDataList.toPodcastEntities()

    @After
    fun teardown() {
        confirmVerified(dao)
    }

    @Test
    fun `Given id, When getPodcastById is called, Then dao getPodcastById is called`() =
        runTest {
            // Given
            val id = podcastEntity.id

            // When
            dataSource.getPodcastById(id)

            // Then
            coVerify {
                dao.getPodcastById(id)
            }
        }

    @Test
    fun `Given ids, When getPodcastsByIds is called, Then dao getPodcastsByIds is called`() =
        runTest {
            // Given
            val ids = listOf(1L, 2L, 3L)

            // When
            dataSource.getPodcastsByIds(ids)

            // Then
            coVerify {
                dao.getPodcastsByIds(ids)
            }
        }

    @Test
    fun `Given query and limit, When getPodcasts is called, Then dao getPodcasts is called`() =
        runTest {
            // Given
            val query = "test"
            val limit = 10

            // When
            dataSource.getPodcasts(query, limit)

            // Then
            coVerify {
                dao.getPodcasts("*$query*", limit)
            }
        }

    @Test
    fun `Given blank query and limit, When getPodcasts is called, Then dao getPodcasts is called with null query`() =
        runTest {
            // Given
            val query = "  "
            val limit = 10

            // When
            dataSource.getPodcasts(query, limit)

            // Then
            coVerify {
                dao.getPodcasts(null, limit)
            }
        }

    @Test
    fun `Given null query and limit, When getPodcasts is called, Then dao getPodcasts is called with null query`() =
        runTest {
            // Given
            val limit = 10

            // When
            dataSource.getPodcasts(null, limit)

            // Then
            coVerify {
                dao.getPodcasts(null, limit)
            }
        }

    @Test
    fun `Given query, When getPodcastsPaging is called, Then dao getPodcastsPaging is called`() =
        runTest {
            // Given
            val query = "test"

            // When
            dataSource.getPodcastsPaging(query)

            // Then
            coVerify {
                dao.getPodcastsPaging("*$query*")
            }
        }

    @Test
    fun `Given blank query, When getPodcastsPaging is called, Then dao getPodcastsPaging is called with null query`() =
        runTest {
            // Given
            val query = "  "

            // When
            dataSource.getPodcastsPaging(query)

            // Then
            coVerify {
                dao.getPodcastsPaging(null)
            }
        }

    @Test
    fun `Given groupKey and limit, When getPodcastsByGroupKey is called, Then dao getPodcastsByGroupKey is called`() =
        runTest {
            // Given
            val groupKey = "testGroup"
            val limit = 10

            // When
            dataSource.getPodcastsByGroupKey(groupKey, limit)

            // Then
            coVerify {
                dao.getPodcastsByGroupKey(groupKey, limit)
            }
        }

    @Test
    fun `Given groupKey, When getPodcastsByGroupKeyPaging is called, Then dao getPodcastsByGroupKeyPaging is called`() =
        runTest {
            // Given
            val groupKey = "testGroup"

            // When
            dataSource.getPodcastsByGroupKeyPaging(groupKey)

            // Then
            coVerify {
                dao.getPodcastsByGroupKeyPaging(groupKey)
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
    fun `Given podcasts and groupKey, When replacePodcasts is called, Then dao replacePodcasts is called`() =
        runTest {
            // Given
            val groupKey = "testGroup"

            // When
            dataSource.replacePodcasts(podcastEntities, groupKey)

            // Then
            coVerify {
                dao.replacePodcasts(podcastEntities, groupKey)
            }
        }

    @Test
    fun `Given id, When isFollowedPodcast is called, Then dao isFollowedPodcast is called`() =
        runTest {
            // Given
            val id = podcastEntity.id

            // When
            dataSource.isFollowedPodcast(id)

            // Then
            coVerify {
                dao.isFollowedPodcast(id)
            }
        }

    @Test
    fun `Given id, When toggleFollowedPodcast is called, Then dao toggleFollowedPodcast is called`() =
        runTest {
            // Given
            val id = podcastEntity.id

            // When
            dataSource.toggleFollowedPodcast(id)

            // Then
            coVerify {
                dao.toggleFollowedPodcast(id)
            }
        }

    @Test
    fun `Given query and limit, When getFollowedPodcasts is called, Then dao getFollowedPodcasts is called`() =
        runTest {
            // Given
            val query = "test"
            val limit = 10

            // When
            dataSource.getFollowedPodcasts(query, limit)

            // Then
            coVerify {
                dao.getFollowedPodcasts("*$query*", limit)
            }
        }

    @Test
    fun `Given blank query and limit, When getFollowedPodcasts is called, Then dao getFollowedPodcasts is called with null query`() =
        runTest {
            // Given
            val query = "  "
            val limit = 10

            // When
            dataSource.getFollowedPodcasts(query, limit)

            // Then
            coVerify {
                dao.getFollowedPodcasts(null, limit)
            }
        }

    @Test
    fun `Given query, When getFollowedPodcastsPaging is called, Then dao getFollowedPodcastsPaging is called`() =
        runTest {
            // Given
            val query = "test"

            // When
            dataSource.getFollowedPodcastsPaging(query)

            // Then
            coVerify {
                dao.getFollowedPodcastsPaging("*$query*")
            }
        }

    @Test
    fun `Given blank query, When getFollowedPodcastsPaging is called, Then dao getFollowedPodcastsPaging is called with null query`() =
        runTest {
            // Given
            val query = "  "

            // When
            dataSource.getFollowedPodcastsPaging(query)

            // Then
            coVerify {
                dao.getFollowedPodcastsPaging(null)
            }
        }

    @Test
    fun `Given podcasts, When upsertPodcastsWithGroup, Then calls dao upsertPodcastsWithGroup`() = runTest {
        // Given
        val groupKey = "trending:en"

        // When
        dataSource.upsertPodcastsWithGroup(listOf(podcastEntity), groupKey)

        // Then
        coVerify {
            dao.upsertPodcastsWithGroup(listOf(podcastEntity), groupKey)
        }
    }

    @Test
    fun `Given ids, When getPodcastsByIdsOnce, Then calls dao getPodcastsByIdsOnce`() = runTest {
        // Given
        val ids = listOf(1L, 2L, 3L)

        // When
        dataSource.getPodcastsByIdsOnce(ids)

        // Then
        coVerify {
            dao.getPodcastsByIdsOnce(ids)
        }
    }
}