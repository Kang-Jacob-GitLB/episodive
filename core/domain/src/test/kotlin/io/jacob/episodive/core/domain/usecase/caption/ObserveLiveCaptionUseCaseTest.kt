package io.jacob.episodive.core.domain.usecase.caption

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.CaptionRepository
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.model.Progress
import io.jacob.episodive.core.model.UserData
import io.jacob.episodive.core.model.caption.CaptionAvailability
import io.jacob.episodive.core.model.caption.CaptionLanguage
import io.jacob.episodive.core.model.caption.CaptionModelState
import io.jacob.episodive.core.model.caption.CaptionSession
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.liveCaptionTestData
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class ObserveLiveCaptionUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val playerRepository = mockk<PlayerRepository>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val captionRepository = mockk<CaptionRepository>(relaxed = true)

    private val useCase = ObserveLiveCaptionUseCase(
        playerRepository = playerRepository,
        userRepository = userRepository,
        captionRepository = captionRepository,
    )

    // episodeTestData 는 feedLanguage="ko", transcriptUrl=null 이라 STT(KOREAN) 경로를 탄다.
    private val koreanEpisode = episodeTestData
    private val anotherKoreanEpisode = episodeTestDataList[1].copy(feedLanguage = "ko", transcriptUrl = null)

    @Test
    fun `Given caption disabled, when invoke called, then model availability reported without opening session`() =
        runTest {
            // Given — 꺼져 있어도 가용성은 계산해야 토글 한 번에 "켜고 받기" 를 정할 수 있다.
            every { userRepository.getUserData() } returns flowOf(UserData(isCaptionEnabled = false))
            every { playerRepository.progress } returns flowOf(Progress(0.seconds, 0.seconds, 0.seconds, episodeId = koreanEpisode.id))
            every { playerRepository.nowPlaying } returns flowOf(koreanEpisode)
            every { captionRepository.modelState(CaptionLanguage.KOREAN) } returns flowOf(CaptionModelState.NotInstalled(sizeBytes = 42L))

            // When & Then
            useCase().test {
                val state = awaitItem()
                assertFalse(state.isEnabled)
                assertEquals(CaptionAvailability.NotInstalled(CaptionLanguage.KOREAN, 42L), state.availability)
                assertEquals(null, state.caption)
                awaitComplete()
            }
            coVerify(exactly = 0) { captionRepository.liveCaptions(any(), any()) }
        }

    @Test
    fun `Given caption disabled and model installed, when invoke called, then Ready without opening session`() =
        runTest {
            // Given
            every { userRepository.getUserData() } returns flowOf(UserData(isCaptionEnabled = false))
            every { playerRepository.progress } returns flowOf(Progress(0.seconds, 0.seconds, 0.seconds, episodeId = koreanEpisode.id))
            every { playerRepository.nowPlaying } returns flowOf(koreanEpisode)
            every { captionRepository.modelState(CaptionLanguage.KOREAN) } returns flowOf(CaptionModelState.Installed)

            // When & Then
            useCase().test {
                val state = awaitItem()
                assertFalse(state.isEnabled)
                assertEquals(CaptionAvailability.Ready(CaptionLanguage.KOREAN), state.availability)
                awaitComplete()
            }
            coVerify(exactly = 0) { captionRepository.liveCaptions(any(), any()) }
        }

    @Test
    fun `Given caption disabled on VTT transcript episode, when invoke called, then Transcript without translating`() =
        runTest {
            // Given
            val transcriptEpisode = koreanEpisode.copy(transcriptUrl = "https://example.com/t.vtt")
            every { userRepository.getUserData() } returns flowOf(UserData(isCaptionEnabled = false))
            every { playerRepository.progress } returns flowOf(Progress(0.seconds, 0.seconds, 0.seconds, episodeId = transcriptEpisode.id))
            every { playerRepository.nowPlaying } returns flowOf(transcriptEpisode)

            // When & Then
            useCase().test {
                val state = awaitItem()
                assertFalse(state.isEnabled)
                assertEquals(CaptionAvailability.Transcript, state.availability)
                assertEquals(null, state.caption)
                awaitComplete()
            }
            coVerify(exactly = 0) { captionRepository.translatedCues(any(), any(), any(), any()) }
        }

    @Test
    fun `Given progress episodeId and nowPlaying id mismatched, when invoke called, then Unknown without opening session`() =
        runTest {
            // Given
            every { userRepository.getUserData() } returns flowOf(UserData(isCaptionEnabled = true))
            every { playerRepository.progress } returns flowOf(Progress(0.seconds, 0.seconds, 0.seconds, episodeId = anotherKoreanEpisode.id))
            every { playerRepository.nowPlaying } returns flowOf(koreanEpisode)

            // When & Then
            useCase().test {
                val state = awaitItem()
                assertEquals(CaptionAvailability.Unknown, state.availability)
                cancelAndIgnoreRemainingEvents()
            }
            coVerify(exactly = 0) { captionRepository.modelState(any()) }
            coVerify(exactly = 0) { captionRepository.liveCaptions(any(), any()) }
        }

    @Test
    fun `Given feedLanguage unsupported, when invoke called, then Unsupported without opening session`() =
        runTest {
            // Given
            val frenchEpisode = koreanEpisode.copy(feedLanguage = "fr", transcriptUrl = null)
            every { userRepository.getUserData() } returns flowOf(UserData(isCaptionEnabled = true))
            every { playerRepository.progress } returns flowOf(Progress(0.seconds, 0.seconds, 0.seconds, episodeId = frenchEpisode.id))
            every { playerRepository.nowPlaying } returns flowOf(frenchEpisode)

            // When & Then
            useCase().test {
                val state = awaitItem()
                assertEquals(CaptionAvailability.Unsupported, state.availability)
                cancelAndIgnoreRemainingEvents()
            }
            coVerify(exactly = 0) { captionRepository.modelState(any()) }
            coVerify(exactly = 0) { captionRepository.liveCaptions(any(), any()) }
        }

    @Test
    fun `Given VTT transcript episode, when invoke called, then translatedCues used instead of liveCaptions`() =
        runTest {
            // Given
            val transcriptEpisode = koreanEpisode.copy(transcriptUrl = "https://example.com/transcript.vtt")
            every { userRepository.getUserData() } returns flowOf(UserData(isCaptionEnabled = true))
            every { playerRepository.progress } returns flowOf(Progress(0.seconds, 0.seconds, 0.seconds, episodeId = transcriptEpisode.id))
            every { playerRepository.nowPlaying } returns flowOf(transcriptEpisode)
            every { playerRepository.cue } returns flowOf("cue")
            every { playerRepository.seeks } returns emptyFlow()
            every {
                captionRepository.translatedCues(transcriptEpisode.id, transcriptEpisode.feedLanguage, any(), any())
            } returns flowOf(liveCaptionTestData)

            // When & Then
            useCase().test {
                val state = awaitItem()
                assertEquals(CaptionAvailability.Transcript, state.availability)
                assertEquals(liveCaptionTestData, state.caption)
                cancelAndIgnoreRemainingEvents()
            }
            coVerify(exactly = 1) { captionRepository.translatedCues(transcriptEpisode.id, transcriptEpisode.feedLanguage, any(), any()) }
            coVerify(exactly = 0) { captionRepository.modelState(any()) }
            coVerify(exactly = 0) { captionRepository.liveCaptions(any(), any()) }
        }

    @Test
    fun `Given episode changes, when invoke called, then previous session cancelled and new session opened`() =
        runTest {
            // Given
            var firstSessionCancelled = false
            val progressFlow = MutableStateFlow(Progress(0.seconds, 0.seconds, 0.seconds, episodeId = koreanEpisode.id))
            val nowPlayingFlow = MutableStateFlow(koreanEpisode)
            every { userRepository.getUserData() } returns flowOf(UserData(isCaptionEnabled = true))
            every { playerRepository.progress } returns progressFlow
            every { playerRepository.nowPlaying } returns nowPlayingFlow
            every { captionRepository.modelState(CaptionLanguage.KOREAN) } returns flowOf(CaptionModelState.Installed)
            every { captionRepository.liveCaptions(koreanEpisode.id, CaptionLanguage.KOREAN) } returns flow {
                try {
                    emit(CaptionSession.Running(caption = null))
                    awaitCancellation()
                } finally {
                    firstSessionCancelled = true
                }
            }
            every { captionRepository.liveCaptions(anotherKoreanEpisode.id, CaptionLanguage.KOREAN) } returns flow {
                emit(CaptionSession.Running(caption = null))
                awaitCancellation()
            }

            // When & Then
            useCase().test {
                val first = awaitItem()
                assertEquals(CaptionAvailability.Ready(CaptionLanguage.KOREAN), first.availability)
                assertFalse(firstSessionCancelled)

                progressFlow.value = progressFlow.value.copy(episodeId = anotherKoreanEpisode.id)
                val transitioning = awaitItem()
                assertEquals(CaptionAvailability.Unknown, transitioning.availability)

                nowPlayingFlow.value = anotherKoreanEpisode
                val second = awaitItem()
                assertEquals(CaptionAvailability.Ready(CaptionLanguage.KOREAN), second.availability)
                assertEquals(true, firstSessionCancelled)

                cancelAndIgnoreRemainingEvents()
            }
            coVerify(exactly = 1) { captionRepository.liveCaptions(koreanEpisode.id, CaptionLanguage.KOREAN) }
            coVerify(exactly = 1) { captionRepository.liveCaptions(anotherKoreanEpisode.id, CaptionLanguage.KOREAN) }
        }

    @Test
    fun `Given model NotInstalled then Downloading then Installed, when invoke called, then session opened only once`() =
        runTest {
            // Given
            var sessionOpenCount = 0
            val modelStateFlow = MutableStateFlow<CaptionModelState>(CaptionModelState.NotInstalled(sizeBytes = 100L))
            val progressFlow = MutableStateFlow(Progress(0.seconds, 0.seconds, 0.seconds, episodeId = koreanEpisode.id))
            every { userRepository.getUserData() } returns flowOf(UserData(isCaptionEnabled = true))
            every { playerRepository.progress } returns progressFlow
            every { playerRepository.nowPlaying } returns flowOf(koreanEpisode)
            every { captionRepository.modelState(CaptionLanguage.KOREAN) } returns modelStateFlow
            every { captionRepository.liveCaptions(koreanEpisode.id, CaptionLanguage.KOREAN) } returns flow {
                sessionOpenCount++
                emit(CaptionSession.Running(caption = null))
                awaitCancellation()
            }

            // When & Then
            useCase().test {
                val notInstalled = awaitItem()
                assertEquals(CaptionAvailability.NotInstalled(CaptionLanguage.KOREAN, 100L), notInstalled.availability)

                modelStateFlow.value = CaptionModelState.Downloading(downloadedBytes = 50L, totalBytes = 100L)
                val downloading = awaitItem()
                assertEquals(CaptionAvailability.Downloading::class, downloading.availability::class)

                modelStateFlow.value = CaptionModelState.Installed
                val ready = awaitItem()
                assertEquals(CaptionAvailability.Ready(CaptionLanguage.KOREAN), ready.availability)

                // position 만 흐르는 동안은 세션이 재시작되지 않는다.
                progressFlow.value = progressFlow.value.copy(position = 5.seconds)
                expectNoEvents()

                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(1, sessionOpenCount)
        }
}
