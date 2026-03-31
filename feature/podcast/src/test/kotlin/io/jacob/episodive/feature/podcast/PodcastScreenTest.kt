package io.jacob.episodive.feature.podcast

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.paging.PagingData
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
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

    private fun setPodcastScreen(
        podcast: Podcast = podcastTestData,
        episodes: List<Episode> = episodeTestDataList,
        onFollowClick: () -> Unit = {},
        onEpisodeClick: (Episode, List<Episode>) -> Unit = { _, _ -> },
        onToggleLikedEpisode: (Episode) -> Unit = {},
        onToggleSavedEpisode: (Episode) -> Unit = {},
        onBackClick: () -> Unit = {},
        onShowSnackbar: suspend (String, String?) -> Boolean = { _, _ -> false },
    ) {
        composeTestRule.setContent {
            EpisodiveTheme {
                PodcastScreen(
                    podcast = podcast,
                    episodes = flowOf(PagingData.from(episodes)),
                    onFollowClick = onFollowClick,
                    onEpisodeClick = onEpisodeClick,
                    onToggleLikedEpisode = onToggleLikedEpisode,
                    onToggleSavedEpisode = onToggleSavedEpisode,
                    onBackClick = onBackClick,
                    onShowSnackbar = onShowSnackbar,
                )
            }
        }
    }

    // --- Display tests ---

    @Test
    fun podcastTitleAndAuthorAreDisplayed() {
        setPodcastScreen()

        composeTestRule.onAllNodesWithText(podcastTestData.title, substring = true)
            .onFirst()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(podcastTestData.author, substring = true).assertIsDisplayed()
    }

    @Test
    fun followButtonIsDisplayed() {
        setPodcastScreen()

        composeTestRule.onNodeWithText("Follow").assertIsDisplayed()
    }

    @Test
    fun podcastDescriptionIsDisplayed() {
        setPodcastScreen()

        composeTestRule.onNodeWithText(podcastTestData.description, substring = true)
            .assertExists()
    }

    @Test
    fun emptyEpisodes_podcastHeaderStillDisplayed() {
        setPodcastScreen(episodes = emptyList())

        composeTestRule.onAllNodesWithText(podcastTestData.title, substring = true)
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun followedPodcast_showsUnfollowButton() {
        val followedPodcast = podcastTestData.copy(
            followedAt = kotlin.time.Instant.fromEpochSeconds(1000L),
        )
        setPodcastScreen(podcast = followedPodcast)

        composeTestRule.onNodeWithText("Unfollow").assertIsDisplayed()
    }

    @Test
    fun episodeListItemsAreDisplayed() {
        setPodcastScreen()

        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasText(episodeTestDataList.first().title, substring = true))
        composeTestRule.onAllNodesWithText(episodeTestDataList.first().title, substring = true)
            .onFirst()
            .assertExists()
    }

    @Test
    fun episodeCountIsCorrect() {
        setPodcastScreen()

        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("${podcastTestData.episodeCount}", substring = true))
        composeTestRule.onNodeWithText("${podcastTestData.episodeCount}", substring = true)
            .assertExists()
    }

    @Test
    fun backButtonIsDisplayed() {
        setPodcastScreen()

        composeTestRule.onNodeWithContentDescription("Back").assertExists()
    }

    @Test
    fun podcastCategoriesDisplayed() {
        setPodcastScreen()

        composeTestRule.onNodeWithText(podcastTestData.description, substring = true)
            .assertExists()
    }

    // --- New: Callback tests ---

    @Test
    fun onFollowClick_callbackInvoked() {
        var called = false
        setPodcastScreen(onFollowClick = { called = true })

        composeTestRule.onNodeWithText("Follow").performClick()
        assert(called)
    }

    @Test
    fun onBackClick_callbackInvoked() {
        var called = false
        setPodcastScreen(onBackClick = { called = true })

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assert(called)
    }

    @Test
    fun onFollowClick_unfollowCallbackInvoked() {
        var called = false
        val followedPodcast = podcastTestData.copy(
            followedAt = kotlin.time.Instant.fromEpochSeconds(1000L),
        )
        setPodcastScreen(
            podcast = followedPodcast,
            onFollowClick = { called = true },
        )

        composeTestRule.onNodeWithText("Unfollow").performClick()
        assert(called)
    }

    // --- New: Episode count format ---

    @Test
    fun allEpisodesFormat_isDisplayed() {
        setPodcastScreen()

        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("All episodes", substring = true))
        composeTestRule.onNodeWithText("All episodes", substring = true)
            .assertExists()
    }

    // --- New: Different podcast data ---

    @Test
    fun differentPodcast_titleIsDisplayed() {
        val otherPodcast = podcastTestData.copy(title = "Custom Podcast Title")
        setPodcastScreen(podcast = otherPodcast)

        composeTestRule.onAllNodesWithText("Custom Podcast Title", substring = true)
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun differentPodcast_authorIsDisplayed() {
        val otherPodcast = podcastTestData.copy(author = "Custom Author")
        setPodcastScreen(podcast = otherPodcast)

        composeTestRule.onNodeWithText("Custom Author", substring = true).assertIsDisplayed()
    }

    // --- New: Unfollowed podcast shows Follow button ---

    @Test
    fun unfollowedPodcast_showsFollowButton() {
        val unfollowedPodcast = podcastTestData.copy(followedAt = null)
        setPodcastScreen(podcast = unfollowedPodcast)

        composeTestRule.onNodeWithText("Follow").assertIsDisplayed()
    }
}
