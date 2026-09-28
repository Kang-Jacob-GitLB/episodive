package io.jacob.episodive.feature.podcast

import androidx.paging.PagingData
import app.cash.turbine.test
import io.jacob.episodive.core.domain.usecase.episode.GetEpisodesByPodcastIdPagingUseCase
import io.jacob.episodive.core.domain.usecase.episode.SaveEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.episode.ToggleLikedEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.player.PlayEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetPodcastUseCase
import io.jacob.episodive.core.domain.usecase.podcast.ToggleFollowedUseCase
import io.jacob.episodive.core.model.DataError
import io.jacob.episodive.core.model.DataErrorException
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.podcastTestData
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PodcastViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getPodcastUseCase = mockk<GetPodcastUseCase>(relaxed = true)
    private val getEpisodesByPodcastIdPagingUseCase =
        mockk<GetEpisodesByPodcastIdPagingUseCase>(relaxed = true)
    private val toggleFollowedUseCase = mockk<ToggleFollowedUseCase>(relaxed = true)
    private val playEpisodeUseCase = mockk<PlayEpisodeUseCase>(relaxed = true)
    private val toggleLikedEpisodeUseCase = mockk<ToggleLikedEpisodeUseCase>(relaxed = true)
    private val saveEpisodeUseCase = mockk<SaveEpisodeUseCase>(relaxed = true)

    private fun createViewModel(id: Long = 1L): PodcastViewModel {
        return PodcastViewModel(
            getPodcastUseCase = getPodcastUseCase,
            getEpisodesByPodcastIdPagingUseCase = getEpisodesByPodcastIdPagingUseCase,
            toggleFollowedUseCase = toggleFollowedUseCase,
            playEpisodeUseCase = playEpisodeUseCase,
            toggleLikedEpisodeUseCase = toggleLikedEpisodeUseCase,
            saveEpisodeUseCase = saveEpisodeUseCase,
            id = id,
        )
    }

    @After
    fun teardown() {
        confirmVerified(toggleFollowedUseCase, playEpisodeUseCase, toggleLikedEpisodeUseCase)
    }

    @Test
    fun `Given no emissions, When ViewModel is created, Then initial state is Loading`() = runTest {
        every { getPodcastUseCase(any()) } returns flowOf()
        every { getEpisodesByPodcastIdPagingUseCase(any()) } returns flowOf(PagingData.empty())

        val viewModel = createViewModel()

        assertEquals(PodcastState.Loading, viewModel.state.value)
    }

    @Test
    fun `Given valid podcast, When flow emits, Then state is Success`() = runTest {
        val podcast = podcastTestData
        every { getPodcastUseCase(1L) } returns flowOf(podcast)
        every { getEpisodesByPodcastIdPagingUseCase(any()) } returns flowOf(PagingData.empty())

        val viewModel = createViewModel()

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state is PodcastState.Success)
            assertEquals(podcast, (state as PodcastState.Success).podcast)
        }
    }

    @Test
    fun `Given null podcast, When flow emits, Then state is Error with NotFound`() = runTest {
        every { getPodcastUseCase(1L) } returns flowOf(null)
        every { getEpisodesByPodcastIdPagingUseCase(any()) } returns flowOf(PagingData.empty())

        val viewModel = createViewModel()

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state is PodcastState.Error)
            assertEquals(DataError.NotFound, (state as PodcastState.Error).error)
        }
    }

    @Test
    fun `Given flow throws unrecognized exception, When collecting, Then state is Error with Unexpected`() =
        runTest {
            val exception = RuntimeException("Network error")
            every { getPodcastUseCase(1L) } returns kotlinx.coroutines.flow.flow {
                throw exception
            }
            every { getEpisodesByPodcastIdPagingUseCase(any()) } returns flowOf(PagingData.empty())

            val viewModel = createViewModel()

            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state is PodcastState.Error)
                val error = (state as PodcastState.Error).error
                assertTrue(error is DataError.Unexpected)
                assertEquals(exception, (error as DataError.Unexpected).throwable)
            }
        }

    @Test
    fun `Given flow throws DataErrorException, When collecting, Then state is Error with the mapped DataError`() =
        runTest {
            every { getPodcastUseCase(1L) } returns kotlinx.coroutines.flow.flow {
                throw DataErrorException(DataError.Offline)
            }
            every { getEpisodesByPodcastIdPagingUseCase(any()) } returns flowOf(PagingData.empty())

            val viewModel = createViewModel()

            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state is PodcastState.Error)
                assertEquals(DataError.Offline, (state as PodcastState.Error).error)
            }
        }

    @Test
    fun `Given Retry action after error, When sent, Then getPodcastUseCase is resubscribed`() =
        runTest {
            var callCount = 0
            every { getPodcastUseCase(1L) } answers {
                callCount++
                if (callCount == 1) flowOf(null) else flowOf(podcastTestData)
            }
            every { getEpisodesByPodcastIdPagingUseCase(any()) } returns flowOf(PagingData.empty())

            val viewModel = createViewModel()

            viewModel.state.test {
                assertTrue(awaitItem() is PodcastState.Error)

                viewModel.sendAction(PodcastAction.Retry)

                assertTrue(awaitItem() is PodcastState.Success)
            }
            assertEquals(2, callCount)
        }

    @Test
    fun `Given ToggleFollowed action, When sent, Then toggleFollowedUseCase is invoked with id`() =
        runTest {
            every { getPodcastUseCase(1L) } returns flowOf(podcastTestData)
            every { getEpisodesByPodcastIdPagingUseCase(any()) } returns flowOf(PagingData.empty())

            val viewModel = createViewModel()

            viewModel.sendAction(PodcastAction.ToggleFollowed)

            coVerify { toggleFollowedUseCase(1L) }
        }

    @Test
    fun `Given PlayEpisode action with episode in visibleEpisodes, When sent, Then playEpisodeUseCase is invoked with reversed subList`() =
        runTest {
            every { getPodcastUseCase(1L) } returns flowOf(podcastTestData)
            every { getEpisodesByPodcastIdPagingUseCase(any()) } returns flowOf(PagingData.empty())

            val viewModel = createViewModel()
            val visibleEpisodes = episodeTestDataList.take(5)
            val targetEpisode = visibleEpisodes[2] // index 2

            viewModel.sendAction(
                PodcastAction.PlayEpisode(
                    episode = targetEpisode,
                    visibleEpisodes = visibleEpisodes,
                )
            )

            val expectedPlaylist = visibleEpisodes.subList(0, 3).reversed()
            coVerify {
                playEpisodeUseCase(playEpisode = targetEpisode, episodes = expectedPlaylist)
            }
        }

    @Test
    fun `Given PlayEpisode action with episode NOT in visibleEpisodes, When sent, Then playEpisodeUseCase is invoked with full list`() =
        runTest {
            every { getPodcastUseCase(1L) } returns flowOf(podcastTestData)
            every { getEpisodesByPodcastIdPagingUseCase(any()) } returns flowOf(PagingData.empty())

            val viewModel = createViewModel()
            val visibleEpisodes = episodeTestDataList.take(3)
            val unknownEpisode = episodeTestDataList[5] // not in visibleEpisodes

            viewModel.sendAction(
                PodcastAction.PlayEpisode(
                    episode = unknownEpisode,
                    visibleEpisodes = visibleEpisodes,
                )
            )

            coVerify {
                playEpisodeUseCase(playEpisode = unknownEpisode, episodes = visibleEpisodes)
            }
        }

    @Test
    fun `Given rapid ToggleFollowed actions where first call is slow, When sent, Then effects are emitted in the order they were sent`() =
        runTest {
            every { getPodcastUseCase(1L) } returns flowOf(podcastTestData)
            every { getEpisodesByPodcastIdPagingUseCase(any()) } returns flowOf(PagingData.empty())

            // 첫 호출만 느리게(100ms) 끝나고 둘째 호출은 즉시 끝나도록 만든다. followMutex 가
            // 없으면 둘째 launch 가 먼저 use case 를 완료해 이펙트 순서가 뒤바뀐다.
            var callCount = 0
            coEvery { toggleFollowedUseCase(1L) } coAnswers {
                callCount++
                if (callCount == 1) {
                    delay(100)
                    true
                } else {
                    false
                }
            }

            val viewModel = createViewModel()

            viewModel.effect.test {
                viewModel.sendAction(PodcastAction.ToggleFollowed)
                viewModel.sendAction(PodcastAction.ToggleFollowed)

                assertEquals(PodcastEffect.ShowFollowSnackbar(true), awaitItem())
                assertEquals(PodcastEffect.ShowFollowSnackbar(false), awaitItem())
            }

            coVerify(exactly = 2) { toggleFollowedUseCase(1L) }
        }

    @Test
    fun `Given ToggleLikedEpisode action, When sent, Then toggleLikedEpisodeUseCase is invoked`() =
        runTest {
            every { getPodcastUseCase(1L) } returns flowOf(podcastTestData)
            every { getEpisodesByPodcastIdPagingUseCase(any()) } returns flowOf(PagingData.empty())

            val viewModel = createViewModel()
            val episode = episodeTestDataList.first()

            viewModel.sendAction(PodcastAction.ToggleLikedEpisode(episode))

            coVerify { toggleLikedEpisodeUseCase(episode) }
        }
}
