package io.jacob.episodive.feature.channel

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.testing.model.channelTestData
import io.jacob.episodive.core.testing.model.podcastTestDataList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChannelScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun channelTitleIsDisplayed() {
        composeTestRule.setContent {
            EpisodiveTheme {
                ChannelScreen(
                    channel = channelTestData,
                    podcasts = podcastTestDataList,
                    onBackClick = {},
                    onPodcastClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText(channelTestData.title).assertIsDisplayed()
    }

    @Test
    fun podcastItemsAreDisplayed() {
        composeTestRule.setContent {
            EpisodiveTheme {
                ChannelScreen(
                    channel = channelTestData,
                    podcasts = podcastTestDataList,
                    onBackClick = {},
                    onPodcastClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText(podcastTestDataList.first().title, substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun channelSubtitleIsDisplayed() {
        composeTestRule.setContent {
            EpisodiveTheme {
                ChannelScreen(
                    channel = channelTestData,
                    podcasts = podcastTestDataList,
                    onBackClick = {},
                    onPodcastClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("12programs", substring = true).assertIsDisplayed()
    }

    @Test
    fun emptyPodcastsList_channelTitleStillDisplayed() {
        composeTestRule.setContent {
            EpisodiveTheme {
                ChannelScreen(
                    channel = channelTestData,
                    podcasts = emptyList(),
                    onBackClick = {},
                    onPodcastClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText(channelTestData.title).assertIsDisplayed()
    }

    @Test
    fun multiplePodcastsAreDisplayed() {
        composeTestRule.setContent {
            EpisodiveTheme {
                ChannelScreen(
                    channel = channelTestData,
                    podcasts = podcastTestDataList.take(3),
                    onBackClick = {},
                    onPodcastClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText(podcastTestDataList[0].title, substring = true)
            .assertExists()
    }
}
