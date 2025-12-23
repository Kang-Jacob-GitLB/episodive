package io.jacob.episodive.core.domain.usecase.player

import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.model.GroupKey
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class PlayEpisodeUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val playerRepository = mockk<PlayerRepository>(relaxed = true)
    private val episodeRepository = mockk<EpisodeRepository>(relaxed = true)

    private val useCase = PlayEpisodeUseCase(
        playerRepository = playerRepository,
        episodeRepository = episodeRepository,
    )

    @After
    fun teardown() {
        confirmVerified(playerRepository, episodeRepository)
    }

    @Test
    fun `Given dependencies, when invoke called, then repository called`() =
        runTest {
            // Given
            val episode = episodeTestData
            every { playerRepository.play(episode = any()) } just Runs
            coEvery { episodeRepository.replaceEpisodes(any(), any()) } just Runs

            // When
            useCase(episode)

            // Then
            coVerifySequence {
                playerRepository.play(episode)
                episodeRepository.replaceEpisodes(
                    episodes = listOf(episode),
                    groupKey = GroupKey.PLAYLIST.toString(),
                )
            }
        }

    @Test
    fun `Given episodes, when invoke without playEpisode, then plays from index 0`() =
        runTest {
            // Given
            val episodes = episodeTestDataList
            every { playerRepository.play(episodes = any(), indexToPlay = any()) } just Runs
            coEvery { episodeRepository.replaceEpisodes(any(), any()) } just Runs

            // When
            useCase(episodes = episodes)

            // Then
            coVerifySequence {
                playerRepository.play(
                    episodes = episodes,
                    indexToPlay = 0,
                )
                episodeRepository.replaceEpisodes(
                    episodes = episodes,
                    groupKey = GroupKey.PLAYLIST.toString(),
                )
            }
        }

    @Test
    fun `Given episodes and playEpisode, when invoke, then plays from specified episode index`() =
        runTest {
            // Given
            val episodes = episodeTestDataList
            val playEpisode = episodes[3]
            every { playerRepository.play(episodes = any(), indexToPlay = any()) } just Runs
            coEvery { episodeRepository.replaceEpisodes(any(), any()) } just Runs

            // When
            useCase(playEpisode, episodes)

            // Then
            coVerifySequence {
                playerRepository.play(
                    episodes = episodes,
                    indexToPlay = 3,
                )
                episodeRepository.replaceEpisodes(
                    episodes = episodes,
                    groupKey = GroupKey.PLAYLIST.toString(),
                )
            }
        }

    @Test
    fun `Given episodes and non-existent playEpisode, when invoke, then plays from index 0`() =
        runTest {
            // Given
            val episodes = episodeTestDataList
            val nonExistentEpisode = episodes.first().copy(id = 999999L)
            every { playerRepository.play(episodes = any(), indexToPlay = any()) } just Runs
            coEvery { episodeRepository.replaceEpisodes(any(), any()) } just Runs

            // When
            useCase(nonExistentEpisode, episodes)

            // Then
            coVerifySequence {
                playerRepository.play(
                    episodes = episodes,
                    indexToPlay = 0,
                )
                episodeRepository.replaceEpisodes(
                    episodes = episodes,
                    groupKey = GroupKey.PLAYLIST.toString(),
                )
            }
        }
}