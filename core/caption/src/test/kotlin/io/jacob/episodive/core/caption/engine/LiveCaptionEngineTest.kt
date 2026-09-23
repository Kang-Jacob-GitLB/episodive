package io.jacob.episodive.core.caption.engine

import io.jacob.episodive.core.caption.asset.InstalledCaptionModel
import io.jacob.episodive.core.model.caption.CaptionLanguage
import io.jacob.episodive.core.model.caption.CaptionSession
import io.jacob.episodive.core.player.audio.PcmChunk
import io.jacob.episodive.core.player.audio.SpeechPcmSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * `poll()` 이 null 이면 20ms 쉬고 다시 보는 루프라, 청크를 넣은 뒤 [TestScope.pump] 로 가상
 * 시계를 한 스텝씩 밀어 그 사이 실행돼야 할 코루틴을 [PlaybackSpectrumMonitorGateTest]
 * (`:core:player`) 와 같은 방식으로 직접 진행시킨다. `advanceUntilIdle` 을 쓰지 않는 이유도
 * 같다 — 루프 자체가 끝나지 않아 그 한 번의 호출이 영영 끝나지 않는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LiveCaptionEngineTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val episodeId = 5778530L

    // RecognizerCache 가 `.loading` 마커를 encoderPath 의 상위 디렉터리에 실제로 쓴다 — 존재하지
    // 않는 경로("/fake/...")를 쓰면 그 쓰기 자체가 (권한 없음으로) 실패해 acquire() 가 조용히
    // Unloadable 을 돌려준다. 실제 쓰기가 가능한 임시 디렉터리를 써야 한다.
    private val model by lazy {
        val dir = tempFolder.newFolder("en")
        InstalledCaptionModel(
            language = CaptionLanguage.ENGLISH,
            encoderPath = File(dir, "encoder.onnx").path,
            decoderPath = File(dir, "decoder.onnx").path,
            joinerPath = File(dir, "joiner.onnx").path,
            tokensPath = File(dir, "tokens.txt").path,
            maxLineChars = 200,
        )
    }

    private fun TestScope.pump(steps: Int = 1, stepMillis: Long = 20) {
        // 지금 시각에 이미 올라와 있는(지연 없이 dispatch 된) 코루틴부터 먼저 비운다 — 시간을
        // 앞으로 밀기 전에도 `runCurrent()` 한 번은 필요하다(예: 이번에 막 launch 한 수집 코루틴).
        testScheduler.runCurrent()
        repeat(steps) {
            testScheduler.advanceTimeBy(stepMillis)
            testScheduler.runCurrent()
        }
    }

    private fun chunk(segment: Int, rateHz: Int = 16_000, size: Int = 10) =
        PcmChunk(segment = segment, sampleRateHz = rateHz, samples = FloatArray(size))

    @Test
    fun `a segment change closes the previous stream and opens a new one, one rate per stream`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val pcm = FakeSpeechPcmSource()
        val factory = FakeEngineRecognizerFactory()
        val recognizers = RecognizerCache(factory, dispatcher)
        val engine = LiveCaptionEngine(
            pcm, recognizers, FakeCaptionTranslatorFactory(null), DeviceLanguageProvider { "en" }, dispatcher,
        ) { testScheduler.currentTime }

        val results = mutableListOf<CaptionSession>()
        backgroundScope.launch(dispatcher) { engine.session(episodeId, model).collect { results.add(it) } }
        pump()

        pcm.enqueue(chunk(segment = 1, rateHz = 16_000))
        pump()
        pcm.enqueue(chunk(segment = 1, rateHz = 16_000))
        pump()

        val recognizer = factory.created.single()
        assertEquals("같은 구간이면 스트림을 다시 만들면 안 된다", 1, recognizer.createdStreams.size)
        assertFalse(recognizer.createdStreams[0].closed)

        pcm.enqueue(chunk(segment = 2, rateHz = 48_000))
        pump()

        assertEquals("구간이 바뀌면 새 스트림을 만든다", 2, recognizer.createdStreams.size)
        assertTrue("이전 스트림은 닫아야 한다", recognizer.createdStreams[0].closed)
        // 두 구간의 레이트가 서로 다르다 — 새 스트림이 "이전 구간 레이트를 그대로 재사용" 하는
        // 것이 아니라 그 구간 청크가 실제로 잰 레이트를 그대로 받는지 검증한다.
        assertEquals(16_000, recognizer.createdStreams[0].sampleRateHz)
        assertEquals(48_000, recognizer.createdStreams[1].sampleRateHz)
    }

    @Test
    fun `endpoint finalizes the line and attaches a translation when languages differ`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val pcm = FakeSpeechPcmSource()
        val factory = FakeEngineRecognizerFactory()
        val recognizers = RecognizerCache(factory, dispatcher)
        val translator = FakeCaptionTranslator(result = "안녕.")
        val engine = LiveCaptionEngine(
            pcm, recognizers, FakeCaptionTranslatorFactory(translator), DeviceLanguageProvider { "ko" }, dispatcher,
        ) { testScheduler.currentTime }

        val results = mutableListOf<CaptionSession>()
        backgroundScope.launch(dispatcher) { engine.session(episodeId, model).collect { results.add(it) } }
        pump()

        pcm.enqueue(chunk(segment = 1))
        pump()
        val stream = factory.created.single().createdStreams.single()
        stream.tokens = listOf(" hello")
        stream.endpoint = true
        pcm.enqueue(chunk(segment = 1))
        pump(steps = 3)

        assertTrue(translator.prepareCalled)
        assertEquals(listOf("Hello"), translator.translatedTexts)
        val running = results.filterIsInstance<CaptionSession.Running>().mapNotNull { it.caption }
        val translated = running.lastOrNull { it.translation != null }
        assertEquals("Hello", translated?.text)
        assertEquals("안녕.", translated?.translation)
        assertTrue("endpoint 는 확정 줄이어야 한다", translated?.isFinal == true)
    }

    @Test
    fun `same source and device language means no translator is created`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val pcm = FakeSpeechPcmSource()
        val factory = FakeEngineRecognizerFactory()
        val recognizers = RecognizerCache(factory, dispatcher)
        val translatorFactory = FakeCaptionTranslatorFactory(FakeCaptionTranslator("이 값은 나오면 안 된다"))
        val engine = LiveCaptionEngine(
            pcm, recognizers, translatorFactory, DeviceLanguageProvider { "en" }, dispatcher,
        ) { testScheduler.currentTime }

        val results = mutableListOf<CaptionSession>()
        backgroundScope.launch(dispatcher) { engine.session(episodeId, model).collect { results.add(it) } }
        pump()

        pcm.enqueue(chunk(segment = 1))
        pump()
        val stream = factory.created.single().createdStreams.single()
        stream.tokens = listOf(" hello")
        stream.endpoint = true
        pcm.enqueue(chunk(segment = 1))
        pump(steps = 3)

        assertEquals(
            "원문 언어(en)와 기기 언어(en)가 같으면 CaptionTranslatorFactory.create 가 null 을 돌려줘야 한다",
            listOf("en" to "en"),
            translatorFactory.requestedTags,
        )
        val running = results.filterIsInstance<CaptionSession.Running>().mapNotNull { it.caption }
        assertTrue(running.isNotEmpty())
        assertNull(running.last().translation)
    }

    @Test
    fun `cancelling the collection stops capture and returns the recognizer to the cache`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val pcm = FakeSpeechPcmSource()
        val factory = FakeEngineRecognizerFactory()
        val recognizers = RecognizerCache(factory, dispatcher, keepAliveMillis = 1_000)
        val engine = LiveCaptionEngine(
            pcm, recognizers, FakeCaptionTranslatorFactory(null), DeviceLanguageProvider { "en" }, dispatcher,
        ) { testScheduler.currentTime }

        pcm.enqueue(chunk(segment = 1))
        val job = backgroundScope.launch(dispatcher) { engine.session(episodeId, model).collect { } }
        pump()
        assertTrue(pcm.startCaptureCalled)

        job.cancel()
        pump()

        assertTrue("수집이 끊기면 캡처를 멈춰야 한다", pcm.stopCaptureCalled)
        val recognizer = factory.created.single()
        assertFalse("release 직후에는 keep-alive 중이라 아직 닫히면 안 된다", recognizer.closed)

        pump(steps = 60) // keep-alive(1s) 를 넘긴다
        assertTrue("keep-alive 가 지나면 캐시가 인식기를 닫아야 한다", recognizer.closed)
    }

    @Test
    fun `translator factory 가 예외를 던져도 캡처를 멈추고 인식기를 반납한다`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val pcm = FakeSpeechPcmSource()
        val factory = FakeEngineRecognizerFactory()
        val recognizers = RecognizerCache(factory, dispatcher, keepAliveMillis = 1_000)
        val throwingTranslatorFactory = object : CaptionTranslatorFactory {
            override fun create(sourceTag: String, targetTag: String): CaptionTranslator? =
                error("번역기 생성 실패(가짜)")
        }
        val engine = LiveCaptionEngine(
            pcm, recognizers, throwingTranslatorFactory, DeviceLanguageProvider { "ko" }, dispatcher,
        ) { testScheduler.currentTime }

        backgroundScope.launch(dispatcher) {
            // translators.create() 가 던지는 예외는 세션 안에서 잡지 않는다 — 여기서 삼켜
            // collect 가 그 예외로 죽지 않게만 한다. 검증 대상은 그 예외가 나는 동안에도
            // startCapture()~stopCapture() 사이의 정리가 끝까지 갔는가이다.
            runCatching { engine.session(episodeId, model).collect { } }
        }
        pump()

        assertTrue("startCapture 이후 예외가 나도 stopCapture 까지 가야 한다", pcm.stopCaptureCalled)
        val recognizer = factory.created.single()
        assertFalse("release 직후에는 keep-alive 중이라 아직 닫히면 안 된다", recognizer.closed)

        pump(steps = 60) // keep-alive(1s) 를 넘긴다
        assertTrue("인식기를 반납했다면 keep-alive 이후 캐시가 닫아야 한다", recognizer.closed)
    }

    @Test
    fun `인식기 로드가 일시 실패하면 Unloadable 을 보내지 않고 세션만 조용히 끝낸다`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val pcm = FakeSpeechPcmSource()
        // 매번 코틀린 예외로 실패하는 factory — 첫 호출은 연속 2회가 아니므로 RecognizerCache 가
        // Unloadable 이 아니라 Failed 를 돌려준다(RecognizerCacheTest 참고).
        val factory = object : SpeechRecognizerFactory {
            override fun create(model: InstalledCaptionModel): SpeechRecognizer = error("로드 실패(가짜, 일시적)")
        }
        val recognizers = RecognizerCache(factory, dispatcher)
        val engine = LiveCaptionEngine(
            pcm, recognizers, FakeCaptionTranslatorFactory(null), DeviceLanguageProvider { "en" }, dispatcher,
        ) { testScheduler.currentTime }

        val results = mutableListOf<CaptionSession>()
        engine.session(episodeId, model).collect { results.add(it) }

        assertEquals(listOf(CaptionSession.Loading), results)
        assertTrue(
            "모델을 지우면 안 되는 일시 실패이니 Unloadable 을 보내면 안 된다",
            results.none { it is CaptionSession.Unloadable },
        )
        assertTrue(pcm.startCaptureCalled)
        assertTrue("일시 실패로 끝나도 캡처는 멈춰야 한다", pcm.stopCaptureCalled)
    }
}

