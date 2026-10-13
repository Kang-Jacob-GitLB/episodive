package io.jacob.episodive.core.data.repository

import app.cash.turbine.test
import io.jacob.episodive.core.data.radio.stationCatalogEntryTestData
import io.jacob.episodive.core.data.radio.stationDetailsResponsesTestData
import io.jacob.episodive.core.database.dao.StationDao
import io.jacob.episodive.core.database.model.RecentStationEntity
import io.jacob.episodive.core.domain.repository.StationRepository
import io.jacob.episodive.core.model.StationKind
import io.jacob.episodive.core.model.StationStream
import io.jacob.episodive.core.network.datasource.StationRemoteDataSource
import io.jacob.episodive.core.testing.model.icecastStreamTestData
import io.jacob.episodive.core.testing.model.radioStationTestData
import io.jacob.episodive.core.testing.model.regionalRadioStationTestData
import io.jacob.episodive.core.testing.model.stationDetailsTestData
import io.jacob.episodive.core.testing.model.tvStationTestData
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class StationRepositoryImplTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val remoteDataSource = mockk<StationRemoteDataSource>()
    private val stationDao = mockk<StationDao>()

    private var nowMillis = 1_000_000L

    private val repository = StationRepositoryImpl(
        remoteDataSource = remoteDataSource,
        stationDao = stationDao,
        catalog = stationCatalogEntryTestData,
        ioDispatcher = UnconfinedTestDispatcher(mainDispatcherRule.testDispatcher.scheduler),
        now = { nowMillis },
    )

    @Test
    fun `Given catalog, When getStations RADIO, Then only radio in catalog order`() = runTest {
        repository.getStations(StationKind.RADIO).test {
            assertEquals(listOf(radioStationTestData, regionalRadioStationTestData), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `Given first candidate succeeds, When resolveStreamUrl, Then second is not tried`() = runTest {
        coEvery { remoteDataSource.resolveStreamUrl("a") } returns "https://live/a"

        assertEquals(
            StationStream("https://live/a", isHls = false),
            repository.resolveStreamUrl(radioStationTestData.id),
        )
        coVerify(exactly = 0) { remoteDataSource.resolveStreamUrl("b") }
    }

    @Test
    fun `Given first candidate throws, When resolveStreamUrl, Then second candidate used`() = runTest {
        coEvery { remoteDataSource.resolveStreamUrl("a") } throws IOException("down")
        coEvery { remoteDataSource.resolveStreamUrl("b") } returns "https://live/b"

        assertEquals(
            StationStream("https://live/b", isHls = false),
            repository.resolveStreamUrl(radioStationTestData.id),
        )
    }

    @Test
    fun `Given first returns null, When resolveStreamUrl, Then second candidate used`() = runTest {
        coEvery { remoteDataSource.resolveStreamUrl("a") } returns null
        coEvery { remoteDataSource.resolveStreamUrl("b") } returns "https://live/b"

        assertEquals(
            StationStream("https://live/b", isHls = false),
            repository.resolveStreamUrl(radioStationTestData.id),
        )
    }

    @Test
    fun `Given all candidates fail, When resolveStreamUrl, Then fallbackUrl`() = runTest {
        coEvery { remoteDataSource.resolveStreamUrl(any()) } throws IOException("down")

        assertEquals(
            StationStream("https://fallback/a", isHls = false),
            repository.resolveStreamUrl(radioStationTestData.id),
        )
    }

    @Test
    fun `Given all fail and no fallback, When resolveStreamUrl, Then null`() = runTest {
        coEvery { remoteDataSource.resolveStreamUrl(any()) } returns null

        assertNull(repository.resolveStreamUrl(regionalRadioStationTestData.id))
    }

    @Test
    fun `Given hls entry, When resolveStreamUrl, Then stream carries isHls`() = runTest {
        coEvery { remoteDataSource.resolveStreamUrl("r") } returns "https://live/r"

        assertEquals(
            StationStream("https://live/r", isHls = true),
            repository.resolveStreamUrl(regionalRadioStationTestData.id),
        )
    }

    @Test
    fun `Given first candidate hangs, When resolveStreamUrl, Then fallback after timeout`() = runTest {
        coEvery { remoteDataSource.resolveStreamUrl("a") } coAnswers { awaitCancellation() }
        coEvery { remoteDataSource.resolveStreamUrl("b") } returns "https://live/b"

        val stream = async { repository.resolveStreamUrl(radioStationTestData.id) }
        advanceTimeBy(14_000)
        runCurrent()
        assertEquals(false, stream.isCompleted)
        advanceTimeBy(2_000)

        assertEquals(StationStream("https://fallback/a", isHls = false), stream.await())
    }

    @Test
    fun `Given first candidate excluded, When resolveStreamUrl, Then second candidate used`() = runTest {
        coEvery { remoteDataSource.resolveStreamUrl("a") } returns "https://live/a"
        coEvery { remoteDataSource.resolveStreamUrl("b") } returns "https://live/b"

        assertEquals(
            icecastStreamTestData.copy(url = "https://live/b"),
            repository.resolveStreamUrl(radioStationTestData.id, excluding = setOf("https://live/a")),
        )
    }

    @Test
    fun `Given every candidate and fallback excluded, When resolveStreamUrl, Then null`() = runTest {
        coEvery { remoteDataSource.resolveStreamUrl("a") } returns "https://live/a"
        coEvery { remoteDataSource.resolveStreamUrl("b") } returns "https://live/b"

        assertNull(
            repository.resolveStreamUrl(
                stationId = radioStationTestData.id,
                excluding = setOf("https://live/a", "https://live/b", "https://fallback/a"),
            )
        )
    }

    @Test
    fun `Given known id, When getStation, Then catalog station`() {
        assertEquals(radioStationTestData, repository.getStation(radioStationTestData.id))
    }

    @Test
    fun `Given unknown id, When getStation, Then null`() {
        assertNull(repository.getStation("unknown"))
    }

    @Test
    fun `Given unknown id, When resolveStreamUrl, Then null`() = runTest {
        assertNull(repository.resolveStreamUrl("unknown"))
    }

    @Test
    fun `Given favorite ids, When getFavoriteStations, Then joined with catalog in dao order`() = runTest {
        every { stationDao.getFavoriteStationIds() } returns
            flowOf(listOf(regionalRadioStationTestData.id, radioStationTestData.id))

        repository.getFavoriteStations().test {
            assertEquals(listOf(regionalRadioStationTestData, radioStationTestData), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `Given id missing from catalog, When getFavoriteStations, Then silently excluded`() = runTest {
        every { stationDao.getFavoriteStationIds() } returns
            flowOf(listOf("removed-station", radioStationTestData.id))

        repository.getFavoriteStations().test {
            assertEquals(listOf(radioStationTestData), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `Given missing ids mixed in, When getRecentStations with limit, Then limit filled after join`() = runTest {
        every { stationDao.getRecentStationIds() } returns flowOf(
            listOf("removed-1", "removed-2", tvStationTestData.id, radioStationTestData.id, regionalRadioStationTestData.id)
        )

        repository.getRecentStations(limit = 2).test {
            assertEquals(listOf(tvStationTestData, radioStationTestData), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `Given dao state, When isFavorite, Then dao flow passed through`() = runTest {
        every { stationDao.isFavorite(radioStationTestData.id) } returns flowOf(true)

        repository.isFavorite(radioStationTestData.id).test {
            assertEquals(true, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `Given dao toggles, When toggleFavorite, Then new state is returned`() = runTest {
        coEvery { stationDao.toggleFavorite(radioStationTestData.id) } returns true andThen false

        assertEquals(true, repository.toggleFavorite(radioStationTestData.id))
        assertEquals(false, repository.toggleFavorite(radioStationTestData.id))
    }

    @Test
    fun `When markPlayed, Then dao records atomically with the max recent limit`() = runTest {
        val recent = slot<RecentStationEntity>()
        coEvery { stationDao.recordPlayed(capture(recent), any()) } just runs

        repository.markPlayed(radioStationTestData.id)

        assertEquals(radioStationTestData.id, recent.captured.stationId)
        coVerify(exactly = 1) {
            stationDao.recordPlayed(any(), StationRepository.MAX_RECENT_STATIONS)
        }
        coVerify(exactly = 0) { stationDao.upsertRecent(any()) }
        coVerify(exactly = 0) { stationDao.trimRecent(any()) }
    }

    @Test
    fun `Given known id, When getStationDetails, Then all candidates requested at once and merged`() = runTest {
        coEvery { remoteDataSource.getStationDetails(listOf("a", "b")) } returns stationDetailsResponsesTestData

        assertEquals(stationDetailsTestData, repository.getStationDetails(radioStationTestData.id))
        coVerify(exactly = 1) { remoteDataSource.getStationDetails(listOf("a", "b")) }
    }

    @Test
    fun `Given within TTL, When getStationDetails twice, Then remote called once`() = runTest {
        coEvery { remoteDataSource.getStationDetails(any()) } returns stationDetailsResponsesTestData

        repository.getStationDetails(radioStationTestData.id)
        nowMillis += 24 * 60 * 60 * 1000L - 1
        repository.getStationDetails(radioStationTestData.id)

        coVerify(exactly = 1) { remoteDataSource.getStationDetails(any()) }
    }

    @Test
    fun `Given TTL elapsed, When getStationDetails, Then remote called again`() = runTest {
        coEvery { remoteDataSource.getStationDetails(any()) } returns stationDetailsResponsesTestData

        repository.getStationDetails(radioStationTestData.id)
        nowMillis += 24 * 60 * 60 * 1000L
        repository.getStationDetails(radioStationTestData.id)

        coVerify(exactly = 2) { remoteDataSource.getStationDetails(any()) }
    }

    @Test
    fun `Given remote fails, When getStationDetails, Then null and not cached`() = runTest {
        coEvery { remoteDataSource.getStationDetails(any()) } throws IOException("down") andThen
            stationDetailsResponsesTestData

        assertNull(repository.getStationDetails(radioStationTestData.id))
        assertEquals(stationDetailsTestData, repository.getStationDetails(radioStationTestData.id))
    }

    @Test
    fun `Given remote hangs, When getStationDetails, Then null after timeout`() = runTest {
        coEvery { remoteDataSource.getStationDetails(any()) } coAnswers { awaitCancellation() }

        val details = async { repository.getStationDetails(radioStationTestData.id) }
        advanceTimeBy(9_000)
        runCurrent()
        assertEquals(false, details.isCompleted)
        advanceTimeBy(2_000)

        assertNull(details.await())
    }

    @Test
    fun `Given nothing usable, When getStationDetails twice, Then null is cached`() = runTest {
        coEvery { remoteDataSource.getStationDetails(any()) } returns emptyList()

        assertNull(repository.getStationDetails(radioStationTestData.id))
        assertNull(repository.getStationDetails(radioStationTestData.id))

        coVerify(exactly = 1) { remoteDataSource.getStationDetails(any()) }
    }

    @Test
    fun `Given unknown id, When getStationDetails, Then null without network`() = runTest {
        assertNull(repository.getStationDetails("unknown"))

        coVerify(exactly = 0) { remoteDataSource.getStationDetails(any()) }
    }
}
