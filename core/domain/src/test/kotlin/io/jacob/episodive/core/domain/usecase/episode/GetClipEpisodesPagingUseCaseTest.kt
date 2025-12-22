package io.jacob.episodive.core.domain.usecase.episode

import androidx.paging.LoadState.Loading.endOfPaginationReached
import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
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
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class GetClipEpisodesPagingUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val episodeRepository = mockk<EpisodeRepository>(relaxed = true)

    private val useCase = GetClipEpisodesPagingUseCase(
        episodeRepository = episodeRepository,
    )

    @After
    fun teardown() {
        confirmVerified(episodeRepository)
    }

    @Test
    fun `Given dependencies, When invoke called, Then return paging data of episodes`() =
        runTest {
            // Given
            coEvery {
                episodeRepository.getSoundbiteEpisodesPaging()
            } returns flowOf(
                PagingData.from(
                    listOf(
                        episodeTestData.copy(
                            clipStartTime = Instant.fromEpochSeconds(1000),
                            clipDuration = 10.seconds,
                        )
                    )
                )
            )

            // When
            val result = useCase().asSnapshot {
                appendScrollWhile { !endOfPaginationReached }
            }

            assertEquals(1, result.size)
            for (episode in result) {
                assertNotNull(episode.clipStartTime)
                assertNotNull(episode.clipDuration)
            }

            // Then
            coVerifySequence {
                episodeRepository.getSoundbiteEpisodesPaging()
            }
        }
}
