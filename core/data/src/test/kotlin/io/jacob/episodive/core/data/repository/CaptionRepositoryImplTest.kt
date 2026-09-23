package io.jacob.episodive.core.data.repository

import app.cash.turbine.test
import io.jacob.episodive.core.caption.asset.CaptionModelManager
import io.jacob.episodive.core.caption.engine.CaptionTranslator
import io.jacob.episodive.core.caption.engine.CaptionTranslatorFactory
import io.jacob.episodive.core.caption.engine.DeviceLanguageProvider
import io.jacob.episodive.core.caption.engine.LiveCaptionEngine
import io.jacob.episodive.core.model.caption.CaptionDownloadFailure
import io.jacob.episodive.core.model.caption.CaptionLanguage
import io.jacob.episodive.core.model.caption.CaptionModelState
import io.jacob.episodive.core.model.caption.CaptionSession
import io.jacob.episodive.core.testing.model.liveCaptionTestData
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CaptionRepositoryImplTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val captionModelManager = mockk<CaptionModelManager>(relaxed = true)
    private val liveCaptionEngine = mockk<LiveCaptionEngine>(relaxed = true)
    private val captionTranslatorFactory = mockk<CaptionTranslatorFactory>()
    private val deviceLanguageProvider = mockk<DeviceLanguageProvider> { every { languageTag() } returns "ko" }
    private val applicationScope = CoroutineScope(SupervisorJob() + mainDispatcherRule.testDispatcher)

    private val repository = CaptionRepositoryImpl(
        captionModelManager = dagger.Lazy { captionModelManager },
        liveCaptionEngine = liveCaptionEngine,
        captionTranslatorFactory = captionTranslatorFactory,
        deviceLanguageProvider = deviceLanguageProvider,
        applicationScope = applicationScope,
        ioDispatcher = mainDispatcherRule.testDispatcher,
    )

    // --- 모델 상태·다운로드 위임 ---

    @Test
    fun `Given a language, When modelState, Then it delegates to the manager`() = runTest {
        val states = MutableStateFlow<CaptionModelState>(CaptionModelState.NotInstalled(100L))
        every { captionModelManager.state(CaptionLanguage.ENGLISH) } returns states

        repository.modelState(CaptionLanguage.ENGLISH).test {
            assertEquals(CaptionModelState.NotInstalled(100L), awaitItem())
            states.value = CaptionModelState.Installed
            assertEquals(CaptionModelState.Installed, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `When downloadFailures, Then it delegates to the manager`() = runTest {
        val failures = MutableSharedFlow<CaptionDownloadFailure>(extraBufferCapacity = 1)
        every { captionModelManager.downloadFailures } returns failures

        repository.downloadFailures.test {
            failures.emit(CaptionDownloadFailure(CaptionLanguage.KOREAN, CaptionDownloadFailure.Reason.NETWORK))
            assertEquals(
                CaptionDownloadFailure(CaptionLanguage.KOREAN, CaptionDownloadFailure.Reason.NETWORK),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `When startDownload, Then it delegates to the manager`() = runTest {
        repository.startDownload(CaptionLanguage.ENGLISH)

        verify { captionModelManager.start(CaptionLanguage.ENGLISH) }
    }

    @Test
    fun `When cancelDownload, Then it delegates to the manager`() = runTest {
        repository.cancelDownload(CaptionLanguage.ENGLISH)

        verify { captionModelManager.cancel(CaptionLanguage.ENGLISH) }
    }

    // --- liveCaptions ---

    @Test
    fun `Given the model is not installed, When liveCaptions, Then it completes without emitting`() = runTest {
        every { captionModelManager.installed(CaptionLanguage.ENGLISH) } returns null

        repository.liveCaptions(episodeId = 1L, language = CaptionLanguage.ENGLISH).test {
            awaitComplete()
        }
        verify(exactly = 0) { liveCaptionEngine.session(any(), any()) }
    }

    @Test
    fun `Given the model is installed, When liveCaptions, Then it forwards the engine session`() = runTest {
        val model = io.jacob.episodive.core.caption.asset.InstalledCaptionModel(
            language = CaptionLanguage.ENGLISH,
            encoderPath = "encoder",
            decoderPath = "decoder",
            joinerPath = "joiner",
            tokensPath = "tokens",
            maxLineChars = 80,
        )
        every { captionModelManager.installed(CaptionLanguage.ENGLISH) } returns model
        val running = CaptionSession.Running(
            liveCaptionTestData.copy(episodeId = 1L, lineId = 0L, text = "hello", translation = null, isFinal = false),
        )
        every { liveCaptionEngine.session(1L, model) } returns flowOf(CaptionSession.Loading, running)

        repository.liveCaptions(episodeId = 1L, language = CaptionLanguage.ENGLISH).test {
            assertEquals(CaptionSession.Loading, awaitItem())
            assertEquals(running, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `Given the session is Unloadable, When liveCaptions, Then it reports the manager`() = runTest {
        val model = io.jacob.episodive.core.caption.asset.InstalledCaptionModel(
            language = CaptionLanguage.ENGLISH,
            encoderPath = "encoder",
            decoderPath = "decoder",
            joinerPath = "joiner",
            tokensPath = "tokens",
            maxLineChars = 80,
        )
        every { captionModelManager.installed(CaptionLanguage.ENGLISH) } returns model
        every { liveCaptionEngine.session(1L, model) } returns flowOf(CaptionSession.Unloadable)

        repository.liveCaptions(episodeId = 1L, language = CaptionLanguage.ENGLISH).test {
            assertEquals(CaptionSession.Unloadable, awaitItem())
            awaitComplete()
        }
        verify { captionModelManager.reportUnloadable(CaptionLanguage.ENGLISH) }
    }

    // --- translatedCues ---

    @Test
    fun `Given no translator is available, When translatedCues, Then it streams the original text untranslated`() =
        runTest {
            every { captionTranslatorFactory.create("en", "ko") } returns null
            val cues = flowOf("hello", "world")

            repository.translatedCues(episodeId = 1L, sourceLanguageTag = "en", cues = cues).test {
                assertEquals(
                    liveCaptionTestData.copy(episodeId = 1L, lineId = 0L, text = "hello", translation = null),
                    awaitItem(),
                )
                assertEquals(
                    liveCaptionTestData.copy(episodeId = 1L, lineId = 1L, text = "world", translation = null),
                    awaitItem(),
                )
                awaitComplete()
            }
        }

    @Test
    fun `Given an empty cue, When translatedCues, Then it emits null`() = runTest {
        every { captionTranslatorFactory.create("en", "ko") } returns null

        repository.translatedCues(episodeId = 1L, sourceLanguageTag = "en", cues = flowOf("")).test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `Given a translator, When translatedCues, Then it emits the original first and the translation after`() =
        runTest {
            val translator = FakeCaptionTranslator { text -> "$text-번역" }
            every { captionTranslatorFactory.create("en", "ko") } returns translator

            repository.translatedCues(episodeId = 1L, sourceLanguageTag = "en", cues = flowOf("hello")).test {
                assertEquals(
                    liveCaptionTestData.copy(episodeId = 1L, lineId = 0L, text = "hello", translation = null),
                    awaitItem(),
                )
                assertEquals(
                    liveCaptionTestData.copy(episodeId = 1L, lineId = 0L, text = "hello", translation = "hello-번역"),
                    awaitItem(),
                )
                awaitComplete()
            }
            assertTrue("종료 시 번역기를 닫아야 한다", translator.closed)
        }

    @Test
    fun `Given the cue changes before translation finishes, When translatedCues, Then the stale translation is cancelled`() =
        runTest {
            val firstTranslationStarted = CompletableDeferred<Unit>()
            val translator = FakeCaptionTranslator(
                onTranslate = { text ->
                    if (text == "first") {
                        firstTranslationStarted.complete(Unit)
                        // "first" 번역은 영원히 끝나지 않는다 — cue 가 바뀌면 collectLatest 가
                        // 이 suspend 호출 자체를 취소해야 한다.
                        kotlinx.coroutines.awaitCancellation()
                    }
                    "$text-번역"
                },
            )
            every { captionTranslatorFactory.create("en", "ko") } returns translator
            val cues = MutableSharedFlow<String>(extraBufferCapacity = 1)

            repository.translatedCues(episodeId = 1L, sourceLanguageTag = "en", cues = cues).test {
                cues.emit("first")
                assertEquals(
                    liveCaptionTestData.copy(episodeId = 1L, lineId = 0L, text = "first", translation = null),
                    awaitItem(),
                )
                firstTranslationStarted.await()

                cues.emit("second")
                assertEquals(
                    liveCaptionTestData.copy(episodeId = 1L, lineId = 1L, text = "second", translation = null),
                    awaitItem(),
                )
                assertEquals(
                    liveCaptionTestData.copy(episodeId = 1L, lineId = 1L, text = "second", translation = "second-번역"),
                    awaitItem(),
                )

                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(listOf("first", "second"), translator.requestedTexts)
        }
}

/** 가짜 번역기 — 실제 ML Kit 없이 순수 로직만 검증한다. */
private class FakeCaptionTranslator(
    private val onTranslate: suspend (String) -> String?,
) : CaptionTranslator {
    val requestedTexts = mutableListOf<String>()
    var closed = false

    override suspend fun prepare(): Boolean = true

    override suspend fun translate(text: String): String? {
        requestedTexts += text
        return onTranslate(text)
    }

    override fun close() {
        closed = true
    }
}
