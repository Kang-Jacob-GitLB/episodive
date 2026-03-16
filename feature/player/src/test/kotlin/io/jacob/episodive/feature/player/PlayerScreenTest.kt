package io.jacob.episodive.feature.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.model.Chapter
import io.jacob.episodive.core.model.Progress
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.podcastTestData
import kotlin.time.Duration.Companion.seconds
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlayerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setPlayerScreen() {
        composeTestRule.setContent {
            EpisodiveTheme {
                PlayerScreen(
                    podcast = podcastTestData,
                    nowPlaying = episodeTestData,
                    progress = Progress(1000.seconds, 2000.seconds, 6000.seconds),
                    isPlaying = true,
                    onCollapse = {},
                    onToggleLike = {},
                    onSeekTo = {},
                    onPlayOrPause = {},
                    onBackward = {},
                    onForward = {},
                    onPrevious = {},
                    onNext = {},
                    onPodcastClick = {},
                    playlist = episodeTestDataList,
                    indexOfList = 0,
                    onEpisodeClick = {},
                    onPlayIndex = {},
                    onToggleLikedEpisode = {},
                    speed = 1f,
                    onSpeedChange = {},
                    chapters = listOf(
                        Chapter("Chapter 1", 0.seconds, 1000.seconds),
                        Chapter("Chapter 2", 1000.seconds, 3000.seconds),
                        Chapter("Chapter 3", 3000.seconds, 6000.seconds),
                    ),
                    onToggleFollowedPodcast = {},
                    cue = "test cue text",
                )
            }
        }
    }

    @Test
    fun episodeTitleAndPodcastTitleAreDisplayed() {
        setPlayerScreen()

        composeTestRule.onNodeWithText(episodeTestData.title, substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(podcastTestData.title, substring = true).assertIsDisplayed()
    }

    @Test
    fun playbackControlsAreDisplayed() {
        setPlayerScreen()

        composeTestRule.onNodeWithText("1x").assertExists()
    }

    @Test
    fun speedIsDisplayed() {
        setPlayerScreen()

        composeTestRule.onNodeWithText("1x").assertExists()
    }

    @Test
    fun episodeInfoSectionIsDisplayed() {
        setPlayerScreen()

        composeTestRule.onNodeWithText("Episode Info").assertIsDisplayed()
    }

    @Test
    fun podcastTitleInPlayerIsDisplayed() {
        setPlayerScreen()

        composeTestRule.onNodeWithText(podcastTestData.title, substring = true).assertIsDisplayed()
    }

    @Test
    fun nowPlayingTitleInPlayerIsDisplayed() {
        setPlayerScreen()

        composeTestRule.onNodeWithText(episodeTestData.title, substring = true).assertIsDisplayed()
    }

    @Test
    fun replayButtonIsDisplayed() {
        setPlayerScreen()

        composeTestRule.onNodeWithContentDescription("Replay").assertExists()
    }

    @Test
    fun noChapters_chapterSectionIsNotShown() {
        composeTestRule.setContent {
            EpisodiveTheme {
                PlayerScreen(
                    podcast = podcastTestData,
                    nowPlaying = episodeTestData,
                    progress = Progress(1000.seconds, 2000.seconds, 6000.seconds),
                    isPlaying = true,
                    onCollapse = {},
                    onToggleLike = {},
                    onSeekTo = {},
                    onPlayOrPause = {},
                    onBackward = {},
                    onForward = {},
                    onPrevious = {},
                    onNext = {},
                    onPodcastClick = {},
                    playlist = episodeTestDataList,
                    indexOfList = 0,
                    onEpisodeClick = {},
                    onPlayIndex = {},
                    onToggleLikedEpisode = {},
                    speed = 1f,
                    onSpeedChange = {},
                    chapters = emptyList(),
                    onToggleFollowedPodcast = {},
                    cue = "",
                )
            }
        }

        composeTestRule.onNodeWithText("Chapter").assertDoesNotExist()
    }

    @Test
    fun cueTextIsDisplayed() {
        setPlayerScreen()

        composeTestRule.onNodeWithText("test cue text").assertExists()
    }

    @Test
    fun progressTimesAreDisplayed() {
        setPlayerScreen()

        composeTestRule.onNodeWithText("16:40", substring = true).assertExists()
    }
}
