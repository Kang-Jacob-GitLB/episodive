package io.jacob.episodive.core.database.datasource

import io.jacob.episodive.core.database.dao.EpisodeDao
import io.jacob.episodive.core.database.mapper.toEpisodeEntities
import io.jacob.episodive.core.database.mapper.toEpisodeEntity
import io.jacob.episodive.core.database.model.LikedEpisodeEntity
import io.jacob.episodive.core.database.model.PlayedEpisodeEntity
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

class EpisodeLocalDataSourceTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val episodeDao = mockk<EpisodeDao>(relaxed = true)

    private val dataSource: EpisodeLocalDataSource = EpisodeLocalDataSourceImpl(
        episodeDao = episodeDao,
    )

    private val cacheKey = "test_cache"
    private val episodeEntity = episodeTestData.toEpisodeEntity(cacheKey = cacheKey)
    private val episodeEntities = episodeTestDataList.toEpisodeEntities(cacheKey = cacheKey)

    @Test
    fun `Given dependencies, When upsertEpisode is called, Then upsertEpisode of dao is called`() =
        runTest {
            // Given
            coEvery { episodeDao.upsertEpisode(any()) } just Runs

            // When
            dataSource.upsertEpisode(episodeEntity)

            // Then
            coVerify { episodeDao.upsertEpisode(episodeEntity) }
            confirmVerified(
                episodeDao,
            )
        }

    @Test
    fun `Given dependencies, When upsertEpisodes is called, Then upsertEpisodes of dao is called`() =
        runTest {
            // Given
            coEvery { episodeDao.upsertEpisodes(any()) } just Runs

            // When
            dataSource.upsertEpisodes(episodeEntities)

            // Then
            coVerify { episodeDao.upsertEpisodes(episodeEntities) }
            confirmVerified(
                episodeDao,
            )
        }

    @Test
    fun `Given dependencies, When deleteEpisode is called, Then deleteEpisode of dao is called`() =
        runTest {
            // Given
            coEvery { episodeDao.deleteEpisode(any()) } just Runs

            // When
            dataSource.deleteEpisode(episodeEntity.id)

            // Then
            coVerify { episodeDao.deleteEpisode(episodeEntity.id) }
            confirmVerified(
                episodeDao,
            )
        }

    @Test
    fun `Given dependencies, When deleteEpisodes is called, Then deleteEpisodes of dao is called`() =
        runTest {
            // Given
            coEvery { episodeDao.deleteEpisodes() } just Runs

            // When
            dataSource.deleteEpisodes()

            // Then
            coVerify { episodeDao.deleteEpisodes() }
            confirmVerified(
                episodeDao,
            )
        }

    @Test
    fun `Given dependencies, When deleteEpisodesByCacheKey is called, Then deleteEpisodesByCacheKey of dao is called`() =
        runTest {
            // Given
            coEvery { episodeDao.deleteEpisodesByCacheKey(any()) } just Runs

            // When
            dataSource.deleteEpisodesByCacheKey(cacheKey)

            // Then
            coVerify { episodeDao.deleteEpisodesByCacheKey(cacheKey) }
            confirmVerified(
                episodeDao,
            )
        }

    @Test
    fun `Given dependencies, When replaceEpisodes is called, Then replaceEpisodes of dao is called`() =
        runTest {
            // Given
            coEvery { episodeDao.replaceEpisodes(any()) } just Runs

            // When
            dataSource.replaceEpisodes(episodeEntities)

            // Then
            coVerify { episodeDao.replaceEpisodes(episodeEntities) }
            confirmVerified(
                episodeDao,
            )
        }

    @Test
    fun `Given dependencies, When updateDurationOfEpisodes is called, Then updateDurationOfEpisodes of dao is called`() =
        runTest {
            // Given
            coEvery { episodeDao.updateDurationOfEpisodes(any(), any()) } just Runs

            // When
            dataSource.updateDurationOfEpisodes(episodeEntities[0].id, 2000.seconds)

            // Then
            coVerify { episodeDao.updateDurationOfEpisodes(any(), any()) }
        }

    @Test
    fun `Given dependencies, When getEpisode is called, Then getEpisode of dao is called`() =
        runTest {
            // Given
            coEvery { episodeDao.getEpisodeById(any()) } returns mockk()

            // When
            dataSource.getEpisodeById(episodeEntity.id)

            // Then
            coVerify { episodeDao.getEpisodeById(episodeEntity.id) }
            confirmVerified(
                episodeDao,
            )
        }

    @Test
    fun `Given dependencies, When getEpisodes is called, Then getEpisodes of dao is called`() =
        runTest {
            // Given
            coEvery { episodeDao.getEpisodes(10) } returns mockk()

            // When
            dataSource.getEpisodes(10)

            // Then
            coVerify { episodeDao.getEpisodes(10) }
            confirmVerified(
                episodeDao,
            )
        }

    @Test
    fun `Given dependencies, When getEpisodesByCacheKey is called, Then getEpisodesByCacheKey of dao is called`() =
        runTest {
            // Given
            coEvery { episodeDao.getEpisodesByCacheKey(any(), 10) } returns mockk()

            // When
            dataSource.getEpisodesByCacheKey(cacheKey, 10)

            // Then
            coVerify { episodeDao.getEpisodesByCacheKey(cacheKey, 10) }
            confirmVerified(
                episodeDao,
            )
        }

    @Test
    fun `Given dependencies, When addLiked is called, Then addLiked of dao is called`() =
        runTest {
            // Given
            coEvery { episodeDao.addLiked(any()) } just Runs

            // When
            dataSource.addLiked(
                LikedEpisodeEntity(
                    id = episodeEntity.id,
                    likedAt = Clock.System.now()
                )
            )

            // Then
            coVerifySequence {
                episodeDao.addLiked(any())
            }
            confirmVerified(
                episodeDao,
            )
        }

    @Test
    fun `Given dependencies, When removeLiked is called, Then removeLiked of dao is called`() =
        runTest {
            // Given
            coEvery { episodeDao.removeLiked(any()) } just Runs

            // When
            dataSource.removeLiked(episodeEntity.id)

            // Then
            coVerifySequence {
                episodeDao.removeLiked(any())
            }
            confirmVerified(
                episodeDao,
            )
        }

    @Test
    fun `Given dependencies, When isLiked is called, Then isLiked of dao is called`() =
        runTest {
            // Given
            coEvery { episodeDao.isLiked(any()) } returns flowOf(false)

            // When
            dataSource.isLiked(episodeEntity.id)

            // Then
            coVerify { episodeDao.isLiked(episodeEntity.id) }
            confirmVerified(
                episodeDao,
            )
        }

    @Test
    fun `Given dependencies, When toggleLiked is called, Then toggleLiked of dao is called`() =
        runTest {
            // Given
            coEvery { episodeDao.toggleLiked(any()) } returns mockk()

            // When
            dataSource.toggleLiked(episodeEntity.id)

            // Then
            coVerify { episodeDao.toggleLiked(episodeEntity.id) }
        }

    @Test
    fun `Given dependencies, When getLikedEpisodes is called, Then getLikedEpisodes of dao is called`() =
        runTest {
            // Given
            coEvery { episodeDao.getLikedEpisodes(10) } returns mockk()

            // When
            dataSource.getLikedEpisodes(10)

            // Then
            coVerify { episodeDao.getLikedEpisodes(10) }
            confirmVerified(
                episodeDao,
            )
        }

    @Test
    fun `Given dependencies, When upsertPlayed is called, Then upsertPlayed of dao is called`() =
        runTest {
            // Given
            coEvery { episodeDao.upsertPlayed(any()) } just Runs

            // When
            dataSource.upsertPlayed(
                PlayedEpisodeEntity(
                    id = episodeEntity.id,
                    playedAt = Clock.System.now(),
                    position = 2000.seconds,
                )
            )

            // Then
            coVerify { episodeDao.upsertPlayed(any()) }
            confirmVerified(
                episodeDao,
            )
        }

    @Test
    fun `Given dependencies, When removePlayed is called, Then removePlayed of dao is called`() =
        runTest {
            // Given
            coEvery { episodeDao.removePlayed(any()) } just Runs

            // When
            dataSource.removePlayed(episodeEntity.id)

            // Then
            coVerify { episodeDao.removePlayed(episodeEntity.id) }
            confirmVerified(
                episodeDao,
            )
        }

    @Test
    fun `Given dependencies, When getPlayedEpisodes is called, Then getPlayedEpisodes of dao is called`() =
        runTest {
            // Given
            coEvery { episodeDao.getPlayedEpisodes(10) } returns mockk()

            // When
            dataSource.getPlayedEpisodes(10)

            // Then
            coVerify { episodeDao.getPlayedEpisodes(10) }
            confirmVerified(
                episodeDao,
            )
        }

}