package io.jacob.episodive.feature.onboarding

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.paging.PagingData
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.SelectableCategory
import io.jacob.episodive.core.testing.model.podcastTestDataList
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnboardingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun welcomePage_isDisplayed() {
        composeTestRule.setContent {
            EpisodiveTheme {
                OnboardingScreen(
                    pagerState = rememberPagerState(initialPage = 0) { OnboardingPage.entries.size },
                    categories = Category.entries.map { SelectableCategory(it, false) },
                    podcasts = flowOf(PagingData.from(podcastTestDataList)),
                    onChooseCategory = {},
                    onChoosePodcast = {},
                    onNextPage = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Access Engaging Content", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun nextButtonIsDisplayed() {
        composeTestRule.setContent {
            EpisodiveTheme {
                OnboardingScreen(
                    pagerState = rememberPagerState(initialPage = 0) { OnboardingPage.entries.size },
                    categories = Category.entries.map { SelectableCategory(it, false) },
                    podcasts = flowOf(PagingData.from(podcastTestDataList)),
                    onChooseCategory = {},
                    onChoosePodcast = {},
                    onNextPage = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Next").assertIsDisplayed()
    }

    @Test
    fun categorySelectionPage_displaysCategories() {
        composeTestRule.setContent {
            EpisodiveTheme {
                OnboardingScreen(
                    pagerState = rememberPagerState(initialPage = 1) { OnboardingPage.entries.size },
                    categories = Category.entries.map { SelectableCategory(it, false) },
                    podcasts = flowOf(PagingData.from(podcastTestDataList)),
                    onChooseCategory = {},
                    onChoosePodcast = {},
                    onNextPage = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Choose your taste", substring = true).assertIsDisplayed()
    }
}
