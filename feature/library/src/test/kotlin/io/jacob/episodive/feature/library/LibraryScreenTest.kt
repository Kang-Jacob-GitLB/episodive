package io.jacob.episodive.feature.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.paging.PagingData
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.SelectableCategory
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.podcastTestDataList
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LibraryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setLibraryScreen(section: LibrarySection = LibrarySection.All) {
        composeTestRule.setContent {
            EpisodiveTheme {
                LibraryScreen(
                    query = "",
                    onQueryChange = {},
                    onFind = {},
                    section = section,
                    onSectionChange = {},
                    playedEpisodes = episodeTestDataList,
                    likedEpisodes = episodeTestDataList,
                    followedPodcasts = podcastTestDataList,
                    preferredCategories = listOf(Category.BUSINESS, Category.COMEDY),
                    selectableCategories = Category.entries.map { SelectableCategory(it, true) },
                    playedEpisodesPaging = flowOf(PagingData.from(episodeTestDataList.map { PlayedUiModel.EpisodeModel(it) })),
                    likedEpisodesPaging = flowOf(PagingData.from(episodeTestDataList.map { LikedUiModel.EpisodeModel(it) })),
                    followedPodcastsPaging = flowOf(PagingData.from(podcastTestDataList.map { FollowedUiModel.PodcastModel(it) })),
                )
            }
        }
    }

    @Test
    fun allSection_displaysData() {
        setLibraryScreen()

        composeTestRule.onNodeWithText("Recently listened episodes").assertIsDisplayed()
    }

    @Test
    fun filterChipsAreDisplayed() {
        setLibraryScreen()

        composeTestRule.onNodeWithText("All").assertIsDisplayed()
        composeTestRule.onNodeWithText("Liked").assertIsDisplayed()
        composeTestRule.onNodeWithText("Followed").assertIsDisplayed()
    }

    @Test
    fun libraryTitleIsDisplayed() {
        setLibraryScreen()

        composeTestRule.onNodeWithText("Library").assertIsDisplayed()
    }
}
