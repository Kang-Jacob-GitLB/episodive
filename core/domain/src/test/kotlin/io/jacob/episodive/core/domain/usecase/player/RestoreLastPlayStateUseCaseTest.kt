package io.jacob.episodive.core.domain.usecase.player

import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.model.GroupKey
import io.jacob.episodive.core.model.LastPlayState
import io.jacob.episodive.core.model.Repeat
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.lastPlayStateTestData
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
import kotlin.time.Duration.Companion.seconds

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
                playerRepository.hasMediaItem()
                playerRepository.prepare(playlist, 1, 5000L)
                playerRepository.setShuffle(false)
                playerRepository.setRepeat(Repeat.OFF)
            }
        }

    // 저장된 에피소드(123L)가 플레이리스트에 없어 인덱스로 폴백하는 경우다.
    // 이때 저장된 위치는 사라진 에피소드의 것이므로 함께 버려야 한다.
    // 그대로 물려주면 폴백 대상 에피소드가 그 위치에서 시작하고, 그 값이 곧바로
    // 저장되어 해당 에피소드의 이어듣기 지점이 오염된다.
    @Test
    fun `Given saved episode is missing from playlist, When invoke, Then coerces index and discards position`() =
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
                playerRepository.hasMediaItem()
                playerRepository.prepare(playlist, 2, 0L)
                playerRepository.setShuffle(false)
                playerRepository.setRepeat(Repeat.OFF)
            }
        }

    // DataStore 스냅샷은 5초 간격으로만 저장되어 0.5초마다 갱신되는 DB 보다 최대 그만큼
    // 뒤처질 수 있다. DB 위치가 스냅샷보다 앞서면 DB 위치를 써야 앱을 켤 때마다 되감기는
    // 문제가 생기지 않는다.
    @Test
    fun `Given db position ahead of snapshot, When invoke, Then prepares with db position`() =
        runTest {
            // Given
            val playlist = episodeTestDataList.take(3).toMutableList()
            playlist[1] = playlist[1].copy(position = 13.seconds)
            val lastState = LastPlayState(
                episodeId = playlist[1].id,
                index = 1,
                positionMs = 10_000L,
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
                playerRepository.hasMediaItem()
                playerRepository.prepare(playlist, 1, 13_000L)
                playerRepository.setShuffle(false)
                playerRepository.setRepeat(Repeat.OFF)
            }
        }

    // 반대로 스냅샷이 DB 보다 앞설 수도 있다(예: 종료 직전 위치가 DB 에 아직 반영되지
    // 않은 경우). 이때는 스냅샷 값을 써야 한다 — maxOf 를 minOf 로 잘못 구현하면 이 테스트가
    // 실패한다.
    @Test
    fun `Given snapshot ahead of db position, When invoke, Then prepares with snapshot position`() =
        runTest {
            // Given
            val playlist = episodeTestDataList.take(3).toMutableList()
            playlist[1] = playlist[1].copy(position = 13.seconds)
            val lastState = LastPlayState(
                episodeId = playlist[1].id,
                index = 1,
                positionMs = 20_000L,
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
                playerRepository.hasMediaItem()
                playerRepository.prepare(playlist, 1, 20_000L)
                playerRepository.setShuffle(false)
                playerRepository.setRepeat(Repeat.OFF)
            }
        }

    // 저장된 에피소드가 목록에 없어 인덱스로 폴백하는 경우, 스냅샷 위치는 사라진
    // 에피소드의 것이므로 버리고 폴백 대상 자신의 DB 위치를 써야 한다. 폴백 대상의
    // position 이 0 이 아닌 값으로 세팅되어 있을 때, 스냅샷 값도 0 도 아닌 그 값이
    // 그대로 쓰이는지 검증한다.
    @Test
    fun `Given saved episode missing and fallback target has db position, When invoke, Then prepares with fallback target's own db position`() =
        runTest {
            // Given
            val playlist = episodeTestDataList.take(3).toMutableList()
            playlist[2] = playlist[2].copy(position = 42.seconds)
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
                playerRepository.hasMediaItem()
                playerRepository.prepare(playlist, 2, 42_000L)
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
                playerRepository.hasMediaItem()
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
                playerRepository.hasMediaItem()
                playerRepository.prepare(playlist, 0, 0L)
                playerRepository.setShuffle(false)
                playerRepository.setRepeat(Repeat.ALL)
            }
        }

    // VM 의 확인은 DB 를 읽기 전의 것이다. 읽는 사이 스테이션이 올라갔다면 prepare 가 그것을 덮어쓴다.
    @Test
    fun `Given media item loaded while reading db, When invoke, Then does not prepare and returns false`() =
        runTest {
            // Given
            val playlist = episodeTestDataList.take(3)
            coEvery { userRepository.getLastPlayState() } returns
                lastPlayStateTestData.copy(episodeId = playlist[0].id)
            coEvery {
                episodeRepository.getEpisodesByGroupKey(GroupKey.PLAYLIST.toString())
            } returns playlist
            every { playerRepository.hasMediaItem() } returns true

            // When
            val result = useCase()

            // Then
            assertFalse(result)
            coVerifySequence {
                userRepository.getLastPlayState()
                episodeRepository.getEpisodesByGroupKey(GroupKey.PLAYLIST.toString())
                playerRepository.hasMediaItem()
            }
        }
}
