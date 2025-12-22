package io.jacob.episodive.core.domain.usecase.episode

import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.model.Progress
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class UpdatePlayedEpisodeUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val episodeRepository = mockk<EpisodeRepository>(relaxed = true)

    private val useCase = UpdatePlayedEpisodeUseCase(
        episodeRepository = episodeRepository,
    )

    @After
    fun teardown() {
        confirmVerified(episodeRepository)
    }

    @Test
    fun `Given dependencies, when invoke called, then repository called`() =
        runTest {
            // Given
            val id = 1L
            val progress = Progress(
                position = 1000L.seconds,
                buffered = 2000L.seconds,
                duration = 4000L.seconds,
            )
            coEvery { episodeRepository.updatePlayed(any(), any(), any()) } just Runs
            coEvery { episodeRepository.updateEpisodeDuration(any(), any()) } just Runs

            // When
            useCase(id, progress)

            // Then
            coVerifySequence {
                episodeRepository.updatePlayed(
                    id = id,
                    position = progress.position,
                    isCompleted = false,
                )
                episodeRepository.updateEpisodeDuration(
                    id = id,
                    duration = progress.duration,
                )
            }
        }
}