private class FakeSpeechPcmSource : SpeechPcmSource {
    var startCaptureCalled = false
        private set
    var stopCaptureCalled = false
        private set
    private val queue = ArrayDeque<PcmChunk>()

    fun enqueue(chunk: PcmChunk) {
        queue.addLast(chunk)
    }

    override fun startCapture() {
        startCaptureCalled = true
    }

    override fun stopCapture() {
        stopCaptureCalled = true
    }

    override fun poll(): PcmChunk? = if (queue.isEmpty()) null else queue.removeFirst()
}

private class FakeRecognitionStream(override val sampleRateHz: Int) : RecognitionStream {
    var closed = false
        private set
    var resetCount = 0
        private set

    /** 테스트가 `decodeAvailable()` 을 거치지 않고 바로 다음 `tokens()` 결과를 정해 둔다. */
    var tokens: List<String> = emptyList()
    var endpoint = false

    override fun accept(samples: FloatArray, sampleRateHz: Int) {
        require(sampleRateHz == this.sampleRateHz) { "레이트 불일치: $sampleRateHz != ${this.sampleRateHz}" }
    }

    override fun decodeAvailable() = Unit

    override fun isEndpoint(): Boolean = endpoint

    override fun tokens(): List<String> = tokens

    override fun resetUtterance() {
        resetCount++
        tokens = emptyList()
        endpoint = false
    }

