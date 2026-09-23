package io.jacob.episodive.core.domain.usecase.caption

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.CaptionRepository
import io.jacob.episodive.core.model.caption.CaptionDownloadFailure
import io.jacob.episodive.core.model.caption.CaptionLanguage
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ObserveCaptionDownloadFailuresUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val captionRepository = mockk<CaptionRepository>(relaxed = true)

    private val useCase = ObserveCaptionDownloadFailuresUseCase(
        captionRepository = captionRepository,
    )

    @After
    fun teardown() {
        confirmVerified(captionRepository)
    }

    @Test
    fun `Given dependencies, when invoke called, then repository delegated`() =
        runTest {
            // Given
            val failure = CaptionDownloadFailure(CaptionLanguage.KOREAN, CaptionDownloadFailure.Reason.NETWORK)
            every { captionRepository.downloadFailures } returns flowOf(failure)

            // When & Then
            useCase().test {
                assertEquals(failure, awaitItem())
                awaitComplete()
            }
            coVerify { captionRepository.downloadFailures }
        }
}
