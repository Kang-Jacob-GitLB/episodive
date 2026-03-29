package io.jacob.episodive.feature.search

import app.cash.turbine.test
import io.jacob.episodive.core.domain.usecase.episode.GetEpisodeByIdUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetRecentEpisodesUseCase
import io.jacob.episodive.core.domain.usecase.episode.ToggleLikedEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.player.PlayEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetTrendingPodcastsUseCase
import io.jacob.episodive.core.domain.usecase.search.ClearRecentSearchesUseCase
import io.jacob.episodive.core.domain.usecase.search.DeleteRecentSearchUseCase
import io.jacob.episodive.core.domain.usecase.search.GetRecentSearchesUseCase
import io.jacob.episodive.core.domain.usecase.search.SearchUseCase
import io.jacob.episodive.core.domain.usecase.search.UpsertRecentSearchUseCase
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.RecentSearch
import io.jacob.episodive.core.model.SearchResult
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.podcastTestData
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

class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val searchUseCase = mockk<SearchUseCase>(relaxed = true)
    private val getRecentEpisodesUseCase = mockk<GetRecentEpisodesUseCase>(relaxed = true)
    private val getTrendingPodcastsUseCase = mockk<GetTrendingPodcastsUseCase>(relaxed = true)
    private val playEpisodeUseCase = mockk<PlayEpisodeUseCase>(relaxed = true)
    private val toggleLikedEpisodeUseCase = mockk<ToggleLikedEpisodeUseCase>(relaxed = true)
    private val getEpisodeByIdUseCase = mockk<GetEpisodeByIdUseCase>(relaxed = true)
    private val getRecentSearchesUseCase = mockk<GetRecentSearchesUseCase>(relaxed = true)
    private val upsertRecentSearchUseCase = mockk<UpsertRecentSearchUseCase>(relaxed = true)
    private val deleteRecentSearchUseCase = mockk<DeleteRecentSearchUseCase>(relaxed = true)
    private val clearRecentSearchesUseCase = mockk<ClearRecentSearchesUseCase>(relaxed = true)

    private fun createViewModel(): SearchViewModel {
        return SearchViewModel(
            searchUseCase = searchUseCase,
            getRecentEpisodesUseCase = getRecentEpisodesUseCase,
            getTrendingPodcastsUseCase = getTrendingPodcastsUseCase,
            playEpisodeUseCase = playEpisodeUseCase,
            toggleLikedEpisodeUseCase = toggleLikedEpisodeUseCase,
            getEpisodeByIdUseCase = getEpisodeByIdUseCase,
            getRecentSearchesUseCase = getRecentSearchesUseCase,
            upsertRecentSearchUseCase = upsertRecentSearchUseCase,
            deleteRecentSearchUseCase = deleteRecentSearchUseCase,
            clearRecentSearchesUseCase = clearRecentSearchesUseCase,
        )
    }

    @After
    fun teardown() {
        confirmVerified(
            playEpisodeUseCase,
            toggleLikedEpisodeUseCase,
            upsertRecentSearchUseCase,
            deleteRecentSearchUseCase,
            clearRecentSearchesUseCase,
        )
    }

    @Test
    fun `Given no emissions, When ViewModel is created, Then initial state is Loading`() = runTest {
        every { getRecentSearchesUseCase(any()) } returns flowOf()
        every { getRecentEpisodesUseCase(any()) } returns flowOf()
        every { getTrendingPodcastsUseCase(any()) } returns flowOf()

        val viewModel = createViewModel()

        assertEquals(SearchState.Loading, viewModel.state.value)
    }

    @Test
    fun `Given all flows emit, When collecting, Then state is Success with all fields`() = runTest {
        val recentSearches = listOf(
            RecentSearch.Query(id = 1, query = "kotlin", searchedAt = kotlin.time.Clock.System.now()),
            RecentSearch.Query(id = 2, query = "android", searchedAt = kotlin.time.Clock.System.now()),
        )
        val recentEpisodes = episodeTestDataList.take(3)
        val trendingPodcasts = podcastTestDataList.take(3)

        every { getRecentSearchesUseCase(100) } returns flowOf(recentSearches)
        every { getRecentEpisodesUseCase(max = 6) } returns flowOf(recentEpisodes)
        every { getTrendingPodcastsUseCase(max = 10) } returns flowOf(trendingPodcasts)

        val viewModel = createViewModel()

        viewModel.state.test {
            // Initial state is Loading
            assertEquals(SearchState.Loading, awaitItem())
            // Advance past debounce(500L) on _searchResult after subscription starts upstream
            mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(600)
            val state = awaitItem()
            assertTrue(state is SearchState.Success)
            val success = state as SearchState.Success
            assertEquals("", success.searchQuery)
            assertEquals(recentSearches, success.recentSearches)
            assertEquals(Category.entries.toList(), success.categories)
            assertEquals(recentEpisodes, success.recentEpisodes)
            assertEquals(trendingPodcasts, success.trendingPodcasts)
        }
    }

    @Test
    fun `Given flow throws, When collecting, Then state is Error`() = runTest {
        every { getRecentSearchesUseCase(any()) } returns kotlinx.coroutines.flow.flow {
            throw RuntimeException("Error")
        }
        every { getRecentEpisodesUseCase(any()) } returns flowOf(emptyList())
        every { getTrendingPodcastsUseCase(any()) } returns flowOf(emptyList())

        val viewModel = createViewModel()

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state is SearchState.Error)
        }
    }

    @Test
    fun `Given ClickSearch action, When sent, Then searchQuery is set and upsertRecentSearchUseCase is invoked`() =
        runTest {
            every { getRecentSearchesUseCase(any()) } returns flowOf(emptyList())
            every { getRecentEpisodesUseCase(any()) } returns flowOf(emptyList())
            every { getTrendingPodcastsUseCase(any()) } returns flowOf(emptyList())

            val viewModel = createViewModel()

            viewModel.sendAction(SearchAction.ClickSearch("test"))

            coVerify { upsertRecentSearchUseCase("test") }
        }

    @Test
    fun `Given ClearQuery action, When sent, Then searchQuery becomes empty`() = runTest {
        every { getRecentSearchesUseCase(any()) } returns flowOf(emptyList())
        every { getRecentEpisodesUseCase(any()) } returns flowOf(emptyList())
        every { getTrendingPodcastsUseCase(any()) } returns flowOf(emptyList())

        val viewModel = createViewModel()

        // First set a query via QueryChanged (doesn't trigger upsertRecentSearchUseCase)
        viewModel.sendAction(SearchAction.QueryChanged("test"))
        // Then clear it
        viewModel.sendAction(SearchAction.ClearQuery)

        viewModel.state.test {
            assertEquals(SearchState.Loading, awaitItem())
            // Advance past debounce(500L) on _searchResult
            mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(600)
            val state = awaitItem()
            assertTrue(state is SearchState.Success)
            assertEquals("", (state as SearchState.Success).searchQuery)
        }
    }

    @Test
    fun `Given ClickRecentSearch action, When sent, Then upsertRecentSearchUseCase is invoked`() =
        runTest {
            every { getRecentSearchesUseCase(any()) } returns flowOf(emptyList())
            every { getRecentEpisodesUseCase(any()) } returns flowOf(emptyList())
            every { getTrendingPodcastsUseCase(any()) } returns flowOf(emptyList())

            val viewModel = createViewModel()

            val recentSearch = RecentSearch.Query(id = 1, query = "query", searchedAt = kotlin.time.Clock.System.now())
            viewModel.sendAction(SearchAction.ClickRecentSearch(recentSearch))

            coVerify { upsertRecentSearchUseCase("query") }
        }

    @Test
    fun `Given RemoveRecentSearch action, When sent, Then deleteRecentSearchUseCase is invoked`() =
        runTest {
            every { getRecentSearchesUseCase(any()) } returns flowOf(emptyList())
            every { getRecentEpisodesUseCase(any()) } returns flowOf(emptyList())
            every { getTrendingPodcastsUseCase(any()) } returns flowOf(emptyList())

            val viewModel = createViewModel()

            val recentSearch = RecentSearch.Query(id = 1, query = "query", searchedAt = kotlin.time.Clock.System.now())
            viewModel.sendAction(SearchAction.RemoveRecentSearch(recentSearch))

            coVerify { deleteRecentSearchUseCase(recentSearch) }
        }

    @Test
    fun `Given ClearRecentSearches action, When sent, Then clearRecentSearchesUseCase is invoked`() =
        runTest {
            every { getRecentSearchesUseCase(any()) } returns flowOf(emptyList())
            every { getRecentEpisodesUseCase(any()) } returns flowOf(emptyList())
            every { getTrendingPodcastsUseCase(any()) } returns flowOf(emptyList())

            val viewModel = createViewModel()

            viewModel.sendAction(SearchAction.ClearRecentSearches)

            coVerify { clearRecentSearchesUseCase() }
        }

    @Test
    fun `Given ClickCategory action, When sent, Then NavigateToCategory effect is emitted`() =
        runTest {
            every { getRecentSearchesUseCase(any()) } returns flowOf(emptyList())
            every { getRecentEpisodesUseCase(any()) } returns flowOf(emptyList())
            every { getTrendingPodcastsUseCase(any()) } returns flowOf(emptyList())

            val viewModel = createViewModel()

            viewModel.effect.test {
                viewModel.sendAction(SearchAction.ClickCategory(Category.BUSINESS))
                assertEquals(SearchEffect.NavigateToCategory(Category.BUSINESS), awaitItem())
            }
        }

    @Test
    fun `Given ClickPodcast action, When sent, Then NavigateToPodcast effect is emitted`() =
        runTest {
            every { getRecentSearchesUseCase(any()) } returns flowOf(emptyList())
            every { getRecentEpisodesUseCase(any()) } returns flowOf(emptyList())
            every { getTrendingPodcastsUseCase(any()) } returns flowOf(emptyList())

            val viewModel = createViewModel()
            val podcast = podcastTestData

            viewModel.effect.test {
                viewModel.sendAction(SearchAction.ClickPodcast(podcast))
                assertEquals(SearchEffect.NavigateToPodcast(podcast.id), awaitItem())
            }

            coVerify { upsertRecentSearchUseCase(podcast) }
        }

    @Test
    fun `Given ClickEpisode action, When sent, Then playEpisodeUseCase is invoked`() = runTest {
        every { getRecentSearchesUseCase(any()) } returns flowOf(emptyList())
        every { getRecentEpisodesUseCase(any()) } returns flowOf(emptyList())
        every { getTrendingPodcastsUseCase(any()) } returns flowOf(emptyList())

        val viewModel = createViewModel()
        val episode = episodeTestDataList.first()

        viewModel.sendAction(SearchAction.ClickEpisode(episode))

        coVerify { upsertRecentSearchUseCase(episode) }
        coVerify { playEpisodeUseCase(episode) }
    }

    @Test
    fun `Given ToggleLikedEpisode action, When sent, Then toggleLikedEpisodeUseCase is invoked`() =
        runTest {
            every { getRecentSearchesUseCase(any()) } returns flowOf(emptyList())
            every { getRecentEpisodesUseCase(any()) } returns flowOf(emptyList())
            every { getTrendingPodcastsUseCase(any()) } returns flowOf(emptyList())

            val viewModel = createViewModel()
            val episode = episodeTestDataList.first()

            viewModel.sendAction(SearchAction.ToggleLikedEpisode(episode))

            coVerify { toggleLikedEpisodeUseCase(episode) }
        }
}
