package io.jacob.episodive.core.database.dao

import io.jacob.episodive.core.database.mapper.toEpisodeEntities
import io.jacob.episodive.core.database.model.EpisodeGroupEntity
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * Transaction 함수의 내부 호출을 검증하는 Mock 기반 단위 테스트.
 *
 * 주의: 이 테스트는 실제 데이터베이스 동작을 검증하지 않습니다.
 * 실제 동작 검증은 [EpisodeDaoTest]의 통합 테스트를 참고하세요.
 */
class EpisodeDaoTransactionTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dao = mockk<EpisodeDao>(relaxed = true)

    private val episodeEntities = episodeTestDataList.toEpisodeEntities()

    @Test
    fun `Given episodes and groupKey, When upsertEpisodesWithGroup is called, Then upsertEpisodes and upsertEpisodeGroups are called`() =
        runTest {
            // Given
            coEvery { dao.upsertEpisodes(any()) } just Runs
            coEvery { dao.upsertEpisodeGroups(any()) } just Runs
            coEvery { dao.upsertEpisodesWithGroup(any(), any()) } answers {
                callOriginal()
            }

            // When
            dao.upsertEpisodesWithGroup(episodeEntities, "test_group")

            // Then
            coVerify(exactly = 1) {
                dao.upsertEpisodes(episodeEntities)
            }
            coVerify(exactly = 1) {
                dao.upsertEpisodeGroups(match { groups ->
                    groups.size == episodeEntities.size &&
                            groups.all { it.groupKey == "test_group" }
                })
            }
        }

    @Test
    fun `Given episodes and groupKey, When replaceEpisodes is called, Then correct sequence is called`() =
        runTest {
            // Given
            val oldEpisodeIds = listOf(1L, 2L, 3L)

            coEvery { dao.getEpisodeGroupsByGroupKey("test_group") } returns oldEpisodeIds.map {
                EpisodeGroupEntity(
                    groupKey = "test_group",
                    id = it,
                    order = 0,
                    createdAt = kotlin.time.Clock.System.now(),
                )
            }
            coEvery { dao.deleteEpisodeGroupsByGroupKey(any()) } just Runs
            coEvery { dao.upsertEpisodesWithGroup(any(), any()) } just Runs
            coEvery { dao.deleteEpisodesIfOrphaned(any()) } just Runs
            coEvery { dao.replaceEpisodes(any(), any()) } answers {
                callOriginal()
            }

            // When
            dao.replaceEpisodes(episodeEntities, "test_group")

            // Then
            coVerify(exactly = 1) {
                dao.getEpisodeGroupsByGroupKey("test_group")
            }
            coVerify(exactly = 1) {
                dao.deleteEpisodeGroupsByGroupKey("test_group")
            }
            coVerify(exactly = 1) {
                dao.upsertEpisodesWithGroup(episodeEntities, "test_group")
            }
            coVerify(exactly = 1) {
                dao.deleteEpisodesIfOrphaned(oldEpisodeIds)
            }
        }

    @Test
    fun `Given liked episode, When toggleLikedEpisode is called, Then removeLikedEpisode and deleteEpisodesIfOrphaned are called`() =
        runTest {
            // Given
            val episode = episodeEntities[0]

            coEvery { dao.isLikedEpisode(any()) } returns flowOf(true)
            coEvery { dao.removeLikedEpisode(any()) } just Runs
            coEvery { dao.deleteEpisodesIfOrphaned(any()) } just Runs
            coEvery { dao.toggleLikedEpisode(any()) } answers {
                callOriginal()
            }

            // When
            val result = dao.toggleLikedEpisode(episode)

            // Then
            coVerify(exactly = 1) {
                dao.removeLikedEpisode(episode.id)
            }
            coVerify(exactly = 1) {
                dao.deleteEpisodesIfOrphaned(listOf(episode.id))
            }
            assert(!result)
        }

    @Test
    fun `Given unliked episode, When toggleLikedEpisode is called, Then addLikedEpisode is called`() =
        runTest {
            // Given
            val episode = episodeEntities[0]

            coEvery { dao.isLikedEpisode(any()) } returns flowOf(false)
            coEvery { dao.addLikedEpisode(any()) } just Runs
            coEvery { dao.toggleLikedEpisode(any()) } answers {
                callOriginal()
            }

            // When
            val result = dao.toggleLikedEpisode(episode)

            // Then
            coVerify(exactly = 1) {
                dao.addLikedEpisode(match { it.id == episode.id })
            }
            coVerify(exactly = 0) {
                dao.deleteEpisodesIfOrphaned(any())
            }
            assert(result)
        }

    @Test
    fun `Given played episode, When removePlayedEpisode is called, Then deletePlayedEpisode and deleteEpisodesIfOrphaned are called`() =
        runTest {
            // Given
            val episodeId = 100L

            coEvery { dao.deletePlayedEpisode(any()) } just Runs
            coEvery { dao.deleteEpisodesIfOrphaned(any()) } just Runs
            coEvery { dao.removePlayedEpisode(any()) } answers {
                callOriginal()
            }

            // When
            dao.removePlayedEpisode(episodeId)

            // Then
            coVerify(exactly = 1) {
                dao.deletePlayedEpisode(episodeId)
            }
            coVerify(exactly = 1) {
                dao.deleteEpisodesIfOrphaned(listOf(episodeId))
            }
        }
}