package io.jacob.episodive.core.data.repository

import app.cash.turbine.test
import io.jacob.episodive.core.data.radio.stationCatalogEntryTestData
import io.jacob.episodive.core.model.StationKind
import io.jacob.episodive.core.model.StationStream
import io.jacob.episodive.core.network.datasource.StationRemoteDataSource
import io.jacob.episodive.core.testing.model.radioStationTestData
import io.jacob.episodive.core.testing.model.regionalRadioStationTestData
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
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

    private val repository = StationRepositoryImpl(
        remoteDataSource = remoteDataSource,
        catalog = stationCatalogEntryTestData,
        ioDispatcher = UnconfinedTestDispatcher(mainDispatcherRule.testDispatcher.scheduler),
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
    fun `Given unknown id, When resolveStreamUrl, Then null`() = runTest {
        assertNull(repository.resolveStreamUrl("unknown"))
    }
}
