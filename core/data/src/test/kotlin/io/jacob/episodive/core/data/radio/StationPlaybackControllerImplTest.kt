package io.jacob.episodive.core.data.radio

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.repository.StationRepository
import io.jacob.episodive.core.domain.usecase.player.RestoreLastPlayStateUseCase
import io.jacob.episodive.core.domain.usecase.station.PlayStationResult
import io.jacob.episodive.core.domain.usecase.station.PlayStationUseCase
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.testing.model.kbsFirstRadioStationTestData
import io.jacob.episodive.core.testing.model.radioStationTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StationPlaybackControllerImplTest {

    private val playStationUseCase = mockk<PlayStationUseCase>()
    private val playerRepository = mockk<PlayerRepository>(relaxed = true)
    private val stationRepository = mockk<StationRepository>()
    private val restoreLastPlayState = mockk<RestoreLastPlayStateUseCase>()

    private val nowPlayingStation = MutableStateFlow<Station?>(null)

    private val a = radioStationTestData
    private val b = kbsFirstRadioStationTestData

    private fun TestScope.createController(): StationPlaybackControllerImpl {
        every { playerRepository.nowPlayingStation } returns nowPlayingStation
        coEvery { restoreLastPlayState() } returns true
        coEvery { stationRepository.getStation(a.id) } returns a
        coEvery { stationRepository.getStation(b.id) } returns b
        return StationPlaybackControllerImpl(
            playStationUseCase = playStationUseCase,
            playerRepository = playerRepository,
            stationRepository = stationRepository,
            restoreLastPlayState = restoreLastPlayState,
            scope = backgroundScope,
            mainDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
    }

    @Test
    fun `Given A 를 받는 중, When B 를 누르면, Then A 는 취소되고 B 만 호출되며 pending 은 B 다`() = runTest {
        var aCancelled = false
        coEvery { playStationUseCase(a.id) } coAnswers {
            try {
                awaitCancellation()
            } finally {
                aCancelled = true
            }
        }
        coEvery { playStationUseCase(b.id) } coAnswers { awaitCancellation() }
        val controller = createController()

        controller.play(a.id)
        assertEquals(a.id, controller.pendingStationId.value)
        controller.play(b.id)

        assertTrue(aCancelled)
        assertEquals(b.id, controller.pendingStationId.value)
        coVerify(exactly = 1) { playStationUseCase(a.id) }
        coVerify(exactly = 1) { playStationUseCase(b.id) }
    }

    @Test
    fun `Given 취소된 A 의 정리가 늦음, When 끝나면, Then B 의 pending 을 지우지 않는다`() = runTest {
        val release = CompletableDeferred<Unit>()
        coEvery { playStationUseCase(a.id) } coAnswers {
            try {
                awaitCancellation()
            } finally {
                // 취소된 뒤에도 정리가 한동안 이어진다.
                withContext(NonCancellable) { release.await() }
            }
        }
        coEvery { playStationUseCase(b.id) } coAnswers { awaitCancellation() }
        val controller = createController()

        controller.play(a.id)
        controller.play(b.id)
        release.complete(Unit)

        assertEquals(b.id, controller.pendingStationId.value)
    }

    @Test
    fun `Given 재생 중인 스테이션, When 받는 중인 것이 없을 때 누르면, Then playOrPause 만 부른다`() = runTest {
        nowPlayingStation.value = a
        val controller = createController()

        controller.play(a.id)

        verify(exactly = 1) { playerRepository.playOrPause() }
        coVerify(exactly = 0) { playStationUseCase(any()) }
        assertNull(controller.pendingStationId.value)
    }

    @Test
    fun `Given 재생 중인 A 와 받는 중인 B, When A 를 다시 누르면, Then B 만 취소하고 playOrPause 는 부르지 않는다`() = runTest {
        nowPlayingStation.value = a
        var bCancelled = false
        coEvery { playStationUseCase(b.id) } coAnswers {
            try {
                awaitCancellation()
            } finally {
                bCancelled = true
            }
        }
        val controller = createController()

        controller.play(b.id)
        controller.play(a.id)

        assertTrue(bCancelled)
        assertNull(controller.pendingStationId.value)
        verify(exactly = 0) { playerRepository.playOrPause() }
        verify(exactly = 0) { playerRepository.stop() }
    }

    @Test
    fun `Given 받는 중인 스테이션, When 같은 것을 또 누르면, Then 무시한다`() = runTest {
        coEvery { playStationUseCase(a.id) } coAnswers { awaitCancellation() }
        val controller = createController()

        controller.play(a.id)
        controller.play(a.id)

        coVerify(exactly = 1) { playStationUseCase(a.id) }
    }

    @Test
    fun `Given 받는 중, When stop 하면, Then 요청을 취소하고 pending 을 비우고 stop 을 부른다`() = runTest {
        var aCancelled = false
        coEvery { playStationUseCase(a.id) } coAnswers {
            try {
                awaitCancellation()
            } finally {
                aCancelled = true
            }
        }
        val controller = createController()

        controller.play(a.id)
        controller.stop()

        assertTrue(aCancelled)
        assertNull(controller.pendingStationId.value)
        verify(exactly = 1) { playerRepository.stop() }
        verify(exactly = 0) { playerRepository.stopStation(any()) }
    }

    @Test
    fun `Given 스테이션을 듣는 중, When stop 하면, Then 비운 뒤에 마지막 에피소드 복원을 요청한다`() = runTest {
        val controller = createController()

        controller.stop()

        coVerifyOrder {
            playerRepository.stop()
            restoreLastPlayState()
        }
    }

    @Test
    fun `Given 스테이션 없이 재생 중, When play 로 토글만 하면, Then 복원하지 않는다`() = runTest {
        nowPlayingStation.value = a
        val controller = createController()

        controller.play(a.id)

        coVerify(exactly = 0) { restoreLastPlayState() }
    }

    @Test
    fun `Given stop 으로 비운 뒤, When 같은 스테이션을 다시 누르면, Then 다시 받는다`() = runTest {
        coEvery { playStationUseCase(a.id) } coAnswers { awaitCancellation() }
        val controller = createController()

        controller.play(a.id)
        controller.stop()
        controller.play(a.id)

        coVerify(exactly = 2) { playStationUseCase(a.id) }
        assertEquals(a.id, controller.pendingStationId.value)
    }

    @Test
    fun `Given Unavailable, Then unavailable 을 방출하고 pending 을 비운다`() = runTest {
        coEvery { playStationUseCase(a.id) } returns PlayStationResult.Unavailable
        val controller = createController()

        controller.unavailable.test {
            controller.play(a.id)

            assertEquals(a, awaitItem())
        }
        assertNull(controller.pendingStationId.value)
    }

    @Test
    fun `Given Started 와 Superseded, Then 방출하지 않는다`() = runTest {
        coEvery { playStationUseCase(a.id) } returns PlayStationResult.Started
        coEvery { playStationUseCase(b.id) } returns PlayStationResult.Superseded
        val controller = createController()

        controller.unavailable.test {
            controller.play(a.id)
            controller.play(b.id)

            expectNoEvents()
        }
        assertNull(controller.pendingStationId.value)
    }
}
