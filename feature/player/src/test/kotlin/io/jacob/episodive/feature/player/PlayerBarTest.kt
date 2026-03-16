package io.jacob.episodive.feature.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.model.Chapter
import io.jacob.episodive.core.model.Progress
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.podcastTestData
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlayerBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setPlayerBar(
        isPlaying: Boolean = false,
        isLiked: Boolean = false,
    ) {
        val episode = if (isLiked) {
            episodeTestData.copy(likedAt = Instant.fromEpochSeconds(1000L))
        } else {
            episodeTestData.copy(likedAt = null)
        }

        composeTestRule.setContent {
            EpisodiveTheme {
                PlayerBarContent(
                    podcast = podcastTestData,
                    nowPlaying = episode,
                    progress = Progress(30.seconds, 60.seconds, 100.seconds),
                    isPlaying = isPlaying,
                    chapters = listOf(
                        Chapter("Chapter 1", 0.seconds, 50.seconds),
                        Chapter("Chapter 2", 50.seconds, 100.seconds),
                    ),
                    onExpand = {},
                    onToggleLike = {},
                    onPlayOrPause = {},
                )
            }
        }
    }

    @Test
    fun episodeTitleIsDisplayed() {
        setPlayerBar()

        composeTestRule.onNodeWithText(episodeTestData.title, substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun podcastTitleIsDisplayed() {
        setPlayerBar()

        composeTestRule.onNodeWithText(podcastTestData.title, substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun whenNotLiked_likeButtonIsDisplayed() {
        setPlayerBar(isLiked = false)

        composeTestRule.onNodeWithContentDescription("Like").assertIsDisplayed()
    }

    @Test
    fun whenLiked_unlikeButtonIsDisplayed() {
        setPlayerBar(isLiked = true)

        composeTestRule.onNodeWithContentDescription("Unlike").assertIsDisplayed()
    }

    @Test
    fun whenPlaying_pauseButtonIsDisplayed() {
        setPlayerBar(isPlaying = true)

        composeTestRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
    }

    @Test
    fun whenNotPlaying_playButtonIsDisplayed() {
        setPlayerBar(isPlaying = false)

        composeTestRule.onNodeWithContentDescription("Play").assertIsDisplayed()
    }
}
