package io.jacob.episodive.feature.home

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.model.Channel
import io.jacob.episodive.core.model.DataError
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.testing.model.channelTestDataList
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.liveEpisodeTestDataList
import io.jacob.episodive.core.testing.model.podcastTestDataList
import io.jacob.episodive.core.testing.model.radioStationsTestData
import io.jacob.episodive.feature.home.navigation.HomeSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.seconds

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
        radio: List<Station> = emptyList(),
        onRadioMore: () -> Unit = {},
        onPlayEpisode: (Episode) -> Unit = {},
        onResumeEpisode: (Episode) -> Unit = {},
        onToggleLikedEpisode: (Episode) -> Unit = {},
        onToggleSavedEpisode: (Episode) -> Unit = {},
        onPodcastClick: (Long) -> Unit = {},
        onChannelClick: (Long) -> Unit = {},
        onMoreClick: (HomeSection) -> Unit = {},
        onSearchClick: () -> Unit = {},
        // 이어듣기 목록을 화면이 뜬 뒤에 바꿔야 하는 테스트를 위한 통로. 주면 [playingEpisodes]
        // 대신 이 상태를 읽어, 값을 바꾸는 것만으로 카드가 다시 그려진다.
        playingEpisodesState: State<List<Episode>>? = null,
        // 섹션 하나만 로딩·실패로 두는 테스트를 위한 통로. 목록으로 받는 위 인자들은 전부
        // "이미 도착한" 섹션이라, 아직 오지 않은 상태는 이쪽으로만 만들 수 있다.
        //
        // 자리 예약(로딩 자리가 도착할 콘텐츠와 같은 높이인가)은 여기서 검증할 수 없다.
        // Robolectric 이 TextStyle.lineHeight 를 반영하지 않아 — 80sp 를 줘도 Text 높이가
        // 그대로다 — 스켈레톤이 참조하는 lineHeight(17dp)와 실제 Text 높이(35dp)가 이 환경
        // 에서만 두 배 가까이 벌어진다. 높이를 비교하는 테스트를 여기 두면 실제와 무관하게
        // 늘 실패한다. 정합은 양쪽이 같은 상수를 참조하는 구조로 지키고, 눈으로는 기기나
        // HomeScreenPartiallyLoadedPreview 로 확인한다.
        randomEpisodesState: State<SectionState<Episode>>? = null,
    ) {
        composeTestRule.setContent {
            EpisodiveTheme {
                HomeScreen(
                    playingEpisodes = SectionState.Success(
                        playingEpisodesState?.value ?: playingEpisodes
                    ),
                    userRecentPodcasts = SectionState.Success(userRecentPodcasts),
                    randomEpisodes = randomEpisodesState?.value
                        ?: SectionState.Success(randomEpisodes),
                    userTrendingPodcasts = SectionState.Success(userTrendingPodcasts),
                    followedPodcasts = SectionState.Success(followedPodcasts),
                    localTrendingPodcasts = SectionState.Success(localTrendingPodcasts),
                    foreignTrendingPodcasts = SectionState.Success(foreignTrendingPodcasts),
                    liveEpisodes = SectionState.Success(liveEpisodes),
                    channels = SectionState.Success(channels),
                    radio = SectionState.Success(radio),
                    onRadioMore = onRadioMore,
                    onPlayEpisode = onPlayEpisode,
                    onResumeEpisode = onResumeEpisode,
                    onToggleLikedEpisode = onToggleLikedEpisode,
                    onToggleSavedEpisode = onToggleSavedEpisode,
                    onPodcastClick = onPodcastClick,
                    onChannelClick = onChannelClick,
                    onMoreClick = onMoreClick,
                    onSearchClick = onSearchClick,
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
    fun podcastSection_showsMoreAction() {
        // contentDescription 이 섹션 제목을 담으므로 스크린리더 사용자도 어느 섹션의 더
        // 보기인지 구분할 수 있다.
        setHomeScreen()

        composeTestRule.onNodeWithContentDescription("See all My recent published").assertExists()
    }

    @Test
    fun episodeSection_showsMoreAction() {
        // 에피소드 섹션에도 붙는다. 앞 섹션을 비워 화면 안으로 끌어올린다 — LazyColumn 은
        // 화면 밖 항목을 컴포즈하지 않는다.
        setHomeScreen(userRecentPodcasts = emptyList())

        composeTestRule.onNodeWithContentDescription("See all Random episodes").assertExists()
    }

    @Test
    fun moreAction_clicked_callbackReceivesMatchingSection() {
        var clickedSection: HomeSection? = null
        setHomeScreen(onMoreClick = { clickedSection = it })

        composeTestRule
            .onNodeWithContentDescription("See all My recent published")
            .performClick()

        assertEquals(HomeSection.MyRecentPodcasts, clickedSection)
    }

    @Test
    fun moreAction_differentSection_callbackReceivesThatSection() {
        // 섹션마다 다른 값이 실려야 한다. 하나로 뭉뚱그리면 어느 더 보기를 눌러도 같은
        // 목록이 열린다.
        var clickedSection: HomeSection? = null
        setHomeScreen(
            userRecentPodcasts = emptyList(),
            onMoreClick = { clickedSection = it },
        )

        composeTestRule
            .onNodeWithContentDescription("See all Random episodes")
            .performClick()

        assertEquals(HomeSection.RandomEpisodes, clickedSection)
    }

    @Test
    fun emptySection_moreActionIsNotShown() {
        // 섹션 자체가 빠지면 그 더 보기도 함께 사라져야 한다. 갈 곳은 있는데 목록이 비어
        // 있는 화면으로 보내면 막다른 길이 된다.
        setHomeScreen(userRecentPodcasts = emptyList())

        composeTestRule
            .onNodeWithContentDescription("See all My recent published")
            .assertDoesNotExist()
    }

    @Test
    fun emptySection_titleIsNotShown() {
        // 데이터가 없는 섹션은 제목만 남지 않도록 통째로 빠진다.
        setHomeScreen(
            userRecentPodcasts = emptyList(),
            randomEpisodes = emptyList(),
        )

        composeTestRule.onNodeWithText("My recent published").assertDoesNotExist()
        composeTestRule.onNodeWithText("Random episodes").assertDoesNotExist()
        composeTestRule.onNodeWithText("My trending feeds").assertIsDisplayed()
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
    fun whenRadioPresent_moreActionOpensRadio() {
        var radioMoreCount = 0
        setHomeScreen(
            userRecentPodcasts = emptyList(),
            randomEpisodes = emptyList(),
            userTrendingPodcasts = emptyList(),
            followedPodcasts = emptyList(),
            localTrendingPodcasts = emptyList(),
            foreignTrendingPodcasts = emptyList(),
            liveEpisodes = emptyList(),
            channels = emptyList(),
            radio = radioStationsTestData,
            onRadioMore = { radioMoreCount++ },
        )

        composeTestRule.onNodeWithText("Radio").assertExists()
        composeTestRule.onNodeWithContentDescription("See all Radio").performClick()

        assertEquals(1, radioMoreCount)
    }

    @Test
    fun whenRadioAbsent_radioSectionIsNotShownAndOtherSectionsKeepTheirKeys() {
        setHomeScreen()

        composeTestRule.onNodeWithText("Radio").assertDoesNotExist()
        composeTestRule.onNodeWithText("My recent published").assertExists()
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

    // --- Search shortcut ---

    @Test
    fun header_showsSearchAction() {
        setHomeScreen()

        composeTestRule.onNodeWithContentDescription("Search").assertIsDisplayed()
    }

    @Test
    fun searchAction_clicked_invokesCallback() {
        var searchClicked = false
        setHomeScreen(onSearchClick = { searchClicked = true })

        composeTestRule.onNodeWithContentDescription("Search").performClick()

        assertTrue(searchClicked)
    }

    @Test
    fun searchAction_isShownEvenWhenEveryFeedIsEmpty() {
        // 검색은 홈 데이터와 무관한 출구다. 피드가 통째로 비어 볼 것이 없을 때야말로
        // 가장 필요한 버튼이라, 섹션이 하나도 안 그려지는 상황에서도 남아 있어야 한다.
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

        composeTestRule.onNodeWithContentDescription("Search").assertIsDisplayed()
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
    fun allEmpty_hidesEverySectionTitle() {
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

        composeTestRule.onNodeWithText("My recent published").assertDoesNotExist()
        composeTestRule.onNodeWithText("Random episodes").assertDoesNotExist()
        composeTestRule.onNodeWithText("Channels worth listening to").assertDoesNotExist()
    }

    // --- 이어듣기 카드 진행바 ---

    @Test
    fun continueListeningHero_progressBarKeepsWidthWhenRemainLabelShrinks() {
        // 남은 시간 표기는 "5min 1sec Left" → "5min Left" 처럼 항목이 통째로 빠지며 좁아진다.
        // 그 자리를 매번 다시 재면 나머지를 weight 로 먹는 진행바가 함께 늘었다 줄었다 하며
        // 재생 내내 출렁인다.
        val base = episodeTestDataList.first()
        val duration = requireNotNull(base.duration)
        val playingEpisodes = mutableStateOf(listOf(base.copy(position = duration - 301.seconds)))

        // 다른 섹션을 모두 비워 화면 안의 진행바가 이 카드의 것 하나뿐이게 만든다 —
        // [continueListeningProgressBarWidth] 가 그 유일성에 기댄다.
        setHomeScreen(
            userRecentPodcasts = emptyList(),
            randomEpisodes = emptyList(),
            userTrendingPodcasts = emptyList(),
            followedPodcasts = emptyList(),
            localTrendingPodcasts = emptyList(),
            foreignTrendingPodcasts = emptyList(),
            liveEpisodes = emptyList(),
            channels = emptyList(),
            playingEpisodesState = playingEpisodes,
        )

        composeTestRule.onNodeWithText("5min 1sec Left").assertExists()
        val widthBefore = continueListeningProgressBarWidth()

        composeTestRule.runOnIdle {
            playingEpisodes.value = listOf(base.copy(position = duration - 300.seconds))
        }

        composeTestRule.onNodeWithText("5min Left").assertExists()
        assertEquals(widthBefore, continueListeningProgressBarWidth())
    }

    // --- Per-section loading tests ---

    @Test
    fun loadingSection_doesNotBlockTheRest() {
        // 이 화면이 고쳐야 했던 것. 랜덤 에피소드처럼 유독 느린 소스 하나가 아직 도착하지
        // 않아도 이미 온 섹션들은 그대로 보이고 눌린다. 예전에는 아홉 개를 값째로 combine 해
        // 가장 느린 하나가 홈 전체를 스켈레톤에 붙잡아 두었다.
        setHomeScreen(randomEpisodesState = mutableStateOf(SectionState.Loading))

        composeTestRule.onNodeWithText("My recent published").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("See all My recent published").assertExists()
        // 아직 오지 않은 섹션은 스켈레톤이라 제목이 없다.
        composeTestRule.onNodeWithText("Random episodes").assertDoesNotExist()
    }

    @Test
    fun failedSection_isSkippedAndTheRestSurvives() {
        // 섹션 하나가 실패해도 홈 전체가 오류 화면으로 넘어가지 않는다. 빈 섹션과 똑같이
        // 그 자리만 빠진다 — RemoteUpdater 가 예외를 올리는 것은 캐시조차 없을 때뿐이라
        // 대신 보여줄 것이 없다.
        setHomeScreen(
            randomEpisodesState = mutableStateOf(SectionState.Error(DataError.Offline)),
        )

        composeTestRule.onNodeWithText("My recent published").assertIsDisplayed()
        composeTestRule.onNodeWithText("Random episodes").assertDoesNotExist()
        composeTestRule.onNodeWithText("My trending feeds").assertIsDisplayed()
    }

    private fun continueListeningProgressBarWidth(): Dp {
        val bounds = composeTestRule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .getUnclippedBoundsInRoot()

        return bounds.right - bounds.left
    }
}
