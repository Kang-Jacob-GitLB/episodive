package io.jacob.episodive.feature.player

import app.cash.turbine.test
import io.jacob.episodive.core.common.TimeProvider
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.usecase.caption.CaptionToggleResult
import io.jacob.episodive.core.domain.usecase.caption.ObserveCaptionDownloadFailuresUseCase
import io.jacob.episodive.core.domain.usecase.caption.ObserveLiveCaptionUseCase
import io.jacob.episodive.core.domain.usecase.caption.ToggleCaptionUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetChaptersUseCase
import io.jacob.episodive.core.domain.usecase.episode.RefreshEpisodeDescriptionUseCase
import io.jacob.episodive.core.domain.usecase.episode.FetchEpisodeByIdUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetEpisodeByIdUseCase
import io.jacob.episodive.core.domain.usecase.episode.SaveEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.episode.ToggleLikedEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.episode.UpdatePlayedEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.player.GetNowPlayingUseCase
import io.jacob.episodive.core.domain.usecase.player.GetPlaylistUseCase
import io.jacob.episodive.core.domain.usecase.player.PlayEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.player.RestoreLastPlayStateUseCase
import io.jacob.episodive.core.domain.usecase.player.SaveLastPlayStateUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetPodcastUseCase
import io.jacob.episodive.core.domain.usecase.podcast.ToggleFollowedUseCase
import io.jacob.episodive.core.domain.usecase.user.GetUserDataUseCase
import io.jacob.episodive.core.domain.usecase.user.SetSpeedUseCase
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Playback
import io.jacob.episodive.core.model.Progress
import io.jacob.episodive.core.model.Repeat
import io.jacob.episodive.core.model.UserData
import io.jacob.episodive.core.model.caption.CaptionDownloadFailure
import io.jacob.episodive.core.model.caption.CaptionLanguage
import io.jacob.episodive.core.model.caption.LiveCaptionState
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.liveCaptionStateTestData
import io.jacob.episodive.core.testing.model.podcastTestData
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds
class PlayerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val toggleLikedEpisodeUseCase = mockk<ToggleLikedEpisodeUseCase>(relaxed = true)
    private val updatePlayedEpisodeUseCase = mockk<UpdatePlayedEpisodeUseCase>(relaxed = true)
    private val refreshEpisodeDescriptionUseCase = mockk<RefreshEpisodeDescriptionUseCase>(relaxed = true)
    private val getPodcastUseCase = mockk<GetPodcastUseCase>(relaxed = true)
    private val playerRepository = mockk<PlayerRepository>(relaxed = true)
    private val getNowPlayingUseCase = mockk<GetNowPlayingUseCase>(relaxed = true)
    private val getPlaylistUseCase = mockk<GetPlaylistUseCase>(relaxed = true)
    private val setSpeedUseCase = mockk<SetSpeedUseCase>(relaxed = true)
    private val getUserDataUseCase = mockk<GetUserDataUseCase>(relaxed = true)
    private val getChaptersUseCase = mockk<GetChaptersUseCase>(relaxed = true)
    private val toggleFollowedUseCase = mockk<ToggleFollowedUseCase>(relaxed = true)
    private val saveLastPlayStateUseCase = mockk<SaveLastPlayStateUseCase>(relaxed = true)
    private val restoreLastPlayStateUseCase = mockk<RestoreLastPlayStateUseCase>(relaxed = true)
    private val saveEpisodeUseCase = mockk<SaveEpisodeUseCase>(relaxed = true)
    private val timeProvider = mockk<TimeProvider>(relaxed = true)
    private val getEpisodeByIdUseCase = mockk<GetEpisodeByIdUseCase>(relaxed = true)
    private val fetchEpisodeByIdUseCase = mockk<FetchEpisodeByIdUseCase>(relaxed = true)
    private val playEpisodeUseCase = mockk<PlayEpisodeUseCase>(relaxed = true)
    private val observeLiveCaptionUseCase = mockk<ObserveLiveCaptionUseCase>(relaxed = true)
    private val toggleCaptionUseCase = mockk<ToggleCaptionUseCase>(relaxed = true)
    private val observeCaptionDownloadFailuresUseCase = mockk<ObserveCaptionDownloadFailuresUseCase>(relaxed = true)

    private val progressFlow = MutableStateFlow(Progress(0.seconds, 0.seconds, 0.seconds))
    private val isPlayingFlow = MutableStateFlow(false)
    private val speedFlow = MutableStateFlow(1.0f)
    private val indexOfListFlow = MutableStateFlow(0)
    private val cueFlow = MutableStateFlow("")
    private val isShuffleFlow = MutableStateFlow(false)
    private val repeatFlow = MutableStateFlow(Repeat.OFF)
    private val nowPlayingFlow = MutableStateFlow<Episode?>(null)

    private fun setupPlayerRepositoryMocks() {
        every { playerRepository.progress } returns progressFlow
        every { playerRepository.isPlaying } returns isPlayingFlow
        every { playerRepository.speed } returns speedFlow
        every { playerRepository.indexOfList } returns indexOfListFlow
        every { playerRepository.cue } returns cueFlow
        every { playerRepository.playback } returns MutableStateFlow(Playback.IDLE)
        every { playerRepository.isShuffle } returns isShuffleFlow
        every { playerRepository.repeat } returns repeatFlow
        every { playerRepository.nowPlaying } returns nowPlayingFlow
        // caption 관련 유스케이스는 대부분 테스트에서 의미가 없어 기본값을 여기서 공통으로 깐다.
        // 개별 caption 테스트는 필요한 mock 을 각자 다시 stub 한다.
        every { observeLiveCaptionUseCase() } returns flowOf(LiveCaptionState.Initial)
        every { observeCaptionDownloadFailuresUseCase() } returns MutableSharedFlow()
    }

    private fun setupDefaultMocks() {
        setupPlayerRepositoryMocks()
        every { getNowPlayingUseCase() } returns flowOf(null)
        every { getPlaylistUseCase() } returns flowOf(emptyList())
        every { getUserDataUseCase() } returns flowOf(UserData(speed = 1.0f))
    }

    private var viewModelInstance: PlayerViewModel? = null

    private fun createViewModel(): PlayerViewModel {
        return PlayerViewModel(
            toggleLikedEpisodeUseCase = toggleLikedEpisodeUseCase,
            updatePlayedEpisodeUseCase = updatePlayedEpisodeUseCase,
            refreshEpisodeDescriptionUseCase = refreshEpisodeDescriptionUseCase,
            getPodcastUseCase = getPodcastUseCase,
            playerRepository = playerRepository,
            getNowPlayingUseCase = getNowPlayingUseCase,
            getPlaylistUseCase = getPlaylistUseCase,
            setSpeedUseCase = setSpeedUseCase,
            getUserDataUseCase = getUserDataUseCase,
            getChaptersUseCase = getChaptersUseCase,
            toggleFollowedUseCase = toggleFollowedUseCase,
            saveLastPlayStateUseCase = saveLastPlayStateUseCase,
            restoreLastPlayStateUseCase = restoreLastPlayStateUseCase,
            saveEpisodeUseCase = saveEpisodeUseCase,
            getEpisodeByIdUseCase = getEpisodeByIdUseCase,
            fetchEpisodeByIdUseCase = fetchEpisodeByIdUseCase,
            playEpisodeUseCase = playEpisodeUseCase,
            timeProvider = timeProvider,
            observeLiveCaptionUseCase = observeLiveCaptionUseCase,
            toggleCaptionUseCase = toggleCaptionUseCase,
            observeCaptionDownloadFailuresUseCase = observeCaptionDownloadFailuresUseCase,
        ).also { viewModelInstance = it }
    }

    @After
    fun teardown() {
        viewModelInstance?.let {
            it.viewModelScope.cancel()
        }
        confirmVerified(
            toggleLikedEpisodeUseCase,
            setSpeedUseCase,
            toggleFollowedUseCase,
        )
    }

    @Test
    fun `Given no emissions, When ViewModel is created, Then initial state is Loading`() = runTest {
        setupDefaultMocks()

        val viewModel = createViewModel()

        assertEquals(PlayerState.Loading, viewModel.state.value)
    }

    @Test
    fun `Given podcast and nowPlaying flows emit, When collecting, Then state is Success`() =
        runTest {
            setupPlayerRepositoryMocks()
            val episode = episodeTestData
            val podcast = podcastTestData
            val playlist = episodeTestDataList.take(3)

            every { getNowPlayingUseCase() } returns flowOf(episode)
            every { getPodcastUseCase(episode.feedId) } returns flowOf(podcast)
            every { getPlaylistUseCase() } returns flowOf(playlist)
            every { getUserDataUseCase() } returns flowOf(UserData(speed = 1.0f))

            val viewModel = createViewModel()

            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state is PlayerState.Success)
                val success = state as PlayerState.Success
                assertEquals(podcast, success.podcast)
                assertEquals(episode, success.nowPlaying)
                assertEquals(playlist, success.playlist)
            }
        }

    @Test
    fun `Given null nowPlaying, When collecting, Then state remains Loading because podcast flow never emits`() =
        runTest {
            setupDefaultMocks()

            val viewModel = createViewModel()

            // When nowPlaying is null, podcast flow (via mapNotNull on feedId) never emits,
            // so combine never fires and state stays Loading
            assertEquals(PlayerState.Loading, viewModel.state.value)
        }

    @Test
    fun `Given getNowPlayingUseCase throws, When collecting, Then state is Error`() = runTest {
        setupPlayerRepositoryMocks()
        val episode = episodeTestData
        // getNowPlayingUseCase emits an episode first so that podcast flow can emit,
        // then getPodcastUseCase throws
        every { getNowPlayingUseCase() } returns flowOf(episode)
        every { getPodcastUseCase(episode.feedId) } returns kotlinx.coroutines.flow.flow {
            throw RuntimeException("Error")
        }
        every { getPlaylistUseCase() } returns flowOf(emptyList())
        every { getUserDataUseCase() } returns flowOf(UserData(speed = 1.0f))

        val viewModel = createViewModel()

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state is PlayerState.Error)
        }
    }

    // --- episodive:// 딥링크 착지 ---

    @Test
    fun `Given a locally known episode, When OpenDeepLink, Then it plays from the link position`() =
        runTest {
            setupDefaultMocks()
            val episode = episodeTestData
            every { getEpisodeByIdUseCase(episode.id) } returns flowOf(episode)
            val viewModel = createViewModel()

            viewModel.sendAction(PlayerAction.OpenDeepLink(episode.id, startPositionMs = 83_000L))

            coVerify { playEpisodeUseCase(episode) }
            coVerify { playerRepository.seekTo(83_000L) }
            // 로컬에 있으면 원격을 치지 않는다 — 링크를 누를 때마다 요청이 나가면 안 된다.
            coVerify(exactly = 0) { fetchEpisodeByIdUseCase(any()) }
        }

    @Test
    fun `Given an episode this device has never seen, When OpenDeepLink, Then it falls back to remote`() =
        runTest {
            // 남이 보낸 링크의 에피소드는 로컬 DB 에 없다. getEpisodeById 는 DB 만 보므로
            // 폴백이 없으면 공유 링크가 영영 열리지 않는다.
            setupDefaultMocks()
            val episode = episodeTestData
            every { getEpisodeByIdUseCase(episode.id) } returns flowOf(null)
            coEvery { fetchEpisodeByIdUseCase(episode.id) } returns episode
            val viewModel = createViewModel()

            viewModel.sendAction(PlayerAction.OpenDeepLink(episode.id, startPositionMs = null))

            coVerify { fetchEpisodeByIdUseCase(episode.id) }
            coVerify { playEpisodeUseCase(episode) }
            // 지점이 없으면 seek 하지 않는다. 0 으로 채우면 이어듣기 지점을 맨 앞으로 돌린다.
            coVerify(exactly = 0) { playerRepository.seekTo(any()) }
        }

    @Test
    fun `Given a resolvable episode, When OpenDeepLink, Then the sheet is asked to open`() =
        runTest {
            // 시트는 올릴 것이 확정된 뒤에 연다. 미리 열면 링크가 잘못됐을 때 빈 시트만
            // 남고, 그 안의 스낵바 호스트도 없어 실패를 알릴 길이 사라진다.
            setupDefaultMocks()
            val episode = episodeTestData
            every { getEpisodeByIdUseCase(episode.id) } returns flowOf(episode)
            val viewModel = createViewModel()

            viewModel.effect.test {
                viewModel.sendAction(PlayerAction.OpenDeepLink(episode.id, startPositionMs = null))
                assertTrue(awaitItem() is PlayerEffect.ShowPlayerBottomSheet)
                cancel()
            }
        }

    @Test
    fun `Given a zero position, When OpenDeepLink, Then it still seeks`() = runTest {
        // 0 은 없는 값이 아니라 "맨 앞부터"다.
        setupDefaultMocks()
        val episode = episodeTestData
        every { getEpisodeByIdUseCase(episode.id) } returns flowOf(episode)
        val viewModel = createViewModel()

        viewModel.sendAction(PlayerAction.OpenDeepLink(episode.id, startPositionMs = 0L))

        coVerify { playerRepository.seekTo(0L) }
    }

    @Test
    fun `Given the episode cannot be found anywhere, When OpenDeepLink, Then it reports the failure`() =
        runTest {
            setupDefaultMocks()
            every { getEpisodeByIdUseCase(any()) } returns flowOf(null)
            coEvery { fetchEpisodeByIdUseCase(any()) } returns null
            val viewModel = createViewModel()

            viewModel.effect.test {
                viewModel.sendAction(PlayerAction.OpenDeepLink(999L, startPositionMs = null))
                assertTrue(awaitItem() is PlayerEffect.ShowDeepLinkError)
                cancel()
            }

            coVerify(exactly = 0) { playEpisodeUseCase(any<Episode>()) }
        }

    @Test
    fun `Given the episode cannot be found, When OpenDeepLink, Then the sheet is not opened`() =
        runTest {
            setupDefaultMocks()
            every { getEpisodeByIdUseCase(any()) } returns flowOf(null)
            coEvery { fetchEpisodeByIdUseCase(any()) } returns null
            val viewModel = createViewModel()

            viewModel.effect.test {
                viewModel.sendAction(PlayerAction.OpenDeepLink(999L, startPositionMs = null))
                // 첫 이펙트가 에러여야 한다. 시트 열기가 앞서면 빈 시트가 남는다.
                assertTrue(awaitItem() is PlayerEffect.ShowDeepLinkError)
                expectNoEvents()
                cancel()
            }
        }

    @Test
    fun `Given PlayOrPause action, When sent, Then playerRepository playOrPause is invoked`() =
        runTest {
            setupDefaultMocks()
            val viewModel = createViewModel()

            viewModel.sendAction(PlayerAction.PlayOrPause)

            coVerify { playerRepository.playOrPause() }
        }

    @Test
    fun `Given Next action, When sent, Then playerRepository next is invoked`() = runTest {
        setupDefaultMocks()
        val viewModel = createViewModel()

        viewModel.sendAction(PlayerAction.Next)

        verify { playerRepository.next() }
    }

    @Test
    fun `Given Previous action, When sent, Then playerRepository previous is invoked`() = runTest {
        setupDefaultMocks()
        val viewModel = createViewModel()

        viewModel.sendAction(PlayerAction.Previous)

        verify { playerRepository.previous() }
    }

    @Test
    fun `Given Shuffle action, When sent, Then playerRepository shuffle is invoked`() = runTest {
        setupDefaultMocks()
        val viewModel = createViewModel()

        viewModel.sendAction(PlayerAction.Shuffle)

        verify { playerRepository.shuffle() }
    }

    @Test
    fun `Given Repeat action, When sent, Then playerRepository changeRepeat is invoked`() =
        runTest {
            setupDefaultMocks()
            val viewModel = createViewModel()

            viewModel.sendAction(PlayerAction.Repeat)

            verify { playerRepository.changeRepeat() }
        }

    @Test
    fun `Given PlayIndex action, When sent, Then playerRepository playIndex is invoked`() =
        runTest {
            setupDefaultMocks()
            val viewModel = createViewModel()

            viewModel.sendAction(PlayerAction.PlayIndex(2))

            verify { playerRepository.playIndex(2) }
        }

    @Test
    fun `Given SeekTo action, When sent, Then playerRepository seekTo is invoked`() = runTest {
        setupDefaultMocks()
        val viewModel = createViewModel()

        viewModel.sendAction(PlayerAction.SeekTo(5000L))

        verify { playerRepository.seekTo(5000L) }
    }

    @Test
    fun `Given SeekBackward action, When sent, Then playerRepository seekBackward is invoked`() =
        runTest {
            setupDefaultMocks()
            val viewModel = createViewModel()

            viewModel.sendAction(PlayerAction.SeekBackward)

            verify { playerRepository.seekBackward() }
        }

    @Test
    fun `Given SeekForward action, When sent, Then playerRepository seekForward is invoked`() =
        runTest {
            setupDefaultMocks()
            val viewModel = createViewModel()

            viewModel.sendAction(PlayerAction.SeekForward)

            verify { playerRepository.seekForward() }
        }

    @Test
    fun `Given Speed action, When sent, Then playerRepository setSpeed and setSpeedUseCase are invoked`() =
        runTest {
            setupDefaultMocks()
            val viewModel = createViewModel()

            viewModel.sendAction(PlayerAction.Speed(1.5f))

            verify { playerRepository.setSpeed(1.5f) }
            coVerify { setSpeedUseCase(1.5f) }
        }

    @Test
    fun `Given ClickPodcast action, When sent, Then NavigateToPodcast effect is emitted`() =
        runTest {
            setupDefaultMocks()
            val viewModel = createViewModel()
            val podcast = podcastTestData

            viewModel.effect.test {
                viewModel.sendAction(PlayerAction.ClickPodcast(podcast))
                assertEquals(PlayerEffect.NavigateToPodcast(podcast.id), awaitItem())
            }
        }

    @Test
    fun `Given ExpandPlayer action, When sent, Then ShowPlayerBottomSheet effect is emitted`() =
        runTest {
            setupDefaultMocks()
            val viewModel = createViewModel()

            viewModel.effect.test {
                viewModel.sendAction(PlayerAction.ExpandPlayer)
                assertEquals(PlayerEffect.ShowPlayerBottomSheet, awaitItem())
            }
        }

    @Test
    fun `Given CollapsePlayer action, When sent, Then HidePlayerBottomSheet effect is emitted`() =
        runTest {
            setupDefaultMocks()
            val viewModel = createViewModel()

            viewModel.effect.test {
                viewModel.sendAction(PlayerAction.CollapsePlayer)
                assertEquals(PlayerEffect.HidePlayerBottomSheet, awaitItem())
            }
        }

    @Test
    fun `Given ToggleLike action, When state is Success, Then toggleLikedEpisodeUseCase is invoked with nowPlaying`() =
        runTest {
            setupPlayerRepositoryMocks()
            val episode = episodeTestData
            val podcast = podcastTestData
            val playlist = episodeTestDataList.take(3)

            every { getNowPlayingUseCase() } returns flowOf(episode)
            every { getPodcastUseCase(episode.feedId) } returns flowOf(podcast)
            every { getPlaylistUseCase() } returns flowOf(playlist)
            every { getUserDataUseCase() } returns flowOf(UserData(speed = 1.0f))

            val viewModel = createViewModel()

            // Wait for state to become Success
            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state is PlayerState.Success)
            }

            viewModel.sendAction(PlayerAction.ToggleLike)

            coVerify { toggleLikedEpisodeUseCase(episode) }
        }

    @Test
    fun `Given ClickEpisode action, When state is Success, Then playerRepository playIndex is invoked with correct index`() =
        runTest {
            setupPlayerRepositoryMocks()
            val episode = episodeTestData
            val podcast = podcastTestData
            val playlist = episodeTestDataList.take(3)

            every { getNowPlayingUseCase() } returns flowOf(episode)
            every { getPodcastUseCase(episode.feedId) } returns flowOf(podcast)
            every { getPlaylistUseCase() } returns flowOf(playlist)
            every { getUserDataUseCase() } returns flowOf(UserData(speed = 1.0f))

            val viewModel = createViewModel()

            // Wait for state to become Success
            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state is PlayerState.Success)
            }

            val targetEpisode = playlist[1]
            viewModel.sendAction(PlayerAction.ClickEpisode(targetEpisode))

            verify { playerRepository.playIndex(1) }
        }

    @Test
    fun `Given ToggleFollowedPodcast action, When sent, Then toggleFollowedUseCase is invoked`() =
        runTest {
            setupDefaultMocks()
            val viewModel = createViewModel()
            val podcast = podcastTestData

            viewModel.sendAction(PlayerAction.ToggleFollowedPodcast(podcast))

            coVerify { toggleFollowedUseCase(podcast.id) }
        }

    @Test
    fun `Given ToggleLikedEpisode action, When sent, Then toggleLikedEpisodeUseCase is invoked`() =
        runTest {
            setupDefaultMocks()
            val viewModel = createViewModel()
            val episode = episodeTestData

            viewModel.sendAction(PlayerAction.ToggleLikedEpisode(episode))

            coVerify { toggleLikedEpisodeUseCase(episode) }
        }

    @Test
    fun `Given no current episode, When ViewModel is created, Then restoreLastPlayState is called`() =
        runTest {
            setupPlayerRepositoryMocks()
            nowPlayingFlow.value = null
            every { getNowPlayingUseCase() } returns flowOf(null)
            every { getPlaylistUseCase() } returns flowOf(emptyList())
            every { getUserDataUseCase() } returns flowOf(UserData(speed = 1.0f))

            createViewModel()

            coVerify { restoreLastPlayStateUseCase() }
        }

    @Test
    fun `Given current episode exists, When ViewModel is created, Then restoreLastPlayState is not called`() =
        runTest {
            setupPlayerRepositoryMocks()
            val episode = episodeTestData
            nowPlayingFlow.value = episode
            every { getNowPlayingUseCase() } returns flowOf(episode)
            every { getPodcastUseCase(episode.feedId) } returns flowOf(podcastTestData)
            every { getPlaylistUseCase() } returns flowOf(episodeTestDataList.take(3))
            every { getUserDataUseCase() } returns flowOf(UserData(speed = 1.0f))

            createViewModel()

            coVerify(exactly = 0) { restoreLastPlayStateUseCase() }
        }

    // --- Played Progress Persistence Tests ---

    @Test
    fun `Given episode A mid-playback, When progress flips to episode B with stale zero position, Then episode A is not saved with zero position`() =
        runTest {
            setupDefaultMocks()
            val episodeA = episodeTestDataList[0]
            val episodeB = episodeTestDataList[1]
            nowPlayingFlow.value = episodeA
            // getNowPlayingUseCase 는 DB 왕복 지연을 재현하기 위해 계속 A 를 방출한다.
            every { getNowPlayingUseCase() } returns flowOf(episodeA)
            progressFlow.value = Progress(500.seconds, 0.seconds, 0.seconds, episodeId = episodeA.id)

            createViewModel()

            // 에피소드 전환 순간: progress 는 이미 B 를 가리키지만 nowPlaying(DB 파생)은 아직 A 를 방출 중
            progressFlow.value = Progress(0.seconds, 0.seconds, 0.seconds, episodeId = episodeB.id)

            coVerify(exactly = 0) {
                updatePlayedEpisodeUseCase(episodeA.id, match { it.position == 0.seconds })
            }
            coVerify {
                updatePlayedEpisodeUseCase(episodeB.id, match { it.position == 0.seconds })
            }
        }

    @Test
    fun `Given episode A mid-playback, When switching to episode B resume position, Then episode A is not saved with episode B position`() =
        runTest {
            setupDefaultMocks()
            val episodeA = episodeTestDataList[0]
            val episodeB = episodeTestDataList[1]
            nowPlayingFlow.value = episodeA
            every { getNowPlayingUseCase() } returns flowOf(episodeA)
            progressFlow.value = Progress(500.seconds, 0.seconds, 0.seconds, episodeId = episodeA.id)

            createViewModel()

            // 이어듣기 전환: ZERO 방출이 StateFlow conflation 으로 생략된 상태를 재현
            progressFlow.value = Progress(300.seconds, 0.seconds, 0.seconds, episodeId = episodeB.id)

            coVerify(exactly = 0) {
                updatePlayedEpisodeUseCase(episodeA.id, match { it.position == 300.seconds })
            }
            coVerify {
                updatePlayedEpisodeUseCase(episodeB.id, match { it.position == 300.seconds })
            }
        }

    @Test
    fun `Given cold start with nowPlaying restored but progress not yet rehydrated, When ViewModel is created, Then no save is invoked`() =
        runTest {
            setupDefaultMocks()
            val episodeA = episodeTestData
            nowPlayingFlow.value = episodeA
            every { getNowPlayingUseCase() } returns flowOf(episodeA)
            // progressFlow 는 초기값 그대로: episodeId = null (rehydrate 는 progress 를 건드리지 않는다)

            createViewModel()

            coVerify(exactly = 0) { updatePlayedEpisodeUseCase(any(), any()) }
        }

    @Test
    fun `Given episode A rewound to zero, When user restarts from the beginning, Then position zero is saved`() =
        runTest {
            // 사용자가 맨 앞으로 되감거나 완료한 에피소드를 다시 듣기 시작하면 0 이 저장되어야 한다.
            // position==0 저장을 막는 가드를 넣으면 이 테스트가 실패한다.
            setupDefaultMocks()
            val episodeA = episodeTestData
            nowPlayingFlow.value = episodeA
            every { getNowPlayingUseCase() } returns flowOf(episodeA)
            progressFlow.value = Progress(500.seconds, 0.seconds, 0.seconds, episodeId = episodeA.id)

            createViewModel()

            progressFlow.value = Progress(0.seconds, 0.seconds, 0.seconds, episodeId = episodeA.id)

            coVerify {
                updatePlayedEpisodeUseCase(episodeA.id, match { it.position == 0.seconds })
            }
        }

    @Test
    fun `Given progress without episodeId, When ViewModel is created, Then LastPlaySnapshot is not saved`() =
        runTest {
            setupDefaultMocks()
            every { timeProvider.currentTimeMillis() } returns 10_000L
            // nowPlaying 은 채워져 있지만 progress.episodeId 만 null 인 상태를 재현한다.
            // 옛 5-arity combine(index, progress, shuffle, repeat, nowPlaying) 으로 되돌리면
            // nowPlaying 이 non-null 이라 이 테스트가 실패해야 한다. nowPlaying 을 세팅하지 않으면
            // 옛 구현도 nowPlaying==null 때문에 저장을 건너뛰어 이 테스트가 실효 없이 통과해버린다.
            nowPlayingFlow.value = episodeTestData
            progressFlow.value = Progress(100.seconds, 0.seconds, 0.seconds, episodeId = null)

            createViewModel()

            coVerify(exactly = 0) {
                saveLastPlayStateUseCase(any(), any(), any(), any(), any())
            }
        }

    @Test
    fun `Given progress with episodeId, When ViewModel is created, Then LastPlaySnapshot is saved with progress episodeId`() =
        runTest {
            setupDefaultMocks()
            every { timeProvider.currentTimeMillis() } returns 10_000L
            val episodeA = episodeTestData
            progressFlow.value = Progress(100.seconds, 0.seconds, 0.seconds, episodeId = episodeA.id)

            createViewModel()

            coVerify {
                saveLastPlayStateUseCase(episodeA.id, any(), 100_000L, any(), any())
            }
        }

    @Test
    fun `Given episode changes within the 5-second throttle window, When progress flips to episode B, Then both episodes are saved without waiting`() =
        runTest {
            setupDefaultMocks()
            // 시각을 고정해 throttle 창(5초)이 절대 열리지 않게 한다.
            // 그럼에도 에피소드가 바뀌면 lastSavedEpisodeId 가드가 즉시 저장을 강제해야 한다.
            every { timeProvider.currentTimeMillis() } returns 10_000L
            val episodeA = episodeTestDataList[0]
            val episodeB = episodeTestDataList[1]
            progressFlow.value = Progress(100.seconds, 0.seconds, 0.seconds, episodeId = episodeA.id)

            createViewModel()

            // B 의 위치를 A 보다 **크게** 잡는 것이 중요하다. 작게 잡으면 rewound 가드가 참이 되어
            // episodeChanged 를 지워도 저장이 일어나고, 이 테스트는 아무것도 잡지 못한다.
            progressFlow.value = Progress(200.seconds, 0.seconds, 0.seconds, episodeId = episodeB.id)

            coVerify {
                saveLastPlayStateUseCase(episodeA.id, any(), 100_000L, any(), any())
            }
            coVerify {
                saveLastPlayStateUseCase(episodeB.id, any(), 200_000L, any(), any())
            }
        }

    @Test
    fun `Given same episode progress ticks within the 5-second throttle window, When position changes, Then only one snapshot is saved`() =
        runTest {
            setupDefaultMocks()
            // 계약 테스트: throttle 을 통째로 없애면(예: episodeChanged 가드만 남기고 시간 체크를 지우면)
            // 같은 에피소드 안에서도 매 tick 저장이 발생해 이 테스트가 실패해야 한다.
            every { timeProvider.currentTimeMillis() } returns 10_000L
            val episodeA = episodeTestDataList[0]
            progressFlow.value = Progress(100.seconds, 0.seconds, 0.seconds, episodeId = episodeA.id)

            createViewModel()

            progressFlow.value = Progress(200.seconds, 0.seconds, 0.seconds, episodeId = episodeA.id)

            coVerify(exactly = 1) {
                saveLastPlayStateUseCase(episodeA.id, any(), any(), any(), any())
            }
        }

    @Test
    fun `Given user rewinds within the 5-second throttle window, When position jumps backward, Then the rewound position is saved immediately`() =
        runTest {
            // 되감기는 throttle 을 기다리면 안 된다. 창이 닫힌 동안 사용자가 일시정지하면
            // progress 방출이 멈춰 되감기 이전 값이 스냅샷에 남고, 다음 실행에서 그 값으로 복원된다.
            // 되감은 지점이 사라지는 것은 사용자에겐 "내가 한 조작이 무시됐다" 로 보인다.
            // rewound 가드를 지우면 이 테스트가 실패한다.
            setupDefaultMocks()
            every { timeProvider.currentTimeMillis() } returns 10_000L
            val episodeA = episodeTestDataList[0]
            progressFlow.value = Progress(1800.seconds, 0.seconds, 3600.seconds, episodeId = episodeA.id)

            createViewModel()

            // 사용자가 맨 앞으로 되감았다. 시각은 그대로라 throttle 창은 닫혀 있다.
            progressFlow.value = Progress(0.seconds, 0.seconds, 3600.seconds, episodeId = episodeA.id)

            coVerify(exactly = 1) {
                saveLastPlayStateUseCase(episodeA.id, any(), 0L, any(), any())
            }
        }

    @Test
    fun `Given episode A progress ticks consecutively, When collected, Then the last position is persisted`() =
        runTest {
            // 같은 에피소드 안에서 연속 방출된 tick 중 마지막 값이 저장되는지 확인한다.
            //
            // 주의: 이 테스트는 collect 와 collectLatest 를 구별하지 못한다. MainDispatcherRule 의
            // UnconfinedTestDispatcher 에서는 두 구현이 똑같이 동작하기 때문이다(실측 확인).
            // 저장 콜렉터를 collectLatest 로 되돌려도 이 테스트는 통과한다 —
            // "느린 DB 에서 전환 직전 쓰기가 취소되지 않는다" 는 보장은 여기서 검증되지 않는다.
            setupDefaultMocks()
            val episodeA = episodeTestDataList[0]
            progressFlow.value = Progress(100.seconds, 0.seconds, 0.seconds, episodeId = episodeA.id)

            createViewModel()

            progressFlow.value = Progress(200.seconds, 0.seconds, 0.seconds, episodeId = episodeA.id)

            coVerify {
                updatePlayedEpisodeUseCase(episodeA.id, match { it.position == 200.seconds })
            }
        }

    // --- Sleep Timer Tests ---

    @Test
    fun `Given end of episode timer running, When progress moves to another episode, Then timer stops without pausing`() =
        runTest {
            // 타이머의 에피소드 판별도 progress.episodeId 로 한다. nowPlaying 은 DB 왕복 탓에
            // 전환 후에도 한동안 이전 에피소드로 남아 있어서, 그것으로 판별하면 타이머가
            // 이미 넘어간 에피소드를 기준으로 계속 돌며 엉뚱한 시점에 재생을 멈춘다.
            // 판별을 nowPlaying 으로 되돌리면 이 테스트가 실패한다.
            setupDefaultMocks()
            val episodeA = episodeTestDataList[0]
            val episodeB = episodeTestDataList[1]
            // nowPlaying 은 A 로 고정한다 — 전환이 progress 에만 먼저 반영된 상태를 재현한다.
            nowPlayingFlow.value = episodeA
            every { getNowPlayingUseCase() } returns flowOf(episodeA)
            progressFlow.value = Progress(10.seconds, 10.seconds, 600.seconds, episodeId = episodeA.id)

            val viewModel = createViewModel()
            viewModel.sendAction(PlayerAction.SleepTimerEndOfEpisode)

            // 아직 A 를 재생 중이므로 타이머가 살아 있다.
            progressFlow.value = Progress(20.seconds, 20.seconds, 600.seconds, episodeId = episodeA.id)
            verify(exactly = 0) { playerRepository.pause() }

            // B 로 넘어가면 타이머는 중단돼야 한다.
            // 남은 시간을 **0 으로** 주는 것이 중요하다. 만료 조건(remaining <= 500)을 넘기지 않으면
            // 판별을 무엇으로 하든 pause() 가 불리지 않아 이 테스트가 아무것도 잡지 못한다.
            //
            // 다만 이 테스트는 "판별을 nowPlaying 으로 되돌리면 실패한다"를 보이지는 못한다.
            // nowPlaying 은 stateIn(WhileSubscribed) 이라 이 테스트에서 값이 흐르지 않고,
            // 옛 구현도 null 비교로 조기 중단되기 때문이다(실측 확인).
            // 잡아내는 것은 "에피소드 판별을 아예 없애는" 회귀다.
            progressFlow.value = Progress(600.seconds, 600.seconds, 600.seconds, episodeId = episodeB.id)

            verify(exactly = 0) { playerRepository.pause() }
        }

    @Test
    fun `Given SetSleepTimer action, When timer expires, Then pause is called and SleepTimerExpired effect is emitted`() =
        runTest {
            setupDefaultMocks()
            every { timeProvider.currentTimeMillis() } returnsMany listOf(0L, 60_001L)

            val viewModel = createViewModel()

            viewModel.effect.test {
                viewModel.sendAction(PlayerAction.SetSleepTimer(60_000L))
                assertEquals(PlayerEffect.SleepTimerExpired, awaitItem())
            }

            verify { playerRepository.pause() }
        }

    @Test
    fun `Given SetSleepTimer action, When timer expires, Then volume is restored to 1f`() =
        runTest {
            setupDefaultMocks()
            every { timeProvider.currentTimeMillis() } returnsMany listOf(0L, 60_001L)

            val viewModel = createViewModel()

            viewModel.sendAction(PlayerAction.SetSleepTimer(60_000L))

            // finally block restores volume after expiry
            verify { playerRepository.setVolume(0f) }  // fade to 0 at expiry
            verify(atLeast = 1) { playerRepository.setVolume(1f) }  // restored in finally
        }

    @Test
    fun `Given CancelSleepTimer action, When no timer is active, Then no crash occurs`() =
        runTest {
            setupDefaultMocks()
            val viewModel = createViewModel()

            viewModel.sendAction(PlayerAction.CancelSleepTimer)

            // Should not crash; verify no pause called
            verify(exactly = 0) { playerRepository.pause() }
        }

    @Test
    fun `Given two sequential SetSleepTimer actions, When both expire immediately, Then each triggers pause`() =
        runTest {
            setupDefaultMocks()
            // With UnconfinedTestDispatcher, each timer expires instantly via mocked time
            every { timeProvider.currentTimeMillis() } returnsMany listOf(
                0L, 60_001L,       // first timer: start → expire
                0L, 30_001L,       // second timer: start → expire
            )

            val viewModel = createViewModel()

            viewModel.sendAction(PlayerAction.SetSleepTimer(60_000L))
            viewModel.sendAction(PlayerAction.SetSleepTimer(30_000L))

            verify(exactly = 2) { playerRepository.pause() }
        }

    @Test
    fun `Given SleepTimerEndOfEpisode action with live stream, When duration is zero, Then no timer starts`() =
        runTest {
            setupDefaultMocks()
            nowPlayingFlow.value = episodeTestData
            // episodeId 를 채워서 조기 return(episodeId == null) 이 아니라
            // duration == 0(라이브 스트림) 때문에 타이머가 뜨지 않는 경로를 실제로 검증한다.
            progressFlow.value = Progress(0.seconds, 0.seconds, 0.seconds, episodeId = episodeTestData.id)

            val viewModel = createViewModel()

            viewModel.sendAction(PlayerAction.SleepTimerEndOfEpisode)

            // No pause should be called for live stream (duration = 0)
            verify(exactly = 0) { playerRepository.pause() }
        }

    // --- Episode Description Refresh Tests ---

    @Test
    fun `Given nowPlaying emits the same episode id repeatedly, When collecting, Then refreshEpisodeDescriptionUseCase is called only once`() =
        runTest {
            // distinctUntilChanged 회귀 테스트: 재생 중 progress tick 마다 played_episodes 에
            // 쓰기가 일어나 episode_with_extras 뷰가 무효화되면 nowPlaying 이 같은 id 의 에피소드를
            // (매번 새 인스턴스로) 반복 방출할 수 있다. distinctUntilChanged 를 지우면 매 tick
            // 마다 원격 보강을 다시 쳐서 초당 여러 번 네트워크를 호출하게 된다 — 이 테스트가 그
            // 회귀를 잡는다.
            //
            // getNowPlayingUseCase() 를 콜드 flowOf(a, b) 로 주면 nowPlaying 이 감싸는
            // stateIn(WhileSubscribed) 이 구독자가 붙기 전에 두 값을 한 번에 흘려보내 앞선 값이
            // 유실된다(StateFlow 는 값을 conflate 한다) — 그래서 MutableStateFlow 를 직접 조작해,
            // 첫 값은 구독 전에 실어 두고 다음 값은 ViewModel 이 만들어진 뒤(구독이 이미 붙은
            // 뒤)에 흘려보낸다. 이 파일의 다른 테스트(progressFlow 등)와 같은 패턴이다.
            setupDefaultMocks()
            val episodeA = episodeTestDataList[0]
            val nowPlayingUseCaseFlow = MutableStateFlow<Episode?>(episodeA)
            every { getNowPlayingUseCase() } returns nowPlayingUseCaseFlow

            createViewModel()

            // 같은 id 지만 다른 필드가 바뀐 새 인스턴스 — DB 뷰가 재구성됐지만 재생 중인 에피소드는
            // 그대로인 상황을 흉내낸다. id 만 보는 distinctUntilChanged 라면 이 재방출은 걸러져야
            // 한다.
            nowPlayingUseCaseFlow.value = episodeA.copy(title = "같은 id, 다른 인스턴스로 재방출됨")

            coVerify(exactly = 1) { refreshEpisodeDescriptionUseCase(episodeA.id) }
        }

    @Test
    fun `Given nowPlaying switches from one episode to another, When collecting, Then refreshEpisodeDescriptionUseCase is called once for each episode`() =
        runTest {
            setupDefaultMocks()
            val episodeA = episodeTestDataList[0]
            val episodeB = episodeTestDataList[1]
            val nowPlayingUseCaseFlow = MutableStateFlow<Episode?>(episodeA)
            every { getNowPlayingUseCase() } returns nowPlayingUseCaseFlow

            createViewModel()

            // ViewModel 이 만들어져 구독이 이미 붙은 뒤에 전환해야 두 값이 각각 관찰된다(위 테스트와
            // 같은 이유).
            nowPlayingUseCaseFlow.value = episodeB

            coVerify(exactly = 1) { refreshEpisodeDescriptionUseCase(episodeA.id) }
            coVerify(exactly = 1) { refreshEpisodeDescriptionUseCase(episodeB.id) }
        }

    @Test
    fun `Given refreshEpisodeDescriptionUseCase throws, When nowPlaying emits, Then the exception does not propagate`() =
        runTest {
            // onEach 안에서 던지면 collect 가 죽어 VM 수명 내내 보강이 영구 중단되므로 catch
            // 에러 경계를 뒀다. 그 경계가 실제로 예외를 흡수해 크래시를 막는지 확인한다. catch
            // 이후로는 스트림 자체가 끝나므로 "그 다음 에피소드도 계속 보강된다" 는 이 구조로는
            // 보장되지 않는다 — 그래서 여기서는 단정하지 않는다.
            setupDefaultMocks()
            val episodeA = episodeTestDataList[0]
            every { getNowPlayingUseCase() } returns flowOf(episodeA)
            coEvery { refreshEpisodeDescriptionUseCase(episodeA.id) } throws RuntimeException("boom")

            // createViewModel 자체가 예외 없이 끝나야 한다 — catch 경계가 없다면 여기서 크래시한다.
            createViewModel()

            coVerify(exactly = 1) { refreshEpisodeDescriptionUseCase(episodeA.id) }
        }

    // --- 자막(caption) 테스트 ---

    @Test
    fun `Given only state is collected, When observing, Then the caption upstream is not collected`() =
        runTest {
            // caption 은 state 의 10-arity combine 에 끼우지 않는다. PlayerBar 는 state 를 늘
            // 수집하므로, 여기에 자막 업스트림이 딸려 있으면 시트가 닫혀 있어도(미니바만 보여도)
            // 인식이 계속 돈다 — 그 회귀를 잡는다.
            setupDefaultMocks()
            var collected = false
            every { observeLiveCaptionUseCase() } returns flowOf(liveCaptionStateTestData)
                .onStart { collected = true }

            val viewModel = createViewModel()

            viewModel.state.test {
                awaitItem()
                cancel()
            }

            assertFalse(collected)
        }

    @Test
    fun `Given ToggleCaption action, When sent, Then toggleCaptionUseCase is invoked with the current caption value`() =
        runTest {
            setupDefaultMocks()
            every { observeLiveCaptionUseCase() } returns flowOf(liveCaptionStateTestData)
            coEvery { toggleCaptionUseCase(any()) } returns CaptionToggleResult.Enabled

            val viewModel = createViewModel()

            // caption StateFlow 를 실제로 흐르게 한다 — WhileSubscribed 라 구독자가 없으면
            // 업스트림이 시작되지 않아 caption.value 가 Initial 에 머문다. awaitItem 이 실제
            // 방출값을 받은 뒤에 토글을 보내야 caption.value 가 Initial 이 아님을 보장한다.
            viewModel.caption.test {
                assertEquals(liveCaptionStateTestData, awaitItem())

                viewModel.sendAction(PlayerAction.ToggleCaption)

                cancel()
            }

            coVerify { toggleCaptionUseCase(liveCaptionStateTestData) }
        }

    @Test
    fun `Given the caption upstream throws once, When resubscribed, Then it retries with backoff and reflects the later value`() =
        runTest {
            // catch{emit(Initial)} 로 업스트림을 끝내면 저장값은 켜짐인데 화면은 Initial 로
            // 굳어 토글이 죽는다(리뷰 지적). retryWhen 백오프 재구독으로 바꿨으니, 한 번
            // 던져도 재구독되어 다음 값이 반영되는지 확인한다.
            setupDefaultMocks()
            var attempt = 0
            every { observeLiveCaptionUseCase() } returns flow {
                attempt++
                if (attempt == 1) {
                    throw RuntimeException("스트림 끊김")
                }
                emit(liveCaptionStateTestData)
            }

            val viewModel = createViewModel()

            viewModel.caption.test {
                // 첫 구독 직후엔 업스트림이 아직 던지기 전이라도 StateFlow 의 초기값(Initial)이
                // 먼저 온다. 백오프 delay 는 viewModelScope(Main) 스케줄러 위에서 도는데,
                // runTest 의 스케줄러와 별개라 명시적으로 시간을 밀어줘야 재구독이 일어난다.
                assertEquals(LiveCaptionState.Initial, awaitItem())

                mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(1_100)
                mainDispatcherRule.testDispatcher.scheduler.runCurrent()

                assertEquals(liveCaptionStateTestData, awaitItem())
                cancel()
            }

            assertEquals(2, attempt)
        }

    @Test
    fun `Given toggleCaptionUseCase returns DownloadStarted, When ToggleCaption is sent, Then CaptionDownloadStarted effect is emitted`() =
        runTest {
            setupDefaultMocks()
            coEvery { toggleCaptionUseCase(any()) } returns CaptionToggleResult.DownloadStarted(132_000_000L)

            val viewModel = createViewModel()

            viewModel.effect.test {
                viewModel.sendAction(PlayerAction.ToggleCaption)
                assertEquals(PlayerEffect.CaptionDownloadStarted(132_000_000L), awaitItem())
                cancel()
            }
        }

    @Test
    fun `Given toggleCaptionUseCase returns UnsupportedLanguage, When ToggleCaption is sent, Then CaptionUnsupported effect is emitted`() =
        runTest {
            setupDefaultMocks()
            coEvery { toggleCaptionUseCase(any()) } returns CaptionToggleResult.UnsupportedLanguage

            val viewModel = createViewModel()

            viewModel.effect.test {
                viewModel.sendAction(PlayerAction.ToggleCaption)
                assertEquals(PlayerEffect.CaptionUnsupported, awaitItem())
                cancel()
            }
        }

    @Test
    fun `Given the download failure stream emits, When collecting, Then CaptionDownloadFailed effect is emitted`() =
        runTest {
            setupDefaultMocks()
            val failure = CaptionDownloadFailure(
                language = CaptionLanguage.ENGLISH,
                reason = CaptionDownloadFailure.Reason.NETWORK,
            )
            val failuresFlow = MutableSharedFlow<CaptionDownloadFailure>(extraBufferCapacity = 1)
            every { observeCaptionDownloadFailuresUseCase() } returns failuresFlow

            val viewModel = createViewModel()

            viewModel.effect.test {
                failuresFlow.emit(failure)
                assertEquals(PlayerEffect.CaptionDownloadFailed(failure.reason), awaitItem())
                cancel()
            }
        }
}
