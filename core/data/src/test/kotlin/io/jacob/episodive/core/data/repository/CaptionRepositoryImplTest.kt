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
import io.jacob.episodive.core.testing.model.captionLineTestData
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
import kotlinx.coroutines.flow.emptyFlow
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
            liveCaptionTestData.copy(episodeId = 1L, lines = listOf(captionLineTestData.copy(id = 0L, text = "hello", isFinal = false))),
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

            repository.translatedCues(episodeId = 1L, sourceLanguageTag = "en", cues = cues, seeks = emptyFlow()).test {
                assertEquals(
                    liveCaptionTestData.copy(
                        episodeId = 1L,
                        lines = listOf(captionLineTestData.copy(id = 0L, text = "hello", isFinal = true)),
                        translations = emptyList(),
                        isTranslating = false,
                    ),
                    awaitItem(),
                )
                // 늦게 온 cue 도 앞줄을 밀어내지 않고 롤링 창에 함께 쌓인다.
                assertEquals(
                    liveCaptionTestData.copy(
                        episodeId = 1L,
                        lines = listOf(
                            captionLineTestData.copy(id = 0L, text = "hello", isFinal = true),
                            captionLineTestData.copy(id = 1L, text = "world", isFinal = true),
                        ),
                        translations = emptyList(),
                        isTranslating = false,
                    ),
                    awaitItem(),
                )
                awaitComplete()
            }
        }

    @Test
    fun `Given an empty cue, When translatedCues, Then it is ignored and the kept lines are not cleared`() = runTest {
        every { captionTranslatorFactory.create("en", "ko") } returns null
        val cues = flowOf("hello", "", "world")

        repository.translatedCues(episodeId = 1L, sourceLanguageTag = "en", cues = cues, seeks = emptyFlow()).test {
            assertEquals(listOf(captionLineTestData.copy(id = 0L, text = "hello", isFinal = true)), awaitItem()?.lines)
            // 빈 cue 는 새 방출을 내지 않고 건너뛴다 — 그 다음 cue 가 앞줄 뒤에 그대로 이어붙는다.
            assertEquals(
                listOf(captionLineTestData.copy(id = 0L, text = "hello", isFinal = true), captionLineTestData.copy(id = 1L, text = "world", isFinal = true)),
                awaitItem()?.lines,
            )
            awaitComplete()
        }
    }

    @Test
    fun `Given a translator, When translatedCues, Then it emits the original first and the translation after`() =
        runTest {
            val translator = FakeCaptionTranslator { text -> "$text-번역" }
            every { captionTranslatorFactory.create("en", "ko") } returns translator

            repository.translatedCues(episodeId = 1L, sourceLanguageTag = "en", cues = flowOf("hello"), seeks = emptyFlow()).test {
                val original = awaitItem()
                assertEquals(listOf(captionLineTestData.copy(id = 0L, text = "hello", isFinal = true)), original?.lines)
                assertTrue(original?.translations.orEmpty().isEmpty())

                val translated = awaitItem()
                assertEquals(listOf(captionLineTestData.copy(id = 0L, text = "hello-번역", isFinal = true)), translated?.translations)

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

            repository.translatedCues(episodeId = 1L, sourceLanguageTag = "en", cues = cues, seeks = emptyFlow()).test {
                cues.emit("first")
                val firstOriginal = awaitItem()
                assertEquals(listOf(captionLineTestData.copy(id = 0L, text = "first", isFinal = true)), firstOriginal?.lines)
                firstTranslationStarted.await()

                cues.emit("second")
                val secondOriginal = awaitItem()
                assertEquals(
                    listOf(captionLineTestData.copy(id = 0L, text = "first", isFinal = true), captionLineTestData.copy(id = 1L, text = "second", isFinal = true)),
                    secondOriginal?.lines,
                )
                assertTrue("second 의 번역이 오기 전이니 아직 없다", secondOriginal?.translations.orEmpty().isEmpty())

                val secondTranslated = awaitItem()
                assertEquals(listOf(captionLineTestData.copy(id = 1L, text = "second-번역", isFinal = true)), secondTranslated?.translations)

                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(listOf("first", "second"), translator.requestedTexts)
        }

    // 늦은 번역이 기존 번역을 덮지 못하는 것은 CaptionPresenter 계약 테스트에서 직접 검증한다
    // (translatedCues 가 그 규칙을 그대로 위임하므로 여기서 다시 재현하지 않는다).

    // --- translatedCues: seeks ---

    @Test
    fun `Given a seek, When translatedCues, Then it clears the kept lines`() = runTest {
        every { captionTranslatorFactory.create("en", "ko") } returns null
        val cues = MutableSharedFlow<String>(extraBufferCapacity = 1)
        val seeks = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        repository.translatedCues(episodeId = 1L, sourceLanguageTag = "en", cues = cues, seeks = seeks).test {
            cues.emit("hello")
            assertEquals(listOf(captionLineTestData.copy(id = 0L, text = "hello", isFinal = true)), awaitItem()?.lines)

            seeks.emit(Unit)
            assertNull("시크는 쌓인 줄을 통째로 비워야 한다", awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given a seek, When a new cue arrives, Then line ids keep increasing`() = runTest {
        every { captionTranslatorFactory.create("en", "ko") } returns null
        val cues = MutableSharedFlow<String>(extraBufferCapacity = 1)
        val seeks = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        repository.translatedCues(episodeId = 1L, sourceLanguageTag = "en", cues = cues, seeks = seeks).test {
            cues.emit("hello")
            awaitItem()

            seeks.emit(Unit)
            awaitItem()

            cues.emit("world")
            assertEquals(
                "시크를 지나도 줄 번호는 계속 늘어야 번역 하한이 지난 구간을 정확히 가른다",
                listOf(captionLineTestData.copy(id = 1L, text = "world", isFinal = true)),
                awaitItem()?.lines,
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given a translation requested before a seek, When it settles after, Then it is dropped`() = runTest {
        val firstTranslationStarted = CompletableDeferred<Unit>()
        val releaseTranslation = CompletableDeferred<Unit>()
        val translator = FakeCaptionTranslator(
            onTranslate = { text ->
                if (text == "hello") {
                    firstTranslationStarted.complete(Unit)
                    releaseTranslation.await()
                }
                "$text-번역"
            },
        )
        every { captionTranslatorFactory.create("en", "ko") } returns translator
        val cues = MutableSharedFlow<String>(extraBufferCapacity = 1)
        val seeks = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        repository.translatedCues(episodeId = 1L, sourceLanguageTag = "en", cues = cues, seeks = seeks).test {
            cues.emit("hello")
            awaitItem()
            firstTranslationStarted.await()

            // 번역이 끝나기 전에 시크가 온다 — 번역 코루틴 자체는 취소되지 않지만 presenter.clear() 가
            // 남긴 번역 하한이 뒤늦게 도착할 이 번역을 걸러낸다.
            seeks.emit(Unit)
            assertNull(awaitItem())

            // 뒤늦게 풀리면 번역 코루틴은 재개돼 onTranslation 까지 부르지만, 하한에 걸려 새 방출은 없다.
            releaseTranslation.complete(Unit)
            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given a translation is in flight, When an empty cue arrives, Then the translation is not cancelled`() =
        runTest {
            val translationStarted = CompletableDeferred<Unit>()
            val releaseTranslation = CompletableDeferred<Unit>()
            val translator = FakeCaptionTranslator(
                onTranslate = { text ->
                    translationStarted.complete(Unit)
                    releaseTranslation.await()
                    "$text-번역"
                },
            )
            every { captionTranslatorFactory.create("en", "ko") } returns translator
            val cues = MutableSharedFlow<String>(extraBufferCapacity = 1)

            repository.translatedCues(episodeId = 1L, sourceLanguageTag = "en", cues = cues, seeks = emptyFlow()).test {
                cues.emit("hello")
                awaitItem() // 원문 "hello"
                translationStarted.await()

                // 번역이 아직 끝나지 않은 채로 빈 cue(cue 사이 공백)가 온다 — 빈 cue 는 건너뛰므로
                // 새 방출도 없어야 하고, 진행 중이던 번역도 취소되면 안 된다.
                cues.emit("")
                expectNoEvents()

                releaseTranslation.complete(Unit)
                val translated = awaitItem()
                assertEquals(
                    "빈 cue 가 진행 중이던 번역을 취소하면 안 된다",
                    listOf(captionLineTestData.copy(id = 0L, text = "hello-번역", isFinal = true)),
                    translated?.translations,
                )

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given a translation is in flight, When the next cue arrives, Then the previous translation still completes`() =
        runTest {
            val helloStarted = CompletableDeferred<Unit>()
            val releaseHello = CompletableDeferred<Unit>()
            val worldStarted = CompletableDeferred<Unit>()
            val releaseWorld = CompletableDeferred<Unit>()
            val translator = FakeCaptionTranslator(
                onTranslate = { text ->
                    when (text) {
                        "hello" -> {
                            helloStarted.complete(Unit)
                            releaseHello.await()
                        }
                        "world" -> {
                            worldStarted.complete(Unit)
                            releaseWorld.await()
                        }
                    }
                    "$text-번역"
                },
            )
            every { captionTranslatorFactory.create("en", "ko") } returns translator
            val cues = MutableSharedFlow<String>(extraBufferCapacity = 1)

            repository.translatedCues(episodeId = 1L, sourceLanguageTag = "en", cues = cues, seeks = emptyFlow()).test {
                cues.emit("hello")
                awaitItem() // 원문 "hello"
                helloStarted.await()

                // hello 의 번역이 아직 끝나지 않은 채로 다음 cue 가 온다.
                cues.emit("world")
                val worldOriginal = awaitItem()
                assertEquals(
                    listOf(
                        captionLineTestData.copy(id = 0L, text = "hello", isFinal = true),
                        captionLineTestData.copy(id = 1L, text = "world", isFinal = true),
                    ),
                    worldOriginal?.lines,
                )
                // world 의 번역이 hello 와 별개로(취소되지 않고) 진행되고 있음을 확인한다.
                worldStarted.await()

                // hello 를 먼저 풀어 그 번역이 world 보다 먼저 반영되게 한다 — 그래야 나중에 world
                // 번역이 추가될 때 presenter 의 "늦게 온 번역은 버린다" 하한에 걸리지 않는다.
                releaseHello.complete(Unit)
                val afterHelloTranslated = awaitItem()
                assertEquals(
                    "다음 cue(world) 가 왔다고 hello 의 번역이 취소되면 안 된다",
                    listOf(captionLineTestData.copy(id = 0L, text = "hello-번역", isFinal = true)),
                    afterHelloTranslated?.translations,
                )

                releaseWorld.complete(Unit)
                val afterWorldTranslated = awaitItem()
                assertEquals(
                    listOf(
                        captionLineTestData.copy(id = 0L, text = "hello-번역", isFinal = true),
                        captionLineTestData.copy(id = 1L, text = "world-번역", isFinal = true),
                    ),
                    afterWorldTranslated?.translations,
                )

                cancelAndIgnoreRemainingEvents()
            }
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
