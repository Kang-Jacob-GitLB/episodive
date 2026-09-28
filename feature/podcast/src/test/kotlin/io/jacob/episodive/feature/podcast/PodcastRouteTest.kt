package io.jacob.episodive.feature.podcast

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.paging.PagingData
import androidx.test.core.app.ApplicationProvider
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.domain.usecase.episode.GetEpisodesByPodcastIdPagingUseCase
import io.jacob.episodive.core.domain.usecase.episode.SaveEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.episode.ToggleLikedEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.player.PlayEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetPodcastUseCase
import io.jacob.episodive.core.domain.usecase.podcast.ToggleFollowedUseCase
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.podcastTestData
import io.jacob.episodive.core.ui.R as uiR
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * [PodcastRoute] 가 팔로우 스낵바만 최신 것 하나로 남기고(별도 Job + cancel), 저장 해제
 * 스낵바는 순차로 기다리는지 고정하는 계약 테스트.
 *
 * `followSnackbar?.cancel()` 을 지우면(버그 재현) 팔로우를 연타해도 두 호출 모두 끝까지
 * 살아남는다. 이펙트 수집 전체를 `collectLatest` 로 바꾸면(버그 재현) 저장 해제 스낵바가
 * 아직 떠 있는 동안 팔로우 이펙트가 오면 저장 해제 호출까지 취소된다.
 */
@RunWith(RobolectricTestRunner::class)
class PodcastRouteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val getPodcastUseCase = mockk<GetPodcastUseCase>()
    private val getEpisodesByPodcastIdPagingUseCase = mockk<GetEpisodesByPodcastIdPagingUseCase>()
    private val toggleFollowedUseCase = mockk<ToggleFollowedUseCase>(relaxed = true)
    private val playEpisodeUseCase = mockk<PlayEpisodeUseCase>(relaxed = true)
    private val toggleLikedEpisodeUseCase = mockk<ToggleLikedEpisodeUseCase>(relaxed = true)
    private val saveEpisodeUseCase = mockk<SaveEpisodeUseCase>(relaxed = true)

    private val context: Context by lazy { ApplicationProvider.getApplicationContext() }
    private val followedMessage by lazy { context.getString(uiR.string.core_ui_snackbar_followed) }
    private val unfollowedMessage by lazy { context.getString(uiR.string.core_ui_snackbar_unfollowed) }
    private val unsavedMessage by lazy { context.getString(uiR.string.core_ui_snackbar_unsaved) }

    /** 호출된 메시지와, 각 호출이 취소로 끝났는지를 순서대로 기록하는 스낵바 대역. */
    private class RecordingSnackbar {
        val calls = mutableListOf<String>()
        val finishedByCancellation = mutableListOf<Boolean>()

        suspend fun show(message: String, actionLabel: String?): Boolean {
            calls.add(message)
            // 실제 SnackbarHostState.showSnackbar 처럼 스낵바가 닫힐 때까지 끝나지 않는다.
            // awaitCancellation() 은 Nothing 을 반환하므로 이 호출은 취소로만 끝난다.
            try {
                awaitCancellation()
            } finally {
                finishedByCancellation.add(true)
            }
        }
    }

    private fun createViewModel(id: Long = 1L): PodcastViewModel {
        every { getPodcastUseCase(id) } returns flowOf(podcastTestData)
        every { getEpisodesByPodcastIdPagingUseCase(id) } returns flowOf(PagingData.empty())

        return PodcastViewModel(
            getPodcastUseCase = getPodcastUseCase,
            getEpisodesByPodcastIdPagingUseCase = getEpisodesByPodcastIdPagingUseCase,
            toggleFollowedUseCase = toggleFollowedUseCase,
            playEpisodeUseCase = playEpisodeUseCase,
            toggleLikedEpisodeUseCase = toggleLikedEpisodeUseCase,
            saveEpisodeUseCase = saveEpisodeUseCase,
            id = id,
        )
    }

    @Test
    fun givenRapidFollowToggles_whenEffectsArrive_thenOnlyLatestSnackbarStaysActiveInOrder() {
        coEvery { toggleFollowedUseCase(any()) } returnsMany listOf(true, false)

        val viewModel = createViewModel()
        val snackbar = RecordingSnackbar()

        composeTestRule.setContent {
            EpisodiveTheme {
                PodcastRoute(
                    viewModel = viewModel,
                    onBackClick = {},
                    onShowSnackbar = snackbar::show,
                )
            }
        }
        composeTestRule.waitForIdle()

        // 팔로우 연타를 흉내낸다 — 실제 화면에서는 버튼을 두 번 빠르게 누른 것과 같다.
        viewModel.sendAction(PodcastAction.ToggleFollowed)
        viewModel.sendAction(PodcastAction.ToggleFollowed)
        composeTestRule.waitForIdle()

        // 두 이펙트 모두 onShowSnackbar 까지 도달해야 한다. collect 였다면 첫 호출이 끝나지
        // 않아 두 번째 호출이 오지 않는다. 순서는 누른 순서(followed → unfollowed) 그대로여야 한다.
        assertEquals(listOf(followedMessage, unfollowedMessage), snackbar.calls)

        // 첫 번째 호출은 두 번째 이펙트가 도착하며 취소돼야 하고, 두 번째(최신) 호출은
        // 아직 살아 있어야 한다(스낵바가 화면에 남아 있는 상태를 의미).
        assertEquals(1, snackbar.finishedByCancellation.size)
    }

    @Test
    fun givenUnsaveSnackbarShowing_whenFollowEffectArrives_thenUnsaveCallIsNotCancelled() {
        coEvery { toggleFollowedUseCase(any()) } returns true
        coEvery { saveEpisodeUseCase(any()) } returns false

        val viewModel = createViewModel()
        val snackbar = RecordingSnackbar()

        composeTestRule.setContent {
            EpisodiveTheme {
                PodcastRoute(
                    viewModel = viewModel,
                    onBackClick = {},
                    onShowSnackbar = snackbar::show,
                )
            }
        }
        composeTestRule.waitForIdle()

        // 저장 해제 스낵바를 먼저 띄운다.
        viewModel.sendAction(PodcastAction.ToggleSavedEpisode(episodeTestData))
        composeTestRule.waitForIdle()

        // 저장 해제 스낵바가 떠 있는 동안 팔로우 이펙트가 온다.
        viewModel.sendAction(PodcastAction.ToggleFollowed)
        composeTestRule.waitForIdle()

        // 이펙트는 순차 collect 이므로, 저장 해제 처리(onShowSnackbar 호출)가 끝나기 전까지는
        // 뒤이은 팔로우 이펙트가 아예 소비되지 않는다 — 저장 해제 호출은 취소되지 않은 채
        // 여전히 떠 있어야 한다. collectLatest 로 바뀌면 팔로우 이펙트 도착이 저장 해제
        // 처리를 취소하고, 곧바로 followedMessage 호출이 추가로 기록된다.
        assertEquals(listOf(unsavedMessage), snackbar.calls)
        assertTrue(snackbar.finishedByCancellation.isEmpty())
    }
}
