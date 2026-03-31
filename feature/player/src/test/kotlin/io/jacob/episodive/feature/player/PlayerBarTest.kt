package io.jacob.episodive.feature.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.model.Chapter
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
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

    private val defaultChapters = listOf(
        Chapter("Chapter 1", 0.seconds, 50.seconds),
        Chapter("Chapter 2", 50.seconds, 100.seconds),
    )

    private fun setPlayerBar(
        podcast: Podcast = podcastTestData,
        nowPlaying: Episode = episodeTestData,
        progress: Progress = Progress(30.seconds, 60.seconds, 100.seconds),
        isPlaying: Boolean = false,
        isLiked: Boolean = false,
        chapters: List<Chapter> = defaultChapters,
        onExpand: () -> Unit = {},
        onToggleLike: () -> Unit = {},
        onPlayOrPause: () -> Unit = {},
    ) {
        val episode = if (isLiked) {
            nowPlaying.copy(likedAt = Instant.fromEpochSeconds(1000L))
        } else {
            nowPlaying.copy(likedAt = null)
        }

        composeTestRule.setContent {
            EpisodiveTheme {
                PlayerBarContent(
                    podcast = podcast,
                    nowPlaying = episode,
                    progress = progress,
                    isPlaying = isPlaying,
                    chapters = chapters,
                    onExpand = onExpand,
                    onToggleLike = onToggleLike,
                    onPlayOrPause = onPlayOrPause,
                )
            }
        }
    }

    // --- Display tests ---

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

    // --- Callback tests ---

    @Test
    fun whenClickPlayButton_callbackInvoked() {
        var playPauseCalled = false
        setPlayerBar(
            isPlaying = false,
            onPlayOrPause = { playPauseCalled = true },
        )

        composeTestRule.onNodeWithContentDescription("Play").performClick()
        assert(playPauseCalled)
    }

    @Test
    fun whenClickPauseButton_callbackInvoked() {
        var playPauseCalled = false
        setPlayerBar(
            isPlaying = true,
            onPlayOrPause = { playPauseCalled = true },
        )

        composeTestRule.onNodeWithContentDescription("Pause").performClick()
        assert(playPauseCalled)
    }

    @Test
    fun whenClickLikeButton_callbackInvoked() {
        var likeCalled = false
        setPlayerBar(
            isLiked = false,
            onToggleLike = { likeCalled = true },
        )

        composeTestRule.onNodeWithContentDescription("Like").performClick()
        assert(likeCalled)
    }

    @Test
    fun whenClickUnlikeButton_callbackInvoked() {
        var likeCalled = false
        setPlayerBar(
            isLiked = true,
            onToggleLike = { likeCalled = true },
        )

        composeTestRule.onNodeWithContentDescription("Unlike").performClick()
        assert(likeCalled)
    }

    @Test
    fun whenClickBar_expandCallbackInvoked() {
        var expandCalled = false
        setPlayerBar(onExpand = { expandCalled = true })

        composeTestRule.onNodeWithText(episodeTestData.title, substring = true).performClick()
        assert(expandCalled)
    }

    // --- New: Different episode data ---

    @Test
    fun differentEpisode_titleIsDisplayed() {
        val otherEpisode = episodeTestData.copy(title = "Other Episode Title")
        setPlayerBar(nowPlaying = otherEpisode)

        composeTestRule.onNodeWithText("Other Episode Title", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun differentPodcast_titleIsDisplayed() {
        val otherPodcast = podcastTestData.copy(title = "Other Podcast Title")
        setPlayerBar(podcast = otherPodcast)

        composeTestRule.onNodeWithText("Other Podcast Title", substring = true)
            .assertIsDisplayed()
    }

    // --- New: Zero progress ---

    @Test
    fun zeroProgress_barStillDisplayed() {
        setPlayerBar(progress = Progress(0.seconds, 0.seconds, 100.seconds))

        composeTestRule.onNodeWithText(episodeTestData.title, substring = true)
            .assertIsDisplayed()
    }

    // --- New: Empty chapters ---

    @Test
    fun emptyChapters_barStillDisplayed() {
        setPlayerBar(chapters = emptyList())

        composeTestRule.onNodeWithText(episodeTestData.title, substring = true)
            .assertIsDisplayed()
    }

    // --- New: Full progress ---

    @Test
    fun fullProgress_barStillDisplayed() {
        setPlayerBar(progress = Progress(100.seconds, 100.seconds, 100.seconds))

        composeTestRule.onNodeWithText(episodeTestData.title, substring = true)
            .assertIsDisplayed()
    }
}
