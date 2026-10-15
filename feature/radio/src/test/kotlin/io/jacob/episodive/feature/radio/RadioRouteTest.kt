package io.jacob.episodive.feature.radio

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.station.StationPlaybackController
import io.jacob.episodive.core.domain.usecase.station.GetFavoriteStationsUseCase
import io.jacob.episodive.core.domain.usecase.station.GetRecentStationsUseCase
import io.jacob.episodive.core.domain.usecase.station.GetStationsUseCase
import io.jacob.episodive.core.domain.usecase.station.ObserveStationProgramImagesUseCase
import io.jacob.episodive.core.domain.usecase.station.ToggleFavoriteStationUseCase
import io.jacob.episodive.core.model.StationKind
import io.jacob.episodive.core.testing.model.radioStationTestData
import io.jacob.episodive.core.testing.model.curatedStationListingTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * [RadioRoute] 가 즐겨찾기 스낵바를 최신 하나로 남기는지(별도 Job + cancel) 고정하는 계약 테스트.
 *
 * `favoriteSnackbar?.cancel()` 을 지우면 연타해도 두 호출이 모두 살아남는다.
 */
@RunWith(RobolectricTestRunner::class)
class RadioRouteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val getStationsUseCase = mockk<GetStationsUseCase>()
    private val getFavoriteStationsUseCase = mockk<GetFavoriteStationsUseCase>()
    private val getRecentStationsUseCase = mockk<GetRecentStationsUseCase>()
    private val toggleFavoriteStationUseCase = mockk<ToggleFavoriteStationUseCase>()
    private val observeStationProgramImagesUseCase = mockk<ObserveStationProgramImagesUseCase>()
    private val stationPlaybackController = mockk<StationPlaybackController>(relaxed = true)
    private val playerRepository = mockk<PlayerRepository>(relaxed = true)

    private val context: Context by lazy { ApplicationProvider.getApplicationContext() }
    private val favoritedMessage by lazy { context.getString(R.string.feature_radio_snackbar_favorited) }
    private val unfavoritedMessage by lazy { context.getString(R.string.feature_radio_snackbar_unfavorited) }

    private class RecordingSnackbar {
        val calls = mutableListOf<String>()
        val finishedByCancellation = mutableListOf<Boolean>()

        suspend fun show(message: String, actionLabel: String?): Boolean {
            calls.add(message)
            // 실제 showSnackbar 처럼 닫힐 때까지 끝나지 않는다. 이 호출은 취소로만 끝난다.
            try {
                awaitCancellation()
            } finally {
                finishedByCancellation.add(true)
            }
        }
    }

    private fun createViewModel(): RadioViewModel {
        every { getStationsUseCase(StationKind.RADIO) } returns flowOf(curatedStationListingTestData)
        every { getFavoriteStationsUseCase() } returns flowOf(emptyList())
        every { getRecentStationsUseCase(any()) } returns flowOf(emptyList())
        every { playerRepository.nowPlayingStation } returns MutableStateFlow(null)
        every { playerRepository.isPlaying } returns MutableStateFlow(false)
        every { playerRepository.isBuffering } returns MutableStateFlow(false)
        every { stationPlaybackController.pendingStationId } returns MutableStateFlow(null)
        every { observeStationProgramImagesUseCase(any()) } returns flowOf(emptyMap())

        return RadioViewModel(
            getStationsUseCase = getStationsUseCase,
            getFavoriteStationsUseCase = getFavoriteStationsUseCase,
            getRecentStationsUseCase = getRecentStationsUseCase,
            observeStationProgramImagesUseCase = observeStationProgramImagesUseCase,
            toggleFavoriteStationUseCase = toggleFavoriteStationUseCase,
            stationPlaybackController = stationPlaybackController,
            playerRepository = playerRepository,
        )
    }

    private fun setRoute(viewModel: RadioViewModel, snackbar: RecordingSnackbar) {
        composeTestRule.setContent {
            EpisodiveTheme {
                RadioRoute(
                    viewModel = viewModel,
                    onBackClick = {},
                    onShowSnackbar = snackbar::show,
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun givenRapidFavoriteToggles_thenOnlyLatestSnackbarStaysActiveInOrder() {
        coEvery { toggleFavoriteStationUseCase(any()) } returnsMany listOf(true, false)
        val viewModel = createViewModel()
        val snackbar = RecordingSnackbar()
        setRoute(viewModel, snackbar)

        viewModel.sendAction(RadioAction.ToggleFavorite(radioStationTestData.id))
        viewModel.sendAction(RadioAction.ToggleFavorite(radioStationTestData.id))
        composeTestRule.waitForIdle()

        assertEquals(listOf(favoritedMessage, unfavoritedMessage), snackbar.calls)
        assertEquals(1, snackbar.finishedByCancellation.size)
    }
}
