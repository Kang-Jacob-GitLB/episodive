package io.jacob.episodive.feature.clip

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.paging.PagingData
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.model.Playback
import io.jacob.episodive.core.model.Progress
import io.jacob.episodive.core.testing.model.episodeTestDataList
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClipScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun whenEpisodesExist_clipItemsAreRendered() {
        val clipEpisodes = episodeTestDataList.map {
            it.copy(
                clipStartTime = Instant.fromEpochMilliseconds(60_000L),
                clipDuration = 1278.seconds,
            )
        }

        composeTestRule.setContent {
            EpisodiveTheme {
                ClipScreen(
                    episodes = flowOf(PagingData.from(clipEpisodes)),
                    playback = Playback.READY,
                    progress = Progress(1000.seconds, 1278.seconds, 2000.seconds),
                    isPlaying = true,
                )
            }
        }

        composeTestRule.onNodeWithText(clipEpisodes.first().title, substring = true)
            .assertExists()
    }

    @Test
    fun whenEpisodesEmpty_noClipItemsShown() {
        composeTestRule.setContent {
            EpisodiveTheme {
                ClipScreen(
                    episodes = flowOf(PagingData.from(emptyList())),
                    playback = Playback.IDLE,
                    progress = Progress(0.seconds, 0.seconds, 0.seconds),
                    isPlaying = false,
                )
            }
        }

        composeTestRule.onNodeWithText(episodeTestDataList.first().title, substring = true)
            .assertDoesNotExist()
    }

    @Test
    fun whenMultipleEpisodesExist_firstClipIsRendered() {
        val clipEpisodes = episodeTestDataList.take(5).map {
            it.copy(
                clipStartTime = Instant.fromEpochMilliseconds(60_000L),
                clipDuration = 1278.seconds,
            )
        }

        composeTestRule.setContent {
            EpisodiveTheme {
                ClipScreen(
                    episodes = flowOf(PagingData.from(clipEpisodes)),
                    playback = Playback.READY,
                    progress = Progress(500.seconds, 1000.seconds, 1278.seconds),
                    isPlaying = true,
                )
            }
        }

        composeTestRule.onNodeWithText(clipEpisodes.first().title, substring = true)
            .assertExists()
    }

    @Test
    fun whenPlaybackEnded_clipItemsStillShown() {
        val clipEpisodes = episodeTestDataList.take(3).map {
            it.copy(
                clipStartTime = Instant.fromEpochMilliseconds(60_000L),
                clipDuration = 1278.seconds,
            )
        }

        composeTestRule.setContent {
            EpisodiveTheme {
                ClipScreen(
                    episodes = flowOf(PagingData.from(clipEpisodes)),
                    playback = Playback.ENDED,
                    progress = Progress(1278.seconds, 1278.seconds, 1278.seconds),
                    isPlaying = false,
                )
            }
        }

        composeTestRule.onNodeWithText(clipEpisodes.first().title, substring = true)
            .assertExists()
    }

    @Test
    fun whenPlaybackIdle_clipItemsShown() {
        val clipEpisodes = episodeTestDataList.take(3).map {
            it.copy(
                clipStartTime = Instant.fromEpochMilliseconds(60_000L),
                clipDuration = 1278.seconds,
            )
        }

        composeTestRule.setContent {
            EpisodiveTheme {
                ClipScreen(
                    episodes = flowOf(PagingData.from(clipEpisodes)),
                    playback = Playback.IDLE,
                    progress = Progress(0.seconds, 0.seconds, 0.seconds),
                    isPlaying = false,
                )
            }
        }

        composeTestRule.onNodeWithText(clipEpisodes.first().title, substring = true)
            .assertExists()
    }
}
