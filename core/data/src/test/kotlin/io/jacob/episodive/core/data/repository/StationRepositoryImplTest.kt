package io.jacob.episodive.core.data.repository

import app.cash.turbine.test
import io.jacob.episodive.core.data.radio.StationCatalogEntry
import io.jacob.episodive.core.data.radio.broadcasterRadioEntryTestData
import io.jacob.episodive.core.data.radio.hlsRadioEntryTestData
import io.jacob.episodive.core.data.radio.onAirKbsEntryTestData
import io.jacob.episodive.core.data.radio.onAirMbcEntryTestData
import io.jacob.episodive.core.data.radio.stationCatalogEntryTestData
import io.jacob.episodive.core.data.radio.stationDetailsResponsesTestData
import io.jacob.episodive.core.database.dao.StationDao
import io.jacob.episodive.core.database.model.RecentStationEntity
import io.jacob.episodive.core.domain.repository.StationRepository
import io.jacob.episodive.core.model.StationKind
import io.jacob.episodive.core.model.StationStream
import io.jacob.episodive.core.network.datasource.BroadcasterStreamDataSource
import io.jacob.episodive.core.network.datasource.OnAirDataSource
import io.jacob.episodive.core.network.datasource.StationRemoteDataSource
import io.jacob.episodive.core.network.model.BroadcasterSource
import io.jacob.episodive.core.testing.model.icecastStreamTestData
import io.jacob.episodive.core.testing.model.kbsFirstRadioStationTestData
import io.jacob.episodive.core.testing.model.kbsHappyRadioStationTestData
import io.jacob.episodive.core.testing.model.onAirProgramTestData
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
import kotlinx.coroutines.delay
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
import java.time.LocalDateTime
import java.time.ZoneId

class StationRepositoryImplTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val remoteDataSource = mockk<StationRemoteDataSource>()
    private val broadcasterDataSource = mockk<BroadcasterStreamDataSource>()
    private val onAirDataSource = mockk<OnAirDataSource>()
    private val stationDao = mockk<StationDao>()

    private var nowMillis = 1_000_000L

    private fun repositoryOf(catalog: List<StationCatalogEntry>) = StationRepositoryImpl(
        remoteDataSource = remoteDataSource,
        broadcasterDataSource = broadcasterDataSource,
        onAirDataSource = onAirDataSource,
        stationDao = stationDao,
        catalog = catalog,
        ioDispatcher = UnconfinedTestDispatcher(mainDispatcherRule.testDispatcher.scheduler),
        now = { nowMillis },
    )

    private val repository = repositoryOf(stationCatalogEntryTestData)

    private val broadcasterRepository = repositoryOf(listOf(broadcasterRadioEntryTestData))

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
    fun `Given broadcaster succeeds, When resolveStreamUrl, Then its url and Radio Browser not called`() = runTest {
        coEvery { broadcasterDataSource.resolve(BroadcasterSource.Kbs("21")) } returns "https://kbs/new"

        assertEquals(
            StationStream("https://kbs/new", isHls = true),
            broadcasterRepository.resolveStreamUrl(kbsFirstRadioStationTestData.id),
        )
        coVerify(exactly = 0) { remoteDataSource.resolveStreamUrl(any()) }
    }

    @Test
    fun `Given broadcaster returns null, When resolveStreamUrl, Then Radio Browser used`() = runTest {
        coEvery { broadcasterDataSource.resolve(any()) } returns null
        coEvery { remoteDataSource.resolveStreamUrl("k") } returns "https://live/k"

        assertEquals(
            StationStream("https://live/k", isHls = true),
            broadcasterRepository.resolveStreamUrl(kbsFirstRadioStationTestData.id),
        )
    }

    @Test
    fun `Given broadcaster throws, When resolveStreamUrl, Then Radio Browser used`() = runTest {
        coEvery { broadcasterDataSource.resolve(any()) } throws IOException("403")
        coEvery { remoteDataSource.resolveStreamUrl("k") } returns "https://live/k"

        assertEquals(
            StationStream("https://live/k", isHls = true),
            broadcasterRepository.resolveStreamUrl(kbsFirstRadioStationTestData.id),
        )
    }

    @Test
    fun `Given broadcaster url excluded, When resolveStreamUrl, Then Radio Browser used`() = runTest {
        coEvery { broadcasterDataSource.resolve(any()) } returns "https://kbs/old"
        coEvery { remoteDataSource.resolveStreamUrl("k") } returns "https://live/k"

        assertEquals(
            StationStream("https://live/k", isHls = true),
            broadcasterRepository.resolveStreamUrl(
                kbsFirstRadioStationTestData.id,
                excluding = setOf("https://kbs/old"),
            ),
        )
    }

    @Test
    fun `Given broadcaster hangs, When resolveStreamUrl, Then Radio Browser used after broadcaster timeout`() = runTest {
        coEvery { broadcasterDataSource.resolve(any()) } coAnswers { awaitCancellation() }
        coEvery { remoteDataSource.resolveStreamUrl("k") } returns "https://live/k"

        val stream = async { broadcasterRepository.resolveStreamUrl(kbsFirstRadioStationTestData.id) }
        advanceTimeBy(4_000)
        runCurrent()
        assertEquals(false, stream.isCompleted)
        advanceTimeBy(2_000)

        assertEquals(StationStream("https://live/k", isHls = true), stream.await())
    }

    @Test
    fun `Given broadcaster fails after delay, When resolveStreamUrl, Then Radio Browser still called`() = runTest {
        coEvery { broadcasterDataSource.resolve(any()) } coAnswers {
            delay(4_000)
            throw IOException("slow")
        }
        coEvery { remoteDataSource.resolveStreamUrl("k") } returns "https://live/k"

        val stream = async { broadcasterRepository.resolveStreamUrl(kbsFirstRadioStationTestData.id) }
        advanceTimeBy(5_000)

        assertEquals(StationStream("https://live/k", isHls = true), stream.await())
    }

    @Test
    fun `Given excluding not empty, When resolveStreamUrl, Then broadcaster skipped`() = runTest {
        coEvery { remoteDataSource.resolveStreamUrl("k") } returns "https://live/k"

        assertEquals(
            StationStream("https://live/k", isHls = true),
            broadcasterRepository.resolveStreamUrl(
                kbsFirstRadioStationTestData.id,
                excluding = setOf("https://kbs/failed"),
            ),
        )
        coVerify(exactly = 0) { broadcasterDataSource.resolve(any()) }
    }

    @Test
    fun `Given entry without broadcaster source, When resolveStreamUrl, Then broadcaster not called`() = runTest {
        coEvery { remoteDataSource.resolveStreamUrl("a") } returns "https://live/a"

        repository.resolveStreamUrl(radioStationTestData.id)

        coVerify(exactly = 0) { broadcasterDataSource.resolve(any()) }
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

    // --- observeOnAir ---

    private val seoul = ZoneId.of("Asia/Seoul")
    private val tenAm = LocalDateTime.of(2026, 10, 14, 10, 0).atZone(seoul).toInstant().toEpochMilli()
    private val minute = 60_000L

    private val morning = onAirProgramTestData("아침", tenAm - 60 * minute, tenAm + 30 * minute, host = "가")
    private val noon = onAirProgramTestData("점심", tenAm + 30 * minute, tenAm + 90 * minute, host = "나")

    private val onAirRepository = repositoryOf(listOf(onAirKbsEntryTestData, onAirMbcEntryTestData, hlsRadioEntryTestData))

    @Test
    fun `Given schedule, When observeOnAir, Then current program emitted`() = runTest {
        nowMillis = tenAm
        coEvery { onAirDataSource.schedule(any(), any()) } returns listOf(noon, morning)

        onAirRepository.observeOnAir(kbsFirstRadioStationTestData.id).test {
            assertEquals(morning, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given boundary passes, When observeOnAir, Then next program without refetch`() = runTest {
        nowMillis = tenAm
        coEvery { onAirDataSource.schedule(any(), any()) } returns listOf(morning, noon)

        onAirRepository.observeOnAir(kbsFirstRadioStationTestData.id).test {
            assertEquals(morning, awaitItem())
            // 가상 시간이 흐르는 만큼 시계도 흐르게 한다. 경계(30분)까지 5분 상한으로 쪼개 깬다.
            repeat(6) {
                nowMillis += 5 * minute
                testScheduler.advanceTimeBy(5 * minute)
                testScheduler.runCurrent()
            }
            assertEquals(noon, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        // 5분 TTL 마다 안전 재조회는 하되, 경계 전환 때문에 더 부르지는 않는다.
        coVerify(atMost = 7) { onAirDataSource.schedule(any(), any()) }
    }

    @Test
    fun `Given within TTL, When observeOnAir twice, Then schedule requested once`() = runTest {
        nowMillis = tenAm
        coEvery { onAirDataSource.schedule(any(), any()) } returns listOf(morning)

        onAirRepository.observeOnAir(kbsFirstRadioStationTestData.id).test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        onAirRepository.observeOnAir(kbsFirstRadioStationTestData.id).test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { onAirDataSource.schedule(any(), any()) }
    }

    @Test
    fun `Given date changed, When observeOnAir, Then schedule requested again`() = runTest {
        coEvery { onAirDataSource.schedule(any(), any()) } returns listOf(morning)
        nowMillis = tenAm
        onAirRepository.observeOnAir(kbsFirstRadioStationTestData.id).test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        nowMillis = tenAm + 24 * 60 * minute
        onAirRepository.observeOnAir(kbsFirstRadioStationTestData.id).test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 2) { onAirDataSource.schedule(any(), any()) }
    }

    @Test
    fun `Given fetch fails, When observeOnAir, Then null and not cached`() = runTest {
        nowMillis = tenAm
        coEvery { onAirDataSource.schedule(any(), any()) } throws IOException("down") andThen listOf(morning)

        onAirRepository.observeOnAir(kbsFirstRadioStationTestData.id).test {
            assertNull(awaitItem())
            nowMillis += minute
            testScheduler.advanceTimeBy(minute)
            testScheduler.runCurrent()
            assertEquals(morning, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given no program covers now, When observeOnAir, Then null`() = runTest {
        nowMillis = tenAm
        coEvery { onAirDataSource.schedule(any(), any()) } returns listOf(
            onAirProgramTestData("먼 미래", tenAm + 300 * minute, tenAm + 360 * minute),
        )

        onAirRepository.observeOnAir(kbsFirstRadioStationTestData.id).test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given station without onAirSource or unknown id, When observeOnAir, Then null and network untouched`() = runTest {
        onAirRepository.observeOnAir(regionalRadioStationTestData.id).test {
            assertNull(awaitItem())
            awaitComplete()
        }
        onAirRepository.observeOnAir("unknown").test {
            assertNull(awaitItem())
            awaitComplete()
        }

        coVerify(exactly = 0) { onAirDataSource.schedule(any(), any()) }
    }

    @Test
    fun `Given MBC, When observeOnAir, Then song polled every 45s and merged`() = runTest {
        nowMillis = tenAm
        coEvery { onAirDataSource.schedule(any(), any()) } returns listOf(morning)
        coEvery { onAirDataSource.currentSong(any()) } returnsMany listOf("곡1 - 가수", "곡1 - 가수", "곡2 - 가수")

        onAirRepository.observeOnAir(kbsHappyRadioStationTestData.id).test {
            assertEquals(morning.copy(song = "곡1 - 가수"), awaitItem())
            nowMillis += 45_000L
            testScheduler.advanceTimeBy(45_000L)
            testScheduler.runCurrent()
            // 같은 곡이면 다시 발행하지 않는다.
            expectNoEvents()
            nowMillis += 45_000L
            testScheduler.advanceTimeBy(45_000L)
            testScheduler.runCurrent()
            assertEquals(morning.copy(song = "곡2 - 가수"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given song fetch fails, When observeOnAir, Then program without song`() = runTest {
        nowMillis = tenAm
        coEvery { onAirDataSource.schedule(any(), any()) } returns listOf(morning)
        coEvery { onAirDataSource.currentSong(any()) } throws IOException("down")

        onAirRepository.observeOnAir(kbsHappyRadioStationTestData.id).test {
            assertEquals(morning, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
