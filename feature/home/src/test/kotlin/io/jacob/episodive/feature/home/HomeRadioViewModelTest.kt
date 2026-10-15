package io.jacob.episodive.feature.home

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.station.StationPlaybackController
import io.jacob.episodive.core.domain.usecase.station.GetFavoriteStationsUseCase
import io.jacob.episodive.core.domain.usecase.station.GetRecentStationsUseCase
import io.jacob.episodive.core.domain.usecase.station.GetStationsUseCase
import io.jacob.episodive.core.domain.usecase.station.ObserveStationProgramImagesUseCase
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.StationKind
import io.jacob.episodive.core.model.StationListing
import io.jacob.episodive.core.testing.model.curatedStationListingTestData
import io.jacob.episodive.core.testing.model.popularStationListingTestData
import io.jacob.episodive.core.testing.model.radioStationTestData
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeRadioViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getStationsUseCase = mockk<GetStationsUseCase>()
    private val getFavoriteStationsUseCase = mockk<GetFavoriteStationsUseCase>()
    private val getRecentStationsUseCase = mockk<GetRecentStationsUseCase>()
    private val observeStationProgramImagesUseCase = mockk<ObserveStationProgramImagesUseCase>()
    private val stationPlaybackController = mockk<StationPlaybackController>(relaxed = true)
    private val playerRepository = mockk<PlayerRepository>(relaxed = true)

    private val nowPlayingStation = MutableStateFlow<Station?>(null)
    private val pendingStationId = MutableStateFlow<String?>(null)

    private fun station(index: Int) = radioStationTestData.copy(id = "s$index", name = "방송 $index")

    private fun createViewModel(
        favorites: List<Station> = emptyList(),
        recents: List<Station> = emptyList(),
        catalog: List<Station> = (0 until 12).map(::station),
        listing: Flow<StationListing> = flowOf(curatedStationListingTestData.copy(stations = catalog)),
    ): HomeRadioViewModel {
        every { getFavoriteStationsUseCase() } returns flowOf(favorites)
        every { getRecentStationsUseCase(any()) } returns flowOf(recents)
        every { getStationsUseCase(StationKind.RADIO) } returns listing
        every { playerRepository.nowPlayingStation } returns nowPlayingStation
        every { playerRepository.isPlaying } returns MutableStateFlow(false)
        every { playerRepository.isBuffering } returns MutableStateFlow(false)
        every { stationPlaybackController.pendingStationId } returns pendingStationId
        every { observeStationProgramImagesUseCase(any()) } returns flowOf(emptyMap())

        return HomeRadioViewModel(
            getStationsUseCase = getStationsUseCase,
            getFavoriteStationsUseCase = getFavoriteStationsUseCase,
            getRecentStationsUseCase = getRecentStationsUseCase,
            observeStationProgramImagesUseCase = observeStationProgramImagesUseCase,
            stationPlaybackController = stationPlaybackController,
            playerRepository = playerRepository,
        )
    }

    private fun TestScope.subscribe(viewModel: HomeRadioViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }
    }

    @Test
    fun `Given 즐겨찾기와 최근이 겹침, Then 즐겨찾기가 앞서고 중복 없이 합친 뒤 카탈로그로 여덟 개까지 채운다`() = runTest {
        val viewModel = createViewModel(
            favorites = listOf(station(1), station(2)),
            recents = listOf(station(2), station(3)),
        )

        viewModel.state.test {
            assertEquals(
                listOf("s1", "s2", "s3", "s0", "s4", "s5", "s6", "s7"),
                (expectMostRecentItem() as HomeRadioState.Success).stations.map { it.id },
            )
        }
    }

    @Test
    fun `Given 최근 하나뿐, Then 그 하나가 앞서고 나머지는 카탈로그로 채운다`() = runTest {
        val viewModel = createViewModel(recents = listOf(station(5)))

        viewModel.state.test {
            val ids = (expectMostRecentItem() as HomeRadioState.Success).stations.map { it.id }
            assertEquals(8, ids.size)
            assertEquals("s5", ids.first())
            assertEquals(ids.size, ids.toSet().size)
        }
    }

    @Test
    fun `Given 열 개를 넘음, Then 열 개로 자른다`() = runTest {
        val viewModel = createViewModel(
            favorites = (0 until 8).map(::station),
            recents = (8 until 15).map(::station),
        )

        viewModel.state.test {
            assertEquals(10, (expectMostRecentItem() as HomeRadioState.Success).stations.size)
        }
    }

    @Test
    fun `Given 즐겨찾기와 최근이 모두 비어 있음, Then 카탈로그 앞 여덟 개`() = runTest {
        val viewModel = createViewModel()

        viewModel.state.test {
            assertEquals((0 until 8).map { "s$it" }, (expectMostRecentItem() as HomeRadioState.Success).stations.map { it.id })
        }
    }

    @Test
    fun `Given 아직 오지 않음, Then 초기 상태는 Loading 이다`() = runTest {
        val viewModel = createViewModel()

        assertEquals(HomeRadioState.Loading, viewModel.state.value)
    }

    @Test
    fun `Given 즐겨찾기를 읽다 실패, Then Error 상태다`() = runTest {
        val viewModel = createViewModel()
        every { getFavoriteStationsUseCase() } returns flow { throw IllegalStateException("boom") }
        val failing = HomeRadioViewModel(
            getStationsUseCase = getStationsUseCase,
            getFavoriteStationsUseCase = getFavoriteStationsUseCase,
            getRecentStationsUseCase = getRecentStationsUseCase,
            observeStationProgramImagesUseCase = observeStationProgramImagesUseCase,
            stationPlaybackController = stationPlaybackController,
            playerRepository = playerRepository,
        )

        failing.state.test {
            assertTrue(expectMostRecentItem() is HomeRadioState.Error)
        }
        assertEquals(HomeRadioState.Loading, viewModel.state.value)
    }

    @Test
    fun `Given 스테이션 클릭, Then 컨트롤러에 위임한다`() = runTest {
        val viewModel = createViewModel()

        viewModel.sendAction(HomeRadioAction.ClickStation(station(0)))

        verify(exactly = 1) { stationPlaybackController.play("s0") }
    }

    @Test
    fun `Given 컨트롤러가 받는 중, Then pendingStationId 가 상태에 반영된다`() = runTest {
        val viewModel = createViewModel()
        subscribe(viewModel)

        pendingStationId.value = "s1"

        assertEquals("s1", (viewModel.state.value as HomeRadioState.Success).pendingStationId)
    }

    @Test
    fun `Given 섹션에 보이는 스테이션, Then 그 id 들로만 프로그램 이미지를 구독한다`() = runTest {
        val viewModel = createViewModel(favorites = listOf(station(1)), catalog = (0 until 12).map(::station))
        every { observeStationProgramImagesUseCase(any()) } answers {
            flowOf(firstArg<List<String>>().filter { it == "s1" }.associateWith { "https://img/$it.jpg" })
        }

        viewModel.programImages.test {
            assertEquals(mapOf("s1" to "https://img/s1.jpg"), expectMostRecentItem())
            cancelAndIgnoreRemainingEvents()
        }
        // 섹션은 즐겨찾기 1개 + 카탈로그로 채운 7개 = 8개다. 카탈로그 12개 전부를 구독하지 않는다.
        verify(exactly = 1) {
            observeStationProgramImagesUseCase(match { it.size == 8 && "s1" in it })
        }
    }

    @Test
    fun `Given 재생 상태만 바뀜, Then 섹션 구성이 같아 프로그램 이미지를 다시 구독하지 않는다`() = runTest {
        val viewModel = createViewModel(favorites = listOf(station(1)))

        viewModel.programImages.test {
            awaitItem()
            nowPlayingStation.value = station(1)
            nowPlayingStation.value = station(2)
            nowPlayingStation.value = null
            cancelAndIgnoreRemainingEvents()
        }

        // 로딩 중의 빈 구성 한 번을 빼면, 섹션이 채워진 뒤로는 구독이 한 번뿐이다.
        verify(exactly = 1) { observeStationProgramImagesUseCase(match { it.isNotEmpty() }) }
    }

    @Test
    fun `Given 해외 목록이 아직 오지 않음, Then 기다리지 않고 즐겨찾기와 최근으로 먼저 그린다`() = runTest {
        val viewModel = createViewModel(
            favorites = listOf(station(1)),
            recents = listOf(station(2)),
            listing = flow { awaitCancellation() },
        )

        viewModel.state.test {
            // 목록의 첫 값을 잠깐 기다린 뒤(깜빡임 방지) 기다리지 않고 먼저 그린다.
            advanceTimeBy(1_000)
            runCurrent()
            assertEquals(
                listOf("s1", "s2"),
                (expectMostRecentItem() as HomeRadioState.Success).stations.map { it.id },
            )
        }
    }

    @Test
    fun `Given 해외 목록을 받지 못함, Then 오류가 아니라 즐겨찾기와 최근만으로 Success 고 백오프로 복구한다`() = runTest {
        var attempt = 0
        val recovered = popularStationListingTestData("US", (0 until 3).map(::station))
        val listing = flow {
            if (attempt++ < 2) throw java.io.IOException("offline")
            emit(recovered)
        }
        val viewModel = createViewModel(
            favorites = listOf(station(9)),
            listing = listing,
        )

        viewModel.state.test {
            assertEquals(
                listOf("s9"),
                (expectMostRecentItem() as HomeRadioState.Success).stations.map { it.id },
            )

            // 1초 뒤 첫 재시도도 실패하고, 그로부터 2초 뒤 두 번째 재시도에서 복구한다.
            advanceTimeBy(1_000)
            runCurrent()
            assertEquals(2, attempt)
            advanceTimeBy(2_000)
            runCurrent()
            assertEquals(3, attempt)

            assertEquals(
                listOf("s9", "s0", "s1", "s2"),
                (expectMostRecentItem() as HomeRadioState.Success).stations.map { it.id },
            )
        }
    }

    @Test
    fun `Given 해외 목록이 아직 오지 않고 내 방송도 없음, Then 섹션 자리를 비우지 않고 Loading 을 유지한다`() = runTest {
        val viewModel = createViewModel(listing = flow { awaitCancellation() })

        viewModel.state.test {
            advanceTimeBy(1_000)
            runCurrent()
            assertEquals(HomeRadioState.Loading, expectMostRecentItem())
        }
    }

    @Test
    fun `Given 해외 목록이 오래 걸린 뒤 도착, Then Loading 에서 곧바로 채워진 Success 가 된다`() = runTest {
        val listing = flow {
            delay(5_000)
            emit(popularStationListingTestData("US", (0 until 3).map(::station)))
        }
        val viewModel = createViewModel(listing = listing)

        viewModel.state.test {
            advanceTimeBy(1_000)
            runCurrent()
            assertEquals(HomeRadioState.Loading, expectMostRecentItem())

            advanceTimeBy(5_000)
            runCurrent()
            assertEquals(
                listOf("s0", "s1", "s2"),
                (expectMostRecentItem() as HomeRadioState.Success).stations.map { it.id },
            )
        }
    }

    @Test
    fun `Given 네트워크가 아닌 목록 실패, Then 재시도하지 않고 입구만 남긴다`() = runTest {
        var attempt = 0
        val listing = flow<StationListing> {
            attempt++
            throw IllegalStateException("boom")
        }
        val viewModel = createViewModel(listing = listing)

        viewModel.state.test {
            advanceTimeBy(60_000)
            runCurrent()
            assertTrue((expectMostRecentItem() as HomeRadioState.Success).isEntranceOnly)
        }
        assertEquals(1, attempt)
    }

    @Test
    fun `Given 해외 목록이 비어 있고 즐겨찾기도 없음, Then 입구만 남기는 상태다`() = runTest {
        val viewModel = createViewModel(listing = flowOf(popularStationListingTestData("US", emptyList())))

        viewModel.state.test {
            val state = expectMostRecentItem() as HomeRadioState.Success
            assertTrue(state.stations.isEmpty())
            assertTrue(state.isEntranceOnly)
        }
    }

    @Test
    fun `Given 해외 목록을 받지 못하고 즐겨찾기도 없음, Then 입구만 남기는 상태다`() = runTest {
        val viewModel = createViewModel(listing = flow { throw java.io.IOException("offline") })

        viewModel.state.test {
            assertTrue((expectMostRecentItem() as HomeRadioState.Success).isEntranceOnly)
        }
    }

    @Test
    fun `Given 한국 카탈로그, Then 입구만 남기는 상태가 아니다`() = runTest {
        val viewModel = createViewModel()

        viewModel.state.test {
            assertEquals(false, (expectMostRecentItem() as HomeRadioState.Success).isEntranceOnly)
        }
    }
}
