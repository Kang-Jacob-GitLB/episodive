package io.jacob.episodive.feature.radio

import io.jacob.episodive.core.domain.player.SleepTimer
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.station.StationPlaybackController
import io.jacob.episodive.core.domain.usecase.caption.CaptionToggleResult
import io.jacob.episodive.core.domain.usecase.caption.ObserveCaptionDownloadFailuresUseCase
import io.jacob.episodive.core.domain.usecase.caption.ObserveStationCaptionUseCase
import io.jacob.episodive.core.domain.usecase.caption.StationCaption
import io.jacob.episodive.core.domain.usecase.caption.ToggleCaptionUseCase
import io.jacob.episodive.core.domain.usecase.station.GetFavoriteStationsUseCase
import io.jacob.episodive.core.domain.usecase.station.GetRecentStationsUseCase
import io.jacob.episodive.core.domain.usecase.station.GetStationDetailsUseCase
import io.jacob.episodive.core.domain.usecase.station.GetStationUseCase
import io.jacob.episodive.core.domain.usecase.station.GetStationsUseCase
import io.jacob.episodive.core.domain.usecase.station.ObserveOnAirProgramUseCase
import io.jacob.episodive.core.domain.usecase.station.ObserveScheduleUseCase
import io.jacob.episodive.core.domain.usecase.station.ToggleFavoriteStationUseCase
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.StationKind
import io.jacob.episodive.core.model.StationListing
import io.jacob.episodive.core.model.StationSchedule
import io.jacob.episodive.core.model.caption.CaptionDownloadFailure
import io.jacob.episodive.core.model.caption.CaptionLanguage
import io.jacob.episodive.core.model.caption.LiveCaptionState
import io.jacob.episodive.core.testing.model.bareOnAirProgramTestData
import io.jacob.episodive.core.testing.model.codecOnlyStationDetailsTestData
import io.jacob.episodive.core.testing.model.curatedStationListingTestData
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
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class StationPlayerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val playerRepository = mockk<PlayerRepository>(relaxed = true)
    private val stationPlaybackController = mockk<StationPlaybackController>(relaxed = true)
    private val getStationsUseCase = mockk<GetStationsUseCase>()
    private val getFavoriteStationsUseCase = mockk<GetFavoriteStationsUseCase>()
    private val getRecentStationsUseCase = mockk<GetRecentStationsUseCase>()
    private val getStationUseCase = mockk<GetStationUseCase>()
    private val toggleFavoriteStationUseCase = mockk<ToggleFavoriteStationUseCase>(relaxed = true)
    private val getStationDetailsUseCase = mockk<GetStationDetailsUseCase>()
    private val observeOnAirProgramUseCase = mockk<ObserveOnAirProgramUseCase>()
    private val observeScheduleUseCase = mockk<ObserveScheduleUseCase>()
    private val sleepTimer = mockk<SleepTimer>(relaxed = true)
    private val observeStationCaptionUseCase = mockk<ObserveStationCaptionUseCase>()
    private val toggleCaptionUseCase = mockk<ToggleCaptionUseCase>()
    private val observeCaptionDownloadFailuresUseCase = mockk<ObserveCaptionDownloadFailuresUseCase>()

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
    private val recents = MutableStateFlow<List<Station>>(emptyList())
    private var listingFlow: Flow<StationListing>? = null
    /** 저장소가 아는 스테이션(카탈로그·메모리·Room). 현재 국가의 목록과 별개다. */
    private val knownStations = mutableMapOf<String, Station>()
    private val sleepRemaining = MutableStateFlow<Long?>(null)
    private val captionState = MutableStateFlow(StationCaption(null, LiveCaptionState.Initial))
    private val captionFailures = MutableSharedFlow<CaptionDownloadFailure>(extraBufferCapacity = 1)
    private var captionSubscriptions = 0

    private fun createViewModel(): StationPlayerViewModel {
        every { playerRepository.nowPlayingStation } returns nowPlayingStation
        every { playerRepository.streamTitle } returns streamTitle
        every { playerRepository.isPlaying } returns isPlaying
        every { playerRepository.isBuffering } returns isBuffering
        every { stationPlaybackController.pendingStationId } returns pendingStationId
        every { getFavoriteStationsUseCase() } returns favorites
        every { getStationsUseCase(StationKind.RADIO) } returns
            (listingFlow ?: stations.map { curatedStationListingTestData.copy(stations = it) })
        every { getRecentStationsUseCase(any()) } returns recents
        coEvery { getStationUseCase(any()) } coAnswers { knownStations[firstArg()] }
        every { sleepTimer.remainingMs } returns sleepRemaining
        every { observeOnAirProgramUseCase(any()) } returns flowOf(null)
        every { observeScheduleUseCase(any()) } returns flowOf(StationSchedule(0L, emptyList()))
        every { observeStationCaptionUseCase() } returns captionState.onStart { captionSubscriptions++ }
        every { observeCaptionDownloadFailuresUseCase() } returns captionFailures
        // play 가 pending 을 즉시 세우는 컨트롤러를 흉내 낸다. 두 번째 호출이 이 값을 기준으로 삼아야 한다.
        every { stationPlaybackController.play(any()) } answers { pendingStationId.value = firstArg() }
        return StationPlayerViewModel(
            playerRepository = playerRepository,
            stationPlaybackController = stationPlaybackController,
            getStationsUseCase = getStationsUseCase,
            getFavoriteStationsUseCase = getFavoriteStationsUseCase,
            getRecentStationsUseCase = getRecentStationsUseCase,
            getStationUseCase = getStationUseCase,
            toggleFavoriteStationUseCase = toggleFavoriteStationUseCase,
            getStationDetailsUseCase = getStationDetailsUseCase,
            observeOnAirProgramUseCase = observeOnAirProgramUseCase,
            observeScheduleUseCase = observeScheduleUseCase,
            sleepTimer = sleepTimer,
            observeStationCaptionUseCase = observeStationCaptionUseCase,
            toggleCaptionUseCase = toggleCaptionUseCase,
            observeCaptionDownloadFailuresUseCase = observeCaptionDownloadFailuresUseCase,
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
    fun `Given 현재 국가 목록에 없는 최근 스테이션을 받는 중, Then 최근에서 찾아 Loading 에 고착되지 않는다`() = runTest {
        val abroad = radioStationTestData.copy(id = "rb:u9", name = "Abroad", broadcaster = "", language = "en")
        recents.value = listOf(abroad)
        pendingStationId.value = abroad.id
        val viewModel = createViewModel()
        val job = launchCollect(viewModel)

        val s = viewModel.activeState()
        assertEquals(abroad, s.station)
        assertTrue(s.isPending)
        job.cancel()
    }

    @Test
    fun `Given 목록에도 즐겨찾기에도 최근에도 없는 스테이션을 받는 중, Then 저장소 조회로 찾는다`() = runTest {
        // 순위 변동으로 목록에서 빠진 해외 스테이션처럼 현재 목록 사본으로는 못 찾는 경우다.
        val dropped = radioStationTestData.copy(id = "rb:u8", name = "Dropped", broadcaster = "", language = "en")
        knownStations[dropped.id] = dropped
        pendingStationId.value = dropped.id
        val viewModel = createViewModel()
        val job = launchCollect(viewModel)

        val s = viewModel.activeState()
        assertEquals(dropped, s.station)
        assertTrue(s.isPending)
        job.cancel()
    }

    @Test
    fun `Given 해외 전체 목록이 아직 오지 않음, Then 목록을 기다리지 않고 재생 중인 스테이션으로 시트가 선다`() = runTest {
        listingFlow = flow { awaitCancellation() }
        nowPlayingStation.value = a
        val viewModel = createViewModel()
        val job = launchCollect(viewModel)

        assertEquals(a, viewModel.activeState().station)
        job.cancel()
    }

    @Test
    fun `Given 기준이 현재 국가 목록에 없고 최근에 있음, Then 이전과 다음은 최근 목록을 돈다`() = runTest {
        val abroad1 = radioStationTestData.copy(id = "rb:u1", name = "Abroad 1", broadcaster = "")
        val abroad2 = radioStationTestData.copy(id = "rb:u2", name = "Abroad 2", broadcaster = "")
        nowPlayingStation.value = abroad1
        recents.value = listOf(abroad1, abroad2)
        val viewModel = createViewModel()
        val job = launchCollect(viewModel)

        val s = viewModel.activeState()
        assertEquals(abroad2, s.next)
        assertEquals(abroad2, s.previous)
        job.cancel()
    }

    @Test
    fun `Given 전체 목록을 받지 못함, Then 시트는 죽지 않고 즐겨찾기에서 찾는다`() = runTest {
        listingFlow = flow { throw IOException("offline") }
        favorites.value = listOf(b)
        pendingStationId.value = b.id
        val viewModel = createViewModel()
        val job = launchCollect(viewModel)

        assertEquals(b, viewModel.activeState().station)
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
        // 시트가 열려 state 를 수집해야 공유 전체 목록이 채워진다(실제로 이전/다음은 시트에서만 눌린다).
        val job = launchCollect(viewModel)

        viewModel.sendAction(StationPlayerAction.Next)
        viewModel.sendAction(StationPlayerAction.Next)

        // 재생 중(a)을 기준으로 하면 두 번 다 b 로 간다.
        verify(exactly = 1) { stationPlaybackController.play(b.id) }
        verify(exactly = 1) { stationPlaybackController.play(c.id) }
        job.cancel()
    }

    @Test
    fun `Given 첫 스테이션, When Previous, Then 반대 끝으로 순환한다`() = runTest {
        nowPlayingStation.value = a
        val viewModel = createViewModel()
        // 시트가 열려 state 를 수집해야 공유 전체 목록이 채워진다(실제로 이전/다음은 시트에서만 눌린다).
        val job = launchCollect(viewModel)

        viewModel.sendAction(StationPlayerAction.Previous)

        verify(exactly = 1) { stationPlaybackController.play(c.id) }
        job.cancel()
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

    @Test
    fun `Given 표시 스테이션이 바뀜, Then 편성표가 그 스테이션의 것을 따라간다`() = runTest {
        nowPlayingStation.value = a
        val viewModel = createViewModel()
        every { observeScheduleUseCase(a.id) } returns flowOf(StationSchedule(0L, listOf(onAirProgramTestData)))
        every { observeScheduleUseCase(b.id) } returns flowOf(StationSchedule(0L, listOf(bareOnAirProgramTestData)))
        val job = launchCollectSchedule(viewModel)

        assertEquals(ScheduleOf(a.id, StationSchedule(0L, listOf(onAirProgramTestData))), viewModel.schedule.value)

        pendingStationId.value = b.id

        assertEquals(ScheduleOf(b.id, StationSchedule(0L, listOf(bareOnAirProgramTestData))), viewModel.schedule.value)
        job.cancel()
    }

    @Test
    fun `Given 편성표를 모름, Then 스테이션 id 와 빈 목록이 실린다`() = runTest {
        nowPlayingStation.value = a
        val viewModel = createViewModel()
        val job = launchCollectSchedule(viewModel)

        assertEquals(ScheduleOf(a.id, StationSchedule(0L, emptyList())), viewModel.schedule.value)
        job.cancel()
    }

    @Test
    fun `Given 재생 상태만 바뀜, Then 편성표를 다시 구독하지 않는다`() = runTest {
        nowPlayingStation.value = a
        val viewModel = createViewModel()
        val job = launchCollectSchedule(viewModel)

        isPlaying.value = true
        streamTitle.value = "Artist - Title"

        verify(exactly = 1) { observeScheduleUseCase(a.id) }
        job.cancel()
    }

    private fun TestScope.launchCollectCaption(viewModel: StationPlayerViewModel) =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.caption.collect {}
        }

    private fun TestScope.collectEffects(viewModel: StationPlayerViewModel): List<StationPlayerEffect> {
        val effects = mutableListOf<StationPlayerEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.collect { effects.add(it) } }
        return effects
    }

    @Test
    fun `Given caption 을 수집하지 않음, Then 자막 흐름을 구독하지 않는다`() = runTest {
        nowPlayingStation.value = a
        val viewModel = createViewModel()
        val job = launchCollect(viewModel)

        assertEquals(0, captionSubscriptions)
        assertEquals(LiveCaptionState.Initial, viewModel.caption.value.state)

        val captionJob = launchCollectCaption(viewModel)
        assertEquals(1, captionSubscriptions)
        captionJob.cancel()
        job.cancel()
    }

    @Test
    fun `Given 모델 다운로드가 시작됨, When 자막 토글, Then 시작 effect 가 나간다`() = runTest {
        nowPlayingStation.value = a
        captionState.value = StationCaption(a.id, LiveCaptionState.Initial)
        coEvery { toggleCaptionUseCase(any()) } returns CaptionToggleResult.DownloadStarted(1234L)
        val viewModel = createViewModel()
        val job = launchCollect(viewModel)
        val captionJob = launchCollectCaption(viewModel)
        val effects = collectEffects(viewModel)

        viewModel.sendAction(StationPlayerAction.ToggleCaption)

        assertEquals(listOf<StationPlayerEffect>(StationPlayerEffect.CaptionDownloadStarted(1234L)), effects)
        captionJob.cancel()
        job.cancel()
    }

    @Test
    fun `Given 지원하지 않는 언어, When 자막 토글, Then Unsupported effect 가 나간다`() = runTest {
        nowPlayingStation.value = a
        captionState.value = StationCaption(a.id, LiveCaptionState.Initial)
        coEvery { toggleCaptionUseCase(any()) } returns CaptionToggleResult.UnsupportedLanguage
        val viewModel = createViewModel()
        val job = launchCollect(viewModel)
        val captionJob = launchCollectCaption(viewModel)
        val effects = collectEffects(viewModel)

        viewModel.sendAction(StationPlayerAction.ToggleCaption)

        assertEquals(listOf<StationPlayerEffect>(StationPlayerEffect.CaptionUnsupported), effects)
        captionJob.cancel()
        job.cancel()
    }

    @Test
    fun `Given 스테이션을 받는 중, When 자막 토글, Then 무시한다`() = runTest {
        nowPlayingStation.value = a
        pendingStationId.value = b.id
        coEvery { toggleCaptionUseCase(any()) } returns CaptionToggleResult.Enabled
        val viewModel = createViewModel()
        val job = launchCollect(viewModel)

        viewModel.sendAction(StationPlayerAction.ToggleCaption)

        coVerify(exactly = 0) { toggleCaptionUseCase(any()) }
        job.cancel()
    }

    @Test
    fun `Given 자막이 아직 이전 스테이션의 값, When 자막 토글, Then 무시한다`() = runTest {
        // 스테이션이 a 로 바뀌었지만 caption 은 아직 b 의 값이다.
        nowPlayingStation.value = a
        captionState.value = StationCaption(b.id, LiveCaptionState.Initial)
        coEvery { toggleCaptionUseCase(any()) } returns CaptionToggleResult.Enabled
        val viewModel = createViewModel()
        val job = launchCollect(viewModel)
        val captionJob = launchCollectCaption(viewModel)

        viewModel.sendAction(StationPlayerAction.ToggleCaption)

        coVerify(exactly = 0) { toggleCaptionUseCase(any()) }
        captionJob.cancel()
        job.cancel()
    }

    @Test
    fun `Given 모델 다운로드 실패, Then 실패 effect 가 나간다`() = runTest {
        val viewModel = createViewModel()
        val effects = collectEffects(viewModel)

        captionFailures.emit(CaptionDownloadFailure(CaptionLanguage.KOREAN, CaptionDownloadFailure.Reason.NETWORK))

        assertEquals(
            listOf<StationPlayerEffect>(StationPlayerEffect.CaptionDownloadFailed(CaptionDownloadFailure.Reason.NETWORK)),
            effects,
        )
    }

    private fun TestScope.launchCollectSchedule(viewModel: StationPlayerViewModel) =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.schedule.collect {}
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
