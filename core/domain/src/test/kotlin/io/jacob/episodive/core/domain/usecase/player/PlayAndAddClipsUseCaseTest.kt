package io.jacob.episodive.core.domain.usecase.player

import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.model.ClipEpisode
import io.jacob.episodive.core.testing.model.episodeTestDataList
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
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class PlayAndAddClipsUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val playerRepository = mockk<PlayerRepository>(relaxed = true)

    private val useCase = PlayAndAddClipsUseCase(
        playerRepository = playerRepository,
    )

    @After
    fun teardown() {
        confirmVerified(playerRepository)
    }

    private val clipEpisodes = episodeTestDataList.map {
        ClipEpisode(
            episode = it,
            clipStartTime = Instant.fromEpochSeconds(2000L),
            clipDuration = 15000L.seconds,
        )
    }

    @Test
    fun `Given dependencies, when invoke called, then repository called`() =
        runTest {
            // Given
            coEvery { playerRepository.playClips(any()) } just Runs
            coEvery { playerRepository.addClipTrack(any()) } just Runs
            coEvery { playerRepository.playlist } returns flowOf(
                clipEpisodes.take(1).map { it.episode })

            // When
            useCase(
                clipEpisodes = clipEpisodes.take(1)
            )
            useCase(
                clipEpisodes = clipEpisodes
            )

            // Then
            coVerifySequence {
                playerRepository.playClips(
                    clipEpisodes = clipEpisodes.take(1)
                )
                playerRepository.playlist
                playerRepository.addClipTracks(
                    clipEpisodes = clipEpisodes.drop(1)
                )
            }
        }
}