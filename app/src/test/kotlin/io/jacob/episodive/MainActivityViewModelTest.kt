package io.jacob.episodive

import android.content.Intent
import app.cash.turbine.test
import io.jacob.episodive.core.domain.usecase.user.GetUserDataUseCase
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.jacob.episodive.sync.EpisodeSyncNotificationHelper
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainActivityViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getUserDataUseCase = mockk<GetUserDataUseCase>(relaxed = true)

    private val viewModel = MainActivityViewModel(
        getUserDataUseCase = getUserDataUseCase,
    )

    @Test
    fun `Given intent with podcast_id, When handleDeepLink, Then emits Podcast event`() =
        runTest {
            // Given
            val intent = mockk<Intent> {
                every { getLongExtra(EpisodeSyncNotificationHelper.EXTRA_PODCAST_ID, -1L) } returns 42L
            }

            // When & Then
            viewModel.deepLinkEvent.test {
                viewModel.handleDeepLink(intent)
                val event = awaitItem()
                assertEquals(DeepLinkEvent.Podcast(42L), event)
                cancel()
            }
        }

    @Test
    fun `Given intent without podcast_id, When handleDeepLink, Then does not emit`() =
        runTest {
            // Given
            val intent = mockk<Intent> {
                every { getLongExtra(EpisodeSyncNotificationHelper.EXTRA_PODCAST_ID, -1L) } returns -1L
            }

            // When & Then
            viewModel.deepLinkEvent.test {
                viewModel.handleDeepLink(intent)
                expectNoEvents()
                cancel()
            }
        }

    @Test
    fun `Given null intent, When handleDeepLink, Then does not emit`() =
        runTest {
            viewModel.deepLinkEvent.test {
                viewModel.handleDeepLink(null)
                expectNoEvents()
                cancel()
            }
        }

    @Test
    fun `When consumeDeepLink, Then replay cache is cleared`() =
        runTest {
            // Given - emit an event
            val intent = mockk<Intent> {
                every { getLongExtra(EpisodeSyncNotificationHelper.EXTRA_PODCAST_ID, -1L) } returns 42L
            }
            viewModel.handleDeepLink(intent)

            // When
            viewModel.consumeDeepLink()

            // Then - new subscriber should not receive the event
            viewModel.deepLinkEvent.test {
                expectNoEvents()
                cancel()
            }
        }
}
