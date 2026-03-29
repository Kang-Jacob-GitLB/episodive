package io.jacob.episodive.feature.search

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.model.RecentSearch
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

    @Test
    fun expandedState_withSearchResults_showsPodcastsSection() {
        composeTestRule.setContent {
            EpisodiveTheme {
                SearchScreen(
                    query = "test",
                    onQueryChange = {},
                    onSearch = {},
                    recentSearches = emptyList(),
                    searchResult = SearchResult(
                        podcasts = podcastTestDataList.take(3),
                        episodes = episodeTestDataList,
                    ),
                    podcasts = podcastTestDataList,
                    episodes = episodeTestDataList,
                    isExpanded = true,
                )
            }
        }

        composeTestRule.onNodeWithText("Podcasts").assertIsDisplayed()
    }

    @Test
    fun expandedState_withNoResults_showsRecentSearches() {
        composeTestRule.setContent {
            EpisodiveTheme {
                SearchScreen(
                    query = "test",
                    onQueryChange = {},
                    onSearch = {},
                    recentSearches = listOf(
                        RecentSearch.Query(id = 1, query = "kotlin", searchedAt = kotlin.time.Clock.System.now()),
                        RecentSearch.Query(id = 2, query = "android", searchedAt = kotlin.time.Clock.System.now()),
                    ),
                    searchResult = SearchResult(),
                    podcasts = podcastTestDataList,
                    episodes = episodeTestDataList,
                    isExpanded = true,
                )
            }
        }

        composeTestRule.onNodeWithText("Recent searches").assertIsDisplayed()
        composeTestRule.onNodeWithText("kotlin").assertIsDisplayed()
    }

    @Test
    fun collapsedState_withEmptyPodcasts_hidesGlobalTrendingSection() {
        composeTestRule.setContent {
            EpisodiveTheme {
                SearchScreen(
                    query = "",
                    onQueryChange = {},
                    onSearch = {},
                    recentSearches = emptyList(),
                    searchResult = SearchResult(),
                    podcasts = emptyList(),
                    episodes = episodeTestDataList,
                    isExpanded = false,
                )
            }
        }

        composeTestRule.onNodeWithText("Global trending feeds").assertDoesNotExist()
    }

    @Test
    fun collapsedState_withEmptyEpisodes_hidesRecentEpisodesSection() {
        composeTestRule.setContent {
            EpisodiveTheme {
                SearchScreen(
                    query = "",
                    onQueryChange = {},
                    onSearch = {},
                    recentSearches = emptyList(),
                    searchResult = SearchResult(),
                    podcasts = podcastTestDataList,
                    episodes = emptyList(),
                    isExpanded = false,
                )
            }
        }

        composeTestRule.onNodeWithText("Global recent episodes").assertDoesNotExist()
    }

    @Test
    fun expandedState_withSearchResults_showsEpisodesSection() {
        composeTestRule.setContent {
            EpisodiveTheme {
                SearchScreen(
                    query = "test",
                    onQueryChange = {},
                    onSearch = {},
                    recentSearches = emptyList(),
                    searchResult = SearchResult(
                        podcasts = emptyList(),
                        episodes = episodeTestDataList,
                    ),
                    podcasts = podcastTestDataList,
                    episodes = episodeTestDataList,
                    isExpanded = true,
                )
            }
        }

        composeTestRule.onNodeWithText("Episodes").assertIsDisplayed()
    }

    @Test
    fun collapsedState_showsGlobalRecentEpisodesSection() {
        composeTestRule.setContent {
            EpisodiveTheme {
                SearchScreen(
                    query = "",
                    onQueryChange = {},
                    onSearch = {},
                    recentSearches = emptyList(),
                    searchResult = SearchResult(),
                    podcasts = emptyList(),
                    episodes = episodeTestDataList,
                    isExpanded = false,
                )
            }
        }

        composeTestRule.onNodeWithText("Global recent episodes").assertIsDisplayed()
    }
}
