package io.jacob.episodive.core.data.repository

import app.cash.turbine.test
import io.jacob.episodive.core.database.datasource.EpisodeLocalDataSource
import io.jacob.episodive.core.database.mapper.toEpisodeDtos
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.player.datasource.PlayerDataSource
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class PlayerRepositoryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val playerDataSource = mockk<PlayerDataSource>(relaxed = true)
    private val episodeLocalDataSource = mockk<EpisodeLocalDataSource>(relaxed = true)

    private val repository: PlayerRepository = PlayerRepositoryImpl(
        playerDataSource = playerDataSource,
        episodeLocalDataSource = episodeLocalDataSource,
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
        coVerify { playerDataSource.cue }
        coVerify { episodeLocalDataSource.getEpisodes() }
        confirmVerified(playerDataSource, episodeLocalDataSource)
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
            episodeTestData.copy(
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
            episodes = listOf(
                episodeTestData.copy(
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
    fun `Given dependencies, When pause is called, Then calls playerDataSource pause`() {
        // When
        repository.pause()

        // Then
        coVerify { playerDataSource.pause() }
    }

    @Test
    fun `Given dependencies, When resume is called, Then calls playerDataSource resume`() {
        // When
        repository.resume()

        // Then
        coVerify { playerDataSource.resume() }
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
    fun `Given dependencies, When addClicpTrack is called, Then calls playerDataSource addClipTrack`() {
        // When
        val clipEpisode = episodeTestData.copy(
            clipStartTime = Instant.fromEpochSeconds(10000L),
            clipDuration = 50.seconds,
        )
        repository.addClipTrack(clipEpisode, index = 0)

        // Then
        coVerify { playerDataSource.addClipTrack(clipEpisode, index = 0) }
    }

    @Test
    fun `Given dependencies, When addClipTracks is called, Then calls playerDataSource addClipTracks`() {
        // When
        val clipEpisodes = listOf(
            episodeTestData.copy(
                clipStartTime = Instant.fromEpochSeconds(10000L),
                clipDuration = 50.seconds,
            )
        )
        repository.addClipTracks(clipEpisodes, index = 0)

        // Then
        coVerify { playerDataSource.addClipTracks(clipEpisodes, index = 0) }
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
    fun `Given playlist and episodes with matching IDs, When accessing playlist, Then combines with liked and played info`() =
        runTest {
            // Given
            val playlistEpisodes =
                episodeTestDataList.take(3).map { it.copy(likedAt = null, playedAt = null) }
            val likedAt = Instant.fromEpochSeconds(1757883600L)
            val playedAt = Instant.fromEpochSeconds(1757883700L)
            val position = 30.seconds

            val episodeDtos = episodeTestDataList.toEpisodeDtos(cacheKey = "test")
            val episodesFromDb = listOf(
                episodeDtos[0].copy(
                    likedAt = likedAt,
                    playedAt = playedAt,
                    position = position,
                    isCompleted = false
                ),
                episodeDtos[1].copy(
                    likedAt = null,
                    playedAt = playedAt,
                    position = 60.seconds,
                    isCompleted = true
                ),
                episodeDtos[2]
            )

            every { playerDataSource.playlist } returns flowOf(playlistEpisodes)
            every { episodeLocalDataSource.getEpisodes() } returns flowOf(episodesFromDb)

            // Create a new repository instance for this test to avoid teardown conflicts
            val testRepository = PlayerRepositoryImpl(
                playerDataSource = playerDataSource,
                episodeLocalDataSource = episodeLocalDataSource,
            )

            // When
            testRepository.playlist.test {
                val result = awaitItem()

                // Then - First episode should have liked and played info
                assertEquals(3, result.size)
                assertEquals(likedAt, result[0].likedAt)
                assertEquals(playedAt, result[0].playedAt)
                assertEquals(position, result[0].position)
                assertEquals(false, result[0].isCompleted)

                // Then - Second episode should have played info but not liked
                assertEquals(null, result[1].likedAt)
                assertEquals(playedAt, result[1].playedAt)
                assertEquals(60.seconds, result[1].position)
                assertEquals(true, result[1].isCompleted)

                // Then - Third episode should remain unchanged (no liked/played info in DB)
                assertEquals(null, result[2].likedAt)
                assertEquals(null, result[2].playedAt)

                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerify(atLeast = 1) { playerDataSource.playlist }
            coVerify(atLeast = 1) { episodeLocalDataSource.getEpisodes() }
        }

    @Test
    fun `Given playlist with episode not in DB, When accessing playlist, Then returns episode unchanged`() =
        runTest {
            // Given
            val playlistEpisodes = listOf(
                episodeTestDataList[0],
                episodeTestDataList[1],
                episodeTestData.copy(id = 999L, likedAt = null, playedAt = null)
            )
            val episodeDtos = episodeTestDataList.toEpisodeDtos(cacheKey = "test")
            val episodesFromDb = listOf(
                episodeDtos[0].copy(likedAt = Instant.fromEpochSeconds(1757883600L)),
                episodeDtos[1]
            )

            every { playerDataSource.playlist } returns flowOf(playlistEpisodes)
            every { episodeLocalDataSource.getEpisodes() } returns flowOf(episodesFromDb)

            // Create a new repository instance for this test to avoid teardown conflicts
            val testRepository = PlayerRepositoryImpl(
                playerDataSource = playerDataSource,
                episodeLocalDataSource = episodeLocalDataSource,
            )

            // When
            testRepository.playlist.test {
                val result = awaitItem()

                // Then - Third episode (ID 999) should remain unchanged
                assertEquals(3, result.size)
                assertEquals(999L, result[2].id)
                assertEquals(null, result[2].likedAt)
                assertEquals(null, result[2].playedAt)

                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerify(atLeast = 1) { playerDataSource.playlist }
            coVerify(atLeast = 1) { episodeLocalDataSource.getEpisodes() }
        }

    @Test
    fun `Given empty playlist, When accessing playlist, Then returns empty list`() =
        runTest {
            // Given
            val episodeDtos = episodeTestDataList.toEpisodeDtos(cacheKey = "test")
            every { playerDataSource.playlist } returns flowOf(emptyList())
            every { episodeLocalDataSource.getEpisodes() } returns flowOf(episodeDtos)

            // Create a new repository instance for this test to avoid teardown conflicts
            val testRepository = PlayerRepositoryImpl(
                playerDataSource = playerDataSource,
                episodeLocalDataSource = episodeLocalDataSource,
            )

            // When
            testRepository.playlist.test {
                val result = awaitItem()

                // Then
                assertEquals(0, result.size)

                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerify(atLeast = 1) { playerDataSource.playlist }
            coVerify(atLeast = 1) { episodeLocalDataSource.getEpisodes() }
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