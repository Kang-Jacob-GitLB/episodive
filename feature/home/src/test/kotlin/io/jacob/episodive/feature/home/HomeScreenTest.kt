package io.jacob.episodive.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.testing.model.channelTestDataList
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.liveEpisodeTestDataList
import io.jacob.episodive.core.testing.model.podcastTestDataList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun whenDataLoaded_sectionTitlesAreDisplayed() {
        composeTestRule.setContent {
            EpisodiveTheme {
                HomeScreen(
                    playingEpisodes = episodeTestDataList,
                    userRecentPodcasts = podcastTestDataList,
                    randomEpisodes = episodeTestDataList,
                    userTrendingPodcasts = podcastTestDataList,
                    followedPodcasts = podcastTestDataList,
                    localTrendingPodcasts = podcastTestDataList,
                    foreignTrendingPodcasts = podcastTestDataList,
                    liveEpisodes = liveEpisodeTestDataList,
                    channels = channelTestDataList,
                    onPlayEpisode = {},
                    onResumeEpisode = {},
                    onToggleLikedEpisode = {},
                    onPodcastClick = {},
                    onChannelClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Random episodes").assertExists()
    }

    @Test
    fun whenPlayingEpisodesEmpty_sectionIsNotShown() {
        composeTestRule.setContent {
            EpisodiveTheme {
                HomeScreen(
                    playingEpisodes = emptyList(),
                    userRecentPodcasts = podcastTestDataList,
                    randomEpisodes = episodeTestDataList,
                    userTrendingPodcasts = podcastTestDataList,
                    followedPodcasts = podcastTestDataList,
                    localTrendingPodcasts = podcastTestDataList,
                    foreignTrendingPodcasts = podcastTestDataList,
                    liveEpisodes = liveEpisodeTestDataList,
                    channels = channelTestDataList,
                    onPlayEpisode = {},
                    onResumeEpisode = {},
                    onToggleLikedEpisode = {},
                    onPodcastClick = {},
                    onChannelClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("My recent published").assertIsDisplayed()
    }

    @Test
    fun podcastTitleIsDisplayed() {
        composeTestRule.setContent {
            EpisodiveTheme {
                HomeScreen(
                    playingEpisodes = episodeTestDataList,
                    userRecentPodcasts = podcastTestDataList,
                    randomEpisodes = episodeTestDataList,
                    userTrendingPodcasts = podcastTestDataList,
                    followedPodcasts = podcastTestDataList,
                    localTrendingPodcasts = podcastTestDataList,
                    foreignTrendingPodcasts = podcastTestDataList,
                    liveEpisodes = liveEpisodeTestDataList,
                    channels = channelTestDataList,
                    onPlayEpisode = {},
                    onResumeEpisode = {},
                    onToggleLikedEpisode = {},
                    onPodcastClick = {},
                    onChannelClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("슈카월드", substring = true).assertIsDisplayed()
    }

    @Test
    fun episodeTitleIsDisplayed() {
        composeTestRule.setContent {
            EpisodiveTheme {
                HomeScreen(
                    playingEpisodes = episodeTestDataList,
                    userRecentPodcasts = podcastTestDataList,
                    randomEpisodes = episodeTestDataList,
                    userTrendingPodcasts = podcastTestDataList,
                    followedPodcasts = podcastTestDataList,
                    localTrendingPodcasts = podcastTestDataList,
                    foreignTrendingPodcasts = podcastTestDataList,
                    liveEpisodes = liveEpisodeTestDataList,
                    channels = channelTestDataList,
                    onPlayEpisode = {},
                    onResumeEpisode = {},
                    onToggleLikedEpisode = {},
                    onPodcastClick = {},
                    onChannelClick = {},
                )
            }
        }

        composeTestRule.onAllNodesWithText(episodeTestDataList.first().title, substring = true)
            .onFirst()
            .assertIsDisplayed()
    }
}
