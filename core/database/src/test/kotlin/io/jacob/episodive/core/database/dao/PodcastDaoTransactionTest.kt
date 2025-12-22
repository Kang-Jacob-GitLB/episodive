package io.jacob.episodive.core.database.dao

import io.jacob.episodive.core.database.mapper.toPodcastEntities
import io.jacob.episodive.core.database.model.PodcastGroupEntity
import io.jacob.episodive.core.testing.model.podcastTestDataList
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
 * 실제 동작 검증은 [PodcastDaoTest]의 통합 테스트를 참고하세요.
 */
class PodcastDaoTransactionTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dao = mockk<PodcastDao>(relaxed = true)

    private val podcastEntities = podcastTestDataList.toPodcastEntities()

    @Test
    fun `Given podcasts and groupKey, When upsertPodcastsWithGroup is called, Then upsertPodcasts and upsertPodcastGroups are called`() =
        runTest {
            // Given
            coEvery { dao.upsertPodcasts(any()) } just Runs
            coEvery { dao.upsertPodcastGroups(any()) } just Runs
            coEvery { dao.upsertPodcastsWithGroup(any(), any()) } answers {
                callOriginal()
            }

            // When
            dao.upsertPodcastsWithGroup(podcastEntities, "test_group")

            // Then
            coVerify(exactly = 1) {
                dao.upsertPodcasts(podcastEntities)
            }
            coVerify(exactly = 1) {
                dao.upsertPodcastGroups(match { groups ->
                    groups.size == podcastEntities.size &&
                            groups.all { it.groupKey == "test_group" }
                })
            }
        }

    @Test
    fun `Given podcasts and groupKey, When replacePodcasts is called, Then correct sequence is called`() =
        runTest {
            // Given
            val oldPodcastIds = listOf(1L, 2L, 3L)

            coEvery { dao.getPodcastGroupsByGroupKey("test_group") } returns oldPodcastIds.map {
                PodcastGroupEntity(
                    groupKey = "test_group",
                    id = it,
                    order = 0,
                    createdAt = kotlin.time.Clock.System.now(),
                )
            }
            coEvery { dao.deletePodcastGroupsByGroupKey(any()) } just Runs
            coEvery { dao.upsertPodcastsWithGroup(any(), any()) } just Runs
            coEvery { dao.deletePodcastsIfOrphaned(any()) } just Runs
            coEvery { dao.replacePodcasts(any(), any()) } answers {
                callOriginal()
            }

            // When
            dao.replacePodcasts(podcastEntities, "test_group")

            // Then
            coVerify(exactly = 1) {
                dao.getPodcastGroupsByGroupKey("test_group")
            }
            coVerify(exactly = 1) {
                dao.deletePodcastGroupsByGroupKey("test_group")
            }
            coVerify(exactly = 1) {
                dao.upsertPodcastsWithGroup(podcastEntities, "test_group")
            }
            coVerify(exactly = 1) {
                dao.deletePodcastsIfOrphaned(oldPodcastIds)
            }
        }

    @Test
    fun `Given followed podcast, When toggleFollowedPodcast is called, Then removeFollowedPodcast and deletePodcastsIfOrphaned are called`() =
        runTest {
            // Given
            val podcastId = 100L

            coEvery { dao.isFollowedPodcast(podcastId) } returns flowOf(true)
            coEvery { dao.removeFollowedPodcast(any()) } just Runs
            coEvery { dao.deletePodcastsIfOrphaned(any()) } just Runs
            coEvery { dao.toggleFollowedPodcast(any()) } answers {
                callOriginal()
            }

            // When
            val result = dao.toggleFollowedPodcast(podcastId)

            // Then
            coVerify(exactly = 1) {
                dao.removeFollowedPodcast(podcastId)
            }
            coVerify(exactly = 1) {
                dao.deletePodcastsIfOrphaned(listOf(podcastId))
            }
            assert(!result)
        }

    @Test
    fun `Given unfollowed podcast, When toggleFollowedPodcast is called, Then addFollowedPodcast is called`() =
        runTest {
            // Given
            val podcastId = 100L

            coEvery { dao.isFollowedPodcast(podcastId) } returns flowOf(false)
            coEvery { dao.addFollowedPodcast(any()) } just Runs
            coEvery { dao.toggleFollowedPodcast(any()) } answers {
                callOriginal()
            }

            // When
            val result = dao.toggleFollowedPodcast(podcastId)

            // Then
            coVerify(exactly = 1) {
                dao.addFollowedPodcast(match { it.id == podcastId })
            }
            coVerify(exactly = 0) {
                dao.deletePodcastsIfOrphaned(any())
            }
            assert(result)
        }
}