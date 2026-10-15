package io.jacob.episodive.feature.radio

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.station.StationPlaybackController
import io.jacob.episodive.core.domain.usecase.station.GetFavoriteStationsUseCase
import io.jacob.episodive.core.domain.usecase.station.GetRecentStationsUseCase
import io.jacob.episodive.core.domain.usecase.station.GetStationsUseCase
import io.jacob.episodive.core.domain.usecase.station.ObserveStationProgramImagesUseCase
import io.jacob.episodive.core.domain.usecase.station.ToggleFavoriteStationUseCase
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.StationKind
import io.jacob.episodive.core.model.StationListing
import io.jacob.episodive.core.model.StationListingSource
import io.jacob.episodive.core.testing.model.kbsFirstRadioStationTestData
import io.jacob.episodive.core.testing.model.curatedStationListingTestData
import io.jacob.episodive.core.testing.model.popularStationListingTestData
import io.jacob.episodive.core.testing.model.radioStationTestData
import io.jacob.episodive.core.testing.model.radioStationsTestData
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class RadioViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getStationsUseCase = mockk<GetStationsUseCase>()
    private val getFavoriteStationsUseCase = mockk<GetFavoriteStationsUseCase>()
    private val getRecentStationsUseCase = mockk<GetRecentStationsUseCase>()
    private val toggleFavoriteStationUseCase = mockk<ToggleFavoriteStationUseCase>()
    private val observeStationProgramImagesUseCase = mockk<ObserveStationProgramImagesUseCase>()
    private val stationPlaybackController = mockk<StationPlaybackController>(relaxed = true)
    private val playerRepository = mockk<PlayerRepository>(relaxed = true)

    private val nowPlayingStation = MutableStateFlow<Station?>(null)
    private val isPlaying = MutableStateFlow(false)
    private val isBuffering = MutableStateFlow(false)
    private val pendingStationId = MutableStateFlow<String?>(null)

    private val a = radioStationTestData
    private val b = kbsFirstRadioStationTestData

    private fun createViewModel(
        favorites: List<Station> = emptyList(),
        recents: List<Station> = emptyList(),
        listing: Flow<StationListing> = flowOf(curatedStationListingTestData),
    ): RadioViewModel {
        every { getStationsUseCase(StationKind.RADIO) } returns listing
        every { getFavoriteStationsUseCase() } returns flowOf(favorites)
        every { getRecentStationsUseCase(any()) } returns flowOf(recents)
        every { playerRepository.nowPlayingStation } returns nowPlayingStation
        every { playerRepository.isPlaying } returns isPlaying
        every { playerRepository.isBuffering } returns isBuffering
        every { stationPlaybackController.pendingStationId } returns pendingStationId
        every { observeStationProgramImagesUseCase(any()) } returns flowOf(emptyMap())

        return RadioViewModel(
            getStationsUseCase = getStationsUseCase,
            getFavoriteStationsUseCase = getFavoriteStationsUseCase,
            getRecentStationsUseCase = getRecentStationsUseCase,
            observeStationProgramImagesUseCase = observeStationProgramImagesUseCase,
            toggleFavoriteStationUseCase = toggleFavoriteStationUseCase,
            stationPlaybackController = stationPlaybackController,
            playerRepository = playerRepository,
        )
    }

    /** state 는 WhileSubscribed 라 구독이 있어야 값이 흐른다. 클릭 판정이 state.value 를 읽는다. */
    private fun TestScope.subscribe(viewModel: RadioViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }
    }

    private fun success(viewModel: RadioViewModel) = viewModel.state.value as RadioState.Success

    @Test
    fun `Given 전국과 지역 방송, When 상태를 만들면, Then 둘로 나뉘고 favoriteIds 는 즐겨찾기에서 파생된다`() = runTest {
        val viewModel = createViewModel(favorites = listOf(a))

        viewModel.state.test {
            val state = expectMostRecentItem() as RadioState.Success

            assertTrue(state.nationwide.all { it.region == null })
            assertTrue(state.regional.all { it.region != null })
            assertEquals(radioStationsTestData.size, state.nationwide.size + state.regional.size)
            assertEquals(setOf(a.id), state.favoriteIds)
        }
    }

    @Test
    fun `Given 한국 큐레이션 목록, Then Curated 이고 popular 는 비어 있다`() = runTest {
        val viewModel = createViewModel()

        viewModel.state.test {
            val state = expectMostRecentItem() as RadioState.Success

            assertEquals(StationListingSource.Curated, state.listingSource)
            assertEquals("KR", state.countryCode)
            assertTrue(state.popular.isEmpty())
        }
    }

    @Test
    fun `Given 해외 인기 목록, Then popular 하나에 담기고 전국과 지역은 비어 있다`() = runTest {
        val listing = popularStationListingTestData("US")
        val viewModel = createViewModel(listing = flowOf(listing))

        viewModel.state.test {
            val state = expectMostRecentItem() as RadioState.Success

            assertEquals(StationListingSource.RadioBrowser, state.listingSource)
            assertEquals("US", state.countryCode)
            // region 이 있는 스테이션도 지역 방송으로 가르지 않는다.
            assertEquals(listing.stations, state.popular)
            assertTrue(state.nationwide.isEmpty())
            assertTrue(state.regional.isEmpty())
        }
    }

    @Test
    fun `Given 해외 목록이 비어 있음, Then 빈 popular 의 Success 다`() = runTest {
        val viewModel = createViewModel(listing = flowOf(popularStationListingTestData("US", emptyList())))

        viewModel.state.test {
            val state = expectMostRecentItem() as RadioState.Success

            assertEquals(StationListingSource.RadioBrowser, state.listingSource)
            assertTrue(state.popular.isEmpty())
        }
    }

    @Test
    fun `Given 해외 목록을 받지 못함, Then Error 이고 다시 시도하면 목록이 나온다`() = runTest {
        var attempt = 0
        val listing = flow {
            if (attempt++ == 0) throw IOException("offline")
            emit(popularStationListingTestData("US"))
        }
        val viewModel = createViewModel(listing = listing)
        subscribe(viewModel)

        assertTrue(viewModel.state.value is RadioState.Error)

        viewModel.sendAction(RadioAction.Retry)

        assertTrue(viewModel.state.value is RadioState.Success)
    }

    @Test
    fun `Given 스테이션 클릭, Then 컨트롤러에 위임한다`() = runTest {
        val viewModel = createViewModel()

        viewModel.sendAction(RadioAction.ClickStation(a))

        verify(exactly = 1) { stationPlaybackController.play(a.id) }
    }

    @Test
    fun `Given 컨트롤러가 받는 중, Then pendingStationId 가 상태에 반영된다`() = runTest {
        val viewModel = createViewModel()
        subscribe(viewModel)

        pendingStationId.value = b.id

        assertEquals(b.id, success(viewModel).pendingStationId)
    }

    @Test
    fun `Given 첫 토글이 느림, When 연타하면, Then 이펙트는 누른 순서대로 나온다`() = runTest {
        var callCount = 0
        coEvery { toggleFavoriteStationUseCase(a.id) } coAnswers {
            callCount++
            if (callCount == 1) {
                delay(100)
                true
            } else {
                false
            }
        }
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.sendAction(RadioAction.ToggleFavorite(a.id))
            viewModel.sendAction(RadioAction.ToggleFavorite(a.id))

            assertEquals(RadioEffect.ShowFavoriteSnackbar(a.id, true), awaitItem())
            assertEquals(RadioEffect.ShowFavoriteSnackbar(a.id, false), awaitItem())
        }
    }

    @Test
    fun `Given 카탈로그 전체, Then 프로그램 이미지는 라디오 스테이션 id 전부로 구독하고 이미지가 있는 것만 매핑된다`() = runTest {
        val viewModel = createViewModel()
        every { observeStationProgramImagesUseCase(any()) } returns flowOf(mapOf(a.id to "https://img/a.jpg"))

        viewModel.programImages.test {
            // 초기값(빈 맵) 뒤에 매핑이 온다. onAirSource 가 없는 스테이션은 맵에 없다.
            assertEquals(mapOf(a.id to "https://img/a.jpg"), expectMostRecentItem())
            cancelAndIgnoreRemainingEvents()
        }
        verify(exactly = 1) { observeStationProgramImagesUseCase(radioStationsTestData.map { it.id }) }
    }

    @Test
    fun `Given 이미지 구독 실패, Then 빈 맵이고 목록 상태는 영향받지 않는다`() = runTest {
        val viewModel = createViewModel()
        every { observeStationProgramImagesUseCase(any()) } returns flow { throw IllegalStateException("boom") }

        viewModel.programImages.test {
            assertEquals(emptyMap<String, String>(), expectMostRecentItem())
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.state.test {
            assertTrue(expectMostRecentItem() is RadioState.Success)
        }
    }
}