    override fun close() {
        closed = true
    }
}

private class FakeEngineRecognizer : SpeechRecognizer {
    val createdStreams = mutableListOf<FakeRecognitionStream>()
    var closed = false
        private set

    override fun createStream(sampleRateHz: Int): RecognitionStream =
        FakeRecognitionStream(sampleRateHz).also { createdStreams.add(it) }

    override fun close() {
        closed = true
    }
}

private class FakeEngineRecognizerFactory : SpeechRecognizerFactory {
    val created = mutableListOf<FakeEngineRecognizer>()

    override fun create(model: InstalledCaptionModel): SpeechRecognizer =
        FakeEngineRecognizer().also { created.add(it) }
}

private class FakeCaptionTranslator(private val result: String?) : CaptionTranslator {
    var prepareCalled = false
        private set
    var closed = false
        private set
    val translatedTexts = mutableListOf<String>()

    override suspend fun prepare(): Boolean {
        prepareCalled = true
        return true
    }

    override suspend fun translate(text: String): String? {
        translatedTexts.add(text)
        return result
    }

    override fun close() {
        closed = true
    }
}

/** `sourceTag == targetTag` 면 실제 `CaptionTranslatorFactory` 계약대로 null 을 돌려준다. */
private class FakeCaptionTranslatorFactory(private val translator: CaptionTranslator?) : CaptionTranslatorFactory {
    val requestedTags = mutableListOf<Pair<String, String>>()

    override fun create(sourceTag: String, targetTag: String): CaptionTranslator? {
        requestedTags.add(sourceTag to targetTag)
        return if (sourceTag == targetTag) null else translator
    }
}
