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
                    savedEpisodes = episodeTestDataList,
                    followedPodcasts = podcastTestDataList,
                    preferredCategories = listOf(Category.BUSINESS, Category.COMEDY),
                    selectableCategories = Category.entries.map { SelectableCategory(it, true) },
                    playedEpisodesPaging = flowOf(PagingData.from(episodeTestDataList.map { SeparatedUiModel.Content(it) })),
                    likedEpisodesPaging = flowOf(PagingData.from(episodeTestDataList.map { SeparatedUiModel.Content(it) })),
                    savedEpisodesPaging = flowOf(PagingData.from(episodeTestDataList.map { SeparatedUiModel.Content(it) })),
                    followedPodcastsPaging = flowOf(PagingData.from(podcastTestDataList.map { SeparatedUiModel.Content(it) })),
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

    @Test
    fun recentlyListenedSection_displaysData() {
        setLibraryScreen(section = LibrarySection.RecentlyListened)

        composeTestRule.onNodeWithText(episodeTestDataList.first().title, substring = true)
            .assertExists()
    }

    @Test
    fun likedSection_displaysData() {
        setLibraryScreen(section = LibrarySection.Liked)

        composeTestRule.onNodeWithText(episodeTestDataList.first().title, substring = true)
            .assertExists()
    }

    @Test
    fun followedSection_displaysData() {
        setLibraryScreen(section = LibrarySection.Followed)

        composeTestRule.onNodeWithText(podcastTestDataList.first().title, substring = true)
            .assertExists()
    }

    @Test
    fun preferredSection_displaysCategories() {
        setLibraryScreen(section = LibrarySection.Preferred)

        composeTestRule.onNodeWithText("Arts", substring = true).assertExists()
    }

    @Test
    fun allSection_withEmptyData_showsNoResults() {
        composeTestRule.setContent {
            EpisodiveTheme {
                LibraryScreen(
                    query = "",
                    onQueryChange = {},
                    onFind = {},
                    section = LibrarySection.All,
                    onSectionChange = {},
                    playedEpisodes = emptyList(),
                    likedEpisodes = emptyList(),
                    savedEpisodes = emptyList(),
                    followedPodcasts = emptyList(),
                    preferredCategories = emptyList(),
                    selectableCategories = emptyList(),
                    playedEpisodesPaging = flowOf(PagingData.from(emptyList())),
                    likedEpisodesPaging = flowOf(PagingData.from(emptyList())),
                    savedEpisodesPaging = flowOf(PagingData.from(emptyList())),
                    followedPodcastsPaging = flowOf(PagingData.from(emptyList())),
                )
            }
        }

        composeTestRule.onNodeWithText("No results found.").assertIsDisplayed()
    }

    @Test
    fun allSection_displaysLikedEpisodesSection() {
        setLibraryScreen()

        composeTestRule.onNodeWithText("Liked episodes").assertIsDisplayed()
    }

    @Test
    fun allSection_displaysFollowedPodcastsSection() {
        setLibraryScreen()

        composeTestRule.onNodeWithText("Followed podcasts").assertExists()
    }

    @Test
    fun allSection_displaysPreferredCategoriesSection() {
        composeTestRule.setContent {
            EpisodiveTheme {
                LibraryScreen(
                    query = "",
                    onQueryChange = {},
                    onFind = {},
                    section = LibrarySection.All,
                    onSectionChange = {},
                    playedEpisodes = emptyList(),
                    likedEpisodes = emptyList(),
                    savedEpisodes = emptyList(),
                    followedPodcasts = emptyList(),
                    preferredCategories = listOf(Category.BUSINESS),
                    selectableCategories = emptyList(),
                    playedEpisodesPaging = flowOf(PagingData.from(emptyList())),
                    likedEpisodesPaging = flowOf(PagingData.from(emptyList())),
                    savedEpisodesPaging = flowOf(PagingData.from(emptyList())),
                    followedPodcastsPaging = flowOf(PagingData.from(emptyList())),
                )
            }
        }

        composeTestRule.onNodeWithText("Preferred categories").assertIsDisplayed()
    }

    @Test
    fun recentlyListenedFilterChipIsDisplayed() {
        setLibraryScreen()

        composeTestRule.onNodeWithText("Recently listened").assertIsDisplayed()
    }

    @Test
    fun preferredFilterChipIsDisplayed() {
        setLibraryScreen()

        composeTestRule.onNodeWithText("Preferred").assertIsDisplayed()
    }
}
