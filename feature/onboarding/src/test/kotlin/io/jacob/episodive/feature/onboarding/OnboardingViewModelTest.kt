package io.jacob.episodive.feature.onboarding

import androidx.paging.PagingData
import app.cash.turbine.test
import io.jacob.episodive.core.domain.usecase.podcast.GetUserRecommendedPodcastsPagingUseCase
import io.jacob.episodive.core.domain.usecase.podcast.ToggleFollowedUseCase
import io.jacob.episodive.core.domain.usecase.user.GetPreferredCategoriesUseCase
import io.jacob.episodive.core.domain.usecase.user.SetFirstLaunchOffUseCase
import io.jacob.episodive.core.domain.usecase.user.ToggleCategoryUseCase
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.testing.model.podcastTestData
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val setFirstLaunchOffUseCase = mockk<SetFirstLaunchOffUseCase>(relaxed = true)
    private val toggleCategoryUseCase = mockk<ToggleCategoryUseCase>(relaxed = true)
    private val toggleFollowedUseCase = mockk<ToggleFollowedUseCase>(relaxed = true)
    private val getPreferredCategoriesUseCase = mockk<GetPreferredCategoriesUseCase>(relaxed = true)
    private val getUserRecommendedPodcastsPagingUseCase =
        mockk<GetUserRecommendedPodcastsPagingUseCase>(relaxed = true)

    private fun createViewModel(): OnboardingViewModel {
        return OnboardingViewModel(
            setFirstLaunchOffUseCase = setFirstLaunchOffUseCase,
            toggleCategoryUseCase = toggleCategoryUseCase,
            toggleFollowedUseCase = toggleFollowedUseCase,
            getPreferredCategoriesUseCase = getPreferredCategoriesUseCase,
            getUserRecommendedPodcastsPagingUseCase = getUserRecommendedPodcastsPagingUseCase,
        )
    }

    @After
    fun teardown() {
        confirmVerified(
            setFirstLaunchOffUseCase,
            toggleCategoryUseCase,
            toggleFollowedUseCase,
        )
    }

    @Test
    fun `Given no emissions, When ViewModel is created, Then initial state is Loading`() = runTest {
        every { getPreferredCategoriesUseCase() } returns flowOf()
        every { getUserRecommendedPodcastsPagingUseCase(any()) } returns flowOf(PagingData.empty())

        val viewModel = createViewModel()

        assertEquals(OnboardingState.Loading, viewModel.state.value)
    }

    @Test
    fun `Given categories flow emits, When collecting, Then state is Success with SelectableCategory list`() =
        runTest {
            val preferred = listOf(Category.BUSINESS, Category.COMEDY)
            every { getPreferredCategoriesUseCase() } returns flowOf(preferred)
            every { getUserRecommendedPodcastsPagingUseCase(any()) } returns flowOf(PagingData.empty())

            val viewModel = createViewModel()

            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state is OnboardingState.Success)
                val success = state as OnboardingState.Success
                assertEquals(Category.entries.size, success.categories.size)
                assertTrue(success.categories.first { it.category == Category.BUSINESS }.isSelected)
                assertTrue(success.categories.first { it.category == Category.COMEDY }.isSelected)
                assertTrue(!success.categories.first { it.category == Category.EDUCATION }.isSelected)
            }
        }

    @Test
    fun `Given categories flow throws, When collecting, Then state is Error`() = runTest {
        every { getPreferredCategoriesUseCase() } returns kotlinx.coroutines.flow.flow {
            throw RuntimeException("Error")
        }
        every { getUserRecommendedPodcastsPagingUseCase(any()) } returns flowOf(PagingData.empty())

        val viewModel = createViewModel()

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state is OnboardingState.Error)
        }
    }

    @Test
    fun `Given Welcome page, When NextPage action sent, Then MoveToPage CategorySelection effect emitted`() =
        runTest {
            every { getPreferredCategoriesUseCase() } returns flowOf(emptyList())
            every { getUserRecommendedPodcastsPagingUseCase(any()) } returns flowOf(PagingData.empty())

            val viewModel = createViewModel()

            viewModel.effect.test {
                viewModel.sendAction(OnboardingAction.NextPage)
                val effect = awaitItem()
                assertEquals(
                    OnboardingEffect.MoveToPage(OnboardingPage.CategorySelection),
                    effect
                )
            }
        }

    @Test
    fun `Given CategorySelection with less than 3 categories, When NextPage, Then ToastMoreCategories effect emitted`() =
        runTest {
            val preferred = listOf(Category.BUSINESS, Category.COMEDY)
            every { getPreferredCategoriesUseCase() } returns flowOf(preferred)
            every { getUserRecommendedPodcastsPagingUseCase(any()) } returns flowOf(PagingData.empty())

            val viewModel = createViewModel()

            // Move to CategorySelection first
            viewModel.effect.test {
                viewModel.sendAction(OnboardingAction.NextPage)
                assertEquals(
                    OnboardingEffect.MoveToPage(OnboardingPage.CategorySelection),
                    awaitItem()
                )

                // Now try next with <3 categories
                viewModel.sendAction(OnboardingAction.NextPage)
                assertEquals(OnboardingEffect.ToastMoreCategories, awaitItem())
            }
        }

    @Test
    fun `Given CategorySelection with 3 or more categories, When NextPage, Then MoveToPage PodcastSelection effect emitted`() =
        runTest {
            val preferred = listOf(Category.BUSINESS, Category.COMEDY, Category.EDUCATION)
            every { getPreferredCategoriesUseCase() } returns flowOf(preferred)
            every { getUserRecommendedPodcastsPagingUseCase(any()) } returns flowOf(PagingData.empty())

            val viewModel = createViewModel()

            viewModel.effect.test {
                // Move to CategorySelection first
                viewModel.sendAction(OnboardingAction.NextPage)
                assertEquals(
                    OnboardingEffect.MoveToPage(OnboardingPage.CategorySelection),
                    awaitItem()
                )

                // Now next with >=3 categories
                viewModel.sendAction(OnboardingAction.NextPage)
                assertEquals(
                    OnboardingEffect.MoveToPage(OnboardingPage.PodcastSelection),
                    awaitItem()
                )
            }
        }

    @Test
    fun `Given PodcastSelection page, When NextPage, Then finishOnboarding calls setFirstLaunchOffUseCase after delay`() =
        runTest {
            val preferred = listOf(Category.BUSINESS, Category.COMEDY, Category.EDUCATION)
            every { getPreferredCategoriesUseCase() } returns flowOf(preferred)
            every { getUserRecommendedPodcastsPagingUseCase(any()) } returns flowOf(PagingData.empty())

            val viewModel = createViewModel()

            // Navigate to PodcastSelection
            viewModel.sendAction(OnboardingAction.NextPage) // Welcome -> CategorySelection
            viewModel.sendAction(OnboardingAction.NextPage) // CategorySelection -> PodcastSelection

            // Trigger NextPage on PodcastSelection to finish onboarding
            viewModel.sendAction(OnboardingAction.NextPage)

            advanceTimeBy(3100)

            coVerify { setFirstLaunchOffUseCase() }
        }

    @Test
    fun `Given Welcome page, When PreviousPage, Then no effect emitted`() = runTest {
        every { getPreferredCategoriesUseCase() } returns flowOf(emptyList())
        every { getUserRecommendedPodcastsPagingUseCase(any()) } returns flowOf(PagingData.empty())

        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.sendAction(OnboardingAction.PreviousPage)
            expectNoEvents()
        }
    }

    @Test
    fun `Given CategorySelection page, When PreviousPage, Then MoveToPage Welcome effect emitted`() =
        runTest {
            every { getPreferredCategoriesUseCase() } returns flowOf(emptyList())
            every { getUserRecommendedPodcastsPagingUseCase(any()) } returns flowOf(PagingData.empty())

            val viewModel = createViewModel()

            viewModel.effect.test {
                // Move to CategorySelection first
                viewModel.sendAction(OnboardingAction.NextPage)
                assertEquals(
                    OnboardingEffect.MoveToPage(OnboardingPage.CategorySelection),
                    awaitItem()
                )

                // Now go back
                viewModel.sendAction(OnboardingAction.PreviousPage)
                assertEquals(
                    OnboardingEffect.MoveToPage(OnboardingPage.Welcome),
                    awaitItem()
                )
            }
        }

    @Test
    fun `OnboardingPage companion helpers return correct values`() {
        assertEquals(4, OnboardingPage.count)
        assertEquals(OnboardingPage.Welcome, OnboardingPage.fromIndex(0))
        assertEquals(OnboardingPage.CategorySelection, OnboardingPage.fromIndex(1))
        assertEquals(OnboardingPage.PodcastSelection, OnboardingPage.fromIndex(2))
        assertEquals(OnboardingPage.Completion, OnboardingPage.fromIndex(3))
        assertNull(OnboardingPage.fromIndex(4))
        assertEquals(0, OnboardingPage.firstIndex())
        assertEquals(3, OnboardingPage.lastIndex())
    }

    @Test
    fun `Given ChooseCategory action, When sent, Then toggleCategoryUseCase is invoked`() =
        runTest {
            every { getPreferredCategoriesUseCase() } returns flowOf(emptyList())
            every { getUserRecommendedPodcastsPagingUseCase(any()) } returns flowOf(PagingData.empty())

            val viewModel = createViewModel()

            viewModel.sendAction(OnboardingAction.ChooseCategory(Category.BUSINESS))

            coVerify { toggleCategoryUseCase(Category.BUSINESS) }
        }

    @Test
    fun `Given ChoosePodcast action, When sent, Then toggleFollowedUseCase is invoked`() =
        runTest {
            every { getPreferredCategoriesUseCase() } returns flowOf(emptyList())
            every { getUserRecommendedPodcastsPagingUseCase(any()) } returns flowOf(PagingData.empty())

            val viewModel = createViewModel()
            val podcast = podcastTestData

            viewModel.sendAction(OnboardingAction.ChoosePodcast(podcast))

            coVerify { toggleFollowedUseCase(podcast.id) }
        }
}
