package io.jacob.episodive.feature.library

import app.cash.turbine.test
import io.jacob.episodive.core.domain.usecase.FindInLibraryUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetAllPlayedEpisodesPagingUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetAllPlayedEpisodesUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetLikedEpisodesPagingUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetLikedEpisodesUseCase
import io.jacob.episodive.core.domain.usecase.episode.ToggleLikedEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.player.PlayEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.player.ResumeEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetFollowedPodcastsPagingUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetFollowedPodcastsUseCase
import io.jacob.episodive.core.domain.usecase.podcast.ToggleFollowedUseCase
import io.jacob.episodive.core.domain.usecase.user.GetPreferredCategoriesUseCase
import io.jacob.episodive.core.domain.usecase.user.GetSelectableCategoriesUseCase
import io.jacob.episodive.core.domain.usecase.user.ToggleCategoryUseCase
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.LibraryFindResult
import io.jacob.episodive.core.model.SelectableCategory
import io.jacob.episodive.core.testing.model.episodeTestData
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

class LibraryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val findInLibraryUseCase = mockk<FindInLibraryUseCase>(relaxed = true)
    private val getAllPlayedEpisodesUseCase = mockk<GetAllPlayedEpisodesUseCase>(relaxed = true)
    private val getLikedEpisodesUseCase = mockk<GetLikedEpisodesUseCase>(relaxed = true)
    private val getFollowedPodcastsUseCase = mockk<GetFollowedPodcastsUseCase>(relaxed = true)
    private val getPreferredCategoriesUseCase =
        mockk<GetPreferredCategoriesUseCase>(relaxed = true)
    private val getSelectableCategoriesUseCase =
        mockk<GetSelectableCategoriesUseCase>(relaxed = true)
    private val getAllPlayedEpisodesPagingUseCase =
        mockk<GetAllPlayedEpisodesPagingUseCase>(relaxed = true)
    private val getLikedEpisodesPagingUseCase =
        mockk<GetLikedEpisodesPagingUseCase>(relaxed = true)
    private val getFollowedPodcastsPagingUseCase =
        mockk<GetFollowedPodcastsPagingUseCase>(relaxed = true)
    private val playEpisodeUseCase = mockk<PlayEpisodeUseCase>(relaxed = true)
    private val resumeEpisodeUseCase = mockk<ResumeEpisodeUseCase>(relaxed = true)
    private val toggleLikedEpisodeUseCase = mockk<ToggleLikedEpisodeUseCase>(relaxed = true)
    private val toggleFollowedUseCase = mockk<ToggleFollowedUseCase>(relaxed = true)
    private val toggleCategoryUseCase = mockk<ToggleCategoryUseCase>(relaxed = true)

    private fun setupDefaultMocks() {
        every { findInLibraryUseCase(any()) } returns flowOf(LibraryFindResult())
        every { getAllPlayedEpisodesUseCase(max = any()) } returns flowOf(emptyList())
        every { getLikedEpisodesUseCase(max = any()) } returns flowOf(emptyList())
        every { getFollowedPodcastsUseCase(max = any()) } returns flowOf(emptyList())
        every { getPreferredCategoriesUseCase() } returns flowOf(emptyList())
        every { getSelectableCategoriesUseCase() } returns flowOf(emptyList())
    }

    private fun createViewModel(): LibraryViewModel {
        return LibraryViewModel(
            findInLibraryUseCase = findInLibraryUseCase,
            getAllPlayedEpisodesUseCase = getAllPlayedEpisodesUseCase,
            getLikedEpisodesUseCase = getLikedEpisodesUseCase,
            getFollowedPodcastsUseCase = getFollowedPodcastsUseCase,
            getPreferredCategoriesUseCase = getPreferredCategoriesUseCase,
            getSelectableCategoriesUseCase = getSelectableCategoriesUseCase,
            getAllPlayedEpisodesPagingUseCase = getAllPlayedEpisodesPagingUseCase,
            getLikedEpisodesPagingUseCase = getLikedEpisodesPagingUseCase,
            getFollowedPodcastsPagingUseCase = getFollowedPodcastsPagingUseCase,
            playEpisodeUseCase = playEpisodeUseCase,
            resumeEpisodeUseCase = resumeEpisodeUseCase,
            toggleLikedEpisodeUseCase = toggleLikedEpisodeUseCase,
            toggleFollowedUseCase = toggleFollowedUseCase,
            toggleCategoryUseCase = toggleCategoryUseCase,
        )
    }

    @After
    fun teardown() {
        confirmVerified(
            playEpisodeUseCase,
            resumeEpisodeUseCase,
            toggleLikedEpisodeUseCase,
            toggleFollowedUseCase,
            toggleCategoryUseCase,
        )
    }

    @Test
    fun `Given no emissions, When ViewModel is created, Then initial state is Loading`() = runTest {
        every { getAllPlayedEpisodesUseCase(max = any()) } returns flowOf()
        every { getLikedEpisodesUseCase(max = any()) } returns flowOf()
        every { getFollowedPodcastsUseCase(max = any()) } returns flowOf()
        every { getPreferredCategoriesUseCase() } returns flowOf()
        every { getSelectableCategoriesUseCase() } returns flowOf()

        val viewModel = createViewModel()

        assertEquals(LibraryState.Loading, viewModel.state.value)
    }

    @Test
    fun `Given all flows emit with empty query, When collecting, Then state is Success`() =
        runTest {
            val playedEpisodes = episodeTestDataList.take(3)
            val likedEpisodes = episodeTestDataList.take(2)
            val followedPodcasts = podcastTestDataList.take(2)
            val preferredCategories = listOf(Category.BUSINESS)
            val selectableCategories = listOf(
                SelectableCategory(Category.BUSINESS, true),
                SelectableCategory(Category.COMEDY, false),
            )

            every { getAllPlayedEpisodesUseCase(max = any()) } returns flowOf(playedEpisodes)
            every { getLikedEpisodesUseCase(max = any()) } returns flowOf(likedEpisodes)
            every { getFollowedPodcastsUseCase(max = any()) } returns flowOf(followedPodcasts)
            every { getPreferredCategoriesUseCase() } returns flowOf(preferredCategories)
            every { getSelectableCategoriesUseCase() } returns flowOf(selectableCategories)

            val viewModel = createViewModel()

            viewModel.state.test {
                assertEquals(LibraryState.Loading, awaitItem())
                // Advance past debounce(500L) on _findResult after subscription starts upstream
                mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(600)
                val state = awaitItem()
                assertTrue(state is LibraryState.Success)
                val success = state as LibraryState.Success
                assertEquals("", success.findQuery)
                assertEquals(playedEpisodes, success.allPlayedEpisodes)
                assertEquals(likedEpisodes, success.likedEpisodes)
                assertEquals(followedPodcasts, success.followedPodcasts)
                assertEquals(preferredCategories, success.preferredCategories)
                assertEquals(selectableCategories, success.selectableCategories)
                assertEquals(LibrarySection.All, success.section)
            }
        }

    @Test
    fun `Given flow throws, When collecting, Then state is Error`() = runTest {
        every { getAllPlayedEpisodesUseCase(max = any()) } returns kotlinx.coroutines.flow.flow {
            throw RuntimeException("Error")
        }
        every { getLikedEpisodesUseCase(max = any()) } returns flowOf(emptyList())
        every { getFollowedPodcastsUseCase(max = any()) } returns flowOf(emptyList())
        every { getPreferredCategoriesUseCase() } returns flowOf(emptyList())
        every { getSelectableCategoriesUseCase() } returns flowOf(emptyList())

        val viewModel = createViewModel()

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state is LibraryState.Error)
        }
    }

    @Test
    fun `Given ClickFind action, When sent, Then findQuery updates`() = runTest {
        setupDefaultMocks()
        val viewModel = createViewModel()

        viewModel.sendAction(LibraryAction.ClickFind("query"))

        viewModel.state.test {
            assertEquals(LibraryState.Loading, awaitItem())
            // Advance past debounce(500L) on _findResult
            mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(600)
            val state = awaitItem()
            assertTrue(state is LibraryState.Success)
            assertEquals("query", (state as LibraryState.Success).findQuery)
        }
    }

    @Test
    fun `Given ClearQuery action, When sent, Then findQuery becomes empty`() = runTest {
        setupDefaultMocks()
        val viewModel = createViewModel()

        viewModel.sendAction(LibraryAction.ClickFind("query"))
        viewModel.sendAction(LibraryAction.ClearQuery)

        viewModel.state.test {
            assertEquals(LibraryState.Loading, awaitItem())
            // Advance past debounce(500L) on _findResult
            mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(600)
            val state = awaitItem()
            assertTrue(state is LibraryState.Success)
            assertEquals("", (state as LibraryState.Success).findQuery)
        }
    }

    @Test
    fun `Given ClickPlayingEpisode with isCompleted true, When sent, Then playEpisodeUseCase is invoked`() =
        runTest {
            setupDefaultMocks()
            val viewModel = createViewModel()
            val episode = episodeTestData.copy(isCompleted = true)

            viewModel.sendAction(LibraryAction.ClickPlayingEpisode(episode))

            coVerify { playEpisodeUseCase(episode) }
        }

    @Test
    fun `Given ClickPlayingEpisode with isCompleted false, When sent, Then resumeEpisodeUseCase is invoked`() =
        runTest {
            setupDefaultMocks()
            val viewModel = createViewModel()
            val episode = episodeTestData.copy(isCompleted = false)

            viewModel.sendAction(LibraryAction.ClickPlayingEpisode(episode))

            coVerify { resumeEpisodeUseCase(episode) }
        }

    @Test
    fun `Given ClickEpisode action, When sent, Then playEpisodeUseCase is invoked`() = runTest {
        setupDefaultMocks()
        val viewModel = createViewModel()
        val episode = episodeTestData

        viewModel.sendAction(LibraryAction.ClickEpisode(episode))

        coVerify { playEpisodeUseCase(episode) }
    }

    @Test
    fun `Given ClickPodcast action, When sent, Then NavigateToPodcast effect is emitted`() =
        runTest {
            setupDefaultMocks()
            val viewModel = createViewModel()
            val podcast = podcastTestData

            viewModel.effect.test {
                viewModel.sendAction(LibraryAction.ClickPodcast(podcast))
                assertEquals(LibraryEffect.NavigateToPodcast(podcast), awaitItem())
            }
        }

    @Test
    fun `Given ToggleLikedEpisode action, When sent, Then toggleLikedEpisodeUseCase is invoked`() =
        runTest {
            setupDefaultMocks()
            val viewModel = createViewModel()
            val episode = episodeTestData

            viewModel.sendAction(LibraryAction.ToggleLikedEpisode(episode))

            coVerify { toggleLikedEpisodeUseCase(episode) }
        }

    @Test
    fun `Given ToggleFollowedPodcast action, When sent, Then toggleFollowedUseCase is invoked`() =
        runTest {
            setupDefaultMocks()
            val viewModel = createViewModel()
            val podcast = podcastTestData

            viewModel.sendAction(LibraryAction.ToggleFollowedPodcast(podcast))

            coVerify { toggleFollowedUseCase(podcast.id) }
        }

    @Test
    fun `Given TogglePreferredCategory action, When sent, Then toggleCategoryUseCase is invoked`() =
        runTest {
            setupDefaultMocks()
            val viewModel = createViewModel()

            viewModel.sendAction(LibraryAction.TogglePreferredCategory(Category.BUSINESS))

            coVerify { toggleCategoryUseCase(Category.BUSINESS) }
        }

    @Test
    fun `Given QueryChanged action, When sent, Then findQuery updates after debounce`() = runTest {
        setupDefaultMocks()
        val viewModel = createViewModel()

        viewModel.sendAction(LibraryAction.QueryChanged("search"))

        viewModel.state.test {
            assertEquals(LibraryState.Loading, awaitItem())
            mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(600)
            val state = awaitItem()
            assertTrue(state is LibraryState.Success)
            assertEquals("search", (state as LibraryState.Success).findQuery)
        }
    }

    @Test
    fun `Given non-empty find result with query, When collecting, Then state uses find result data`() =
        runTest {
            val findResult = LibraryFindResult(
                playingEpisodes = episodeTestDataList.take(1),
                likedEpisodes = episodeTestDataList.drop(1).take(1),
                followedPodcasts = podcastTestDataList.take(1),
            )
            every { findInLibraryUseCase(any()) } returns flowOf(findResult)
            every { getAllPlayedEpisodesUseCase(max = any()) } returns flowOf(episodeTestDataList.take(3))
            every { getLikedEpisodesUseCase(max = any()) } returns flowOf(episodeTestDataList.take(2))
            every { getFollowedPodcastsUseCase(max = any()) } returns flowOf(podcastTestDataList.take(2))
            every { getPreferredCategoriesUseCase() } returns flowOf(listOf(Category.BUSINESS))
            every { getSelectableCategoriesUseCase() } returns flowOf(emptyList())

            val viewModel = createViewModel()
            viewModel.sendAction(LibraryAction.ClickFind("query"))

            viewModel.state.test {
                assertEquals(LibraryState.Loading, awaitItem())
                mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(600)
                val state = awaitItem()
                assertTrue(state is LibraryState.Success)
                val success = state as LibraryState.Success
                assertEquals(findResult.playingEpisodes, success.allPlayedEpisodes)
                assertEquals(findResult.likedEpisodes, success.likedEpisodes)
                assertEquals(findResult.followedPodcasts, success.followedPodcasts)
                assertEquals(emptyList<Category>(), success.preferredCategories)
            }
        }

    @Test
    fun `Given all flows emit empty data, When collecting, Then state is Success with empty collections`() =
        runTest {
            setupDefaultMocks()

            val viewModel = createViewModel()

            viewModel.state.test {
                assertEquals(LibraryState.Loading, awaitItem())
                mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(600)
                val state = awaitItem()
                assertTrue(state is LibraryState.Success)
                val success = state as LibraryState.Success
                assertEquals(emptyList<Any>(), success.allPlayedEpisodes)
                assertEquals(emptyList<Any>(), success.likedEpisodes)
                assertEquals(emptyList<Any>(), success.followedPodcasts)
                assertEquals(emptyList<Any>(), success.preferredCategories)
                assertEquals(emptyList<Any>(), success.selectableCategories)
            }
        }

    @Test
    fun `Given SelectSection action, When sent, Then section updates in state`() = runTest {
        setupDefaultMocks()
        val viewModel = createViewModel()

        viewModel.sendAction(LibraryAction.SelectSection(LibrarySection.Liked))

        viewModel.state.test {
            assertEquals(LibraryState.Loading, awaitItem())
            // Advance past debounce(500L) on _findResult
            mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(600)
            val state = awaitItem()
            assertTrue(state is LibraryState.Success)
            assertEquals(LibrarySection.Liked, (state as LibraryState.Success).section)
        }
    }
}
