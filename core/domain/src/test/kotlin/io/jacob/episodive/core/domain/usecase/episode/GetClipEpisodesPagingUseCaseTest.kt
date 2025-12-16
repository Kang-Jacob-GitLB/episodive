package io.jacob.episodive.core.domain.usecase.episode

import androidx.paging.LoadState.Loading.endOfPaginationReached
import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.FeedRepository
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.soundbiteTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class GetClipEpisodesPagingUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val feedRepository = mockk<FeedRepository>(relaxed = true)
    private val episodeRepository = mockk<EpisodeRepository>(relaxed = true)

    private val useCase = GetClipEpisodesPagingUseCase(
        feedRepository = feedRepository,
        episodeRepository = episodeRepository,
    )

    @After
    fun teardown() {
        confirmVerified(feedRepository, episodeRepository)
    }

    @Test
    fun `Given dependencies, When invoke called, Then return paging data of episodes`() =
        runTest {
            // Given
            coEvery {
                feedRepository.getRecentSoundbitesPaging()
            } returns flowOf(PagingData.from(soundbiteTestDataList))
            coEvery {
                episodeRepository.getEpisodeById(any())
            } returns flowOf(episodeTestData)

            // When
            val result = useCase().asSnapshot {
                appendScrollWhile { !endOfPaginationReached }
            }

            assertEquals(10, result.size)
            for (episode in result) {
                assertNotNull(episode.clipStartTime)
                assertNotNull(episode.clipDuration)
            }

            // Then
            coVerifySequence {
                feedRepository.getRecentSoundbitesPaging()
                repeat(10) {
                    episodeRepository.getEpisodeById(any())
                }
            }
        }

    // TODO: redo test when likedAt is implemented
//    @Test
//    fun `Given episode with likedAt changes, When observing, Then emits updated episode`() =
//        runTest {
//            // Given
//            // Create a map to store flows for each episode ID
//            val episodeFlows = mutableMapOf<Long, MutableStateFlow<Episode>>()
//
//            // Setup mock to return different flow for each episode ID
//            coEvery {
//                feedRepository.getRecentSoundbitesPaging()
//            } returns flowOf(PagingData.from(soundbiteTestDataList))
//            coEvery {
//                episodeRepository.getEpisodeById(any())
//            } answers {
//                val episodeId = firstArg<Long>()
//                episodeFlows.getOrPut(episodeId) {
//                    MutableStateFlow(episodeTestData.copy(id = episodeId, likedAt = null))
//                }
//            }
//
//            // When
//            useCase().test {
//                // First chunk: 5 episodes without likedAt
//                val firstEmission = awaitItem()
//                assertEquals(5, firstEmission.size)
//                firstEmission.forEach { episode ->
//                    assertNull(episode.likedAt)
//                }
//
//                // Second chunk: 10 episodes without likedAt
//                val secondEmission = awaitItem()
//                assertEquals(10, secondEmission.size)
//                secondEmission.forEach { episode ->
//                    assertNull(episode.likedAt)
//                }
//
//                // Simulate liking the first episode
//                val likedAt = Instant.fromEpochMilliseconds(1234567890)
//                val firstEpisodeId = soundbiteTestDataList.first().episodeId
//                episodeFlows[firstEpisodeId]?.value =
//                    episodeFlows[firstEpisodeId]!!.value.copy(likedAt = likedAt)
//
//                // Should emit updated list with first episode liked
//                val updatedEmission = awaitItem()
//                assertEquals(10, updatedEmission.size)
//                assertEquals(likedAt, updatedEmission.first().likedAt)
//                // Other episodes should still have null likedAt
//                updatedEmission.drop(1).forEach { episode ->
//                    assertNull(episode.likedAt)
//                }
//
//                cancelAndIgnoreRemainingEvents()
//            }
//        }

    @Test
    fun `Given empty from recent soundbites, When invoke called, Then returns empty list`() =
        runTest {
            // Given
            coEvery {
                feedRepository.getRecentSoundbitesPaging()
            } returns flowOf(PagingData.from(emptyList()))

            // When
            val result = useCase().asSnapshot {
                appendScrollWhile { !endOfPaginationReached }
            }

            assertEquals(0, result.size)

            // Then
            coVerify {
                feedRepository.getRecentSoundbitesPaging()
            }
        }
}
