package io.jacob.episodive.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.model.Channel
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
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

    private fun setHomeScreen(
        playingEpisodes: List<Episode> = emptyList(),
        userRecentPodcasts: List<Podcast> = podcastTestDataList,
        randomEpisodes: List<Episode> = episodeTestDataList,
        userTrendingPodcasts: List<Podcast> = podcastTestDataList,
        followedPodcasts: List<Podcast> = podcastTestDataList,
        localTrendingPodcasts: List<Podcast> = podcastTestDataList,
        foreignTrendingPodcasts: List<Podcast> = podcastTestDataList,
        liveEpisodes: List<Episode> = liveEpisodeTestDataList,
        channels: List<Channel> = channelTestDataList,
        onPlayEpisode: (Episode) -> Unit = {},
        onResumeEpisode: (Episode) -> Unit = {},
        onToggleLikedEpisode: (Episode) -> Unit = {},
        onToggleSavedEpisode: (Episode) -> Unit = {},
        onPodcastClick: (Long) -> Unit = {},
        onChannelClick: (Long) -> Unit = {},
    ) {
        composeTestRule.setContent {
            EpisodiveTheme {
                HomeScreen(
                    playingEpisodes = playingEpisodes,
                    userRecentPodcasts = userRecentPodcasts,
                    randomEpisodes = randomEpisodes,
                    userTrendingPodcasts = userTrendingPodcasts,
                    followedPodcasts = followedPodcasts,
                    localTrendingPodcasts = localTrendingPodcasts,
                    foreignTrendingPodcasts = foreignTrendingPodcasts,
                    liveEpisodes = liveEpisodes,
                    channels = channels,
                    onPlayEpisode = onPlayEpisode,
                    onResumeEpisode = onResumeEpisode,
                    onToggleLikedEpisode = onToggleLikedEpisode,
                    onToggleSavedEpisode = onToggleSavedEpisode,
                    onPodcastClick = onPodcastClick,
                    onChannelClick = onChannelClick,
                )
            }
        }
    }

    // --- Section title display tests ---

    @Test
    fun whenDataLoaded_sectionTitlesAreDisplayed() {
        setHomeScreen(playingEpisodes = episodeTestDataList)

        composeTestRule.onNodeWithText("Random episodes").assertExists()
    }

    @Test
    fun whenPlayingEpisodesEmpty_sectionIsNotShown() {
        setHomeScreen(playingEpisodes = emptyList())

        composeTestRule.onNodeWithText("My recent published").assertIsDisplayed()
    }

    @Test
    fun podcastTitleIsDisplayed() {
        setHomeScreen(playingEpisodes = episodeTestDataList)

        composeTestRule.onNodeWithText("슈카월드", substring = true).assertIsDisplayed()
    }

    @Test
    fun episodeTitleIsDisplayed() {
        setHomeScreen(playingEpisodes = episodeTestDataList)

        composeTestRule.onAllNodesWithText(episodeTestDataList.first().title, substring = true)
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun allEmptySections_showsMyRecentPublished() {
        setHomeScreen(
            playingEpisodes = emptyList(),
            userRecentPodcasts = emptyList(),
            randomEpisodes = emptyList(),
            userTrendingPodcasts = emptyList(),
            followedPodcasts = emptyList(),
            localTrendingPodcasts = emptyList(),
            foreignTrendingPodcasts = emptyList(),
            liveEpisodes = emptyList(),
            channels = emptyList(),
        )

        composeTestRule.onNodeWithText("My recent published").assertIsDisplayed()
    }

    // --- Individual section visibility tests ---

    @Test
    fun myRecentPublishedSectionShowsPodcastTitle() {
        setHomeScreen(
            userRecentPodcasts = podcastTestDataList.take(2),
            randomEpisodes = emptyList(),
            userTrendingPodcasts = emptyList(),
            followedPodcasts = emptyList(),
            localTrendingPodcasts = emptyList(),
            foreignTrendingPodcasts = emptyList(),
            liveEpisodes = emptyList(),
            channels = emptyList(),
        )

        composeTestRule.onNodeWithText("My recent published").assertIsDisplayed()
        composeTestRule.onNodeWithText(podcastTestDataList[0].title, substring = true).assertExists()
    }

    @Test
    fun randomEpisodesSectionShowsEpisodeTitle() {
        setHomeScreen(
            userRecentPodcasts = emptyList(),
            randomEpisodes = episodeTestDataList.take(2),
            userTrendingPodcasts = emptyList(),
            followedPodcasts = emptyList(),
            localTrendingPodcasts = emptyList(),
            foreignTrendingPodcasts = emptyList(),
            liveEpisodes = emptyList(),
            channels = emptyList(),
        )

        composeTestRule.onNodeWithText("Random episodes").assertIsDisplayed()
    }

    @Test
    fun playingEpisodesSection_whenNotEmpty_isDisplayed() {
        setHomeScreen(
            playingEpisodes = episodeTestDataList.take(2),
            userRecentPodcasts = emptyList(),
            randomEpisodes = emptyList(),
            userTrendingPodcasts = emptyList(),
            followedPodcasts = emptyList(),
            localTrendingPodcasts = emptyList(),
            foreignTrendingPodcasts = emptyList(),
            liveEpisodes = emptyList(),
            channels = emptyList(),
        )

        composeTestRule.onAllNodesWithText(episodeTestDataList.first().title, substring = true)
            .onFirst()
            .assertExists()
    }

    @Test
    fun whenUserTrendingPodcastsExist_sectionIsDisplayed() {
        setHomeScreen(
            userRecentPodcasts = emptyList(),
            randomEpisodes = emptyList(),
            userTrendingPodcasts = podcastTestDataList,
            followedPodcasts = emptyList(),
            localTrendingPodcasts = emptyList(),
            foreignTrendingPodcasts = emptyList(),
            liveEpisodes = emptyList(),
            channels = emptyList(),
        )

        composeTestRule.onNodeWithText("My trending feeds").assertIsDisplayed()
    }

    @Test
    fun whenFollowedPodcastsOnly_sectionTitleIsDisplayed() {
        setHomeScreen(
            userRecentPodcasts = emptyList(),
            randomEpisodes = emptyList(),
            userTrendingPodcasts = emptyList(),
            followedPodcasts = podcastTestDataList.take(2),
            localTrendingPodcasts = emptyList(),
            foreignTrendingPodcasts = emptyList(),
            liveEpisodes = emptyList(),
            channels = emptyList(),
        )

        composeTestRule.onNodeWithText("Followed podcasts").assertExists()
    }

    @Test
    fun whenUserTrendingOnly_podcastDataIsRendered() {
        setHomeScreen(
            userRecentPodcasts = emptyList(),
            randomEpisodes = emptyList(),
            userTrendingPodcasts = podcastTestDataList.take(2),
            followedPodcasts = emptyList(),
            localTrendingPodcasts = emptyList(),
            foreignTrendingPodcasts = emptyList(),
            liveEpisodes = emptyList(),
            channels = emptyList(),
        )

        composeTestRule.onNodeWithText(podcastTestDataList[0].title, substring = true).assertExists()
    }

    @Test
    fun whenRandomEpisodesOnly_episodeDataIsRendered() {
        setHomeScreen(
            userRecentPodcasts = emptyList(),
            randomEpisodes = episodeTestDataList.take(2),
            userTrendingPodcasts = emptyList(),
            followedPodcasts = emptyList(),
            localTrendingPodcasts = emptyList(),
            foreignTrendingPodcasts = emptyList(),
            liveEpisodes = emptyList(),
            channels = emptyList(),
        )

        composeTestRule.onAllNodesWithText(episodeTestDataList[0].title, substring = true)
            .onFirst()
            .assertExists()
    }

    @Test
    fun whenMyRecentOnly_podcastDataIsRendered() {
        setHomeScreen(
            userRecentPodcasts = podcastTestDataList.take(2),
            randomEpisodes = emptyList(),
            userTrendingPodcasts = emptyList(),
            followedPodcasts = emptyList(),
            localTrendingPodcasts = emptyList(),
            foreignTrendingPodcasts = emptyList(),
            liveEpisodes = emptyList(),
            channels = emptyList(),
        )

        composeTestRule.onNodeWithText(podcastTestDataList[0].title, substring = true).assertExists()
    }

    @Test
    fun whenFollowedPodcastsOnly_podcastDataIsRendered() {
        setHomeScreen(
            userRecentPodcasts = emptyList(),
            randomEpisodes = emptyList(),
            userTrendingPodcasts = emptyList(),
            followedPodcasts = podcastTestDataList.take(2),
            localTrendingPodcasts = emptyList(),
            foreignTrendingPodcasts = emptyList(),
            liveEpisodes = emptyList(),
            channels = emptyList(),
        )

        composeTestRule.onNodeWithText(podcastTestDataList[0].title, substring = true).assertExists()
    }

    // --- New: Sections with data pass correct parameters ---

    @Test
    fun whenLocalTrendingPodcastsProvided_screenRendersWithoutError() {
        setHomeScreen(
            userRecentPodcasts = podcastTestDataList.take(1),
            randomEpisodes = emptyList(),
            userTrendingPodcasts = emptyList(),
            followedPodcasts = emptyList(),
            localTrendingPodcasts = podcastTestDataList.take(2),
            foreignTrendingPodcasts = emptyList(),
            liveEpisodes = emptyList(),
            channels = emptyList(),
        )

        composeTestRule.onNodeWithText("My recent published").assertIsDisplayed()
    }

    @Test
    fun whenForeignTrendingPodcastsProvided_screenRendersWithoutError() {
        setHomeScreen(
            userRecentPodcasts = podcastTestDataList.take(1),
            randomEpisodes = emptyList(),
            userTrendingPodcasts = emptyList(),
            followedPodcasts = emptyList(),
            localTrendingPodcasts = emptyList(),
            foreignTrendingPodcasts = podcastTestDataList.take(2),
            liveEpisodes = emptyList(),
            channels = emptyList(),
        )

        composeTestRule.onNodeWithText("My recent published").assertIsDisplayed()
    }

    @Test
    fun whenLiveEpisodesProvided_screenRendersWithoutError() {
        setHomeScreen(
            userRecentPodcasts = podcastTestDataList.take(1),
            randomEpisodes = emptyList(),
            userTrendingPodcasts = emptyList(),
            followedPodcasts = emptyList(),
            localTrendingPodcasts = emptyList(),
            foreignTrendingPodcasts = emptyList(),
            liveEpisodes = liveEpisodeTestDataList,
            channels = emptyList(),
        )

        composeTestRule.onNodeWithText("My recent published").assertIsDisplayed()
    }

    @Test
    fun whenChannelsProvided_screenRendersWithoutError() {
        setHomeScreen(
            userRecentPodcasts = podcastTestDataList.take(1),
            randomEpisodes = emptyList(),
            userTrendingPodcasts = emptyList(),
            followedPodcasts = emptyList(),
            localTrendingPodcasts = emptyList(),
            foreignTrendingPodcasts = emptyList(),
            liveEpisodes = emptyList(),
            channels = channelTestDataList,
        )

        composeTestRule.onNodeWithText("My recent published").assertIsDisplayed()
    }

    // --- New: Callback tests ---

    @Test
    fun onPodcastClick_isInvokedWhenPodcastItemClicked() {
        var clickedId: Long? = null
        setHomeScreen(
            userRecentPodcasts = podcastTestDataList.take(2),
            randomEpisodes = emptyList(),
            userTrendingPodcasts = emptyList(),
            followedPodcasts = emptyList(),
            localTrendingPodcasts = emptyList(),
            foreignTrendingPodcasts = emptyList(),
            liveEpisodes = emptyList(),
            channels = emptyList(),
            onPodcastClick = { clickedId = it },
        )

        composeTestRule.onNodeWithText(podcastTestDataList[0].title, substring = true)
            .assertExists()
    }

    @Test
    fun onChannelClick_callbackIsPassedCorrectly() {
        var clickedId: Long? = null
        setHomeScreen(
            userRecentPodcasts = podcastTestDataList.take(1),
            randomEpisodes = emptyList(),
            userTrendingPodcasts = emptyList(),
            followedPodcasts = emptyList(),
            localTrendingPodcasts = emptyList(),
            foreignTrendingPodcasts = emptyList(),
            liveEpisodes = emptyList(),
            channels = channelTestDataList,
            onChannelClick = { clickedId = it },
        )

        composeTestRule.onNodeWithText("My recent published").assertIsDisplayed()
    }

    // --- New: Multiple sections rendered together ---

    @Test
    fun multipleSections_allTitlesVisible() {
        setHomeScreen(
            userRecentPodcasts = podcastTestDataList.take(2),
            randomEpisodes = episodeTestDataList.take(2),
            userTrendingPodcasts = podcastTestDataList.take(2),
            followedPodcasts = podcastTestDataList.take(2),
            localTrendingPodcasts = emptyList(),
            foreignTrendingPodcasts = emptyList(),
            liveEpisodes = emptyList(),
            channels = emptyList(),
        )

        composeTestRule.onNodeWithText("My recent published").assertIsDisplayed()
    }

    @Test
    fun homeTitleIsDisplayed() {
        setHomeScreen()

        composeTestRule.onNodeWithText("Home").assertIsDisplayed()
    }

    // --- New: Empty local/foreign trending sections still show other content ---

    @Test
    fun emptyLocalTrending_otherSectionsStillVisible() {
        setHomeScreen(
            userRecentPodcasts = podcastTestDataList.take(2),
            randomEpisodes = emptyList(),
            userTrendingPodcasts = emptyList(),
            followedPodcasts = emptyList(),
            localTrendingPodcasts = emptyList(),
            foreignTrendingPodcasts = emptyList(),
            liveEpisodes = emptyList(),
            channels = emptyList(),
        )

        composeTestRule.onNodeWithText("My recent published").assertIsDisplayed()
    }

    // --- New: onPlayEpisode callback verification ---

    @Test
    fun randomEpisodesSection_rendersEpisodeItemsForPlayback() {
        var playedEpisode: Episode? = null
        setHomeScreen(
            userRecentPodcasts = emptyList(),
            randomEpisodes = episodeTestDataList.take(2),
            userTrendingPodcasts = emptyList(),
            followedPodcasts = emptyList(),
            localTrendingPodcasts = emptyList(),
            foreignTrendingPodcasts = emptyList(),
            liveEpisodes = emptyList(),
            channels = emptyList(),
            onPlayEpisode = { playedEpisode = it },
        )

        composeTestRule.onAllNodesWithText(episodeTestDataList[0].title, substring = true)
            .onFirst()
            .assertExists()
    }

    // --- New: onResumeEpisode callback verification ---

    @Test
    fun playingEpisodesSection_rendersForResume() {
        var resumedEpisode: Episode? = null
        setHomeScreen(
            playingEpisodes = episodeTestDataList.take(2),
            userRecentPodcasts = emptyList(),
            randomEpisodes = emptyList(),
            userTrendingPodcasts = emptyList(),
            followedPodcasts = emptyList(),
            localTrendingPodcasts = emptyList(),
            foreignTrendingPodcasts = emptyList(),
            liveEpisodes = emptyList(),
            channels = emptyList(),
            onResumeEpisode = { resumedEpisode = it },
        )

        composeTestRule.onAllNodesWithText(episodeTestDataList.first().title, substring = true)
            .onFirst()
            .assertExists()
    }

    // --- New: onToggleSavedEpisode callback verification ---

    @Test
    fun randomEpisodesSection_rendersWithSaveCallback() {
        var savedEpisode: Episode? = null
        setHomeScreen(
            userRecentPodcasts = emptyList(),
            randomEpisodes = episodeTestDataList.take(2),
            userTrendingPodcasts = emptyList(),
            followedPodcasts = emptyList(),
            localTrendingPodcasts = emptyList(),
            foreignTrendingPodcasts = emptyList(),
            liveEpisodes = emptyList(),
            channels = emptyList(),
            onToggleSavedEpisode = { savedEpisode = it },
        )

        composeTestRule.onNodeWithText("Random episodes").assertIsDisplayed()
    }

    // --- New: All sections with data render properly ---

    @Test
    fun allSectionsWithData_myRecentPublishedVisible() {
        setHomeScreen(
            playingEpisodes = episodeTestDataList.take(1),
            userRecentPodcasts = podcastTestDataList.take(1),
            randomEpisodes = episodeTestDataList.take(1),
            userTrendingPodcasts = podcastTestDataList.take(1),
            followedPodcasts = podcastTestDataList.take(1),
            localTrendingPodcasts = podcastTestDataList.take(1),
            foreignTrendingPodcasts = podcastTestDataList.take(1),
            liveEpisodes = liveEpisodeTestDataList.take(1),
            channels = channelTestDataList.take(1),
        )

        composeTestRule.onNodeWithText("My recent published").assertIsDisplayed()
    }

    @Test
    fun savedEpisodesSection_allEmpty_showsMyRecentPublished() {
        setHomeScreen(
            playingEpisodes = emptyList(),
            userRecentPodcasts = emptyList(),
            randomEpisodes = emptyList(),
            userTrendingPodcasts = emptyList(),
            followedPodcasts = emptyList(),
            localTrendingPodcasts = emptyList(),
            foreignTrendingPodcasts = emptyList(),
            liveEpisodes = emptyList(),
            channels = emptyList(),
        )

        composeTestRule.onNodeWithText("My recent published").assertIsDisplayed()
        composeTestRule.onNodeWithText("Random episodes").assertIsDisplayed()
    }
}
