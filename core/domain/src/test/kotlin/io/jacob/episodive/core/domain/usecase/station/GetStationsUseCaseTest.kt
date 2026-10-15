package io.jacob.episodive.core.domain.usecase.station

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.StationRepository
import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.model.StationKind
import io.jacob.episodive.core.model.StationListing
import io.jacob.episodive.core.model.StationListingSource
import io.jacob.episodive.core.model.UserData
import io.jacob.episodive.core.testing.model.radioStationTestData
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GetStationsUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val stationRepository = mockk<StationRepository>()
    private val userRepository = mockk<UserRepository>()

    private val useCase = GetStationsUseCase(
        stationRepository = stationRepository,
        userRepository = userRepository,
    )

    private fun listingOf(country: String) = StationListing(
        countryCode = country,
        source = if (country == "KR") StationListingSource.Curated else StationListingSource.RadioBrowser,
        stations = listOf(radioStationTestData),
    )

    @Test
    fun `Given user country, When invoke, Then listing of that country`() = runTest {
        every { userRepository.getUserData() } returns flowOf(UserData(country = "US"))
        every { stationRepository.getStationListing(StationKind.RADIO, "US") } returns flowOf(listingOf("US"))

        useCase(StationKind.RADIO).test {
            assertEquals(listingOf("US"), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `Given no country, When invoke, Then null is passed so the repository picks the curated catalog`() = runTest {
        every { userRepository.getUserData() } returns flowOf(UserData(country = null))
        every { stationRepository.getStationListing(StationKind.RADIO, null) } returns flowOf(listingOf("KR"))

        useCase(StationKind.RADIO).test {
            assertEquals(listingOf("KR"), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `Given unrelated user data changes, When invoke, Then listing is not resubscribed`() = runTest {
        val userData = MutableStateFlow(UserData(country = "US", speed = 1f))
        every { userRepository.getUserData() } returns userData
        every { stationRepository.getStationListing(StationKind.RADIO, any()) } answers {
            flowOf(listingOf(secondArg()))
        }

        useCase(StationKind.RADIO).test {
            assertEquals(listingOf("US"), awaitItem())

            userData.value = userData.value.copy(speed = 1.5f)
            expectNoEvents()
            verify(exactly = 1) { stationRepository.getStationListing(StationKind.RADIO, "US") }

            userData.value = userData.value.copy(country = "JP")
            assertEquals(listingOf("JP"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
