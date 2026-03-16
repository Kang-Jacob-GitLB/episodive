package io.jacob.episodive.feature.search

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.model.SearchResult
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.podcastTestDataList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SearchScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun collapsedState_showsTrendingSections() {
        composeTestRule.setContent {
            EpisodiveTheme {
                SearchScreen(
                    query = "",
                    onQueryChange = {},
                    onSearch = {},
                    recentSearches = emptyList(),
                    searchResult = SearchResult(),
                    podcasts = podcastTestDataList,
                    episodes = episodeTestDataList,
                    isExpanded = false,
                )
            }
        }

        composeTestRule.onNodeWithText("Global trending feeds").assertIsDisplayed()
    }

    @Test
    fun searchPlaceholderIsDisplayed() {
        composeTestRule.setContent {
            EpisodiveTheme {
                SearchScreen(
                    query = "",
                    onQueryChange = {},
                    onSearch = {},
                    recentSearches = emptyList(),
                    searchResult = SearchResult(),
                    podcasts = podcastTestDataList,
                    episodes = episodeTestDataList,
                    isExpanded = false,
                )
            }
        }

        composeTestRule.onNodeWithText("What do you want to listen to?").assertIsDisplayed()
    }

    @Test
    fun emptyQuery_showsPodcastData() {
        composeTestRule.setContent {
            EpisodiveTheme {
                SearchScreen(
                    query = "",
                    onQueryChange = {},
                    onSearch = {},
                    recentSearches = emptyList(),
                    searchResult = SearchResult(),
                    podcasts = podcastTestDataList,
                    episodes = episodeTestDataList,
                    isExpanded = false,
                )
            }
        }

        composeTestRule.onNodeWithText(podcastTestDataList.first().title, substring = true)
            .assertExists()
    }
}
