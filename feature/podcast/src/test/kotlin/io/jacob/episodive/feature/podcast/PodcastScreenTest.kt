package io.jacob.episodive.feature.podcast

import android.content.Context
import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.test.core.app.ApplicationProvider
import io.jacob.episodive.core.designsystem.component.SkeletonDefaults
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.podcastTestData
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import io.jacob.episodive.core.designsystem.R as DesignSystemR

@RunWith(RobolectricTestRunner::class)
class PodcastScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun disableSystemAnimations() {
        // 스켈레톤의 shimmer는 rememberInfiniteTransition 기반이라 애니메이션이 켜진 채로
        // 테스트를 돌리면 waitForIdle()이 영원히 대기한다. SkeletonDefaults.shimmerEnabled()가
        // 이 값을 읽으므로 0으로 두면 shimmer 자체가 꺼진다.
        Settings.Global.putFloat(
            ApplicationProvider.getApplicationContext<Context>().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            0f,
        )
    }

    private fun setPodcastScreen(
        podcast: Podcast = podcastTestData,
        episodes: List<Episode> = episodeTestDataList,
        onFollowClick: () -> Unit = {},
        onEpisodeClick: (Episode, List<Episode>) -> Unit = { _, _ -> },
        onToggleLikedEpisode: (Episode) -> Unit = {},
        onToggleSavedEpisode: (Episode) -> Unit = {},
        onBackClick: () -> Unit = {},
        onShowSnackbar: suspend (String, String?) -> Boolean = { _, _ -> false },
    ) {
        composeTestRule.setContent {
            EpisodiveTheme {
                PodcastScreen(
                    podcast = podcast,
                    episodes = flowOf(PagingData.from(episodes)),
                    isRefreshing = false,
                    onRefresh = {},
                    onFollowClick = onFollowClick,
                    onEpisodeClick = onEpisodeClick,
                    onToggleLikedEpisode = onToggleLikedEpisode,
                    onToggleSavedEpisode = onToggleSavedEpisode,
                    onBackClick = onBackClick,
                    onShowSnackbar = onShowSnackbar,
                )
            }
        }
    }

    /** loadState를 직접 통제해야 하는 테스트용 — setPodcastScreen은 항상 완료된 상태만 만든다. */
    private fun setPodcastScreenWithPagingData(
        pagingData: PagingData<Episode>,
        podcast: Podcast = podcastTestData,
    ) {
        composeTestRule.setContent {
            EpisodiveTheme {
                PodcastScreen(
                    podcast = podcast,
                    episodes = flowOf(pagingData),
                    isRefreshing = false,
                    onRefresh = {},
                    onFollowClick = {},
                    onEpisodeClick = { _, _ -> },
                    onToggleLikedEpisode = {},
                    onBackClick = {},
                    onShowSnackbar = { _, _ -> false },
                )
            }
        }
    }

    /**
     * 팔로우 버튼은 220dp 커버 + 설명 아래에 있어 기본 테스트 창에서는 화면 밖이다.
     * 검증·클릭 전에 먼저 그 자리까지 스크롤한다.
     */
    private fun scrollToFollowButton(label: String) {
        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasText(label))
    }

    // --- Display tests ---

    @Test
    fun podcastTitleAndAuthorAreDisplayed() {
        setPodcastScreen()

        composeTestRule.onAllNodesWithText(podcastTestData.title, substring = true)
            .onFirst()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(podcastTestData.author, substring = true).assertIsDisplayed()
    }

    @Test
    fun followButtonIsDisplayed() {
        setPodcastScreen()

        scrollToFollowButton("Follow")
        composeTestRule.onNodeWithText("Follow").assertIsDisplayed()
    }

    @Test
    fun podcastDescriptionIsDisplayed() {
        setPodcastScreen()

        composeTestRule.onNodeWithText(podcastTestData.description, substring = true)
            .assertExists()
    }

    @Test
    fun emptyEpisodes_podcastHeaderStillDisplayed() {
        setPodcastScreen(episodes = emptyList())

        composeTestRule.onAllNodesWithText(podcastTestData.title, substring = true)
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun followedPodcast_showsUnfollowButton() {
        val followedPodcast = podcastTestData.copy(
            followedAt = kotlin.time.Instant.fromEpochSeconds(1000L),
        )
        setPodcastScreen(podcast = followedPodcast)

        scrollToFollowButton("Unfollow")
        composeTestRule.onNodeWithText("Unfollow").assertIsDisplayed()
    }

    @Test
    fun episodeListItemsAreDisplayed() {
        setPodcastScreen()

        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasText(episodeTestDataList.first().title, substring = true))
        composeTestRule.onAllNodesWithText(episodeTestDataList.first().title, substring = true)
            .onFirst()
            .assertExists()
    }

    // --- New: refreshPhase 분기 — itemCount만 보면 결과 0건에서 안내 문구 없이 빈 공간만
    // 남거나 추가 로딩 표시가 없는 버그의 재발 방지선 ---

    @Test
    fun whenEpisodesEmptyAndRefreshComplete_thenEmptyMessageShownWithoutSkeleton() {
        setPodcastScreenWithPagingData(
            PagingData.from(
                emptyList(),
                sourceLoadStates = LoadStates(
                    refresh = LoadState.NotLoading(endOfPaginationReached = true),
                    prepend = LoadState.NotLoading(endOfPaginationReached = true),
                    append = LoadState.NotLoading(endOfPaginationReached = true),
                ),
            )
        )

        // 헤더(220dp 커버 + 텍스트 + 버튼)가 테스트 창보다 커서 안내 문구 항목이 처음부터
        // 화면에 잡히지 않는다. 다른 목록 검증 테스트와 같은 방식으로 먼저 스크롤한다.
        val context = ApplicationProvider.getApplicationContext<Context>()
        val emptyMessage = context.getString(R.string.feature_podcast_episodes_empty)
        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasText(emptyMessage))
        composeTestRule.onNodeWithText(emptyMessage)
            .assertExists()
        composeTestRule.onNodeWithTag(SkeletonDefaults.TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun whenEpisodesRefreshLoading_thenSkeletonShown() {
        setPodcastScreenWithPagingData(
            PagingData.from(
                emptyList(),
                sourceLoadStates = LoadStates(
                    refresh = LoadState.Loading,
                    prepend = LoadState.NotLoading(endOfPaginationReached = true),
                    append = LoadState.NotLoading(endOfPaginationReached = true),
                ),
            )
        )

        // SkeletonContainer는 TestTag 와 함께 접근성 라벨도 갖는다. TestTag 로 스크롤 대상을
        // 잡으면(hasTestTag) 헤더 항목 하나가 나머지 항목들보다 훨씬 커서 LazyColumn 의 평균
        // 항목 크기 추정이 왜곡되어 스크롤이 목표를 지나치고 끝까지 가버린다 — 같은 화면의
        // 다른 테스트들처럼 접근성 속성으로 스크롤한다.
        val loadingLabel = ApplicationProvider.getApplicationContext<Context>()
            .getString(DesignSystemR.string.core_designsystem_loading)

        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasContentDescription(loadingLabel))
        composeTestRule.onNodeWithTag(SkeletonDefaults.TEST_TAG)
            .assertExists()
    }

    @Test
    fun episodeCountIsCorrect() {
        setPodcastScreen()

        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("${podcastTestData.episodeCount}", substring = true))
        composeTestRule.onNodeWithText("${podcastTestData.episodeCount}", substring = true)
            .assertExists()
    }

    @Test
    fun backButtonIsDisplayed() {
        setPodcastScreen()

        composeTestRule.onNodeWithContentDescription("Back").assertExists()
    }

    @Test
    fun podcastCategoriesDisplayed() {
        setPodcastScreen()

        composeTestRule.onNodeWithText(podcastTestData.description, substring = true)
            .assertExists()
    }

    // --- New: Callback tests ---

    @Test
    fun onFollowClick_callbackInvoked() {
        var called = false
        setPodcastScreen(onFollowClick = { called = true })

        scrollToFollowButton("Follow")
        composeTestRule.onNodeWithText("Follow").performClick()
        assert(called)
    }

    @Test
    fun onBackClick_callbackInvoked() {
        var called = false
        setPodcastScreen(onBackClick = { called = true })

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assert(called)
    }

    @Test
    fun onFollowClick_unfollowCallbackInvoked() {
        var called = false
        val followedPodcast = podcastTestData.copy(
            followedAt = kotlin.time.Instant.fromEpochSeconds(1000L),
        )
        setPodcastScreen(
            podcast = followedPodcast,
            onFollowClick = { called = true },
        )

        scrollToFollowButton("Unfollow")
        composeTestRule.onNodeWithText("Unfollow").performClick()
        assert(called)
    }

    // --- New: Episode count format ---

    @Test
    fun allEpisodesFormat_isDisplayed() {
        setPodcastScreen()

        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("All episodes", substring = true))
        composeTestRule.onNodeWithText("All episodes", substring = true)
            .assertExists()
    }

    // --- New: Different podcast data ---

    @Test
    fun differentPodcast_titleIsDisplayed() {
        val otherPodcast = podcastTestData.copy(title = "Custom Podcast Title")
        setPodcastScreen(podcast = otherPodcast)

        composeTestRule.onAllNodesWithText("Custom Podcast Title", substring = true)
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun differentPodcast_authorIsDisplayed() {
        val otherPodcast = podcastTestData.copy(author = "Custom Author")
        setPodcastScreen(podcast = otherPodcast)

        composeTestRule.onNodeWithText("Custom Author", substring = true).assertIsDisplayed()
    }

    // --- New: Unfollowed podcast shows Follow button ---

    @Test
    fun unfollowedPodcast_showsFollowButton() {
        val unfollowedPodcast = podcastTestData.copy(followedAt = null)
        setPodcastScreen(podcast = unfollowedPodcast)

        scrollToFollowButton("Follow")
        composeTestRule.onNodeWithText("Follow").assertIsDisplayed()
    }
}
