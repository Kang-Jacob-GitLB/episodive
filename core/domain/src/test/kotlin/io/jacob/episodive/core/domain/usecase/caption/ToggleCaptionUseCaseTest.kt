package io.jacob.episodive.core.domain.usecase.caption

import io.jacob.episodive.core.domain.repository.CaptionRepository
import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.model.caption.CaptionAvailability
import io.jacob.episodive.core.model.caption.CaptionLanguage
import io.jacob.episodive.core.model.caption.LiveCaptionState
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ToggleCaptionUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val captionRepository = mockk<CaptionRepository>(relaxed = true)

    private val useCase = ToggleCaptionUseCase(
        userRepository = userRepository,
        captionRepository = captionRepository,
    )

    @After
    fun teardown() {
        confirmVerified(userRepository, captionRepository)
    }

    @Test
    fun `Given Downloading, when invoke called, then cancel download and disable`() =
        runTest {
            // Given
            val current = LiveCaptionState(
                isEnabled = true,
                availability = CaptionAvailability.Downloading(CaptionLanguage.KOREAN, progress = 0.5f),
                caption = null,
            )

            // When
            val result = useCase(current)

            // Then
            assertEquals(CaptionToggleResult.DownloadCancelled, result)
            coVerifySequence {
                captionRepository.cancelDownload(CaptionLanguage.KOREAN)
                userRepository.setCaptionEnabled(false)
            }
        }

    @Test
    fun `Given enabled and Ready, when invoke called, then disable`() =
        runTest {
            // Given
            val current = LiveCaptionState(
                isEnabled = true,
                availability = CaptionAvailability.Ready(CaptionLanguage.KOREAN),
                caption = null,
            )

            // When
            val result = useCase(current)

            // Then
            assertEquals(CaptionToggleResult.Disabled, result)
            coVerifySequence {
                userRepository.setCaptionEnabled(false)
            }
        }

    @Test
    fun `Given enabled and Loading, when invoke called, then disable`() =
        runTest {
            // Given
            val current = LiveCaptionState(true, CaptionAvailability.Loading(CaptionLanguage.KOREAN), null)

            // When
            val result = useCase(current)

            // Then
            assertEquals(CaptionToggleResult.Disabled, result)
            coVerifySequence { userRepository.setCaptionEnabled(false) }
        }

    @Test
    fun `Given enabled and Unknown, when invoke called, then disable`() =
        runTest {
            // Given
            val current = LiveCaptionState(true, CaptionAvailability.Unknown, null)

            // When
            val result = useCase(current)

            // Then
            assertEquals(CaptionToggleResult.Disabled, result)
            coVerifySequence { userRepository.setCaptionEnabled(false) }
        }

    @Test
    fun `Given enabled and Transcript, when invoke called, then disable`() =
        runTest {
            // Given
            val current = LiveCaptionState(true, CaptionAvailability.Transcript, null)

            // When
            val result = useCase(current)

            // Then
            assertEquals(CaptionToggleResult.Disabled, result)
            coVerifySequence { userRepository.setCaptionEnabled(false) }
        }

    @Test
    fun `Given enabled and Unsupported, when invoke called, then disable`() =
        runTest {
            // Given
            val current = LiveCaptionState(true, CaptionAvailability.Unsupported, null)

            // When
            val result = useCase(current)

            // Then
            assertEquals(CaptionToggleResult.Disabled, result)
            coVerifySequence { userRepository.setCaptionEnabled(false) }
        }

    @Test
    fun `Given enabled and NotInstalled, when invoke called, then start download and keep enabled`() =
        runTest {
            // Given
            val current = LiveCaptionState(
                isEnabled = true,
                availability = CaptionAvailability.NotInstalled(CaptionLanguage.ENGLISH, sizeBytes = 123L),
                caption = null,
            )

            // When
            val result = useCase(current)

            // Then
            assertEquals(CaptionToggleResult.DownloadStarted(123L), result)
            coVerifySequence {
                captionRepository.startDownload(CaptionLanguage.ENGLISH)
            }
        }

    @Test
    fun `Given disabled and NotInstalled, when invoke called, then enable and start download`() =
        runTest {
            // Given
            val current = LiveCaptionState(
                isEnabled = false,
                availability = CaptionAvailability.NotInstalled(CaptionLanguage.ENGLISH, sizeBytes = 456L),
                caption = null,
            )

            // When
            val result = useCase(current)

            // Then
            assertEquals(CaptionToggleResult.DownloadStarted(456L), result)
            coVerifySequence {
                userRepository.setCaptionEnabled(true)
                captionRepository.startDownload(CaptionLanguage.ENGLISH)
            }
        }

    @Test
    fun `Given disabled and Unsupported, when invoke called, then enable and report unsupported`() =
        runTest {
            // Given
            val current = LiveCaptionState(isEnabled = false, availability = CaptionAvailability.Unsupported, caption = null)

            // When
            val result = useCase(current)

            // Then
            assertEquals(CaptionToggleResult.UnsupportedLanguage, result)
            coVerifySequence {
                userRepository.setCaptionEnabled(true)
            }
        }

    @Test
    fun `Given disabled and Unknown, when invoke called, then enable`() =
        runTest {
            // Given
            val current = LiveCaptionState(isEnabled = false, availability = CaptionAvailability.Unknown, caption = null)

            // When
            val result = useCase(current)

            // Then
            assertEquals(CaptionToggleResult.Enabled, result)
            coVerifySequence {
                userRepository.setCaptionEnabled(true)
            }
        }
}
