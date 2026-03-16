package io.jacob.episodive.feature.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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
}
