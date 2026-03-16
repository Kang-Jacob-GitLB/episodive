package io.jacob.episodive.feature.home

import app.cash.turbine.test
import io.jacob.episodive.core.domain.usecase.channel.GetChannelsUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetLiveEpisodesUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetMyRandomEpisodesUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetPlayingEpisodesUseCase
import io.jacob.episodive.core.domain.usecase.episode.ToggleLikedEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.player.PlayEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.player.ResumeEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetFollowedPodcastsUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetForeignTrendingPodcastsUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetLocalTrendingPodcastsUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetUserRecentPodcastsUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetUserTrendingPodcastsUseCase
import io.jacob.episodive.core.testing.model.channelTestDataList
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.podcastTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getPlayingEpisodesUseCase = mockk<GetPlayingEpisodesUseCase>(relaxed = true)
    private val getUserRecentPodcastsUseCase = mockk<GetUserRecentPodcastsUseCase>(relaxed = true)
    private val getMyRandomEpisodesUseCase = mockk<GetMyRandomEpisodesUseCase>(relaxed = true)
    private val getUserTrendingPodcastsUseCase =
        mockk<GetUserTrendingPodcastsUseCase>(relaxed = true)
    private val getFollowedPodcastsUseCase = mockk<GetFollowedPodcastsUseCase>(relaxed = true)
    private val getLocalTrendingPodcastsUseCase =
        mockk<GetLocalTrendingPodcastsUseCase>(relaxed = true)
    private val getForeignTrendingPodcastsUseCase =
        mockk<GetForeignTrendingPodcastsUseCase>(relaxed = true)
    private val getLiveEpisodesUseCase = mockk<GetLiveEpisodesUseCase>(relaxed = true)
    private val getChannelsUseCase = mockk<GetChannelsUseCase>(relaxed = true)
    private val playEpisodeUseCase = mockk<PlayEpisodeUseCase>(relaxed = true)
    private val resumeEpisodeUseCase = mockk<ResumeEpisodeUseCase>(relaxed = true)
    private val toggleLikedEpisodeUseCase = mockk<ToggleLikedEpisodeUseCase>(relaxed = true)

    private fun setupDefaultMocks() {
        every { getPlayingEpisodesUseCase(max = any()) } returns flowOf(emptyList())
        every { getUserRecentPodcastsUseCase(max = any()) } returns flowOf(emptyList())
        every { getMyRandomEpisodesUseCase(max = any()) } returns flowOf(emptyList())
        every { getUserTrendingPodcastsUseCase(max = any()) } returns flowOf(emptyList())
        every { getFollowedPodcastsUseCase(max = any()) } returns flowOf(emptyList())
        every { getLocalTrendingPodcastsUseCase(max = any()) } returns flowOf(emptyList())
        every { getForeignTrendingPodcastsUseCase(max = any()) } returns flowOf(emptyList())
        every { getLiveEpisodesUseCase(max = any()) } returns flowOf(emptyList())
        every { getChannelsUseCase() } returns flowOf(emptyList())
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(
            getPlayingEpisodesUseCase = getPlayingEpisodesUseCase,
            getUserRecentPodcastsUseCase = getUserRecentPodcastsUseCase,
            getMyRandomEpisodesUseCase = getMyRandomEpisodesUseCase,
            getUserTrendingPodcastsUseCase = getUserTrendingPodcastsUseCase,
            getFollowedPodcastsUseCase = getFollowedPodcastsUseCase,
            getLocalTrendingPodcastsUseCase = getLocalTrendingPodcastsUseCase,
            getForeignTrendingPodcastsUseCase = getForeignTrendingPodcastsUseCase,
            getLiveEpisodesUseCase = getLiveEpisodesUseCase,
            getChannelsUseCase = getChannelsUseCase,
            playEpisodeUseCase = playEpisodeUseCase,
            resumeEpisodeUseCase = resumeEpisodeUseCase,
            toggleLikedEpisodeUseCase = toggleLikedEpisodeUseCase,
        )
    }

    @After
    fun teardown() {
        confirmVerified(playEpisodeUseCase, resumeEpisodeUseCase, toggleLikedEpisodeUseCase)
    }

    @Test
    fun `Given no emissions, When ViewModel is created, Then initial state is Loading`() = runTest {
        every { getPlayingEpisodesUseCase(max = any()) } returns flowOf()
        every { getUserRecentPodcastsUseCase(max = any()) } returns flowOf()
        every { getMyRandomEpisodesUseCase(max = any()) } returns flowOf()
        every { getUserTrendingPodcastsUseCase(max = any()) } returns flowOf()
        every { getFollowedPodcastsUseCase(max = any()) } returns flowOf()
        every { getLocalTrendingPodcastsUseCase(max = any()) } returns flowOf()
        every { getForeignTrendingPodcastsUseCase(max = any()) } returns flowOf()
        every { getLiveEpisodesUseCase(max = any()) } returns flowOf()
        every { getChannelsUseCase() } returns flowOf()

        val viewModel = createViewModel()

        assertEquals(HomeState.Loading, viewModel.state.value)
    }

    @Test
    fun `Given all flows emit data, When collecting, Then state is Success with all fields`() =
        runTest {
            val playingEpisodes = episodeTestDataList.take(2)
            val recentPodcasts = podcastTestDataList.take(2)
            val randomEpisodes = episodeTestDataList.take(3)
            val trendingPodcasts = podcastTestDataList.take(2)
            val followedPodcasts = podcastTestDataList.take(1)
            val localTrending = podcastTestDataList.take(2)
            val foreignTrending = podcastTestDataList.take(2)
            val liveEpisodes = episodeTestDataList.take(1)
            val channels = channelTestDataList.take(2)

            every { getPlayingEpisodesUseCase(max = any()) } returns flowOf(playingEpisodes)
            every { getUserRecentPodcastsUseCase(max = any()) } returns flowOf(recentPodcasts)
            every { getMyRandomEpisodesUseCase(max = any()) } returns flowOf(randomEpisodes)
            every { getUserTrendingPodcastsUseCase(max = any()) } returns flowOf(trendingPodcasts)
            every { getFollowedPodcastsUseCase(max = any()) } returns flowOf(followedPodcasts)
            every { getLocalTrendingPodcastsUseCase(max = any()) } returns flowOf(localTrending)
            every { getForeignTrendingPodcastsUseCase(max = any()) } returns flowOf(foreignTrending)
            every { getLiveEpisodesUseCase(max = any()) } returns flowOf(liveEpisodes)
            every { getChannelsUseCase() } returns flowOf(channels)

            val viewModel = createViewModel()

            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state is HomeState.Success)
                val success = state as HomeState.Success
                assertEquals(playingEpisodes, success.playingEpisodes)
                assertEquals(recentPodcasts, success.userRecentPodcasts)
                assertEquals(randomEpisodes, success.randomEpisodes)
                assertEquals(trendingPodcasts, success.userTrendingPodcasts)
                assertEquals(followedPodcasts, success.followedPodcasts)
                assertEquals(localTrending, success.localTrendingPodcasts)
                assertEquals(foreignTrending, success.foreignTrendingPodcasts)
                assertEquals(liveEpisodes, success.liveEpisodes)
                assertEquals(channels, success.channels)
            }
        }

    @Test
    fun `Given all flows emit empty, When collecting, Then state is Success with empty lists`() =
        runTest {
            setupDefaultMocks()

            val viewModel = createViewModel()

            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state is HomeState.Success)
                val success = state as HomeState.Success
                assertTrue(success.playingEpisodes.isEmpty())
                assertTrue(success.userRecentPodcasts.isEmpty())
                assertTrue(success.channels.isEmpty())
            }
        }

    @Test
    fun `Given one flow throws, When collecting, Then state is Error`() = runTest {
        every { getPlayingEpisodesUseCase(max = any()) } returns kotlinx.coroutines.flow.flow {
            throw RuntimeException("Error")
        }
        every { getUserRecentPodcastsUseCase(max = any()) } returns flowOf(emptyList())
        every { getMyRandomEpisodesUseCase(max = any()) } returns flowOf(emptyList())
        every { getUserTrendingPodcastsUseCase(max = any()) } returns flowOf(emptyList())
        every { getFollowedPodcastsUseCase(max = any()) } returns flowOf(emptyList())
        every { getLocalTrendingPodcastsUseCase(max = any()) } returns flowOf(emptyList())
        every { getForeignTrendingPodcastsUseCase(max = any()) } returns flowOf(emptyList())
        every { getLiveEpisodesUseCase(max = any()) } returns flowOf(emptyList())
        every { getChannelsUseCase() } returns flowOf(emptyList())

        val viewModel = createViewModel()

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state is HomeState.Error)
        }
    }

    @Test
    fun `Given PlayEpisode action, When sent, Then playEpisodeUseCase is invoked`() = runTest {
        setupDefaultMocks()
        val viewModel = createViewModel()
        val episode = episodeTestData

        viewModel.sendAction(HomeAction.PlayEpisode(episode))

        coVerify { playEpisodeUseCase(episode) }
    }

    @Test
    fun `Given ResumeEpisode action, When sent, Then resumeEpisodeUseCase is invoked`() = runTest {
        setupDefaultMocks()
        val viewModel = createViewModel()
        val episode = episodeTestData

        viewModel.sendAction(HomeAction.ResumeEpisode(episode))

        coVerify { resumeEpisodeUseCase(episode) }
    }

    @Test
    fun `Given ToggleLikedEpisode action, When sent, Then toggleLikedEpisodeUseCase is invoked`() =
        runTest {
            setupDefaultMocks()
            val viewModel = createViewModel()
            val episode = episodeTestData

            viewModel.sendAction(HomeAction.ToggleLikedEpisode(episode))

            coVerify { toggleLikedEpisodeUseCase(episode) }
        }

    @Test
    fun `Given ClickPodcast action, When sent, Then NavigateToPodcast effect is emitted`() =
        runTest {
            setupDefaultMocks()
            val viewModel = createViewModel()

            viewModel.effect.test {
                viewModel.sendAction(HomeAction.ClickPodcast(42L))
                assertEquals(HomeEffect.NavigateToPodcast(42L), awaitItem())
            }
        }

    @Test
    fun `Given ClickChannel action, When sent, Then NavigateToChannel effect is emitted`() =
        runTest {
            setupDefaultMocks()
            val viewModel = createViewModel()

            viewModel.effect.test {
                viewModel.sendAction(HomeAction.ClickChannel(1L))
                assertEquals(HomeEffect.NavigateToChannel(1L), awaitItem())
            }
        }
}
