package io.jacob.episodive.core.data.repository

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.model.ClipEpisode
import io.jacob.episodive.core.player.datasource.PlayerDataSource
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class PlayerRepositoryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val playerDataSource = mockk<PlayerDataSource>(relaxed = true)

    private val repository: PlayerRepository = PlayerRepositoryImpl(
        playerDataSource = playerDataSource
    )

    @After
    fun teardown() {
        coVerify { playerDataSource.nowPlaying }
        coVerify { playerDataSource.playlist }
        coVerify { playerDataSource.indexOfList }
        coVerify { playerDataSource.progress }
        coVerify { playerDataSource.playback }
        coVerify { playerDataSource.isPlaying }
        coVerify { playerDataSource.isShuffle }
        coVerify { playerDataSource.repeat }
        coVerify { playerDataSource.speed }
        confirmVerified(playerDataSource)
    }

    @Test
    fun `Given dependencies, When play is called, Then calls playerDataSource play`() {
        // When
        repository.play(episodeTestData)

        // Then
        coVerify { playerDataSource.play(episodeTestData) }
    }

    @Test
    fun `Given dependencies, When play with list is called, Then calls playerDataSource play`() {
        // When
        repository.play(listOf(episodeTestData), indexToPlay = 0)

        // Then
        coVerify { playerDataSource.play(listOf(episodeTestData), indexToPlay = 0) }
    }

    @Test
    fun `Given dependencies, When playClip is called, Then calls playerDataSource playClip`() {
        // When
        repository.playClip(
            ClipEpisode(
                episode = episodeTestData,
                clipStartTime = Instant.fromEpochSeconds(10000L),
                clipDuration = 50.seconds,
            )
        )

        // Then
        coVerify { playerDataSource.playClip(any()) }
    }

    @Test
    fun `Given dependencies, When playClips is called, Then calls playerDataSource playClips`() {
        // When
        repository.playClips(
            clipEpisodes = listOf(
                ClipEpisode(
                    episode = episodeTestData,
                    clipStartTime = Instant.fromEpochSeconds(10000L),
                    clipDuration = 50.seconds,
                )
            ),
            indexToPlay = 0,
        )

        // Then
        coVerify { playerDataSource.playClips(any(), indexToPlay = 0) }
    }

    @Test
    fun `Given dependencies, When playIndex is called, Then calls playerDataSource playIndex`() {
        // When
        repository.playIndex(0)

        // Then
        coVerify { playerDataSource.playIndex(0) }
    }

    @Test
    fun `Given dependencies, When playOrPause is called, Then calls playerDataSource playOrPause`() {
        // When
        repository.playOrPause()

        // Then
        coVerify { playerDataSource.playOrPause() }
    }

    @Test
    fun `Given dependencies, When stop is called, Then calls playerDataSource stop`() {
        // When
        repository.stop()

        // Then
        coVerify { playerDataSource.stop() }
    }

    @Test
    fun `Given dependencies, When next is called, Then calls playerDataSource next`() {
        // When
        repository.next()

        // Then
        coVerify { playerDataSource.next() }
    }

    @Test
    fun `Given dependencies, When previous is called, Then calls playerDataSource previous`() {
        // When
        repository.previous()

        // Then
        coVerify { playerDataSource.previous() }
    }

    @Test
    fun `Given dependencies, When seekTo is called, Then calls playerDataSource seekTo`() {
        // When
        repository.seekTo(1000L)

        // Then
        coVerify { playerDataSource.seekTo(1000L) }
    }

    @Test
    fun `Given dependencies, When seekBackward is called, Then calls playerDataSource seekBackward`() {
        // When
        repository.seekBackward()

        // Then
        coVerify { playerDataSource.seekBackward() }
    }

    @Test
    fun `Given dependencies, When seekForward is called, Then calls playerDataSource seekForward`() {
        // When
        repository.seekForward()

        // Then
        coVerify { playerDataSource.seekForward() }
    }

    @Test
    fun `Given dependencies, When shuffle is called, Then calls playerDataSource shuffle`() {
        // When
        repository.shuffle()

        // Then
        coVerify { playerDataSource.shuffle() }
    }

    @Test
    fun `Given dependencies, When changeRepeat is called, Then calls playerDataSource changeRepeat`() {
        // When
        repository.changeRepeat()

        // Then
        coVerify { playerDataSource.changeRepeat() }
    }

    @Test
    fun `Given dependencies, When setSpeed is called, Then calls playerDataSource setSpeed`() {
        // When
        repository.setSpeed(1.5f)

        // Then
        coVerify { playerDataSource.setSpeed(1.5f) }
    }

    @Test
    fun `Given dependencies, When addTrack with episode is called, Then calls playerDataSource addTrack`() {
        // When
        repository.addTrack(episodeTestData, index = 0)

        // Then
        coVerify { playerDataSource.addTrack(episodeTestData, index = 0) }
    }

    @Test
    fun `Given dependencies, When addTrack with list is called, Then calls playerDataSource addTrack`() {
        // When
        repository.addTrack(listOf(episodeTestData), index = 0)

        // Then
        coVerify { playerDataSource.addTrack(listOf(episodeTestData), index = 0) }
    }

    @Test
    fun `Given dependencies, When removeTrack is called, Then calls playerDataSource removeTrack`() {
        // When
        repository.removeTrack(0)

        // Then
        coVerify { playerDataSource.removeTrack(0) }
    }

    @Test
    fun `Given dependencies, When clearPlayList is called, Then calls playerDataSource clearPlayList`() {
        // When
        repository.clearPlayList()

        // Then
        coVerify { playerDataSource.clearPlayList() }
    }

    @Test
    fun `Given dependencies, When release is called, Then calls playerDataSource release`() {
        // When
        repository.release()

        // Then
        coVerify { playerDataSource.release() }
    }

    @Test
    fun `Given dependencies, When accessing nowPlaying, Then calls playerDataSource nowPlaying`() =
        runTest {
            // When
            repository.nowPlaying.test {
                awaitComplete()
            }

            // Then
            coVerify(exactly = 1) { playerDataSource.nowPlaying }
        }

    @Test
    fun `Given dependencies, When accessing playlist, Then calls playerDataSource playlist`() =
        runTest {
            // When
            repository.playlist.test {
                awaitComplete()
            }

            // Then
            coVerify(exactly = 1) { playerDataSource.playlist }
        }

    @Test
    fun `Given dependencies, When accessing indexOfList, Then calls playerDataSource indexOfList`() =
        runTest {
            // When
            repository.indexOfList.test {
                awaitComplete()
            }

            // Then
            coVerify(exactly = 1) { playerDataSource.indexOfList }
        }

    @Test
    fun `Given dependencies, When accessing progress, Then calls playerDataSource progress`() =
        runTest {
            // When
            repository.progress.test {
                awaitComplete()
            }

            // Then
            coVerify(exactly = 1) { playerDataSource.progress }
        }

    @Test
    fun `Given dependencies, When accessing playback, Then calls playerDataSource playback`() =
        runTest {
            // When
            repository.playback.test {
                awaitComplete()
            }

            // Then
            coVerify(exactly = 1) { playerDataSource.playback }
        }

    @Test
    fun `Given dependencies, When accessing isPlaying, Then calls playerDataSource isPlaying`() =
        runTest {
            // When
            repository.isPlaying.test {
                awaitComplete()
            }

            // Then
            coVerify(exactly = 1) { playerDataSource.isPlaying }
        }

    @Test
    fun `Given dependencies, When accessing isShuffle, Then calls playerDataSource isShuffle`() =
        runTest {
            // When
            repository.isShuffle.test {
                awaitComplete()
            }

            // Then
            coVerify(exactly = 1) { playerDataSource.isShuffle }
        }

    @Test
    fun `Given dependencies, When accessing repeat, Then calls playerDataSource repeat`() =
        runTest {
            // When
            repository.repeat.test {
                awaitComplete()
            }

            // Then
            coVerify(exactly = 1) { playerDataSource.repeat }
        }

    @Test
    fun `Given dependencies, When accessing speed, Then calls playerDataSource speed`() =
        runTest {
            // When
            repository.speed.test {
                awaitComplete()
            }

            // Then
            coVerify(exactly = 1) { playerDataSource.speed }
        }
}