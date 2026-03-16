package io.jacob.episodive.feature.podcast

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.paging.PagingData
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.podcastTestData
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PodcastScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun podcastTitleAndAuthorAreDisplayed() {
        composeTestRule.setContent {
            EpisodiveTheme {
                PodcastScreen(
                    podcast = podcastTestData,
                    episodes = flowOf(PagingData.from(episodeTestDataList)),
                    onFollowClick = {},
                    onEpisodeClick = { _, _ -> },
                    onToggleLikedEpisode = {},
                    onBackClick = {},
                    onShowSnackbar = { _, _ -> false },
                )
            }
        }

        composeTestRule.onAllNodesWithText(podcastTestData.title, substring = true)
            .onFirst()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(podcastTestData.author, substring = true).assertIsDisplayed()
    }

    @Test
    fun followButtonIsDisplayed() {
        composeTestRule.setContent {
            EpisodiveTheme {
                PodcastScreen(
                    podcast = podcastTestData,
                    episodes = flowOf(PagingData.from(episodeTestDataList)),
                    onFollowClick = {},
                    onEpisodeClick = { _, _ -> },
                    onToggleLikedEpisode = {},
                    onBackClick = {},
                    onShowSnackbar = { _, _ -> false },
                )
            }
        }

        composeTestRule.onNodeWithText("Follow").assertIsDisplayed()
    }

    @Test
    fun podcastDescriptionIsDisplayed() {
        composeTestRule.setContent {
            EpisodiveTheme {
                PodcastScreen(
                    podcast = podcastTestData,
                    episodes = flowOf(PagingData.from(episodeTestDataList)),
                    onFollowClick = {},
                    onEpisodeClick = { _, _ -> },
                    onToggleLikedEpisode = {},
                    onBackClick = {},
                    onShowSnackbar = { _, _ -> false },
                )
            }
        }

        composeTestRule.onNodeWithText(podcastTestData.description, substring = true)
            .assertExists()
    }

    @Test
    fun emptyEpisodes_podcastHeaderStillDisplayed() {
        composeTestRule.setContent {
            EpisodiveTheme {
                PodcastScreen(
                    podcast = podcastTestData,
                    episodes = flowOf(PagingData.from(emptyList())),
                    onFollowClick = {},
                    onEpisodeClick = { _, _ -> },
                    onToggleLikedEpisode = {},
                    onBackClick = {},
                    onShowSnackbar = { _, _ -> false },
                )
            }
        }

        composeTestRule.onAllNodesWithText(podcastTestData.title, substring = true)
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun followedPodcast_showsUnfollowButton() {
        val followedPodcast = podcastTestData.copy(
            followedAt = kotlin.time.Instant.fromEpochSeconds(1000L),
        )
        composeTestRule.setContent {
            EpisodiveTheme {
                PodcastScreen(
                    podcast = followedPodcast,
                    episodes = flowOf(PagingData.from(episodeTestDataList)),
                    onFollowClick = {},
                    onEpisodeClick = { _, _ -> },
                    onToggleLikedEpisode = {},
                    onBackClick = {},
                    onShowSnackbar = { _, _ -> false },
                )
            }
        }

        composeTestRule.onNodeWithText("Unfollow").assertIsDisplayed()
    }
}
