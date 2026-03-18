package io.jacob.episodive.feature.player

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.usecase.episode.GetChaptersUseCase
import io.jacob.episodive.core.domain.usecase.episode.ToggleLikedEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.episode.UpdatePlayedEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.player.GetNowPlayingUseCase
import io.jacob.episodive.core.domain.usecase.player.GetPlaylistUseCase
import io.jacob.episodive.core.domain.usecase.player.RestoreLastPlayStateUseCase
import io.jacob.episodive.core.domain.usecase.player.SaveLastPlayStateUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetPodcastUseCase
import io.jacob.episodive.core.domain.usecase.podcast.ToggleFollowedUseCase
import io.jacob.episodive.core.domain.usecase.user.GetUserDataUseCase
import io.jacob.episodive.core.domain.usecase.user.SetSpeedUseCase
import io.jacob.episodive.core.model.Progress
import io.jacob.episodive.core.model.UserData
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.podcastTestData
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds
class PlayerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val toggleLikedEpisodeUseCase = mockk<ToggleLikedEpisodeUseCase>(relaxed = true)
    private val updatePlayedEpisodeUseCase = mockk<UpdatePlayedEpisodeUseCase>(relaxed = true)
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

    private val progressFlow = MutableStateFlow(Progress(0.seconds, 0.seconds, 0.seconds))
    private val isPlayingFlow = MutableStateFlow(false)
    private val speedFlow = MutableStateFlow(1.0f)
    private val indexOfListFlow = MutableStateFlow(0)
    private val cueFlow = MutableStateFlow("")
    private val isShuffleFlow = MutableStateFlow(false)
    private val repeatFlow = MutableStateFlow(io.jacob.episodive.core.model.Repeat.OFF)
    private val nowPlayingFlow = MutableStateFlow<io.jacob.episodive.core.model.Episode?>(null)

    private fun setupPlayerRepositoryMocks() {
        every { playerRepository.progress } returns progressFlow
        every { playerRepository.isPlaying } returns isPlayingFlow
        every { playerRepository.speed } returns speedFlow
        every { playerRepository.indexOfList } returns indexOfListFlow
        every { playerRepository.cue } returns cueFlow
        every { playerRepository.playback } returns MutableStateFlow(io.jacob.episodive.core.model.Playback.IDLE)
        every { playerRepository.isShuffle } returns isShuffleFlow
        every { playerRepository.repeat } returns repeatFlow
        every { playerRepository.nowPlaying } returns nowPlayingFlow
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
                assertEquals(PlayerEffect.NavigateToPodcast(podcast), awaitItem())
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
}
