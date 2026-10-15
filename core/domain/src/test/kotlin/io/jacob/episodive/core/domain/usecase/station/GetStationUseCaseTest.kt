package io.jacob.episodive.core.domain.usecase.station

import io.jacob.episodive.core.domain.repository.StationRepository
import io.jacob.episodive.core.testing.model.radioStationTestData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetStationUseCaseTest {
    private val stationRepository = mockk<StationRepository>()
    private val useCase = GetStationUseCase(stationRepository)

    @Test
    fun `Given known id, When invoke, Then the repository station`() = runTest {
        coEvery { stationRepository.getStation(radioStationTestData.id) } returns radioStationTestData

        assertEquals(radioStationTestData, useCase(radioStationTestData.id))
    }

    @Test
    fun `Given unknown id, When invoke, Then null`() = runTest {
        coEvery { stationRepository.getStation("rb:none") } returns null

        assertNull(useCase("rb:none"))
    }
}
