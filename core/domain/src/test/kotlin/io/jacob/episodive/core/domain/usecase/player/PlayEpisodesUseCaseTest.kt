package io.jacob.episodive.core.domain.usecase.player

import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Rule
import org.junit.Test

class PlayEpisodesUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val playerRepository = mockk<PlayerRepository>(relaxed = true)

    private val useCase = PlayEpisodesUseCase(
        playerRepository = playerRepository,
    )

    @After
    fun teardown() {
        confirmVerified(playerRepository)
    }

    @Test
    fun `Given episodes, when invoke without playEpisode, then plays from index 0`() {
        // Given
        val episodes = episodeTestDataList

        // When
        useCase(episodes = episodes)

        // Then
        verify {
            playerRepository.play(
                episodes = episodes,
                indexToPlay = 0,
            )
        }
    }

    @Test
    fun `Given episodes and playEpisode, when invoke, then plays from specified episode index`() {
        // Given
        val episodes = episodeTestDataList
        val playEpisode = episodes[3]

        // When
        useCase(playEpisode, episodes)

        // Then
        verify {
            playerRepository.play(
                episodes = episodes,
                indexToPlay = 3,
            )
        }
    }

    @Test
    fun `Given episodes and non-existent playEpisode, when invoke, then plays from index 0`() {
        // Given
        val episodes = episodeTestDataList
        val nonExistentEpisode = episodes.first().copy(id = 999999L)

        // When
        useCase(nonExistentEpisode, episodes)

        // Then
        verify {
            playerRepository.play(
                episodes = episodes,
                indexToPlay = 0,
            )
        }
    }
}