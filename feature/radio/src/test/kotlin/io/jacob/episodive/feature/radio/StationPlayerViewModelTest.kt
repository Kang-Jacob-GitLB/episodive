package io.jacob.episodive.feature.radio

import io.jacob.episodive.core.domain.player.SleepTimer
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.station.StationPlaybackController
import io.jacob.episodive.core.domain.usecase.station.GetFavoriteStationsUseCase
import io.jacob.episodive.core.domain.usecase.station.GetStationDetailsUseCase
import io.jacob.episodive.core.domain.usecase.station.GetStationsUseCase
import io.jacob.episodive.core.domain.usecase.station.ObserveOnAirProgramUseCase
import io.jacob.episodive.core.domain.usecase.station.ToggleFavoriteStationUseCase
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.StationKind
import io.jacob.episodive.core.testing.model.bareOnAirProgramTestData
import io.jacob.episodive.core.testing.model.codecOnlyStationDetailsTestData
import io.jacob.episodive.core.testing.model.kbsFirstRadioStationTestData
import io.jacob.episodive.core.testing.model.kbsHappyRadioStationTestData
import io.jacob.episodive.core.testing.model.onAirProgramTestData
import io.jacob.episodive.core.testing.model.radioStationTestData
import io.jacob.episodive.core.testing.model.stationDetailsTestData
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StationPlayerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val playerRepository = mockk<PlayerRepository>(relaxed = true)
    private val stationPlaybackController = mockk<StationPlaybackController>(relaxed = true)
    private val getStationsUseCase = mockk<GetStationsUseCase>()
    private val getFavoriteStationsUseCase = mockk<GetFavoriteStationsUseCase>()
    private val toggleFavoriteStationUseCase = mockk<ToggleFavoriteStationUseCase>(relaxed = true)
    private val getStationDetailsUseCase = mockk<GetStationDetailsUseCase>()
    private val observeOnAirProgramUseCase = mockk<ObserveOnAirProgramUseCase>()
    private val sleepTimer = mockk<SleepTimer>(relaxed = true)

    private val a = radioStationTestData
    private val b = kbsFirstRadioStationTestData
    private val c = kbsHappyRadioStationTestData

    private val nowPlayingStation = MutableStateFlow<Station?>(null)
    private val pendingStationId = MutableStateFlow<String?>(null)
    private val streamTitle = MutableStateFlow<String?>(null)
    private val isPlaying = MutableStateFlow(false)
    private val isBuffering = MutableStateFlow(false)
    private val favorites = MutableStateFlow<List<Station>>(emptyList())
    private val stations = MutableStateFlow(listOf(a, b, c))
    private val sleepRemaining = MutableStateFlow<Long?>(null)

    private fun createViewModel(): StationPlayerViewModel {
        every { playerRepository.nowPlayingStation } returns nowPlayingStation
        every { playerRepository.streamTitle } returns streamTitle
        every { playerRepository.isPlaying } returns isPlaying
        every { playerRepository.isBuffering } returns isBuffering
        every { stationPlaybackController.pendingStationId } returns pendingStationId
        every { getFavoriteStationsUseCase() } returns favorites
        every { getStationsUseCase(StationKind.RADIO) } returns stations
        every { sleepTimer.remainingMs } returns sleepRemaining
        every { observeOnAirProgramUseCase(any()) } returns flowOf(null)
        // play 가 pending 을 즉시 세우는 컨트롤러를 흉내 낸다. 두 번째 호출이 이 값을 기준으로 삼아야 한다.
        every { stationPlaybackController.play(any()) } answers { pendingStationId.value = firstArg() }
        return StationPlayerViewModel(
            playerRepository = playerRepository,
            stationPlaybackController = stationPlaybackController,
            getStationsUseCase = getStationsUseCase,
            getFavoriteStationsUseCase = getFavoriteStationsUseCase,
            toggleFavoriteStationUseCase = toggleFavoriteStationUseCase,
            getStationDetailsUseCase = getStationDetailsUseCase,
            observeOnAirProgramUseCase = observeOnAirProgramUseCase,
            sleepTimer = sleepTimer,
        )
    }

    private fun StationPlayerViewModel.activeState(): StationPlayerState.Active {
        // 구독이 있어야 stateIn 이 돈다.
        return state.value as StationPlayerState.Active
    }

    @Test
    fun `Given 구독 전, Then Loading 이고 스테이션이 없으면 Hidden 이 된다`() = runTest {
        val viewModel = createViewModel()
        assertEquals(StationPlayerState.Loading, viewModel.state.value)

        val job = launchCollect(viewModel)

        assertEquals(StationPlayerState.Hidden, viewModel.state.value)
        job.cancel()
    }

    @Test
    fun `Given 재생 중인 스테이션, Then Active 로 곡명과 즐겨찾기 여부가 실린다`() = runTest {
        nowPlayingStation.value = a
        streamTitle.value = "Artist - Title"
        isPlaying.value = true
        favorites.value = listOf(a, c)
        val viewModel = createViewModel()
        val job = launchCollect(viewModel)

        val s = viewModel.activeState()
        assertEquals(a, s.station)
        assertEquals("Artist - Title", s.streamTitle)
        assertTrue(s.isFavorite)
        assertTrue(s.isPlaying)
        // 즐겨찾기 [a, c] 를 돈다.
        assertEquals(c, s.next)
        assertEquals(c, s.previous)
        job.cancel()
    }

    @Test
    fun `Given 다른 스테이션을 받는 중, Then 그 스테이션을 보이고 곡명은 null 이다`() = runTest {
        nowPlayingStation.value = a
        streamTitle.value = "이전 스테이션 곡"
        val viewModel = createViewModel()
        val job = launchCollect(viewModel)

        pendingStationId.value = b.id

        val s = viewModel.activeState()
        assertEquals(b, s.station)
        assertTrue(s.isPending)
        assertNull(s.streamTitle)
        job.cancel()
    }

    @Test
    fun `Given 재생 중이 없고 받는 중만 있음, Then Hidden 이 아니라 Active 다`() = runTest {
        pendingStationId.value = b.id
        val viewModel = createViewModel()
        val job = launchCollect(viewModel)

        assertEquals(b, viewModel.activeState().station)
        job.cancel()
    }

    @Test
    fun `Given Active 에서 정지로 둘 다 비면, Then Hidden`() = runTest {
        nowPlayingStation.value = a
        val viewModel = createViewModel()
        val job = launchCollect(viewModel)
        viewModel.activeState()

        nowPlayingStation.value = null

        assertEquals(StationPlayerState.Hidden, viewModel.state.value)
        job.cancel()
    }

    @Test
    fun `Given 구독이 끊겼다 다시 붙음, Then 낡은 Hidden 이 아니라 Loading 에서 시작한다`() = runTest {
        // 낡은 Hidden 이 남으면 시트가 다시 열리자마자 닫힌다. 그 사이 스테이션이 새로 올라와 있어도 마찬가지다.
        val viewModel = createViewModel()
        val first = launchCollect(viewModel)
        assertEquals(StationPlayerState.Hidden, viewModel.state.value)
        first.cancel()
        // 구독 유예(5초)가 지나 업스트림이 멈춘 뒤 스테이션이 올라온다.
        testScheduler.advanceTimeBy(6_000)
        nowPlayingStation.value = a

        assertEquals(StationPlayerState.Loading, viewModel.state.value)
        val second = launchCollect(viewModel)
        assertEquals(a, viewModel.activeState().station)
        second.cancel()
    }

    @Test
    fun `Given 받는 중, When PlayOrPause, Then 무시한다`() = runTest {
        nowPlayingStation.value = a
        pendingStationId.value = b.id
        val viewModel = createViewModel()

        viewModel.sendAction(StationPlayerAction.PlayOrPause)

        verify(exactly = 0) { playerRepository.playOrPause() }
    }

    @Test
    fun `Given 재생 중, When PlayOrPause, Then 플레이어에 위임한다`() = runTest {
        nowPlayingStation.value = a
        val viewModel = createViewModel()

        viewModel.sendAction(StationPlayerAction.PlayOrPause)

        verify(exactly = 1) { playerRepository.playOrPause() }
    }

    @Test
    fun `Given Next 를 두 번, Then 두 번째는 받는 중인 스테이션을 기준으로 다음으로 간다`() = runTest {
        nowPlayingStation.value = a
        val viewModel = createViewModel()

        viewModel.sendAction(StationPlayerAction.Next)
        viewModel.sendAction(StationPlayerAction.Next)

        // 재생 중(a)을 기준으로 하면 두 번 다 b 로 간다.
        verify(exactly = 1) { stationPlaybackController.play(b.id) }
        verify(exactly = 1) { stationPlaybackController.play(c.id) }
    }

    @Test
    fun `Given 첫 스테이션, When Previous, Then 반대 끝으로 순환한다`() = runTest {
        nowPlayingStation.value = a
        val viewModel = createViewModel()

        viewModel.sendAction(StationPlayerAction.Previous)

        verify(exactly = 1) { stationPlaybackController.play(c.id) }
    }

    @Test
    fun `Given 즐겨찾기 해제, Then 순환 목록이 즐겨찾기에서 전체로 바뀐다`() = runTest {
        nowPlayingStation.value = a
        favorites.value = listOf(a, c)
        val viewModel = createViewModel()
        val job = launchCollect(viewModel)
        assertEquals(c, viewModel.activeState().next)

        // 즐겨찾기가 하나만 남으면 돌 곳이 없어 전체 목록으로 넘어간다.
        favorites.value = listOf(a)

        assertEquals(b, viewModel.activeState().next)
        job.cancel()
    }

    @Test
    fun `Given Stop, Then 컨트롤러의 stop 에 위임하고 stopStation 은 부르지 않는다`() = runTest {
        nowPlayingStation.value = a
        val viewModel = createViewModel()

        viewModel.sendAction(StationPlayerAction.Stop)

        verify(exactly = 1) { stationPlaybackController.stop() }
        verify(exactly = 0) { playerRepository.stopStation(any()) }
    }

    @Test
    fun `Given ToggleFavorite, Then 지금 보이는 스테이션을 토글한다`() = runTest {
        pendingStationId.value = b.id
        nowPlayingStation.value = a
        val viewModel = createViewModel()
        val job = launchCollect(viewModel)
        coEvery { toggleFavoriteStationUseCase(any()) } returns true

        viewModel.sendAction(StationPlayerAction.ToggleFavorite)

        coVerify(exactly = 1) { toggleFavoriteStationUseCase(b.id) }
        job.cancel()
    }

    @Test
    fun `Given 슬립 타이머 액션, Then 타이머에 위임한다`() = runTest {
        val viewModel = createViewModel()

        viewModel.sendAction(StationPlayerAction.SetSleepTimer(60_000L))
        viewModel.sendAction(StationPlayerAction.CancelSleepTimer)

        verify(exactly = 1) { sleepTimer.start(60_000L) }
        verify(exactly = 1) { sleepTimer.cancel() }
    }

    @Test
    fun `Given 표시 스테이션이 바뀜, Then 상세가 그 스테이션의 것을 따라간다`() = runTest {
        coEvery { getStationDetailsUseCase(a.id) } returns stationDetailsTestData
        coEvery { getStationDetailsUseCase(b.id) } returns codecOnlyStationDetailsTestData
        nowPlayingStation.value = a
        val viewModel = createViewModel()
        val job = launchCollectDetails(viewModel)

        assertEquals(StationDetailsOf(a.id, stationDetailsTestData), viewModel.details.value)

        pendingStationId.value = b.id

        assertEquals(StationDetailsOf(b.id, codecOnlyStationDetailsTestData), viewModel.details.value)
        job.cancel()
    }

    @Test
    fun `Given 상세를 받지 못함, Then null 이다`() = runTest {
        coEvery { getStationDetailsUseCase(a.id) } returns null
        nowPlayingStation.value = a
        val viewModel = createViewModel()
        val job = launchCollectDetails(viewModel)

        assertEquals(StationDetailsOf(a.id, null), viewModel.details.value)
        job.cancel()
    }

    @Test
    fun `Given 재생 상태만 바뀜, Then 상세를 다시 받지 않는다`() = runTest {
        coEvery { getStationDetailsUseCase(a.id) } returns stationDetailsTestData
        nowPlayingStation.value = a
        val viewModel = createViewModel()
        val job = launchCollectDetails(viewModel)

        isPlaying.value = true
        streamTitle.value = "Artist - Title"

        coVerify(exactly = 1) { getStationDetailsUseCase(a.id) }
        job.cancel()
    }

    @Test
    fun `Given 표시 스테이션이 바뀜, Then 프로그램이 그 스테이션의 것을 따라간다`() = runTest {
        nowPlayingStation.value = a
        val viewModel = createViewModel()
        every { observeOnAirProgramUseCase(a.id) } returns flowOf(onAirProgramTestData)
        every { observeOnAirProgramUseCase(b.id) } returns flowOf(bareOnAirProgramTestData)
        val job = launchCollectOnAir(viewModel)

        assertEquals(OnAirOf(a.id, onAirProgramTestData), viewModel.onAir.value)

        pendingStationId.value = b.id

        assertEquals(OnAirOf(b.id, bareOnAirProgramTestData), viewModel.onAir.value)
        job.cancel()
    }

    @Test
    fun `Given 프로그램을 모름, Then 스테이션 id 와 null 이 실린다`() = runTest {
        nowPlayingStation.value = a
        val viewModel = createViewModel()
        val job = launchCollectOnAir(viewModel)

        assertEquals(OnAirOf(a.id, null), viewModel.onAir.value)
        job.cancel()
    }

    @Test
    fun `Given 재생 상태만 바뀜, Then 프로그램을 다시 구독하지 않는다`() = runTest {
        nowPlayingStation.value = a
        val viewModel = createViewModel()
        val job = launchCollectOnAir(viewModel)

        isPlaying.value = true
        streamTitle.value = "Artist - Title"

        verify(exactly = 1) { observeOnAirProgramUseCase(a.id) }
        job.cancel()
    }

    @Test
    fun `Given 스테이션이 없음, Then 프로그램은 구독하지 않는다`() = runTest {
        val viewModel = createViewModel()
        val job = launchCollectOnAir(viewModel)

        assertNull(viewModel.onAir.value)
        verify(exactly = 0) { observeOnAirProgramUseCase(any()) }
        job.cancel()
    }

    private fun TestScope.launchCollectOnAir(viewModel: StationPlayerViewModel) =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.onAir.collect {}
        }

    private fun TestScope.launchCollectDetails(viewModel: StationPlayerViewModel) =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.details.collect {}
        }

    private fun TestScope.launchCollect(viewModel: StationPlayerViewModel) =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }
}
