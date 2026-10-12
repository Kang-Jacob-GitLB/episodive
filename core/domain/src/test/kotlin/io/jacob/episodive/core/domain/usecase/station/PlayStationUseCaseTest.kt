package io.jacob.episodive.core.domain.usecase.station

import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.repository.StationRepository
import io.jacob.episodive.core.domain.station.LiveStreamSupervisor
import io.jacob.episodive.core.testing.model.icecastStreamTestData
import io.jacob.episodive.core.testing.model.radioStationTestData
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PlayStationUseCaseTest {
    private val playerRepository = mockk<PlayerRepository>()
    private val stationRepository = mockk<StationRepository>()
    private val supervisor = mockk<LiveStreamSupervisor>()

    private val useCase = PlayStationUseCase(
        playerRepository = playerRepository,
        stationRepository = stationRepository,
        supervisor = supervisor,
    )

    private val station = radioStationTestData
    private val stream = icecastStreamTestData

    @Before
    fun setup() {
        every { supervisor.start() } just Runs
        every { playerRepository.playRequestCount() } returns 7L
        every { playerRepository.playStation(any(), any()) } just Runs
        every { stationRepository.getStation(station.id) } returns station
        coEvery { stationRepository.resolveStreamUrl(station.id) } returns stream
        coEvery { stationRepository.markPlayed(any()) } just Runs
    }

    @Test
    fun `Given resolvable station, When invoke, Then starts supervisor, plays and marks played in order`() = runTest {
        val result = useCase(station.id)

        assertEquals(PlayStationResult.Started, result)
        coVerifyOrder {
            supervisor.start()
            stationRepository.resolveStreamUrl(station.id)
            playerRepository.playStation(station, stream)
            stationRepository.markPlayed(station.id)
        }
    }

    @Test
    fun `Given stream not resolved, When invoke, Then Unavailable and not played`() = runTest {
        coEvery { stationRepository.resolveStreamUrl(station.id) } returns null

        val result = useCase(station.id)

        assertEquals(PlayStationResult.Unavailable, result)
        verify(exactly = 0) { playerRepository.playStation(any(), any()) }
    }

    @Test
    fun `Given unknown station, When invoke, Then Unavailable without touching player`() = runTest {
        every { stationRepository.getStation("unknown") } returns null

        val result = useCase("unknown")

        assertEquals(PlayStationResult.Unavailable, result)
        verify(exactly = 0) { supervisor.start() }
        verify(exactly = 0) { playerRepository.playStation(any(), any()) }
    }

    // 주소를 받는 사이 사용자가 다른 것을 골랐다면, 늦게 도착한 이 요청이 그것을 덮어쓰면 안 된다.
    @Test
    fun `Given user played something while resolving, When invoke, Then Superseded and not played`() = runTest {
        every { playerRepository.playRequestCount() } returnsMany listOf(7L, 8L)

        val result = useCase(station.id)

        assertEquals(PlayStationResult.Superseded, result)
        verify(exactly = 0) { playerRepository.playStation(any(), any()) }
        coVerify(exactly = 0) { stationRepository.markPlayed(any()) }
    }

    // 해석이 실패했더라도 그 사이 사용자가 다른 것을 골랐다면 Unavailable 을 보여 주면 안 된다.
    @Test
    fun `Given user played something while resolving and stream not resolved, When invoke, Then Superseded`() = runTest {
        coEvery { stationRepository.resolveStreamUrl(station.id) } returns null
        every { playerRepository.playRequestCount() } returnsMany listOf(7L, 8L)

        val result = useCase(station.id)

        assertEquals(PlayStationResult.Superseded, result)
    }

    @Test
    fun `Given markPlayed throws, When invoke, Then still Started`() = runTest {
        coEvery { stationRepository.markPlayed(any()) } throws IllegalStateException("db")

        val result = useCase(station.id)

        assertEquals(PlayStationResult.Started, result)
        verify(exactly = 1) { playerRepository.playStation(station, stream) }
    }
}
