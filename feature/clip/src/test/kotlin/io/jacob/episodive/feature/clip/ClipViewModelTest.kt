package io.jacob.episodive.feature.clip

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.usecase.episode.GetClipEpisodesPagingUseCase
import io.jacob.episodive.core.domain.usecase.episode.ToggleLikedEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.player.PlayEpisodeUseCase
import io.jacob.episodive.core.model.Playback
import io.jacob.episodive.core.model.Progress
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class ClipViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getClipEpisodesPagingUseCase = mockk<GetClipEpisodesPagingUseCase>(relaxed = true)
    private val playerRepository = mockk<PlayerRepository>(relaxed = true)
    private val playEpisodeUseCase = mockk<PlayEpisodeUseCase>(relaxed = true)
    private val toggleLikedEpisodeUseCase = mockk<ToggleLikedEpisodeUseCase>(relaxed = true)

    private val playbackFlow = MutableStateFlow(Playback.IDLE)
    private val progressFlow = MutableStateFlow(Progress(0.seconds, 0.seconds, 0.seconds))
    private val isPlayingFlow = MutableStateFlow(false)

    private fun createViewModel(): ClipViewModel {
        every { playerRepository.playback } returns playbackFlow
        every { playerRepository.progress } returns progressFlow
        every { playerRepository.isPlaying } returns isPlayingFlow

        return ClipViewModel(
            getClipEpisodesPagingUseCase = getClipEpisodesPagingUseCase,
            playerRepository = playerRepository,
            playEpisodeUseCase = playEpisodeUseCase,
            toggleLikedEpisodeUseCase = toggleLikedEpisodeUseCase,
        )
    }

    @After
    fun teardown() {
        confirmVerified(playEpisodeUseCase, toggleLikedEpisodeUseCase)
    }

    @Test
    fun `Given ViewModel created, Then initial clipPlayerState has defaults`() = runTest {
        val viewModel = createViewModel()

        val state = viewModel.clipPlayerState.value
        assertEquals(Playback.IDLE, state.playback)
        assertEquals(Progress(0.seconds, 0.seconds, 0.seconds), state.progress)
        assertFalse(state.isPlaying)
    }

    @Test
    fun `Given player flows emit, When collecting, Then clipPlayerState is updated`() = runTest {
        val viewModel = createViewModel()

        val newProgress = Progress(10.seconds, 20.seconds, 60.seconds)
        playbackFlow.emit(Playback.READY)
        progressFlow.emit(newProgress)
        isPlayingFlow.emit(true)

        viewModel.clipPlayerState.test {
            val state = awaitItem()
            assertEquals(Playback.READY, state.playback)
            assertEquals(newProgress, state.progress)
            assertEquals(true, state.isPlaying)
        }
    }

    @Test
    fun `Given PlayClip action, When sent, Then playerRepository playClip is invoked`() = runTest {
        val viewModel = createViewModel()
        val episode = episodeTestData

        viewModel.sendAction(ClipAction.PlayClip(episode))

        verify { playerRepository.playClip(episode) }
    }

    @Test
    fun `Given ClickEpisode action, When sent, Then playEpisodeUseCase is invoked`() = runTest {
        val viewModel = createViewModel()
        val episode = episodeTestData

        viewModel.sendAction(ClipAction.ClickEpisode(episode))

        coVerify { playEpisodeUseCase(episode) }
    }

    @Test
    fun `Given ToggleLikedEpisode action, When sent, Then toggleLikedEpisodeUseCase is invoked`() =
        runTest {
            val viewModel = createViewModel()
            val episode = episodeTestData

            viewModel.sendAction(ClipAction.ToggleLikedEpisode(episode))

            coVerify { toggleLikedEpisodeUseCase(episode) }
        }

    @Test
    fun `Given ClickPodcast action, When sent, Then NavigateToPodcast effect is emitted`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.effect.test {
                viewModel.sendAction(ClipAction.ClickPodcast(42L))
                assertEquals(ClipEffect.NavigateToPodcast(42L), awaitItem())
            }
        }

    @Test
    fun `Given Resume action, When sent, Then playerRepository resume is invoked`() = runTest {
        val viewModel = createViewModel()

        viewModel.sendAction(ClipAction.Resume)

        verify { playerRepository.resume() }
    }

    @Test
    fun `Given Pause action, When sent, Then playerRepository pause is invoked`() = runTest {
        val viewModel = createViewModel()

        viewModel.sendAction(ClipAction.Pause)

        verify { playerRepository.pause() }
    }
}
