package io.jacob.episodive.core.data.widget

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.domain.usecase.player.GetNowPlayingUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetPodcastUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetUserRecentPodcastsUseCase
import io.jacob.episodive.core.domain.usecase.station.ObserveOnAirProgramUseCase
import io.jacob.episodive.core.model.OnAirProgram
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.art
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.onAirProgramTestData
import io.jacob.episodive.core.testing.model.podcastTestDataList
import io.jacob.episodive.core.testing.model.radioStationTestData
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class WidgetDataReaderImplTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getNowPlaying = mockk<GetNowPlayingUseCase>(relaxed = true)
    private val getPodcast = mockk<GetPodcastUseCase>(relaxed = true)
    private val getUserRecentPodcasts = mockk<GetUserRecentPodcastsUseCase>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val episodeRepository = mockk<EpisodeRepository>(relaxed = true)
    private val playerRepository = mockk<PlayerRepository>(relaxed = true)
    private val observeOnAirProgram = mockk<ObserveOnAirProgramUseCase>()

    init {
        // 기본은 스테이션 없음. relaxed mock 의 빈 Flow 는 바로 완료돼 에피소드 경로까지 닫아 버린다.
        every { playerRepository.nowPlayingStation } returns MutableStateFlow(null)
        every { observeOnAirProgram(any(), any()) } returns flowOf(null)
    }

    private val reader = WidgetDataReaderImpl(
        getNowPlaying = getNowPlaying,
        getPodcast = getPodcast,
        getUserRecentPodcasts = getUserRecentPodcasts,
        userRepository = userRepository,
        episodeRepository = episodeRepository,
        playerRepository = playerRepository,
        observeOnAirProgram = observeOnAirProgram,
    )

    @Test
    fun `Given no now playing episode, when snapshotNowPlaying called, then returns null`() =
        runTest {
            every { getNowPlaying() } returns flowOf(null)
            // 활성 재생이 없을 때 폴백하는 "마지막 재생"도 없음 → 최종 null.
            coEvery { userRepository.getLastPlayState() } returns null
            every { playerRepository.isPlaying } returns flowOf(false)

            val snapshot = reader.snapshotNowPlaying()

            assertNull(snapshot)
        }

    @Test
    fun `Given an episode and playing state, when snapshotNowPlaying called, then returns mapped snapshot`() =
        runTest {
            every { getNowPlaying() } returns flowOf(episodeTestData)
            every { getPodcast(episodeTestData.feedId) } returns flowOf(null)
            every { playerRepository.isPlaying } returns flowOf(true)

            val snapshot = reader.snapshotNowPlaying()!!

            assertEquals(episodeTestData.id, snapshot.episodeId)
            assertEquals(episodeTestData.feedId, snapshot.podcastId)
            assertEquals(episodeTestData.title, snapshot.title)
            assertEquals(episodeTestData.feedTitle, snapshot.feedTitle)
            assertEquals(episodeTestData.image, snapshot.imageUrl)
            assertEquals(true, snapshot.isPlaying)
        }

    @Test
    fun `Given episode with blank feedTitle, when snapshotNowPlaying called, then falls back to podcast title`() =
        runTest {
            val episode = episodeTestData.copy(feedTitle = null)
            val podcast = podcastTestDataList.first()
            every { getNowPlaying() } returns flowOf(episode)
            every { getPodcast(episode.feedId) } returns flowOf(podcast)
            every { playerRepository.isPlaying } returns flowOf(true)

            val snapshot = reader.snapshotNowPlaying()!!

            assertEquals(podcast.title, snapshot.feedTitle)
        }

    @Test
    fun `Given blank feedTitle and late podcast, when nowPlayingFlow collected, then re-emits with podcast title`() =
        runTest {
            val episode = episodeTestData.copy(feedTitle = null)
            val podcast = podcastTestDataList.first()
            // 팟캐스트 비동기 로드를 흉내: 처음엔 null(캐시 미스), 이후 팟캐스트 도착.
            val podcastFlow = MutableStateFlow<Podcast?>(null)
            every { getNowPlaying() } returns flowOf(episode)
            every { getPodcast(episode.feedId) } returns podcastFlow
            every { playerRepository.isPlaying } returns flowOf(true)

            reader.nowPlayingFlow().test {
                assertNull(awaitItem()?.feedTitle)
                podcastFlow.value = podcast
                // distinctUntilChanged 가 feedTitle 변경을 흡수하지 않아야 재방출된다.
                assertEquals(podcast.title, awaitItem()?.feedTitle)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given a station, when nowPlayingFlow collected, then emits station snapshot without episode ids`() =
        runTest {
            every { playerRepository.nowPlayingStation } returns flowOf(radioStationTestData)
            every { playerRepository.isPlaying } returns flowOf(true)

            reader.nowPlayingFlow().test {
                val snapshot = awaitItem()!!
                assertEquals(radioStationTestData.id, snapshot.stationId)
                assertEquals(radioStationTestData.art, snapshot.stationArt)
                assertEquals(radioStationTestData.name, snapshot.title)
                assertEquals(radioStationTestData.broadcaster, snapshot.feedTitle)
                assertNull(snapshot.imageUrl)
                assertNull(snapshot.episodeId)
                assertNull(snapshot.podcastId)
                assertEquals(true, snapshot.isStation)
                assertEquals(true, snapshot.isPlaying)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given a station with on-air program, when nowPlayingFlow collected, then program title replaces broadcaster`() =
        runTest {
            val program = MutableStateFlow<OnAirProgram?>(null)
            every { playerRepository.nowPlayingStation } returns MutableStateFlow(radioStationTestData)
            every { playerRepository.isPlaying } returns MutableStateFlow(true)
            every { observeOnAirProgram(radioStationTestData.id, false) } returns program

            reader.nowPlayingFlow().test {
                val initial = awaitItem()!!
                assertEquals(radioStationTestData.broadcaster, initial.feedTitle)
                assertNull(initial.imageUrl)

                program.value = onAirProgramTestData
                val withProgram = awaitItem()!!
                assertEquals(onAirProgramTestData.title, withProgram.feedTitle)
                // 프로그램 이미지가 이니셜 아트 대신 썸네일이 된다. 위젯은 에피소드와 같은 경로로 비트맵화한다.
                assertEquals(onAirProgramTestData.imageUrl, withProgram.imageUrl)

                // 곡만 바뀌면 스냅샷은 그대로다.
                program.value = onAirProgramTestData.copy(song = "곡 - 가수")
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given same title but image changes, when nowPlayingFlow collected, then new snapshot is emitted`() =
        runTest {
            val program = MutableStateFlow<OnAirProgram?>(onAirProgramTestData)
            every { playerRepository.nowPlayingStation } returns MutableStateFlow(radioStationTestData)
            every { playerRepository.isPlaying } returns MutableStateFlow(true)
            every { observeOnAirProgram(radioStationTestData.id, false) } returns program

            reader.nowPlayingFlow().test {
                // 첫 응답을 기다리지 않고 null 로 시작하므로 초기 발행이 한두 번일 수 있다. 마지막 것을 기준으로 삼는다.
                assertEquals(onAirProgramTestData.imageUrl, expectMostRecentItem()!!.imageUrl)
                program.value = onAirProgramTestData.copy(imageUrl = "https://img.example.com/other.jpg")
                assertEquals("https://img.example.com/other.jpg", awaitItem()!!.imageUrl)

                // 이미지가 사라지면 이니셜 아트로 돌아간다.
                program.value = onAirProgramTestData.copy(imageUrl = null)
                assertNull(awaitItem()!!.imageUrl)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given a station, when snapshotNowPlaying called, then returns station snapshot`() =
        runTest {
            every { playerRepository.nowPlayingStation } returns MutableStateFlow(radioStationTestData)
            every { playerRepository.isPlaying } returns MutableStateFlow(true)

            val snapshot = reader.snapshotNowPlaying()!!

            assertEquals(radioStationTestData.id, snapshot.stationId)
            assertEquals(radioStationTestData.art, snapshot.stationArt)
            assertNull(snapshot.episodeId)
            assertEquals(true, snapshot.isPlaying)
        }

    @Test
    fun `Given station then none, when nowPlayingFlow collected, then switches to episode path`() =
        runTest {
            val station = MutableStateFlow<Station?>(radioStationTestData)
            every { playerRepository.nowPlayingStation } returns station
            every { playerRepository.isPlaying } returns flowOf(true)
            every { getNowPlaying() } returns flowOf(episodeTestData)
            every { getPodcast(episodeTestData.feedId) } returns flowOf(null)

            reader.nowPlayingFlow().test {
                assertEquals(radioStationTestData.id, awaitItem()?.stationId)
                station.value = null
                val snapshot = awaitItem()!!
                assertNull(snapshot.stationId)
                assertEquals(episodeTestData.id, snapshot.episodeId)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given a station, when stream title changes, then no new snapshot is emitted`() =
        runTest {
            // 곡 제목은 스냅샷에 싣지 않으므로 바뀌어도 재방출이 없어야 한다.
            val streamTitle = MutableStateFlow<String?>("곡 1")
            every { playerRepository.nowPlayingStation } returns MutableStateFlow(radioStationTestData)
            every { playerRepository.isPlaying } returns MutableStateFlow(true)
            every { playerRepository.streamTitle } returns streamTitle

            reader.nowPlayingFlow().test {
                awaitItem()
                streamTitle.value = "곡 2"
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given user recent podcasts, when userRecentPodcastsFlow collected, then maps to snapshots in order`() =
        runTest {
            val max = 3
            val sourcePodcasts = podcastTestDataList.take(max)
            every { getUserRecentPodcasts(max) } returns flowOf(sourcePodcasts)

            val snapshots = reader.userRecentPodcastsFlow(max).first()

            assertEquals(sourcePodcasts.size, snapshots.size)
            sourcePodcasts.forEachIndexed { index, podcast ->
                val snapshot = snapshots[index]
                assertEquals(podcast.id, snapshot.id)
                assertEquals(podcast.title, snapshot.title)
                assertEquals(
                    podcast.image.ifBlank { podcast.artwork }.ifBlank { null },
                    snapshot.imageUrl,
                )
            }
        }
}
