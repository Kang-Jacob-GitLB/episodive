package io.jacob.episodive.feature.podcast

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.paging.PagingData
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.domain.usecase.episode.GetEpisodesByPodcastIdPagingUseCase
import io.jacob.episodive.core.domain.usecase.episode.SaveEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.episode.ToggleLikedEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.player.PlayEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetPodcastUseCase
import io.jacob.episodive.core.domain.usecase.podcast.ToggleFollowedUseCase
import io.jacob.episodive.core.testing.model.podcastTestData
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
 * [PodcastRoute] 가 `viewModel.effect` 를 `collectLatest` 로 받는지 고정하는 계약 테스트.
 *
 * `collect` 로 되돌리면(버그 재현) `onShowSnackbar` 가 스낵바 종료까지 suspend 하므로, 팔로우를
 * 연타해도 두 번째 이펙트는 첫 번째 `onShowSnackbar` 호출이 끝날 때까지 소비되지 않는다.
 * `collectLatest` 라면 두 번째 이펙트가 도착하는 순간 첫 번째 `onShowSnackbar` 호출이 취소되고
 * 두 번째 호출이 곧바로 시작된다.
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

    /** 호출된 메시지와, 각 호출이 취소로 끝났는지를 순서대로 기록하는 스낵바 대역. */
    private class RecordingSnackbar {
        val calls = mutableListOf<String>()
        val finishedByCancellation = mutableListOf<Boolean>()

        suspend fun show(message: String, actionLabel: String?): Boolean {
            calls.add(message)
            var cancelled = false
            try {
                // 실제 SnackbarHostState.showSnackbar 처럼 스낵바가 닫힐 때까지 끝나지 않는다.
                awaitCancellation()
            } catch (e: CancellationException) {
                cancelled = true
                throw e
            } finally {
                finishedByCancellation.add(cancelled)
            }
            @Suppress("UNREACHABLE_CODE")
            return false
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
    fun givenRapidFollowToggles_whenEffectsArrive_thenOnlyLatestSnackbarStaysActive() {
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
        // 않아 두 번째 호출이 오지 않는다.
        assertEquals(2, snackbar.calls.size)

        // 첫 번째 호출은 두 번째 이펙트가 도착하며 취소돼야 하고, 두 번째(최신) 호출은
        // 아직 살아 있어야 한다(스낵바가 화면에 남아 있는 상태를 의미).
        assertEquals(1, snackbar.finishedByCancellation.size)
        assertTrue(snackbar.finishedByCancellation[0])
    }
}
