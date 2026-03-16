package io.jacob.episodive.core.data.util.paging

import androidx.paging.PagingSource
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import io.jacob.episodive.core.database.datasource.EpisodeLocalDataSource
import io.jacob.episodive.core.database.datasource.SoundbiteLocalDataSource
import io.jacob.episodive.core.database.model.EpisodeWithExtrasView
import io.jacob.episodive.core.database.model.SoundbiteEntity
import io.jacob.episodive.core.model.GroupKey
import io.jacob.episodive.core.network.datasource.EpisodeRemoteDataSource
import io.jacob.episodive.core.network.datasource.SoundbiteRemoteDataSource
import io.jacob.episodive.core.network.model.SoundbiteResponse
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.jacob.episodive.core.testing.util.loadPage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class SoundbiteEpisodePagingSourceTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val invalidationTracker = mockk<InvalidationTracker>(relaxed = true)
    private val database = mockk<RoomDatabase>(relaxed = true) {
        every { invalidationTracker } returns this@SoundbiteEpisodePagingSourceTest.invalidationTracker
    }
    private val episodeLocal = mockk<EpisodeLocalDataSource>(relaxed = true)
    private val episodeRemote = mockk<EpisodeRemoteDataSource>(relaxed = true)
    private val soundbiteLocal = mockk<SoundbiteLocalDataSource>(relaxed = true)
    private val soundbiteRemote = mockk<SoundbiteRemoteDataSource>(relaxed = true)

    private fun createPagingSource(
        timeToLive: Duration = 10.minutes,
    ) = SoundbiteEpisodePagingSource(
        database = database,
        episodeLocal = episodeLocal,
        episodeRemote = episodeRemote,
        soundbiteLocal = soundbiteLocal,
        soundbiteRemote = soundbiteRemote,
        timeToLive = timeToLive,
    )

    private fun mockEpisodeWithExtrasView(id: Long): EpisodeWithExtrasView =
        mockk(relaxed = true) {
            every { episode } returns mockk(relaxed = true) {
                every { this@mockk.id } returns id
            }
        }

    private fun mockSoundbiteEntity(episodeId: Long): SoundbiteEntity =
        mockk(relaxed = true) {
            every { this@mockk.episodeId } returns episodeId
        }

    @Test
    fun `Given fresh cache with all episodes cached, When load, Then returns episodes in soundbite order`() =
        runTest {
            // Given
            val soundbites = listOf(
                mockSoundbiteEntity(100L),
                mockSoundbiteEntity(200L),
                mockSoundbiteEntity(300L),
            )
            val episodes = listOf(
                mockEpisodeWithExtrasView(100L),
                mockEpisodeWithExtrasView(200L),
                mockEpisodeWithExtrasView(300L),
            )
            coEvery { soundbiteLocal.getSoundbitesOldestCachedAt() } returns Clock.System.now()
            coEvery { soundbiteLocal.getSoundbitesPagingList(0, 10) } returns soundbites
            coEvery { episodeLocal.getEpisodesByIdsOnce(listOf(100L, 200L, 300L)) } returns episodes

            // When
            val page = createPagingSource().loadPage(loadSize = 10)

            // Then
            assertEquals(3, page.data.size)
            assertNull(page.prevKey)
            assertEquals(100L, page.data[0].episode.id)
            assertEquals(200L, page.data[1].episode.id)
            assertEquals(300L, page.data[2].episode.id)
        }

    @Test
    fun `Given expired cache, When load, Then fetches from remote and replaces local`() =
        runTest {
            // Given
            coEvery { soundbiteLocal.getSoundbitesOldestCachedAt() } returns null
            coEvery { soundbiteRemote.getSoundbites(1000) } returns listOf(
                mockk<SoundbiteResponse>(relaxed = true)
            )
            coEvery { soundbiteLocal.getSoundbitesPagingList(any(), any()) } returns emptyList()

            // When
            val page = createPagingSource().loadPage(loadSize = 10)

            // Then
            assertEquals(0, page.data.size)
            coVerify { soundbiteRemote.getSoundbites(1000) }
            coVerify { soundbiteLocal.replaceSoundbites(any()) }
            coVerify {
                episodeLocal.replaceEpisodes(
                    emptyList(),
                    GroupKey.SOUNDBITE.toString()
                )
            }
        }

    @Test
    fun `Given missing episodes, When load, Then fetches from remote and upserts`() =
        runTest {
            // Given
            val soundbites = listOf(
                mockSoundbiteEntity(100L),
                mockSoundbiteEntity(200L),
                mockSoundbiteEntity(300L),
            )
            val cachedEpisode = mockEpisodeWithExtrasView(100L)
            val allEpisodes = listOf(
                mockEpisodeWithExtrasView(100L),
                mockEpisodeWithExtrasView(200L),
                mockEpisodeWithExtrasView(300L),
            )

            coEvery { soundbiteLocal.getSoundbitesOldestCachedAt() } returns Clock.System.now()
            coEvery { soundbiteLocal.getSoundbitesPagingList(0, 10) } returns soundbites
            // First call in fetchMissingEpisodes returns only 1 cached episode
            // Second call after upsert returns all 3
            coEvery {
                episodeLocal.getEpisodesByIdsOnce(listOf(100L, 200L, 300L))
            } returns listOf(cachedEpisode) andThen allEpisodes
            coEvery { episodeRemote.getEpisodeById(any()) } returns mockk(relaxed = true)

            // When
            val page = createPagingSource().loadPage(loadSize = 10)

            // Then
            assertEquals(3, page.data.size)
            coVerify { episodeRemote.getEpisodeById(200L) }
            coVerify { episodeRemote.getEpisodeById(300L) }
            coVerify {
                episodeLocal.upsertEpisodesWithGroup(
                    any(),
                    GroupKey.SOUNDBITE.toString()
                )
            }
        }

    @Test
    fun `Given no soundbites, When load, Then returns empty page`() =
        runTest {
            // Given
            coEvery { soundbiteLocal.getSoundbitesOldestCachedAt() } returns Clock.System.now()
            coEvery { soundbiteLocal.getSoundbitesPagingList(0, 10) } returns emptyList()

            // When
            val page = createPagingSource().loadPage(loadSize = 10)

            // Then
            assertEquals(0, page.data.size)
            assertNull(page.prevKey)
            assertNull(page.nextKey)
        }

    @Test
    fun `Given first page full results, When load, Then prevKey null and nextKey is offset plus limit`() =
        runTest {
            // Given
            val soundbites = (1L..5L).map { mockSoundbiteEntity(it) }
            val episodes = (1L..5L).map { mockEpisodeWithExtrasView(it) }

            coEvery { soundbiteLocal.getSoundbitesOldestCachedAt() } returns Clock.System.now()
            coEvery { soundbiteLocal.getSoundbitesPagingList(0, 5) } returns soundbites
            coEvery { episodeLocal.getEpisodesByIdsOnce(any()) } returns episodes

            // When
            val page = createPagingSource().loadPage(loadSize = 5)

            // Then
            assertEquals(5, page.data.size)
            assertNull(page.prevKey)
            assertEquals(5, page.nextKey)
        }

    @Test
    fun `Given middle page, When load with key, Then correct prev and next keys`() =
        runTest {
            // Given
            val soundbites = (1L..5L).map { mockSoundbiteEntity(it) }
            val episodes = (1L..5L).map { mockEpisodeWithExtrasView(it) }

            coEvery { soundbiteLocal.getSoundbitesOldestCachedAt() } returns Clock.System.now()
            coEvery { soundbiteLocal.getSoundbitesPagingList(5, 5) } returns soundbites
            coEvery { episodeLocal.getEpisodesByIdsOnce(any()) } returns episodes

            // When
            val page = createPagingSource().loadPage(key = 5, loadSize = 5)

            // Then
            assertEquals(5, page.data.size)
            assertEquals(0, page.prevKey)
            assertEquals(10, page.nextKey)
        }

    @Test
    fun `Given last page partial results, When load, Then nextKey null`() =
        runTest {
            // Given
            val soundbites = (1L..2L).map { mockSoundbiteEntity(it) }
            val episodes = (1L..2L).map { mockEpisodeWithExtrasView(it) }

            coEvery { soundbiteLocal.getSoundbitesOldestCachedAt() } returns Clock.System.now()
            coEvery { soundbiteLocal.getSoundbitesPagingList(10, 5) } returns soundbites
            coEvery { episodeLocal.getEpisodesByIdsOnce(any()) } returns episodes

            // When
            val page = createPagingSource().loadPage(key = 10, loadSize = 5)

            // Then
            assertEquals(2, page.data.size)
            assertEquals(5, page.prevKey)
            assertNull(page.nextKey)
        }

    @Test
    fun `Given exception during load, When load, Then returns LoadResult Error`() =
        runTest {
            // Given
            coEvery {
                soundbiteLocal.getSoundbitesOldestCachedAt()
            } throws RuntimeException("test error")

            // When
            val result = createPagingSource().load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 10,
                    placeholdersEnabled = false
                )
            )

            // Then
            assertTrue(result is PagingSource.LoadResult.Error)
            val error = result as PagingSource.LoadResult.Error
            assertEquals("test error", error.throwable.message)
        }
}
