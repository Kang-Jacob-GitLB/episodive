package io.jacob.episodive.core.data.repository

import app.cash.turbine.test
import io.jacob.episodive.core.database.datasource.RecentSearchLocalDataSource
import io.jacob.episodive.core.database.model.RecentSearchEntity
import io.jacob.episodive.core.domain.repository.RecentSearchRepository
import io.jacob.episodive.core.model.RecentSearch
import io.jacob.episodive.core.model.RecentSearchType
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.podcastTestData
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RecentSearchRepositoryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val recentSearchLocalDataSource = mockk<RecentSearchLocalDataSource>(relaxed = true)

    private val repository: RecentSearchRepository = RecentSearchRepositoryImpl(
        recentSearchLocalDataSource = recentSearchLocalDataSource
    )

    @After
    fun teardown() {
        confirmVerified(recentSearchLocalDataSource)
    }

    @Test
    fun `Given dependencies, When getRecentSearches called, Then call dataSource method`() =
        runTest {
            // Given
            coEvery {
                recentSearchLocalDataSource.getRecentSearches(any())
            } returns mockk()

            // When
            repository.getRecentSearches(3)

            // Then
            coVerifySequence {
                recentSearchLocalDataSource.getRecentSearches(3)
            }
        }

    @Test
    fun `Given query entity, When getRecentSearches called, Then mapped to RecentSearch Query`() =
        runTest {
            // Given
            val now = kotlin.time.Clock.System.now()
            val entity = RecentSearchEntity(
                id = 1L,
                type = RecentSearchType.QUERY,
                query = "test query",
                searchedAt = now,
            )
            coEvery {
                recentSearchLocalDataSource.getRecentSearches(any())
            } returns flowOf(listOf(entity))

            // When
            repository.getRecentSearches(10).test {
                val result = awaitItem()

                // Then
                assertEquals(1, result.size)
                assertTrue(result[0] is RecentSearch.Query)
                val query = result[0] as RecentSearch.Query
                assertEquals(1L, query.id)
                assertEquals("test query", query.query)
                assertEquals(now, query.searchedAt)
                awaitComplete()
            }

            coVerifySequence {
                recentSearchLocalDataSource.getRecentSearches(10)
            }
        }

    @Test
    fun `Given podcast entity, When getRecentSearches called, Then mapped to RecentSearch PodcastSearch`() =
        runTest {
            // Given
            val now = kotlin.time.Clock.System.now()
            val entity = RecentSearchEntity(
                id = 2L,
                type = RecentSearchType.PODCAST,
                contentId = 100L,
                title = "Test Podcast",
                imageUrl = "https://example.com/image.jpg",
                subtitle = "Test Author",
                searchedAt = now,
            )
            coEvery {
                recentSearchLocalDataSource.getRecentSearches(any())
            } returns flowOf(listOf(entity))

            // When
            repository.getRecentSearches(10).test {
                val result = awaitItem()

                // Then
                assertEquals(1, result.size)
                assertTrue(result[0] is RecentSearch.PodcastSearch)
                val podcast = result[0] as RecentSearch.PodcastSearch
                assertEquals(2L, podcast.id)
                assertEquals(100L, podcast.podcastId)
                assertEquals("Test Podcast", podcast.title)
                assertEquals("https://example.com/image.jpg", podcast.imageUrl)
                assertEquals("Test Author", podcast.author)
                awaitComplete()
            }

            coVerifySequence {
                recentSearchLocalDataSource.getRecentSearches(10)
            }
        }

    @Test
    fun `Given episode entity, When getRecentSearches called, Then mapped to RecentSearch EpisodeSearch`() =
        runTest {
            // Given
            val now = kotlin.time.Clock.System.now()
            val entity = RecentSearchEntity(
                id = 3L,
                type = RecentSearchType.EPISODE,
                contentId = 200L,
                title = "Test Episode",
                imageUrl = "https://example.com/ep.jpg",
                subtitle = "Test Feed",
                searchedAt = now,
            )
            coEvery {
                recentSearchLocalDataSource.getRecentSearches(any())
            } returns flowOf(listOf(entity))

            // When
            repository.getRecentSearches(10).test {
                val result = awaitItem()

                // Then
                assertEquals(1, result.size)
                assertTrue(result[0] is RecentSearch.EpisodeSearch)
                val episode = result[0] as RecentSearch.EpisodeSearch
                assertEquals(3L, episode.id)
                assertEquals(200L, episode.episodeId)
                assertEquals("Test Episode", episode.title)
                assertEquals("https://example.com/ep.jpg", episode.imageUrl)
                assertEquals("Test Feed", episode.feedTitle)
                awaitComplete()
            }

            coVerifySequence {
                recentSearchLocalDataSource.getRecentSearches(10)
            }
        }

    @Test
    fun `Given query entity with null query, When getRecentSearches called, Then entity is filtered out`() =
        runTest {
            // Given
            val now = kotlin.time.Clock.System.now()
            val entity = RecentSearchEntity(
                id = 1L,
                type = RecentSearchType.QUERY,
                query = null,
                searchedAt = now,
            )
            coEvery {
                recentSearchLocalDataSource.getRecentSearches(any())
            } returns flowOf(listOf(entity))

            // When
            repository.getRecentSearches(10).test {
                val result = awaitItem()

                // Then
                assertEquals(0, result.size)
                awaitComplete()
            }

            coVerifySequence {
                recentSearchLocalDataSource.getRecentSearches(10)
            }
        }

    @Test
    fun `Given podcast entity with null contentId, When getRecentSearches called, Then entity is filtered out`() =
        runTest {
            // Given
            val now = kotlin.time.Clock.System.now()
            val entity = RecentSearchEntity(
                id = 2L,
                type = RecentSearchType.PODCAST,
                contentId = null,
                title = "Test Podcast",
                searchedAt = now,
            )
            coEvery {
                recentSearchLocalDataSource.getRecentSearches(any())
            } returns flowOf(listOf(entity))

            // When
            repository.getRecentSearches(10).test {
                val result = awaitItem()

                // Then
                assertEquals(0, result.size)
                awaitComplete()
            }

            coVerifySequence {
                recentSearchLocalDataSource.getRecentSearches(10)
            }
        }

    @Test
    fun `Given podcast entity with null title, When getRecentSearches called, Then entity is filtered out`() =
        runTest {
            // Given
            val now = kotlin.time.Clock.System.now()
            val entity = RecentSearchEntity(
                id = 2L,
                type = RecentSearchType.PODCAST,
                contentId = 100L,
                title = null,
                searchedAt = now,
            )
            coEvery {
                recentSearchLocalDataSource.getRecentSearches(any())
            } returns flowOf(listOf(entity))

            // When
            repository.getRecentSearches(10).test {
                val result = awaitItem()

                // Then
                assertEquals(0, result.size)
                awaitComplete()
            }

            coVerifySequence {
                recentSearchLocalDataSource.getRecentSearches(10)
            }
        }

    @Test
    fun `Given episode entity with null contentId, When getRecentSearches called, Then entity is filtered out`() =
        runTest {
            // Given
            val now = kotlin.time.Clock.System.now()
            val entity = RecentSearchEntity(
                id = 3L,
                type = RecentSearchType.EPISODE,
                contentId = null,
                title = "Test Episode",
                searchedAt = now,
            )
            coEvery {
                recentSearchLocalDataSource.getRecentSearches(any())
            } returns flowOf(listOf(entity))

            // When
            repository.getRecentSearches(10).test {
                val result = awaitItem()

                // Then
                assertEquals(0, result.size)
                awaitComplete()
            }

            coVerifySequence {
                recentSearchLocalDataSource.getRecentSearches(10)
            }
        }

    @Test
    fun `Given episode entity with null title, When getRecentSearches called, Then entity is filtered out`() =
        runTest {
            // Given
            val now = kotlin.time.Clock.System.now()
            val entity = RecentSearchEntity(
                id = 3L,
                type = RecentSearchType.EPISODE,
                contentId = 200L,
                title = null,
                searchedAt = now,
            )
            coEvery {
                recentSearchLocalDataSource.getRecentSearches(any())
            } returns flowOf(listOf(entity))

            // When
            repository.getRecentSearches(10).test {
                val result = awaitItem()

                // Then
                assertEquals(0, result.size)
                awaitComplete()
            }

            coVerifySequence {
                recentSearchLocalDataSource.getRecentSearches(10)
            }
        }

    @Test
    fun `Given mixed entities, When getRecentSearches called, Then valid entities are mapped and invalid filtered out`() =
        runTest {
            // Given
            val now = kotlin.time.Clock.System.now()
            val entities = listOf(
                RecentSearchEntity(id = 1L, type = RecentSearchType.QUERY, query = "valid", searchedAt = now),
                RecentSearchEntity(id = 2L, type = RecentSearchType.QUERY, query = null, searchedAt = now),
                RecentSearchEntity(id = 3L, type = RecentSearchType.PODCAST, contentId = 100L, title = "Podcast", searchedAt = now),
                RecentSearchEntity(id = 4L, type = RecentSearchType.EPISODE, contentId = null, title = "Episode", searchedAt = now),
            )
            coEvery {
                recentSearchLocalDataSource.getRecentSearches(any())
            } returns flowOf(entities)

            // When
            repository.getRecentSearches(10).test {
                val result = awaitItem()

                // Then
                assertEquals(2, result.size)
                assertTrue(result[0] is RecentSearch.Query)
                assertTrue(result[1] is RecentSearch.PodcastSearch)
                awaitComplete()
            }

            coVerifySequence {
                recentSearchLocalDataSource.getRecentSearches(10)
            }
        }

    @Test
    fun `Given podcast entity with null imageUrl and subtitle, When getRecentSearches called, Then defaults to empty strings`() =
        runTest {
            // Given
            val now = kotlin.time.Clock.System.now()
            val entity = RecentSearchEntity(
                id = 2L,
                type = RecentSearchType.PODCAST,
                contentId = 100L,
                title = "Podcast",
                imageUrl = null,
                subtitle = null,
                searchedAt = now,
            )
            coEvery {
                recentSearchLocalDataSource.getRecentSearches(any())
            } returns flowOf(listOf(entity))

            // When
            repository.getRecentSearches(10).test {
                val result = awaitItem()

                // Then
                assertEquals(1, result.size)
                val podcast = result[0] as RecentSearch.PodcastSearch
                assertEquals("", podcast.imageUrl)
                assertEquals("", podcast.author)
                awaitComplete()
            }

            coVerifySequence {
                recentSearchLocalDataSource.getRecentSearches(10)
            }
        }

    @Test
    fun `Given episode entity with null imageUrl and subtitle, When getRecentSearches called, Then defaults to empty strings`() =
        runTest {
            // Given
            val now = kotlin.time.Clock.System.now()
            val entity = RecentSearchEntity(
                id = 3L,
                type = RecentSearchType.EPISODE,
                contentId = 200L,
                title = "Episode",
                imageUrl = null,
                subtitle = null,
                searchedAt = now,
            )
            coEvery {
                recentSearchLocalDataSource.getRecentSearches(any())
            } returns flowOf(listOf(entity))

            // When
            repository.getRecentSearches(10).test {
                val result = awaitItem()

                // Then
                assertEquals(1, result.size)
                val episode = result[0] as RecentSearch.EpisodeSearch
                assertEquals("", episode.imageUrl)
                assertEquals("", episode.feedTitle)
                awaitComplete()
            }

            coVerifySequence {
                recentSearchLocalDataSource.getRecentSearches(10)
            }
        }

    @Test
    fun `Given dependencies, When upsertRecentSearch called, Then call dataSource method`() =
        runTest {
            // Given
            coEvery {
                recentSearchLocalDataSource.upsertRecentSearch(any())
            } just Runs

            // When
            repository.upsertRecentSearch("query")

            // Then
            coVerifySequence {
                recentSearchLocalDataSource.upsertRecentSearch(any())
            }
        }

    @Test
    fun `Given dependencies, When deleteRecentSearch called, Then call dataSource method`() =
        runTest {
            // Given
            coEvery {
                recentSearchLocalDataSource.deleteRecentSearch(any())
            } just Runs

            // When
            val recentSearch = RecentSearch.Query(id = 1, query = "query", searchedAt = kotlin.time.Clock.System.now())
            repository.deleteRecentSearch(recentSearch)

            // Then
            coVerifySequence {
                recentSearchLocalDataSource.deleteRecentSearch(any())
            }
        }

    @Test
    fun `Given dependencies, When clearRecentSearches called, Then call dataSource method`() =
        runTest {
            // Given
            coEvery {
                recentSearchLocalDataSource.clearRecentSearches()
            } just Runs

            // When
            repository.clearRecentSearches()

            // Then
            coVerifySequence {
                recentSearchLocalDataSource.clearRecentSearches()
            }
        }

    @Test
    fun `Given podcast, When upsertRecentSearch with podcast called, Then call dataSource method`() =
        runTest {
            // Given
            coEvery {
                recentSearchLocalDataSource.upsertRecentSearch(any())
            } just Runs

            // When
            repository.upsertRecentSearch(podcastTestData)

            // Then
            coVerifySequence {
                recentSearchLocalDataSource.upsertRecentSearch(any())
            }
        }

    @Test
    fun `Given episode, When upsertRecentSearch with episode called, Then call dataSource method`() =
        runTest {
            // Given
            coEvery {
                recentSearchLocalDataSource.upsertRecentSearch(any())
            } just Runs

            // When
            repository.upsertRecentSearch(episodeTestData)

            // Then
            coVerifySequence {
                recentSearchLocalDataSource.upsertRecentSearch(any())
            }
        }
}