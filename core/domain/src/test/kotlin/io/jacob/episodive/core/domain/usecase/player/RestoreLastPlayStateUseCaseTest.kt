package io.jacob.episodive.core.domain.usecase.player

import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.model.GroupKey
import io.jacob.episodive.core.model.LastPlayState
import io.jacob.episodive.core.model.Repeat
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RestoreLastPlayStateUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val playerRepository = mockk<PlayerRepository>(relaxed = true)
    private val episodeRepository = mockk<EpisodeRepository>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)

    private val useCase = RestoreLastPlayStateUseCase(
        playerRepository = playerRepository,
        episodeRepository = episodeRepository,
        userRepository = userRepository,
    )

    @After
    fun teardown() {
        confirmVerified(playerRepository, episodeRepository, userRepository)
    }

    @Test
    fun `Given no last state, When invoke, Then returns false`() =
        runTest {
            // Given
            coEvery { userRepository.getLastPlayState() } returns null

            // When
            val result = useCase()

            // Then
            assertFalse(result)
            coVerifySequence {
                userRepository.getLastPlayState()
            }
        }

    @Test
    fun `Given last state but empty playlist, When invoke, Then returns false`() =
        runTest {
            // Given
            coEvery { userRepository.getLastPlayState() } returns LastPlayState(
                episodeId = 123L,
                index = 0,
                positionMs = 5000L,
                shuffle = false,
                repeat = Repeat.OFF,
            )
            coEvery {
                episodeRepository.getEpisodesByGroupKey(GroupKey.PLAYLIST.toString())
            } returns emptyList()

            // When
            val result = useCase()

            // Then
            assertFalse(result)
            coVerifySequence {
                userRepository.getLastPlayState()
                episodeRepository.getEpisodesByGroupKey(GroupKey.PLAYLIST.toString())
            }
        }

    @Test
    fun `Given valid state and playlist, When invoke, Then prepares player and returns true`() =
        runTest {
            // Given
            val playlist = episodeTestDataList.take(3)
            val lastState = LastPlayState(
                episodeId = playlist[1].id,
                index = 1,
                positionMs = 5000L,
                shuffle = false,
                repeat = Repeat.OFF,
            )
            coEvery { userRepository.getLastPlayState() } returns lastState
            coEvery {
                episodeRepository.getEpisodesByGroupKey(GroupKey.PLAYLIST.toString())
            } returns playlist
            every { playerRepository.prepare(any(), any(), any()) } just Runs
            every { playerRepository.setShuffle(any()) } just Runs
            every { playerRepository.setRepeat(any()) } just Runs

            // When
            val result = useCase()

            // Then
            assertTrue(result)
            coVerifySequence {
                userRepository.getLastPlayState()
                episodeRepository.getEpisodesByGroupKey(GroupKey.PLAYLIST.toString())
                playerRepository.prepare(playlist, 1, 5000L)
                playerRepository.setShuffle(false)
                playerRepository.setRepeat(Repeat.OFF)
            }
        }

    @Test
    fun `Given index exceeds playlist size, When invoke, Then coerces index to last element`() =
        runTest {
            // Given
            val playlist = episodeTestDataList.take(3)
            val lastState = LastPlayState(
                episodeId = 123L,
                index = 99,
                positionMs = 1000L,
                shuffle = false,
                repeat = Repeat.OFF,
            )
            coEvery { userRepository.getLastPlayState() } returns lastState
            coEvery {
                episodeRepository.getEpisodesByGroupKey(GroupKey.PLAYLIST.toString())
            } returns playlist
            every { playerRepository.prepare(any(), any(), any()) } just Runs
            every { playerRepository.setShuffle(any()) } just Runs
            every { playerRepository.setRepeat(any()) } just Runs

            // When
            val result = useCase()

            // Then
            assertTrue(result)
            coVerifySequence {
                userRepository.getLastPlayState()
                episodeRepository.getEpisodesByGroupKey(GroupKey.PLAYLIST.toString())
                playerRepository.prepare(playlist, 2, 1000L)
                playerRepository.setShuffle(false)
                playerRepository.setRepeat(Repeat.OFF)
            }
        }

    @Test
    fun `Given shuffle enabled, When invoke, Then sets shuffle on player`() =
        runTest {
            // Given
            val playlist = episodeTestDataList.take(2)
            val lastState = LastPlayState(
                episodeId = playlist[0].id,
                index = 0,
                positionMs = 0L,
                shuffle = true,
                repeat = Repeat.OFF,
            )
            coEvery { userRepository.getLastPlayState() } returns lastState
            coEvery {
                episodeRepository.getEpisodesByGroupKey(GroupKey.PLAYLIST.toString())
            } returns playlist
            every { playerRepository.prepare(any(), any(), any()) } just Runs
            every { playerRepository.setShuffle(any()) } just Runs
            every { playerRepository.setRepeat(any()) } just Runs

            // When
            useCase()

            // Then
            coVerifySequence {
                userRepository.getLastPlayState()
                episodeRepository.getEpisodesByGroupKey(GroupKey.PLAYLIST.toString())
                playerRepository.prepare(playlist, 0, 0L)
                playerRepository.setShuffle(true)
                playerRepository.setRepeat(Repeat.OFF)
            }
        }

    @Test
    fun `Given repeat mode set, When invoke, Then sets repeat on player`() =
        runTest {
            // Given
            val playlist = episodeTestDataList.take(2)
            val lastState = LastPlayState(
                episodeId = playlist[0].id,
                index = 0,
                positionMs = 0L,
                shuffle = false,
                repeat = Repeat.ALL,
            )
            coEvery { userRepository.getLastPlayState() } returns lastState
            coEvery {
                episodeRepository.getEpisodesByGroupKey(GroupKey.PLAYLIST.toString())
            } returns playlist
            every { playerRepository.prepare(any(), any(), any()) } just Runs
            every { playerRepository.setShuffle(any()) } just Runs
            every { playerRepository.setRepeat(any()) } just Runs

            // When
            useCase()

            // Then
            coVerifySequence {
                userRepository.getLastPlayState()
                episodeRepository.getEpisodesByGroupKey(GroupKey.PLAYLIST.toString())
                playerRepository.prepare(playlist, 0, 0L)
                playerRepository.setShuffle(false)
                playerRepository.setRepeat(Repeat.ALL)
            }
        }
}
