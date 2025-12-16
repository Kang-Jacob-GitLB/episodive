package io.jacob.episodive.core.domain.usecase.episode

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.UserData
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class GetMyRandomEpisodesUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val episodeRepository = mockk<EpisodeRepository>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)

    private val useCase = GetMyRandomEpisodesUseCase(
        episodeRepository = episodeRepository,
        userRepository = userRepository,
    )

    @After
    fun teardown() {
        confirmVerified(episodeRepository, userRepository)
    }

    @Test
    fun `Given dependencies, when invoke called, then repositories not called`() =
        runTest {
            // Given
            coEvery {
                userRepository.getUserData()
            } returns flowOf(UserData(true, "ko", listOf(Category.CAREERS)))
            coEvery {
                episodeRepository.getRandomEpisodes(any(), any(), any(), any())
            } returns mockk(relaxed = true)

            // When
            useCase(10).test {
                awaitComplete()
            }

            // Then
            coVerifySequence {
                userRepository.getUserData()
                episodeRepository.getRandomEpisodes(any(), any(), any(), any())
            }
        }
}