package io.jacob.episodive.feature.radio

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.station.LiveStreamSupervisor
import io.jacob.episodive.core.domain.station.StationPlaybackController
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.testing.model.radioStationTestData
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StationBarViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val playerRepository = mockk<PlayerRepository>(relaxed = true)
    private val supervisor = mockk<LiveStreamSupervisor>()
    private val stationPlaybackController = mockk<StationPlaybackController>(relaxed = true)

    private val nowPlayingStation = MutableStateFlow<Station?>(null)
    private val streamTitle = MutableStateFlow<String?>(null)
    private val isPlaying = MutableStateFlow(false)
    private val isBuffering = MutableStateFlow(false)
    private val unplayable = MutableSharedFlow<Station>(extraBufferCapacity = 1)
    private val unavailable = MutableSharedFlow<Station>(extraBufferCapacity = 1)

    private fun createViewModel(): StationBarViewModel {
        every { playerRepository.nowPlayingStation } returns nowPlayingStation
        every { playerRepository.streamTitle } returns streamTitle
        every { playerRepository.isPlaying } returns isPlaying
        every { playerRepository.isBuffering } returns isBuffering
        every { supervisor.unplayable } returns unplayable
        every { stationPlaybackController.unavailable } returns unavailable
        return StationBarViewModel(playerRepository, supervisor, stationPlaybackController)
    }

    @Test
    fun `Given 스테이션 없음, Then Hidden`() = runTest {
        val viewModel = createViewModel()

        viewModel.state.test {
            assertEquals(StationBarState.Hidden, expectMostRecentItem())
        }
    }

    @Test
    fun `Given 스테이션이 올라옴, Then Active 이고 제목과 버퍼링이 반영된다`() = runTest {
        val viewModel = createViewModel()

        viewModel.state.test {
            assertEquals(StationBarState.Hidden, awaitItem())

            nowPlayingStation.value = radioStationTestData
            assertEquals(
                StationBarState.Active(radioStationTestData, null, isPlaying = false, isBuffering = false),
                expectMostRecentItem(),
            )

            streamTitle.value = "Artist - Title"
            isBuffering.value = true
            assertEquals(
                StationBarState.Active(radioStationTestData, "Artist - Title", isPlaying = false, isBuffering = true),
                expectMostRecentItem(),
            )

            nowPlayingStation.value = null
            assertEquals(StationBarState.Hidden, expectMostRecentItem())
        }
    }

    @Test
    fun `Given Stop, Then 컨트롤러의 stop 에 위임하고 stopStation 은 부르지 않는다`() = runTest {
        val viewModel = createViewModel()

        viewModel.sendAction(StationBarAction.Stop)

        verify(exactly = 1) { stationPlaybackController.stop() }
        verify(exactly = 0) { playerRepository.stopStation(any()) }
    }

    @Test
    fun `Given PlayOrPause, Then playOrPause 를 부른다`() = runTest {
        val viewModel = createViewModel()

        viewModel.sendAction(StationBarAction.PlayOrPause)

        verify(exactly = 1) { playerRepository.playOrPause() }
    }

    @Test
    fun `Given 감독자가 포기, Then ShowUnplayable 이펙트`() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            unplayable.emit(radioStationTestData)

            assertEquals(StationBarEffect.ShowUnplayable(radioStationTestData), awaitItem())
        }
    }

    @Test
    fun `Given 주소를 얻지 못함, Then ShowUnavailable 이펙트`() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            unavailable.emit(radioStationTestData)

            assertEquals(StationBarEffect.ShowUnavailable(radioStationTestData), awaitItem())
        }
    }

    @Test
    fun `Given 스테이션이 올라 있음, Then isStationActive 는 구독 없이도 참이다`() = runTest {
        nowPlayingStation.value = radioStationTestData
        val viewModel = createViewModel()

        // state 는 구독 전이라 Hidden 이지만 플레이어의 현재 값을 직접 읽는다.
        assertEquals(StationBarState.Hidden, viewModel.state.value)
        assertTrue(viewModel.isStationActive())
    }

    @Test
    fun `Given 스테이션이 없음, Then isStationActive 는 거짓이다`() = runTest {
        val viewModel = createViewModel()

        assertFalse(viewModel.isStationActive())
    }
}
