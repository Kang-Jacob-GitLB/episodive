package io.jacob.episodive.core.domain.usecase.station

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.StationRepository
import io.jacob.episodive.core.model.OnAirProgram
import io.jacob.episodive.core.model.OnAirRefresh
import io.jacob.episodive.core.testing.model.bareOnAirProgramTestData
import io.jacob.episodive.core.testing.model.onAirProgramTestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveStationProgramImagesUseCaseTest {
    private val stationRepository = mockk<StationRepository>()
    private val useCase = ObserveStationProgramImagesUseCase(stationRepository)

    @Test
    fun `Given 빈 목록, Then 빈 맵이고 구독하지 않는다`() = runTest {
        useCase(emptyList()).test {
            assertEquals(emptyMap<String, String>(), awaitItem())
            awaitComplete()
        }
        verify(exactly = 0) { stationRepository.observeOnAir(any(), any(), any()) }
    }

    @Test
    fun `Given 스테이션마다 다른 프로그램, Then 이미지가 있는 스테이션만 id 로 매핑되고 곡 폴링은 끈다`() = runTest {
        every { stationRepository.observeOnAir("a", false, OnAirRefresh.Boundary) } returns flowOf(onAirProgramTestData)
        every { stationRepository.observeOnAir("b", false, OnAirRefresh.Boundary) } returns flowOf(bareOnAirProgramTestData)
        every { stationRepository.observeOnAir("c", false, OnAirRefresh.Boundary) } returns flowOf(null)

        useCase(listOf("a", "b", "c", "a")).test {
            assertEquals(mapOf("a" to onAirProgramTestData.imageUrl!!), expectMostRecentItem())
            cancelAndIgnoreRemainingEvents()
        }
        verify(exactly = 1) { stationRepository.observeOnAir("a", false, OnAirRefresh.Boundary) }
    }

    @Test
    fun `Given 프로그램이 바뀜, Then 맵이 갱신되고 이미지가 사라지면 빠진다`() = runTest {
        val program = MutableStateFlow<OnAirProgram?>(null)
        every { stationRepository.observeOnAir("a", false, OnAirRefresh.Boundary) } returns program

        useCase(listOf("a")).test {
            assertEquals(emptyMap<String, String>(), awaitItem())

            program.value = onAirProgramTestData
            assertEquals(mapOf("a" to onAirProgramTestData.imageUrl!!), awaitItem())

            program.value = bareOnAirProgramTestData
            assertEquals(emptyMap<String, String>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
