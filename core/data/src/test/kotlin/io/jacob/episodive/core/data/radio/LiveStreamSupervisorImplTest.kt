package io.jacob.episodive.core.data.radio

import app.cash.turbine.test
import io.jacob.episodive.core.data.util.NetworkMonitor
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.repository.StationRepository
import io.jacob.episodive.core.domain.usecase.player.RestoreLastPlayStateUseCase
import io.jacob.episodive.core.model.Playback
import io.jacob.episodive.core.model.PlaybackFailure
import io.jacob.episodive.core.model.PlaybackFailure.Cause
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.testing.model.hlsStreamTestData
import io.jacob.episodive.core.testing.model.icecastStreamTestData
import io.jacob.episodive.core.testing.model.playbackFailureTestData
import io.jacob.episodive.core.testing.model.radioStationTestData
import io.jacob.episodive.core.testing.model.tvStationTestData
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LiveStreamSupervisorImplTest {
    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher)

    private val nowPlayingStation = MutableStateFlow<Station?>(radioStationTestData)
    private val failures = MutableSharedFlow<PlaybackFailure>()
    private val playback = MutableStateFlow(Playback.IDLE)
    private val isOnline = MutableStateFlow(true)

    private val playerRepository = mockk<PlayerRepository>()
    private val stationRepository = mockk<StationRepository>()
    private val networkMonitor = object : NetworkMonitor {
        override val isOnline: Flow<Boolean> = this@LiveStreamSupervisorImplTest.isOnline
    }

    private val stationId = radioStationTestData.id

    // 사용자가 튼 횟수. 올리면 같은 스테이션을 다시 튼 것이다.
    private var requestCount = 0L

    @Before
    fun setup() {
        every { playerRepository.nowPlayingStation } returns nowPlayingStation
        every { playerRepository.failures } returns failures
        every { playerRepository.playback } returns playback
        every { playerRepository.playRequestCount() } answers { requestCount }
        every { playerRepository.restartStation(any(), any()) } returns true
        every { playerRepository.stopStation(any()) } returns true
        coEvery { stationRepository.resolveStreamUrl(any(), any()) } returns hlsStreamTestData
        coEvery { restoreLastPlayState() } returns true
    }

    private val restoreLastPlayState = mockk<RestoreLastPlayStateUseCase>()

    private fun TestScope.supervisor() = LiveStreamSupervisorImpl(
        playerRepository = playerRepository,
        stationRepository = stationRepository,
        networkMonitor = networkMonitor,
        restoreLastPlayState = restoreLastPlayState,
        scope = backgroundScope,
        mainDispatcher = dispatcher,
    ).also { it.start() }

    private suspend fun fail(
        cause: Cause,
        url: String? = icecastStreamTestData.url,
        id: String? = stationId,
    ) {
        failures.emit(playbackFailureTestData.copy(cause = cause, stationId = id, streamUrl = url))
        dispatcher.scheduler.runCurrent()
    }

    @Test
    fun `Given Expired, When failure, Then re-resolves once and restarts with new stream`() = runTest {
        supervisor()

        fail(Cause.Expired)

        coVerify(exactly = 1) { stationRepository.resolveStreamUrl(stationId, emptySet()) }
        verify(exactly = 1) { playerRepository.restartStation(stationId, hlsStreamTestData) }
    }

    @Test
    fun `Given Expired twice, When second failure, Then treated as Gone with failed url excluded`() = runTest {
        supervisor()

        fail(Cause.Expired)
        fail(Cause.Expired, url = hlsStreamTestData.url)

        coVerify(exactly = 1) {
            stationRepository.resolveStreamUrl(stationId, setOf(hlsStreamTestData.url))
        }
        verify(exactly = 2) { playerRepository.restartStation(stationId, hlsStreamTestData) }
    }

    @Test
    fun `Given Gone and no stream to resolve, When failure, Then stops station and reports unplayable`() = runTest {
        coEvery { stationRepository.resolveStreamUrl(any(), any()) } returns null
        val supervisor = supervisor()

        supervisor.unplayable.test {
            fail(Cause.Gone)

            assertEquals(radioStationTestData, awaitItem())
        }
        verify(exactly = 1) { playerRepository.stopStation(stationId) }
        verify(exactly = 0) { playerRepository.restartStation(any(), any()) }
    }

    // 토큰이 붙은 주소는 실패할 때마다 새 문자열이라 excluded 로는 같은 주소가 걸러지지 않는다.
    // 그래도 주소를 다시 받는 횟수 상한이 루프를 끊어야 한다.
    @Test
    fun `Given Gone with a fresh token url every time, When repeated, Then gives up after 3 resolves`() = runTest {
        var issued = 0
        coEvery { stationRepository.resolveStreamUrl(any(), any()) } answers {
            icecastStreamTestData.copy(url = "https://live/token-${++issued}")
        }
        supervisor()

        repeat(4) { fail(Cause.Gone, url = "https://live/token-$it") }

        coVerify(exactly = 3) { stationRepository.resolveStreamUrl(any(), any()) }
        verify(exactly = 3) { playerRepository.restartStation(stationId, any()) }
        verify(exactly = 1) { playerRepository.stopStation(stationId) }
    }

    @Test
    fun `Given offline, When Network failure, Then waits for online without spending time and then restarts`() = runTest {
        isOnline.value = false
        supervisor()

        fail(Cause.Network)
        dispatcher.scheduler.advanceTimeBy(120_000)
        dispatcher.scheduler.runCurrent()
        verify(exactly = 0) { playerRepository.restartStation(any(), any()) }

        isOnline.value = true
        dispatcher.scheduler.runCurrent()
        dispatcher.scheduler.advanceTimeBy(999)
        dispatcher.scheduler.runCurrent()
        verify(exactly = 0) { playerRepository.restartStation(any(), any()) }

        dispatcher.scheduler.advanceTimeBy(1)
        dispatcher.scheduler.runCurrent()
        verify(exactly = 1) { playerRepository.restartStation(stationId, null) }
    }

    @Test
    fun `Given Network failures, When repeated, Then backs off 1 2 4 8 16 seconds and gives up on the sixth`() = runTest {
        supervisor()

        listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L).forEachIndexed { index, wait ->
            fail(Cause.Network)
            dispatcher.scheduler.advanceTimeBy(wait - 1)
            dispatcher.scheduler.runCurrent()
            verify(exactly = index) { playerRepository.restartStation(stationId, null) }

            dispatcher.scheduler.advanceTimeBy(1)
            dispatcher.scheduler.runCurrent()
            verify(exactly = index + 1) { playerRepository.restartStation(stationId, null) }
        }
        verify(exactly = 0) { playerRepository.stopStation(any()) }

        fail(Cause.Network)

        verify(exactly = 5) { playerRepository.restartStation(stationId, null) }
        verify(exactly = 1) { playerRepository.stopStation(stationId) }
    }

    // Other 는 기기 쪽 사정(AudioTrack·디코더 초기화)이라 주소를 탓하지 않는다.
    @Test
    fun `Given Other, When failures repeat, Then reconnects same url without resolving and gives up after 5`() = runTest {
        supervisor()

        repeat(5) {
            fail(Cause.Other)
            dispatcher.scheduler.advanceTimeBy(16_000)
            dispatcher.scheduler.runCurrent()
        }
        fail(Cause.Other)

        coVerify(exactly = 0) { stationRepository.resolveStreamUrl(any(), any()) }
        verify(exactly = 5) { playerRepository.restartStation(stationId, null) }
        verify(exactly = 1) { playerRepository.stopStation(stationId) }
    }

    // 계약: 같은 스테이션을 다시 눌러도 낡은 복구가 새 세션에 끼어들거나 예산이 이어지면 안 된다.
    @Test
    fun `Given backing off, When user replays same station, Then no restart and next failure has a fresh budget`() = runTest {
        supervisor()

        fail(Cause.Network)
        requestCount++
        dispatcher.scheduler.advanceTimeBy(60_000)
        dispatcher.scheduler.runCurrent()
        verify(exactly = 0) { playerRepository.restartStation(any(), any()) }

        exhaustReconnects()
        verify(exactly = 5) { playerRepository.restartStation(stationId, null) }
        requestCount++
        fail(Cause.Network)
        dispatcher.scheduler.advanceTimeBy(1_000)
        dispatcher.scheduler.runCurrent()

        verify(exactly = 6) { playerRepository.restartStation(stationId, null) }
        verify(exactly = 0) { playerRepository.stopStation(any()) }
    }

    @Test
    fun `Given user replays during re-resolve, When resolve returns, Then does not restart with stale stream`() = runTest {
        coEvery { stationRepository.resolveStreamUrl(any(), any()) } coAnswers {
            requestCount++
            hlsStreamTestData
        }
        supervisor()

        fail(Cause.Gone)

        verify(exactly = 0) { playerRepository.restartStation(any(), any()) }
    }

    // 오프라인에서 끝난(취소된) 대기는 횟수를 쓰지 않는다. 여섯 번 실패가 와도 실제로 다시 올린 것은 한 번이다.
    @Test
    fun `Given offline, When Network failures arrive repeatedly, Then waits cost no budget`() = runTest {
        isOnline.value = false
        supervisor()

        repeat(6) { fail(Cause.Network) }
        isOnline.value = true
        dispatcher.scheduler.runCurrent()
        dispatcher.scheduler.advanceTimeBy(1_000)
        dispatcher.scheduler.runCurrent()

        verify(exactly = 1) { playerRepository.restartStation(stationId, null) }
        verify(exactly = 0) { playerRepository.stopStation(any()) }
    }

    // 우리가 주소를 교체한 뒤 옛 주소의 실패가 늦게 도착해도 새 주소의 복구를 건드리면 안 된다.
    @Test
    fun `Given stream replaced, When failure of the old url arrives late, Then ignored`() = runTest {
        supervisor()
        fail(Cause.Expired)

        fail(Cause.Gone, url = icecastStreamTestData.url)

        coVerify(exactly = 1) { stationRepository.resolveStreamUrl(any(), any()) }
        verify(exactly = 1) { playerRepository.restartStation(any(), any()) }
        verify(exactly = 0) { playerRepository.stopStation(any()) }
    }

    @Test
    fun `Given BehindLiveWindow, When failure, Then restarts immediately`() = runTest {
        supervisor()

        fail(Cause.BehindLiveWindow)

        verify(exactly = 1) { playerRepository.restartStation(stationId, null) }
    }

    @Test
    fun `Given BehindLiveWindow repeatedly, When fourth failure, Then gives up`() = runTest {
        supervisor()

        repeat(4) { fail(Cause.BehindLiveWindow) }

        verify(exactly = 3) { playerRepository.restartStation(stationId, null) }
        verify(exactly = 1) { playerRepository.stopStation(stationId) }
    }

    @Test
    fun `Given Ended, When failure, Then reconnects after backoff`() = runTest {
        supervisor()

        fail(Cause.Ended)
        verify(exactly = 0) { playerRepository.restartStation(any(), any()) }

        dispatcher.scheduler.advanceTimeBy(1_000)
        dispatcher.scheduler.runCurrent()

        verify(exactly = 1) { playerRepository.restartStation(stationId, null) }
    }

    private suspend fun exhaustReconnects() {
        listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L).forEach { wait ->
            fail(Cause.Network)
            dispatcher.scheduler.advanceTimeBy(wait)
            dispatcher.scheduler.runCurrent()
        }
    }

    @Test
    fun `Given stable READY for 30 seconds, When failure after, Then budget is restored`() = runTest {
        supervisor()
        exhaustReconnects()

        playback.value = Playback.READY
        dispatcher.scheduler.runCurrent()
        dispatcher.scheduler.advanceTimeBy(30_000)
        dispatcher.scheduler.runCurrent()
        fail(Cause.Network)
        dispatcher.scheduler.advanceTimeBy(1_000)
        dispatcher.scheduler.runCurrent()

        verify(exactly = 6) { playerRepository.restartStation(stationId, null) }
        verify(exactly = 0) { playerRepository.stopStation(any()) }
    }

    @Test
    fun `Given READY shorter than 30 seconds, When failure, Then budget is not restored`() = runTest {
        supervisor()
        exhaustReconnects()

        playback.value = Playback.READY
        dispatcher.scheduler.runCurrent()
        dispatcher.scheduler.advanceTimeBy(29_999)
        dispatcher.scheduler.runCurrent()
        fail(Cause.Network)

        verify(exactly = 5) { playerRepository.restartStation(stationId, null) }
        verify(exactly = 1) { playerRepository.stopStation(stationId) }
    }

    // 에피소드 실패(stationId == null)는 감독자가 다루지 않는다. 에피소드 재생이 건드려지면 안 된다.
    @Test
    fun `Given episode failure with null stationId, When emitted, Then nothing happens`() = runTest {
        supervisor()

        fail(Cause.Gone, id = null)
        fail(Cause.Ended, id = null)
        dispatcher.scheduler.advanceTimeBy(60_000)
        dispatcher.scheduler.runCurrent()

        coVerify(exactly = 0) { stationRepository.resolveStreamUrl(any(), any()) }
        verify(exactly = 0) { playerRepository.restartStation(any(), any()) }
        verify(exactly = 0) { playerRepository.stopStation(any()) }
    }

    @Test
    fun `Given failure of a previous station, When emitted, Then ignored`() = runTest {
        nowPlayingStation.value = tvStationTestData
        supervisor()

        fail(Cause.Gone, id = radioStationTestData.id)

        coVerify(exactly = 0) { stationRepository.resolveStreamUrl(any(), any()) }
        verify(exactly = 0) { playerRepository.stopStation(any()) }
    }

    @Test
    fun `Given backing off, When station changes, Then recovery is cancelled and no restart`() = runTest {
        supervisor()

        fail(Cause.Network)
        nowPlayingStation.value = tvStationTestData
        dispatcher.scheduler.runCurrent()
        dispatcher.scheduler.advanceTimeBy(60_000)
        dispatcher.scheduler.runCurrent()

        verify(exactly = 0) { playerRepository.restartStation(any(), any()) }
    }

    @Test
    fun `Given stopStation 성공, When giving up, Then 마지막 에피소드 복원을 요청한다`() = runTest {
        coEvery { stationRepository.resolveStreamUrl(any(), any()) } returns null
        val supervisor = supervisor()

        fail(Cause.Gone)
        dispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) { restoreLastPlayState() }
    }

    @Test
    fun `Given stopStation returns false, When giving up, Then 복원하지 않는다`() = runTest {
        coEvery { stationRepository.resolveStreamUrl(any(), any()) } returns null
        every { playerRepository.stopStation(any()) } returns false
        val supervisor = supervisor()

        fail(Cause.Gone)
        dispatcher.scheduler.runCurrent()

        coVerify(exactly = 0) { restoreLastPlayState() }
    }

    @Test
    fun `Given stopStation returns false, When giving up, Then unplayable is not emitted`() = runTest {
        coEvery { stationRepository.resolveStreamUrl(any(), any()) } returns null
        every { playerRepository.stopStation(any()) } returns false
        val supervisor = supervisor()

        supervisor.unplayable.test {
            fail(Cause.Gone)

            expectNoEvents()
        }
        verify(exactly = 1) { playerRepository.stopStation(stationId) }
    }

    @Test
    fun `Given start called twice, When failure, Then handled once`() = runTest {
        val supervisor = supervisor()
        supervisor.start()

        assertEquals(1, failures.subscriptionCount.value)
        fail(Cause.BehindLiveWindow)

        verify(exactly = 1) { playerRepository.restartStation(stationId, null) }
    }
}
