package io.jacob.episodive.feature.library

import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import app.cash.turbine.test
import io.jacob.episodive.core.domain.usecase.FindInLibraryUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetAllPlayedEpisodesPagingUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetAllPlayedEpisodesUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetLikedEpisodesPagingUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetLikedEpisodesUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetSavedEpisodesPagingUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetSavedEpisodesUseCase
import io.jacob.episodive.core.domain.usecase.episode.SaveEpisodeUseCase
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
import kotlin.time.Instant
import androidx.lifecycle.viewModelScope
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.cancel
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
    private val getSavedEpisodesUseCase = mockk<GetSavedEpisodesUseCase>(relaxed = true)
    private val getSavedEpisodesPagingUseCase = mockk<GetSavedEpisodesPagingUseCase>(relaxed = true)
    private val saveEpisodeUseCase = mockk<SaveEpisodeUseCase>(relaxed = true)

    private fun setupDefaultMocks() {
        every { findInLibraryUseCase(any()) } returns flowOf(LibraryFindResult())
        every { getAllPlayedEpisodesUseCase(max = any()) } returns flowOf(emptyList())
        every { getLikedEpisodesUseCase(max = any()) } returns flowOf(emptyList())
        every { getFollowedPodcastsUseCase(max = any()) } returns flowOf(emptyList())
        every { getPreferredCategoriesUseCase() } returns flowOf(emptyList())
        every { getSelectableCategoriesUseCase() } returns flowOf(emptyList())
        every { getSavedEpisodesUseCase(max = any()) } returns flowOf(emptyList())
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
            getSavedEpisodesUseCase = getSavedEpisodesUseCase,
            getSavedEpisodesPagingUseCase = getSavedEpisodesPagingUseCase,
            saveEpisodeUseCase = saveEpisodeUseCase,
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
            saveEpisodeUseCase,
        )
    }

    @Test
    fun `Given no emissions, When ViewModel is created, Then initial state is Loading`() = runTest {
        every { getAllPlayedEpisodesUseCase(max = any()) } returns flowOf()
        every { getLikedEpisodesUseCase(max = any()) } returns flowOf()
        every { getSavedEpisodesUseCase(max = any()) } returns flowOf()
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
            every { getSavedEpisodesUseCase(max = any()) } returns flowOf(emptyList())
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
        every { getSavedEpisodesUseCase(max = any()) } returns flowOf(emptyList())
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
                assertEquals(LibraryEffect.NavigateToPodcast(podcast.id), awaitItem())
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
            every { getSavedEpisodesUseCase(max = any()) } returns flowOf(emptyList())
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

    @Test
    fun `Given SelectSection Followed, When sent, Then section updates to Followed`() = runTest {
        setupDefaultMocks()
        val viewModel = createViewModel()

        viewModel.sendAction(LibraryAction.SelectSection(LibrarySection.Followed))

        viewModel.state.test {
            assertEquals(LibraryState.Loading, awaitItem())
            mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(600)
            val state = awaitItem()
            assertTrue(state is LibraryState.Success)
            assertEquals(LibrarySection.Followed, (state as LibraryState.Success).section)
        }
    }

    @Test
    fun `Given SelectSection RecentlyListened, When sent, Then section updates to RecentlyListened`() = runTest {
        setupDefaultMocks()
        val viewModel = createViewModel()

        viewModel.sendAction(LibraryAction.SelectSection(LibrarySection.RecentlyListened))

        viewModel.state.test {
            assertEquals(LibraryState.Loading, awaitItem())
            mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(600)
            val state = awaitItem()
            assertTrue(state is LibraryState.Success)
            assertEquals(LibrarySection.RecentlyListened, (state as LibraryState.Success).section)
        }
    }

    @Test
    fun `Given SelectSection Preferred, When sent, Then section updates to Preferred`() = runTest {
        setupDefaultMocks()
        val viewModel = createViewModel()

        viewModel.sendAction(LibraryAction.SelectSection(LibrarySection.Preferred))

        viewModel.state.test {
            assertEquals(LibraryState.Loading, awaitItem())
            mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(600)
            val state = awaitItem()
            assertTrue(state is LibraryState.Success)
            assertEquals(LibrarySection.Preferred, (state as LibraryState.Success).section)
        }
    }

    @Test
    fun `Given ToggleSavedEpisode action, When sent, Then saveEpisodeUseCase is invoked`() =
        runTest {
            setupDefaultMocks()
            val viewModel = createViewModel()
            val episode = episodeTestData

            viewModel.sendAction(LibraryAction.ToggleSavedEpisode(episode))

            coVerify { saveEpisodeUseCase(episode) }
        }

    @Test
    fun `Given saved episodes exist, When collecting, Then state includes saved episodes`() =
        runTest {
            val savedEpisodes = episodeTestDataList.take(2)
            every { getAllPlayedEpisodesUseCase(max = any()) } returns flowOf(emptyList())
            every { getLikedEpisodesUseCase(max = any()) } returns flowOf(emptyList())
            every { getSavedEpisodesUseCase(max = any()) } returns flowOf(savedEpisodes)
            every { getFollowedPodcastsUseCase(max = any()) } returns flowOf(emptyList())
            every { getPreferredCategoriesUseCase() } returns flowOf(emptyList())
            every { getSelectableCategoriesUseCase() } returns flowOf(emptyList())

            val viewModel = createViewModel()

            viewModel.state.test {
                assertEquals(LibraryState.Loading, awaitItem())
                mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(600)
                val state = awaitItem()
                assertTrue(state is LibraryState.Success)
                assertEquals(savedEpisodes, (state as LibraryState.Success).savedEpisodes)
            }
        }

    @Test
    fun `Given SelectSection Saved, When sent, Then section updates to Saved`() = runTest {
        setupDefaultMocks()
        val viewModel = createViewModel()

        viewModel.sendAction(LibraryAction.SelectSection(LibrarySection.Saved))

        viewModel.state.test {
            assertEquals(LibraryState.Loading, awaitItem())
            mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(600)
            val state = awaitItem()
            assertTrue(state is LibraryState.Success)
            assertEquals(LibrarySection.Saved, (state as LibraryState.Success).section)
        }
    }

    @Test
    fun `Given played episodes with different dates, When collecting paging, Then separators are inserted`() =
        runTest {
            // Given - episodes with different playedAt dates
            val episode1 = episodeTestData.copy(
                id = 1L,
                playedAt = Instant.fromEpochSeconds(1700000000L),
            )
            val episode2 = episodeTestData.copy(
                id = 2L,
                playedAt = Instant.fromEpochSeconds(1700000000L),
            )
            val episode3 = episodeTestData.copy(
                id = 3L,
                playedAt = Instant.fromEpochSeconds(1690000000L),
            )

            every { getAllPlayedEpisodesPagingUseCase() } returns flowOf(
                PagingData.from(listOf(episode1, episode2, episode3))
            )
            setupDefaultMocks()

            val viewModel = createViewModel()

            // When
            val result = viewModel.playedEpisodesPaging.asSnapshot()

            // Then - expect separator before episode1, no separator between episode1 & episode2 (same date), separator before episode3
            assertTrue(result.isNotEmpty())
            assertTrue(result[0] is SeparatedUiModel.Separator)
            assertTrue(result[1] is SeparatedUiModel.Content)
            assertEquals(episode1, (result[1] as SeparatedUiModel.Content).data)
            // No separator between same-date episodes
            assertTrue(result[2] is SeparatedUiModel.Content)
            assertEquals(episode2, (result[2] as SeparatedUiModel.Content).data)
            // Separator before different-date episode
            assertTrue(result[3] is SeparatedUiModel.Separator)
            assertTrue(result[4] is SeparatedUiModel.Content)
            assertEquals(episode3, (result[4] as SeparatedUiModel.Content).data)

            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `Given liked episodes with different dates, When collecting paging, Then separators are inserted`() =
        runTest {
            val episode1 = episodeTestData.copy(
                id = 1L,
                likedAt = Instant.fromEpochSeconds(1700000000L),
            )
            val episode2 = episodeTestData.copy(
                id = 2L,
                likedAt = Instant.fromEpochSeconds(1690000000L),
            )

            every { getLikedEpisodesPagingUseCase() } returns flowOf(
                PagingData.from(listOf(episode1, episode2))
            )
            setupDefaultMocks()

            val viewModel = createViewModel()

            val result = viewModel.likedEpisodesPaging.asSnapshot()

            assertTrue(result.isNotEmpty())
            assertTrue(result[0] is SeparatedUiModel.Separator)
            assertTrue(result[1] is SeparatedUiModel.Content)
            assertEquals(episode1, (result[1] as SeparatedUiModel.Content).data)

            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `Given saved episodes with different dates, When collecting paging, Then separators are inserted`() =
        runTest {
            val episode1 = episodeTestData.copy(
                id = 1L,
                savedAt = Instant.fromEpochSeconds(1700000000L),
            )
            val episode2 = episodeTestData.copy(
                id = 2L,
                savedAt = Instant.fromEpochSeconds(1690000000L),
            )

            every { getSavedEpisodesPagingUseCase() } returns flowOf(
                PagingData.from(listOf(episode1, episode2))
            )
            setupDefaultMocks()

            val viewModel = createViewModel()

            val result = viewModel.savedEpisodesPaging.asSnapshot()

            assertTrue(result.isNotEmpty())
            assertTrue(result[0] is SeparatedUiModel.Separator)
            assertTrue(result[1] is SeparatedUiModel.Content)
            assertEquals(episode1, (result[1] as SeparatedUiModel.Content).data)

            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `Given followed podcasts with different dates, When collecting paging, Then separators are inserted`() =
        runTest {
            val podcast1 = podcastTestData.copy(
                id = 1L,
                followedAt = Instant.fromEpochSeconds(1700000000L),
            )
            val podcast2 = podcastTestData.copy(
                id = 2L,
                followedAt = Instant.fromEpochSeconds(1690000000L),
            )

            every { getFollowedPodcastsPagingUseCase() } returns flowOf(
                PagingData.from(listOf(podcast1, podcast2))
            )
            setupDefaultMocks()

            val viewModel = createViewModel()

            val result = viewModel.followedPodcastsPaging.asSnapshot()

            assertTrue(result.isNotEmpty())
            assertTrue(result[0] is SeparatedUiModel.Separator)
            assertTrue(result[1] is SeparatedUiModel.Content)
            assertEquals(podcast1, (result[1] as SeparatedUiModel.Content).data)

            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `Given played episodes with same date, When collecting paging, Then no separator between them`() =
        runTest {
            val sameDate = Instant.fromEpochSeconds(1700000000L)
            val episode1 = episodeTestData.copy(id = 1L, playedAt = sameDate)
            val episode2 = episodeTestData.copy(id = 2L, playedAt = sameDate)

            every { getAllPlayedEpisodesPagingUseCase() } returns flowOf(
                PagingData.from(listOf(episode1, episode2))
            )
            setupDefaultMocks()

            val viewModel = createViewModel()

            val result = viewModel.playedEpisodesPaging.asSnapshot()

            // Expect: Separator, Content(episode1), Content(episode2) - no separator between same dates
            assertEquals(3, result.size)
            assertTrue(result[0] is SeparatedUiModel.Separator)
            assertTrue(result[1] is SeparatedUiModel.Content)
            assertTrue(result[2] is SeparatedUiModel.Content)

            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `Given empty played episodes, When collecting paging, Then no items emitted`() =
        runTest {
            every { getAllPlayedEpisodesPagingUseCase() } returns flowOf(PagingData.from(emptyList()))
            setupDefaultMocks()

            val viewModel = createViewModel()

            val result = viewModel.playedEpisodesPaging.asSnapshot()

            assertTrue(result.isEmpty())

            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `Given find query with results, When collecting, Then state uses find result data`() =
        runTest {
            val findResult = LibraryFindResult(
                playingEpisodes = episodeTestDataList.take(1),
                likedEpisodes = episodeTestDataList.take(1),
                savedEpisodes = emptyList(),
                followedPodcasts = podcastTestDataList.take(1),
            )
            every { findInLibraryUseCase(any()) } returns flowOf(findResult)
            every { getAllPlayedEpisodesUseCase(max = any()) } returns flowOf(episodeTestDataList)
            every { getLikedEpisodesUseCase(max = any()) } returns flowOf(episodeTestDataList)
            every { getSavedEpisodesUseCase(max = any()) } returns flowOf(emptyList())
            every { getFollowedPodcastsUseCase(max = any()) } returns flowOf(podcastTestDataList)
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
                assertEquals("query", success.findQuery)
                assertEquals(findResult.playingEpisodes, success.allPlayedEpisodes)
                assertEquals(findResult.likedEpisodes, success.likedEpisodes)
                assertEquals(findResult.followedPodcasts, success.followedPodcasts)
            }
        }
}
