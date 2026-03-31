package io.jacob.episodive.feature.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.PagingData
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.SelectableCategory
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.podcastTestDataList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LibraryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setLibraryScreen(
        query: String = "",
        onQueryChange: (String) -> Unit = {},
        onFind: (String) -> Unit = {},
        section: LibrarySection = LibrarySection.All,
        onSectionChange: (LibrarySection) -> Unit = {},
        playedEpisodes: List<Episode> = episodeTestDataList,
        likedEpisodes: List<Episode> = episodeTestDataList,
        savedEpisodes: List<Episode> = episodeTestDataList,
        followedPodcasts: List<Podcast> = podcastTestDataList,
        preferredCategories: List<Category> = listOf(Category.BUSINESS, Category.COMEDY),
        selectableCategories: List<SelectableCategory> = Category.entries.map { SelectableCategory(it, true) },
        playedEpisodesPaging: Flow<PagingData<SeparatedUiModel<Episode>>> = flowOf(PagingData.from(episodeTestDataList.map { SeparatedUiModel.Content(it) })),
        likedEpisodesPaging: Flow<PagingData<SeparatedUiModel<Episode>>> = flowOf(PagingData.from(episodeTestDataList.map { SeparatedUiModel.Content(it) })),
        savedEpisodesPaging: Flow<PagingData<SeparatedUiModel<Episode>>> = flowOf(PagingData.from(episodeTestDataList.map { SeparatedUiModel.Content(it) })),
        followedPodcastsPaging: Flow<PagingData<SeparatedUiModel<Podcast>>> = flowOf(PagingData.from(podcastTestDataList.map { SeparatedUiModel.Content(it) })),
        onPlayedEpisodeClick: (Episode) -> Unit = {},
        onEpisodeClick: (Episode) -> Unit = {},
        onPodcastClick: (Podcast) -> Unit = {},
        onToggleLikedEpisode: (Episode) -> Unit = {},
        onToggleSavedEpisode: (Episode) -> Unit = {},
        onToggleFollowedPodcast: (Podcast) -> Unit = {},
        onTogglePreferredCategory: (Category) -> Unit = {},
    ) {
        composeTestRule.setContent {
            EpisodiveTheme {
                LibraryScreen(
                    query = query,
                    onQueryChange = onQueryChange,
                    onFind = onFind,
                    section = section,
                    onSectionChange = onSectionChange,
                    playedEpisodes = playedEpisodes,
                    likedEpisodes = likedEpisodes,
                    savedEpisodes = savedEpisodes,
                    followedPodcasts = followedPodcasts,
                    preferredCategories = preferredCategories,
                    selectableCategories = selectableCategories,
                    playedEpisodesPaging = playedEpisodesPaging,
                    likedEpisodesPaging = likedEpisodesPaging,
                    savedEpisodesPaging = savedEpisodesPaging,
                    followedPodcastsPaging = followedPodcastsPaging,
                    onPlayedEpisodeClick = onPlayedEpisodeClick,
                    onEpisodeClick = onEpisodeClick,
                    onPodcastClick = onPodcastClick,
                    onToggleLikedEpisode = onToggleLikedEpisode,
                    onToggleSavedEpisode = onToggleSavedEpisode,
                    onToggleFollowedPodcast = onToggleFollowedPodcast,
                    onTogglePreferredCategory = onTogglePreferredCategory,
                )
            }
        }
    }

    // --- Display tests ---

    @Test
    fun allSection_displaysData() {
        setLibraryScreen()

        composeTestRule.onNodeWithText("Recently listened episodes").assertIsDisplayed()
    }

    @Test
    fun filterChipsAreDisplayed() {
        setLibraryScreen()

        composeTestRule.onNodeWithText("All").assertIsDisplayed()
        composeTestRule.onNodeWithText("Liked").assertIsDisplayed()
        composeTestRule.onNodeWithText("Followed").assertIsDisplayed()
    }

    @Test
    fun libraryTitleIsDisplayed() {
        setLibraryScreen()

        composeTestRule.onNodeWithText("Library").assertIsDisplayed()
    }

    @Test
    fun recentlyListenedSection_displaysData() {
        setLibraryScreen(section = LibrarySection.RecentlyListened)

        composeTestRule.onNodeWithText(episodeTestDataList.first().title, substring = true)
            .assertExists()
    }

    @Test
    fun likedSection_displaysData() {
        setLibraryScreen(section = LibrarySection.Liked)

        composeTestRule.onNodeWithText(episodeTestDataList.first().title, substring = true)
            .assertExists()
    }

    @Test
    fun followedSection_displaysData() {
        setLibraryScreen(section = LibrarySection.Followed)

        composeTestRule.onNodeWithText(podcastTestDataList.first().title, substring = true)
            .assertExists()
    }

    @Test
    fun preferredSection_displaysCategories() {
        setLibraryScreen(section = LibrarySection.Preferred)

        composeTestRule.onNodeWithText("Arts", substring = true).assertExists()
    }

    @Test
    fun allSection_withEmptyData_showsNoResults() {
        setLibraryScreen(
            playedEpisodes = emptyList(),
            likedEpisodes = emptyList(),
            savedEpisodes = emptyList(),
            followedPodcasts = emptyList(),
            preferredCategories = emptyList(),
            selectableCategories = emptyList(),
            playedEpisodesPaging = flowOf(PagingData.from(emptyList())),
            likedEpisodesPaging = flowOf(PagingData.from(emptyList())),
            savedEpisodesPaging = flowOf(PagingData.from(emptyList())),
            followedPodcastsPaging = flowOf(PagingData.from(emptyList())),
        )

        composeTestRule.onNodeWithText("No results found.").assertIsDisplayed()
    }

    @Test
    fun allSection_displaysLikedEpisodesSection() {
        setLibraryScreen()

        composeTestRule.onNodeWithText("Liked episodes").assertIsDisplayed()
    }

    @Test
    fun allSection_displaysFollowedPodcastsSection() {
        setLibraryScreen()

        composeTestRule.onNodeWithText("Followed podcasts").assertExists()
    }

    @Test
    fun allSection_displaysPreferredCategoriesSection() {
        setLibraryScreen(
            playedEpisodes = emptyList(),
            likedEpisodes = emptyList(),
            savedEpisodes = emptyList(),
            followedPodcasts = emptyList(),
            preferredCategories = listOf(Category.BUSINESS),
            playedEpisodesPaging = flowOf(PagingData.from(emptyList())),
            likedEpisodesPaging = flowOf(PagingData.from(emptyList())),
            savedEpisodesPaging = flowOf(PagingData.from(emptyList())),
            followedPodcastsPaging = flowOf(PagingData.from(emptyList())),
        )

        composeTestRule.onNodeWithText("Preferred categories").assertIsDisplayed()
    }

    @Test
    fun recentlyListenedFilterChipIsDisplayed() {
        setLibraryScreen()

        composeTestRule.onNodeWithText("Recently listened").assertIsDisplayed()
    }

    @Test
    fun preferredFilterChipIsDisplayed() {
        setLibraryScreen()

        composeTestRule.onNodeWithText("Preferred").assertIsDisplayed()
    }

    @Test
    fun savedSectionTitle_isDisplayed() {
        setLibraryScreen()

        composeTestRule.onNodeWithText("Saved").assertIsDisplayed()
    }

    @Test
    fun searchFieldIsDisplayed() {
        setLibraryScreen()

        composeTestRule.onNodeWithContentDescription("search").assertExists()
    }

    @Test
    fun followedPodcastsSection_displaysData() {
        setLibraryScreen()

        composeTestRule.onNodeWithText(podcastTestDataList.first().title, substring = true)
            .assertExists()
    }

    // --- New: Saved section ---

    @Test
    fun savedSection_displaysData() {
        setLibraryScreen(section = LibrarySection.Saved)

        composeTestRule.onNodeWithText(episodeTestDataList.first().title, substring = true)
            .assertExists()
    }

    // --- New: All section conditional rendering ---

    @Test
    fun allSection_onlyPlayedEpisodes_showsRecentlyListened() {
        setLibraryScreen(
            playedEpisodes = episodeTestDataList.take(2),
            likedEpisodes = emptyList(),
            savedEpisodes = emptyList(),
            followedPodcasts = emptyList(),
            preferredCategories = emptyList(),
        )

        composeTestRule.onNodeWithText("Recently listened episodes").assertIsDisplayed()
    }

    @Test
    fun allSection_onlyLikedEpisodes_showsLikedSection() {
        setLibraryScreen(
            playedEpisodes = emptyList(),
            likedEpisodes = episodeTestDataList.take(2),
            savedEpisodes = emptyList(),
            followedPodcasts = emptyList(),
            preferredCategories = emptyList(),
        )

        composeTestRule.onNodeWithText("Liked episodes").assertIsDisplayed()
    }

    @Test
    fun allSection_onlySavedEpisodes_showsSavedSection() {
        setLibraryScreen(
            playedEpisodes = emptyList(),
            likedEpisodes = emptyList(),
            savedEpisodes = episodeTestDataList.take(2),
            followedPodcasts = emptyList(),
            preferredCategories = emptyList(),
        )

        composeTestRule.onNodeWithText("Saved episodes").assertIsDisplayed()
    }

    @Test
    fun allSection_onlyFollowedPodcasts_showsFollowedSection() {
        setLibraryScreen(
            playedEpisodes = emptyList(),
            likedEpisodes = emptyList(),
            savedEpisodes = emptyList(),
            followedPodcasts = podcastTestDataList.take(2),
            preferredCategories = emptyList(),
        )

        composeTestRule.onNodeWithText("Followed podcasts").assertExists()
    }

    @Test
    fun allSection_onlyPreferredCategories_showsPreferredSection() {
        setLibraryScreen(
            playedEpisodes = emptyList(),
            likedEpisodes = emptyList(),
            savedEpisodes = emptyList(),
            followedPodcasts = emptyList(),
            preferredCategories = listOf(Category.ARTS),
        )

        composeTestRule.onNodeWithText("Preferred categories").assertIsDisplayed()
    }

    // --- New: Filter chip section change callback ---

    @Test
    fun clickLikedFilterChip_callbackInvoked() {
        var selectedSection: LibrarySection? = null
        setLibraryScreen(
            onSectionChange = { selectedSection = it },
        )

        composeTestRule.onNodeWithText("Liked").performClick()
        assert(selectedSection == LibrarySection.Liked)
    }

    @Test
    fun clickFollowedFilterChip_callbackInvoked() {
        var selectedSection: LibrarySection? = null
        setLibraryScreen(
            onSectionChange = { selectedSection = it },
        )

        composeTestRule.onNodeWithText("Followed").performClick()
        assert(selectedSection == LibrarySection.Followed)
    }

    @Test
    fun clickSavedFilterChip_callbackInvoked() {
        var selectedSection: LibrarySection? = null
        setLibraryScreen(
            onSectionChange = { selectedSection = it },
        )

        composeTestRule.onNodeWithText("Saved").performClick()
        assert(selectedSection == LibrarySection.Saved)
    }

    @Test
    fun clickPreferredFilterChip_callbackInvoked() {
        var selectedSection: LibrarySection? = null
        setLibraryScreen(
            onSectionChange = { selectedSection = it },
        )

        composeTestRule.onNodeWithText("Preferred").performClick()
        assert(selectedSection == LibrarySection.Preferred)
    }

    @Test
    fun clickRecentlyListenedFilterChip_callbackInvoked() {
        var selectedSection: LibrarySection? = null
        setLibraryScreen(
            onSectionChange = { selectedSection = it },
        )

        composeTestRule.onNodeWithText("Recently listened").performClick()
        assert(selectedSection == LibrarySection.RecentlyListened)
    }

    // --- New: All section shows saved episodes ---

    @Test
    fun allSection_displaysSavedEpisodesSection() {
        setLibraryScreen()

        composeTestRule.onNodeWithText("Saved episodes").assertExists()
    }

    // --- New: Empty played but other data ---

    @Test
    fun allSection_emptyPlayed_noRecentlyListenedSection() {
        setLibraryScreen(
            playedEpisodes = emptyList(),
            likedEpisodes = episodeTestDataList,
            savedEpisodes = episodeTestDataList,
            followedPodcasts = podcastTestDataList,
            preferredCategories = listOf(Category.BUSINESS),
        )

        composeTestRule.onNodeWithText("Recently listened episodes").assertDoesNotExist()
        composeTestRule.onNodeWithText("Liked episodes").assertIsDisplayed()
    }

    @Test
    fun allSection_emptyLiked_noLikedSection() {
        setLibraryScreen(
            playedEpisodes = episodeTestDataList,
            likedEpisodes = emptyList(),
            savedEpisodes = episodeTestDataList,
            followedPodcasts = podcastTestDataList,
            preferredCategories = listOf(Category.BUSINESS),
        )

        composeTestRule.onNodeWithText("Liked episodes").assertDoesNotExist()
    }

    @Test
    fun allSection_emptySaved_noSavedSection() {
        setLibraryScreen(
            playedEpisodes = episodeTestDataList,
            likedEpisodes = episodeTestDataList,
            savedEpisodes = emptyList(),
            followedPodcasts = podcastTestDataList,
            preferredCategories = listOf(Category.BUSINESS),
        )

        composeTestRule.onNodeWithText("Saved episodes").assertDoesNotExist()
    }

    @Test
    fun allSection_emptyFollowed_noFollowedSection() {
        setLibraryScreen(
            playedEpisodes = episodeTestDataList,
            likedEpisodes = episodeTestDataList,
            savedEpisodes = episodeTestDataList,
            followedPodcasts = emptyList(),
            preferredCategories = listOf(Category.BUSINESS),
        )

        composeTestRule.onNodeWithText("Followed podcasts").assertDoesNotExist()
    }

    @Test
    fun allSection_emptyPreferred_noPreferredSection() {
        setLibraryScreen(
            playedEpisodes = episodeTestDataList,
            likedEpisodes = episodeTestDataList,
            savedEpisodes = episodeTestDataList,
            followedPodcasts = podcastTestDataList,
            preferredCategories = emptyList(),
        )

        composeTestRule.onNodeWithText("Preferred categories").assertDoesNotExist()
    }
}
